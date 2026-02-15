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
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ExpenseService } from '../services/expense.service';
import { AllocationRuleService } from '../services/allocation-rule.service';
import { ExpenseEntryResponse } from '../models/expense-entry.model';
import { AllocationRuleResponse } from '../models/allocation-rule.model';
import { DepreciationYearEntry } from '../models/depreciation.model';
import { AllocationSliderComponent } from './allocation-slider/allocation-slider.component';
import { AfaScheduleComponent } from './afa-schedule/afa-schedule.component';
import { HomeOfficeCalculatorComponent } from './home-office-calculator/home-office-calculator.component';

@Component({
  selector: 'app-expense',
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
    MatButtonToggleModule,
    MatTooltipModule,
    AllocationSliderComponent,
    AfaScheduleComponent,
    HomeOfficeCalculatorComponent,
  ],
  templateUrl: './expense.component.html',
  styleUrl: './expense.component.scss',
})
export class ExpenseComponent implements OnInit {
  private readonly expenseService = inject(ExpenseService);
  private readonly allocationRuleService = inject(AllocationRuleService);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);

  // --- Expense entries ---
  readonly entries = signal<ExpenseEntryResponse[]>([]);
  readonly allocationRules = signal<AllocationRuleResponse[]>([]);
  readonly loading = signal(false);
  readonly showExpenseForm = signal(false);

  readonly displayedColumns = ['entryDate', 'category', 'amount', 'gwg', 'allocationRule', 'description', 'actions'];

  readonly expenseTotal = computed(() =>
    this.entries().reduce((sum, e) => sum + (e.amount ?? 0), 0)
  );

  readonly expenseForm: FormGroup = this.fb.group({
    amount: [null, [Validators.required, Validators.min(0.01)]],
    category: ['', [Validators.required, Validators.maxLength(100)]],
    entryDate: [null, Validators.required],
    allocationRuleId: [null],
    description: [''],
  });

  // --- Allocation rules ---
  readonly showRuleForm = signal(false);
  readonly ruleDisplayedColumns = ['name', 'freiberufPct', 'gewerbePct', 'personalPct', 'actions'];

  readonly ruleForm: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    freiberufPct: [34],
    gewerbePct: [33],
    personalPct: [33],
  });

  // --- AfA ---
  readonly selectedExpenseForAfa = signal<ExpenseEntryResponse | null>(null);
  readonly afaSchedule = signal<DepreciationYearEntry[]>([]);
  readonly afaLoading = signal(false);

  readonly nonGwgEntries = computed(() =>
    this.entries().filter(e => !e.gwg && e.amount > 800)
  );

  ngOnInit(): void {
    this.loadEntries();
    this.loadRules();
  }

  // === Expense CRUD ===

  loadEntries(): void {
    this.loading.set(true);
    this.expenseService.list().subscribe({
      next: (data) => {
        this.entries.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.snackBar.open('Fehler beim Laden der Ausgaben', 'OK', { duration: 3000 });
        this.loading.set(false);
      },
    });
  }

  toggleExpenseForm(): void {
    this.showExpenseForm.update(v => !v);
    if (this.showExpenseForm()) {
      this.expenseForm.reset();
    }
  }

  onSubmitExpense(): void {
    if (this.expenseForm.invalid) return;

    const val = this.expenseForm.value;
    const entryDate: Date = val.entryDate;
    const dateStr = `${entryDate.getFullYear()}-${String(entryDate.getMonth() + 1).padStart(2, '0')}-${String(entryDate.getDate()).padStart(2, '0')}`;

    this.expenseService.create({
      amount: val.amount,
      category: val.category,
      entryDate: dateStr,
      allocationRuleId: val.allocationRuleId || undefined,
      description: val.description || undefined,
    }).subscribe({
      next: () => {
        this.snackBar.open('Ausgabe erfolgreich erstellt', 'OK', { duration: 2000 });
        this.showExpenseForm.set(false);
        this.expenseForm.reset();
        this.loadEntries();
      },
      error: () => {
        this.snackBar.open('Fehler beim Erstellen der Ausgabe', 'OK', { duration: 3000 });
      },
    });
  }

  deleteExpense(id: number): void {
    this.expenseService.delete(id).subscribe({
      next: () => {
        this.snackBar.open('Ausgabe gelöscht', 'OK', { duration: 2000 });
        this.loadEntries();
      },
      error: () => {
        this.snackBar.open('Fehler beim Löschen', 'OK', { duration: 3000 });
      },
    });
  }

  // === Allocation rules ===

  loadRules(): void {
    this.allocationRuleService.list().subscribe({
      next: (data) => this.allocationRules.set(data),
      error: () => {
        this.snackBar.open('Fehler beim Laden der Zuordnungsregeln', 'OK', { duration: 3000 });
      },
    });
  }

  toggleRuleForm(): void {
    this.showRuleForm.update(v => !v);
    if (this.showRuleForm()) {
      this.ruleForm.reset({ name: '', freiberufPct: 34, gewerbePct: 33, personalPct: 33 });
    }
  }

  onAllocationChange(values: { freiberufPct: number; gewerbePct: number; personalPct: number }): void {
    this.ruleForm.patchValue(values);
  }

  onSubmitRule(): void {
    if (this.ruleForm.invalid) return;

    const val = this.ruleForm.value;
    if (val.freiberufPct + val.gewerbePct + val.personalPct !== 100) {
      this.snackBar.open('Prozentsätze müssen 100% ergeben', 'OK', { duration: 3000 });
      return;
    }

    this.allocationRuleService.create({
      name: val.name,
      freiberufPct: val.freiberufPct,
      gewerbePct: val.gewerbePct,
      personalPct: val.personalPct,
    }).subscribe({
      next: () => {
        this.snackBar.open('Zuordnungsregel erstellt', 'OK', { duration: 2000 });
        this.showRuleForm.set(false);
        this.ruleForm.reset();
        this.loadRules();
      },
      error: () => {
        this.snackBar.open('Fehler beim Erstellen der Regel', 'OK', { duration: 3000 });
      },
    });
  }

  deleteRule(id: number): void {
    this.allocationRuleService.delete(id).subscribe({
      next: () => {
        this.snackBar.open('Regel gelöscht', 'OK', { duration: 2000 });
        this.loadRules();
      },
      error: () => {
        this.snackBar.open('Fehler beim Löschen', 'OK', { duration: 3000 });
      },
    });
  }

  // === AfA schedule ===

  loadAfaSchedule(expense: ExpenseEntryResponse): void {
    this.selectedExpenseForAfa.set(expense);
    this.afaLoading.set(true);
    this.expenseService.getDepreciationSchedule(expense.id).subscribe({
      next: (data) => {
        this.afaSchedule.set(data);
        this.afaLoading.set(false);
      },
      error: () => {
        this.snackBar.open('Kein AfA-Plan für diese Ausgabe verfügbar', 'OK', { duration: 3000 });
        this.afaSchedule.set([]);
        this.afaLoading.set(false);
      },
    });
  }
}
