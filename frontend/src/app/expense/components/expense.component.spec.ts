import { TestBed } from '@angular/core/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { LOCALE_ID } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeDe from '@angular/common/locales/de';
import { of } from 'rxjs';
import { ExpenseComponent } from './expense.component';
import { ExpenseService } from '../services/expense.service';
import { AllocationRuleService } from '../services/allocation-rule.service';
import { ExpenseEntryResponse } from '../models/expense-entry.model';
import { AllocationRuleResponse } from '../models/allocation-rule.model';

registerLocaleData(localeDe);

describe('ExpenseComponent', () => {
  const mockEntries: ExpenseEntryResponse[] = [
    { id: 1, amount: 49.90, currency: 'EUR', category: 'Büromaterial', entryDate: '2026-03-10', gwg: true, createdAt: '', updatedAt: '' },
    { id: 2, amount: 1200, currency: 'EUR', category: 'Laptop', entryDate: '2026-03-15', gwg: false, createdAt: '', updatedAt: '' },
    { id: 3, amount: 250, currency: 'EUR', category: 'Telefon', entryDate: '2026-04-01', gwg: true, createdAt: '', updatedAt: '',
      allocationRule: { id: 1, name: 'Büro 60/30/10', freiberufPct: 60, gewerbePct: 30, personalPct: 10 } },
  ];

  const mockRules: AllocationRuleResponse[] = [
    { id: 1, name: 'Büro 60/30/10', freiberufPct: 60, gewerbePct: 30, personalPct: 10, createdAt: '', updatedAt: '' },
    { id: 2, name: 'Privat 0/0/100', freiberufPct: 0, gewerbePct: 0, personalPct: 100, createdAt: '', updatedAt: '' },
  ];

  let mockExpenseService: Partial<ExpenseService>;
  let mockRuleService: Partial<AllocationRuleService>;

  beforeEach(async () => {
    mockExpenseService = {
      list: vi.fn().mockReturnValue(of(mockEntries)),
      create: vi.fn().mockReturnValue(of(mockEntries[0])),
      delete: vi.fn().mockReturnValue(of(undefined)),
      getDepreciationSchedule: vi.fn().mockReturnValue(of([])),
    };
    mockRuleService = {
      list: vi.fn().mockReturnValue(of(mockRules)),
      create: vi.fn().mockReturnValue(of(mockRules[0])),
      delete: vi.fn().mockReturnValue(of(undefined)),
    };

    await TestBed.configureTestingModule({
      imports: [ExpenseComponent],
      providers: [
        provideAnimationsAsync(),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ExpenseService, useValue: mockExpenseService },
        { provide: AllocationRuleService, useValue: mockRuleService },
        { provide: LOCALE_ID, useValue: 'de-DE' },
      ],
    }).compileComponents();
  });

  it('should create the component', () => {
    const fixture = TestBed.createComponent(ExpenseComponent);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should load entries on init', () => {
    const fixture = TestBed.createComponent(ExpenseComponent);
    fixture.detectChanges();
    expect(mockExpenseService.list).toHaveBeenCalled();
    expect(fixture.componentInstance.entries().length).toBe(3);
  });

  it('should load allocation rules on init', () => {
    const fixture = TestBed.createComponent(ExpenseComponent);
    fixture.detectChanges();
    expect(mockRuleService.list).toHaveBeenCalled();
    expect(fixture.componentInstance.allocationRules().length).toBe(2);
  });

  it('should compute expense total', () => {
    const fixture = TestBed.createComponent(ExpenseComponent);
    fixture.detectChanges();
    expect(fixture.componentInstance.expenseTotal()).toBeCloseTo(1499.9);
  });

  it('should filter non-GWG entries for AfA', () => {
    const fixture = TestBed.createComponent(ExpenseComponent);
    fixture.detectChanges();
    const nonGwg = fixture.componentInstance.nonGwgEntries();
    expect(nonGwg.length).toBe(1);
    expect(nonGwg[0].category).toBe('Laptop');
  });

  it('should toggle expense form visibility', () => {
    const fixture = TestBed.createComponent(ExpenseComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    expect(component.showExpenseForm()).toBe(false);
    component.toggleExpenseForm();
    expect(component.showExpenseForm()).toBe(true);
    component.toggleExpenseForm();
    expect(component.showExpenseForm()).toBe(false);
  });

  it('should toggle rule form visibility', () => {
    const fixture = TestBed.createComponent(ExpenseComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    expect(component.showRuleForm()).toBe(false);
    component.toggleRuleForm();
    expect(component.showRuleForm()).toBe(true);
    component.toggleRuleForm();
    expect(component.showRuleForm()).toBe(false);
  });

  it('should validate expense form requires amount, category, and date', () => {
    const fixture = TestBed.createComponent(ExpenseComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    expect(component.expenseForm.valid).toBe(false);

    component.expenseForm.patchValue({ amount: 100, category: 'Test', entryDate: new Date() });
    expect(component.expenseForm.valid).toBe(true);
  });

  it('should render four tabs', () => {
    const fixture = TestBed.createComponent(ExpenseComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const tabLabels = compiled.querySelectorAll('.mdc-tab__text-label');
    expect(tabLabels.length).toBe(4);
  });
});
