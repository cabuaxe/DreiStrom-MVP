import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
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
import { IncomeService, StreamType } from '../services/income.service';
import { IncomeEntryResponse } from '../../api/generated/model/income-entry-response.model';
import { CreateIncomeEntryRequestStreamTypeEnum } from '../../api/generated/model/create-income-entry-request.model';

interface StreamTab {
  label: string;
  type: StreamType;
  color: string;
  icon: string;
}

@Component({
  selector: 'app-income',
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
  ],
  templateUrl: './income.component.html',
  styleUrl: './income.component.scss',
})
export class IncomeComponent implements OnInit {
  private readonly incomeService = inject(IncomeService);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);

  readonly tabs: StreamTab[] = [
    { label: 'Anstellung', type: 'EMPLOYMENT', color: '#3884F4', icon: 'work' },
    { label: 'Freiberuf', type: 'FREIBERUF', color: '#2EA043', icon: 'edit_note' },
    { label: 'Gewerbe', type: 'GEWERBE', color: '#CC092F', icon: 'store' },
  ];

  readonly entries = signal<IncomeEntryResponse[]>([]);
  readonly loading = signal(false);
  readonly showForm = signal(false);
  readonly selectedTabIndex = signal(0);

  readonly displayedColumns = ['entryDate', 'source', 'amount', 'description', 'actions'];

  readonly selectedStreamType = computed<StreamType>(() => this.tabs[this.selectedTabIndex()].type);

  readonly filteredEntries = computed(() =>
    this.entries().filter(e => e.streamType === this.selectedStreamType())
  );

  readonly streamTotal = computed(() =>
    this.filteredEntries().reduce((sum, e) => sum + (e.amount ?? 0), 0)
  );

  readonly entryForm: FormGroup = this.fb.group({
    amount: [null, [Validators.required, Validators.min(0.01)]],
    entryDate: [null, Validators.required],
    source: [''],
    description: [''],
  });

  ngOnInit(): void {
    this.loadEntries();
  }

  onTabChange(index: number): void {
    this.selectedTabIndex.set(index);
    this.showForm.set(false);
  }

  loadEntries(): void {
    this.loading.set(true);
    this.incomeService.list().subscribe({
      next: (data) => {
        this.entries.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Fehler beim Laden der Einnahmen', 'OK', { duration: 3000 });
        this.loading.set(false);
      },
    });
  }

  toggleForm(): void {
    this.showForm.update(v => !v);
    if (this.showForm()) {
      this.entryForm.reset();
    }
  }

  onSubmit(): void {
    if (this.entryForm.invalid) return;

    const val = this.entryForm.value;
    const entryDate: Date = val.entryDate;
    const dateStr = `${entryDate.getFullYear()}-${String(entryDate.getMonth() + 1).padStart(2, '0')}-${String(entryDate.getDate()).padStart(2, '0')}`;

    this.incomeService.create({
      streamType: this.selectedStreamType() as CreateIncomeEntryRequestStreamTypeEnum,
      amount: val.amount,
      entryDate: dateStr,
      source: val.source || undefined,
      description: val.description || undefined,
    }).subscribe({
      next: () => {
        this.snackBar.open('Einnahme erfolgreich erstellt', 'OK', { duration: 2000 });
        this.showForm.set(false);
        this.entryForm.reset();
        this.loadEntries();
      },
      error: () => {
        this.snackBar.open('Fehler beim Erstellen der Einnahme', 'OK', { duration: 3000 });
      },
    });
  }

  deleteEntry(id: number): void {
    this.incomeService.delete(id).subscribe({
      next: () => {
        this.snackBar.open('Einnahme gelöscht', 'OK', { duration: 2000 });
        this.loadEntries();
      },
      error: () => {
        this.snackBar.open('Fehler beim Löschen', 'OK', { duration: 3000 });
      },
    });
  }
}
