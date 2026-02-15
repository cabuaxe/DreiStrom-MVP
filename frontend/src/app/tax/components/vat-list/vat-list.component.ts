import { Component, inject, signal, OnInit } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatMenuModule } from '@angular/material/menu';
import { VatService } from '../../services/vat.service';
import { VatReturn, VatReturnStatus } from '../../models/vat-return.model';

@Component({
  selector: 'app-vat-list',
  imports: [
    CurrencyPipe,
    MatCardModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatSelectModule,
    MatFormFieldModule,
    MatMenuModule,
  ],
  templateUrl: './vat-list.component.html',
  styleUrl: './vat-list.component.scss',
})
export class VatListComponent implements OnInit {
  private readonly vatService = inject(VatService);

  readonly returns = signal<VatReturn[]>([]);
  readonly loading = signal(true);
  readonly selectedYear = signal(new Date().getFullYear());

  readonly displayedColumns = ['period', 'outputVat', 'inputVat', 'netPayable', 'status', 'actions'];

  ngOnInit(): void {
    this.loadReturns();
  }

  onYearChange(year: number): void {
    this.selectedYear.set(year);
    this.loadReturns();
  }

  formatPeriod(vr: VatReturn): string {
    switch (vr.periodType) {
      case 'MONTHLY':
        return `${String(vr.periodNumber).padStart(2, '0')}/${vr.year}`;
      case 'QUARTERLY':
        return `Q${vr.periodNumber}/${vr.year}`;
      case 'ANNUAL':
        return `${vr.year}`;
    }
  }

  statusLabel(status: VatReturnStatus): string {
    switch (status) {
      case 'DRAFT': return 'Entwurf';
      case 'SUBMITTED': return 'Abgegeben';
      case 'ACCEPTED': return 'Akzeptiert';
      case 'CORRECTED': return 'Korrigiert';
    }
  }

  statusColor(status: VatReturnStatus): string {
    switch (status) {
      case 'DRAFT': return '';
      case 'SUBMITTED': return 'primary';
      case 'ACCEPTED': return 'accent';
      case 'CORRECTED': return 'warn';
    }
  }

  onSubmit(vr: VatReturn): void {
    this.vatService.submitReturn(vr.id).subscribe({
      next: () => this.loadReturns(),
    });
  }

  onExportXml(vr: VatReturn): void {
    window.open(this.vatService.getElsterXmlUrl(vr.id), '_blank');
  }

  onExportCsv(vr: VatReturn): void {
    window.open(this.vatService.getElsterCsvUrl(vr.id), '_blank');
  }

  get yearOptions(): number[] {
    const current = new Date().getFullYear();
    return [current - 1, current, current + 1];
  }

  private loadReturns(): void {
    this.loading.set(true);
    this.vatService.listReturns(this.selectedYear()).subscribe({
      next: (data) => {
        this.returns.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
