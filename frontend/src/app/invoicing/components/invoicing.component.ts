import { Component, inject, signal, computed, OnInit } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  FormArray,
  Validators,
  ReactiveFormsModule,
} from '@angular/forms';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { MatTabsModule } from '@angular/material/tabs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { MatBadgeModule } from '@angular/material/badge';
import { InvoicingService } from '../services/invoicing.service';
import { ClientsApiService } from '../../api/generated/api/clients.service';
import { ClientResponse } from '../../api/generated/model/client-response.model';
import {
  InvoiceResponse,
  InvoiceStream,
  InvoiceStatus,
  VatTreatment,
} from '../models/invoice.model';

/** EU countries (non-DE) for reverse charge auto-detection */
const EU_COUNTRIES = new Set([
  'AT', 'BE', 'BG', 'CY', 'CZ', 'DK', 'EE', 'ES', 'FI', 'FR', 'GR', 'HR',
  'HU', 'IE', 'IT', 'LT', 'LU', 'LV', 'MT', 'NL', 'PL', 'PT', 'RO', 'SE',
  'SI', 'SK',
]);

interface StreamTab {
  label: string;
  type: InvoiceStream;
  color: string;
  icon: string;
}

@Component({
  selector: 'app-invoicing',
  imports: [
    ReactiveFormsModule,
    CurrencyPipe,
    DatePipe,
    MatTabsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatTableModule,
    MatSnackBarModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatTooltipModule,
    MatMenuModule,
    MatBadgeModule,
  ],
  templateUrl: './invoicing.component.html',
  styleUrl: './invoicing.component.scss',
})
export class InvoicingComponent implements OnInit {
  private readonly invoicingService = inject(InvoicingService);
  private readonly clientsApi = inject(ClientsApiService);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);

  readonly tabs: StreamTab[] = [
    { label: 'Freiberuf', type: 'FREIBERUF', color: '#00529B', icon: 'edit_note' },
    { label: 'Gewerbe', type: 'GEWERBE', color: '#00783C', icon: 'store' },
  ];

  readonly invoices = signal<InvoiceResponse[]>([]);
  readonly clients = signal<ClientResponse[]>([]);
  readonly loading = signal(false);
  readonly showForm = signal(false);
  readonly selectedTabIndex = signal(0);

  readonly displayedColumns = [
    'number', 'client', 'invoiceDate', 'grossTotal', 'status', 'vatTreatment', 'actions',
  ];

  readonly selectedStreamType = computed<InvoiceStream>(
    () => this.tabs[this.selectedTabIndex()].type,
  );

  readonly filteredInvoices = computed(() =>
    this.invoices().filter(i => i.streamType === this.selectedStreamType()),
  );

  readonly streamTotal = computed(() =>
    this.filteredInvoices().reduce((sum, i) => sum + (i.grossTotal ?? 0), 0),
  );

  readonly streamClients = computed(() =>
    this.clients().filter(
      c => c.streamType === this.selectedStreamType() && c.active !== false,
    ),
  );

  /** Detected VatTreatment based on selected client */
  readonly detectedVatTreatment = signal<VatTreatment | null>(null);

  // ── Invoice form ──────────────────────────────────────────────────

  readonly invoiceForm: FormGroup = this.fb.group({
    clientId: [null, Validators.required],
    invoiceDate: [null, Validators.required],
    dueDate: [null],
    vatTreatment: [null],
    notes: ['', Validators.maxLength(2000)],
    lineItems: this.fb.array([]),
  });

  get lineItems(): FormArray {
    return this.invoiceForm.get('lineItems') as FormArray;
  }

  ngOnInit(): void {
    this.loadInvoices();
    this.loadClients();
  }

  // ── Data loading ──────────────────────────────────────────────────

  loadInvoices(): void {
    this.loading.set(true);
    this.invoicingService.list().subscribe({
      next: (data) => {
        this.invoices.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Fehler beim Laden der Rechnungen', 'OK', { duration: 3000 });
        this.loading.set(false);
      },
    });
  }

  loadClients(): void {
    this.clientsApi.list1().subscribe({
      next: (data) => this.clients.set(data.clients ?? []),
      error: () => {
        this.snackBar.open('Fehler beim Laden der Kunden', 'OK', { duration: 3000 });
      },
    });
  }

  // ── Tab switching ─────────────────────────────────────────────────

  onTabChange(index: number): void {
    this.selectedTabIndex.set(index);
    this.showForm.set(false);
  }

  // ── Form management ───────────────────────────────────────────────

  toggleForm(): void {
    this.showForm.update(v => !v);
    if (this.showForm()) {
      this.invoiceForm.reset();
      this.lineItems.clear();
      this.addLineItem();
      this.detectedVatTreatment.set(null);
    }
  }

  addLineItem(): void {
    this.lineItems.push(
      this.fb.group({
        description: ['', Validators.required],
        quantity: [1, [Validators.required, Validators.min(0.01)]],
        unitPrice: [null, [Validators.required, Validators.min(0)]],
        vatRate: [19, [Validators.required, Validators.min(0)]],
      }),
    );
  }

  removeLineItem(index: number): void {
    if (this.lineItems.length > 1) {
      this.lineItems.removeAt(index);
    }
  }

  // ── Auto VAT calculation ──────────────────────────────────────────

  get calculatedNetTotal(): number {
    return this.lineItems.controls.reduce((sum, ctrl) => {
      const q = ctrl.get('quantity')?.value ?? 0;
      const p = ctrl.get('unitPrice')?.value ?? 0;
      return sum + q * p;
    }, 0);
  }

  get calculatedVat(): number {
    const treatment = this.invoiceForm.get('vatTreatment')?.value
      ?? this.detectedVatTreatment();
    if (
      treatment === 'SMALL_BUSINESS' ||
      treatment === 'REVERSE_CHARGE' ||
      treatment === 'INTRA_EU' ||
      treatment === 'THIRD_COUNTRY'
    ) {
      return 0;
    }
    return this.lineItems.controls.reduce((sum, ctrl) => {
      const q = ctrl.get('quantity')?.value ?? 0;
      const p = ctrl.get('unitPrice')?.value ?? 0;
      const rate = ctrl.get('vatRate')?.value ?? 0;
      return sum + q * p * rate / 100;
    }, 0);
  }

  get calculatedGrossTotal(): number {
    return this.calculatedNetTotal + this.calculatedVat;
  }

  // ── Reverse charge auto-detection ─────────────────────────────────

  onClientChange(clientId: number): void {
    const client = this.clients().find(c => c.id === clientId);
    if (!client) {
      this.detectedVatTreatment.set(null);
      return;
    }

    const country = client.country ?? 'DE';
    const clientType = client.clientType;
    const hasUstIdNr = !!client.ustIdNr;

    if (country === 'DE') {
      this.detectedVatTreatment.set('REGULAR');
    } else if (EU_COUNTRIES.has(country)) {
      if (clientType === 'B2B' && hasUstIdNr) {
        this.detectedVatTreatment.set('REVERSE_CHARGE');
      } else {
        this.detectedVatTreatment.set('REGULAR');
      }
    } else {
      this.detectedVatTreatment.set('THIRD_COUNTRY');
    }

    // Auto-set VAT rate to 0 for VAT-free treatments
    const detected = this.detectedVatTreatment();
    if (detected && detected !== 'REGULAR') {
      this.lineItems.controls.forEach(ctrl => {
        ctrl.get('vatRate')?.setValue(0);
      });
    }
  }

  // ── Submit invoice ────────────────────────────────────────────────

  onSubmit(): void {
    if (this.invoiceForm.invalid || this.lineItems.length === 0) return;

    const val = this.invoiceForm.value;
    const invoiceDate = this.formatDate(val.invoiceDate);
    const dueDate = val.dueDate ? this.formatDate(val.dueDate) : undefined;

    const vatTreatment =
      val.vatTreatment || this.detectedVatTreatment() || undefined;

    // Auto-append §19 notice for Kleinunternehmer
    let notes = val.notes || undefined;
    if (vatTreatment === 'SMALL_BUSINESS' && (!notes || !notes.includes('§19'))) {
      const notice = 'Gemäß §19 UStG wird keine Umsatzsteuer berechnet.';
      notes = notes ? notes + '\n' + notice : notice;
    }

    this.invoicingService.create({
      streamType: this.selectedStreamType(),
      clientId: val.clientId,
      invoiceDate,
      dueDate,
      lineItems: val.lineItems,
      netTotal: Math.round(this.calculatedNetTotal * 100) / 100,
      vat: Math.round(this.calculatedVat * 100) / 100,
      grossTotal: Math.round(this.calculatedGrossTotal * 100) / 100,
      vatTreatment,
      notes,
    }).subscribe({
      next: () => {
        this.snackBar.open('Rechnung erfolgreich erstellt', 'OK', { duration: 2000 });
        this.showForm.set(false);
        this.loadInvoices();
      },
      error: (err) => {
        const msg = err.error?.message || 'Fehler beim Erstellen der Rechnung';
        this.snackBar.open(msg, 'OK', { duration: 5000 });
      },
    });
  }

  // ── Invoice actions ───────────────────────────────────────────────

  updateStatus(invoice: InvoiceResponse, status: InvoiceStatus): void {
    this.invoicingService.updateStatus(invoice.id, { status }).subscribe({
      next: () => {
        this.snackBar.open('Status aktualisiert', 'OK', { duration: 2000 });
        this.loadInvoices();
      },
      error: (err) => {
        const msg = err.error?.message || 'Ungültiger Statusübergang';
        this.snackBar.open(msg, 'OK', { duration: 3000 });
      },
    });
  }

  deleteInvoice(id: number): void {
    this.invoicingService.delete(id).subscribe({
      next: () => {
        this.snackBar.open('Rechnung gelöscht', 'OK', { duration: 2000 });
        this.loadInvoices();
      },
      error: (err) => {
        const msg = err.error?.message || 'Fehler beim Löschen';
        this.snackBar.open(msg, 'OK', { duration: 3000 });
      },
    });
  }

  downloadPdf(invoice: InvoiceResponse): void {
    this.invoicingService.downloadPdf(invoice.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${invoice.number}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => {
        this.snackBar.open('Fehler beim PDF-Download', 'OK', { duration: 3000 });
      },
    });
  }

  // ── Status helpers ────────────────────────────────────────────────

  getStatusLabel(status: InvoiceStatus): string {
    const labels: Record<InvoiceStatus, string> = {
      DRAFT: 'Entwurf',
      SENT: 'Versendet',
      PAID: 'Bezahlt',
      OVERDUE: 'Überfällig',
      CANCELLED: 'Storniert',
    };
    return labels[status] ?? status;
  }

  getStatusColor(status: InvoiceStatus): string {
    const colors: Record<InvoiceStatus, string> = {
      DRAFT: '#757575',
      SENT: '#1976D2',
      PAID: '#2E7D32',
      OVERDUE: '#D32F2F',
      CANCELLED: '#9E9E9E',
    };
    return colors[status] ?? '#757575';
  }

  getVatTreatmentLabel(treatment: VatTreatment): string {
    const labels: Record<VatTreatment, string> = {
      REGULAR: 'Regelbesteuerung',
      REVERSE_CHARGE: 'Reverse Charge',
      SMALL_BUSINESS: '§19 Kleinunternehmer',
      INTRA_EU: 'Innergemeinschaftlich',
      THIRD_COUNTRY: 'Drittland',
    };
    return labels[treatment] ?? treatment;
  }

  getAvailableTransitions(status: InvoiceStatus): InvoiceStatus[] {
    const transitions: Record<InvoiceStatus, InvoiceStatus[]> = {
      DRAFT: ['SENT', 'CANCELLED'],
      SENT: ['PAID', 'OVERDUE', 'CANCELLED'],
      OVERDUE: ['PAID', 'CANCELLED'],
      PAID: [],
      CANCELLED: [],
    };
    return transitions[status] ?? [];
  }

  // ── Utility ───────────────────────────────────────────────────────

  private formatDate(date: Date): string {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }
}
