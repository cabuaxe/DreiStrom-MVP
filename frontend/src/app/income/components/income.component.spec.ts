import { TestBed } from '@angular/core/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { LOCALE_ID } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeDe from '@angular/common/locales/de';
import { IncomeComponent } from './income.component';
import { IncomeService } from '../services/income.service';
import { of } from 'rxjs';
import { IncomeEntryResponse, IncomeEntryResponseStreamTypeEnum } from '../../api/generated/model/income-entry-response.model';

registerLocaleData(localeDe);

describe('IncomeComponent', () => {
  const mockEntries: IncomeEntryResponse[] = [
    { id: 1, streamType: IncomeEntryResponseStreamTypeEnum.EMPLOYMENT, amount: 3000, currency: 'EUR', entryDate: '2026-03-01', source: 'Gehalt' },
    { id: 2, streamType: IncomeEntryResponseStreamTypeEnum.FREIBERUF, amount: 5000, currency: 'EUR', entryDate: '2026-03-15', source: 'Beratung' },
    { id: 3, streamType: IncomeEntryResponseStreamTypeEnum.FREIBERUF, amount: 2000, currency: 'EUR', entryDate: '2026-03-20', source: 'Workshop' },
    { id: 4, streamType: IncomeEntryResponseStreamTypeEnum.GEWERBE, amount: 8000, currency: 'EUR', entryDate: '2026-04-01', source: 'Verkauf' },
  ];

  let mockIncomeService: Partial<IncomeService>;

  beforeEach(async () => {
    mockIncomeService = {
      list: vi.fn().mockReturnValue(of(mockEntries)),
      create: vi.fn().mockReturnValue(of(mockEntries[0])),
      delete: vi.fn().mockReturnValue(of({})),
    };

    await TestBed.configureTestingModule({
      imports: [IncomeComponent],
      providers: [
        provideAnimationsAsync(),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: IncomeService, useValue: mockIncomeService },
        { provide: LOCALE_ID, useValue: 'de-DE' },
      ],
    }).compileComponents();
  });

  it('should create the component', () => {
    const fixture = TestBed.createComponent(IncomeComponent);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should have three stream tabs', () => {
    const fixture = TestBed.createComponent(IncomeComponent);
    const component = fixture.componentInstance;
    expect(component.tabs.length).toBe(3);
    expect(component.tabs.map(t => t.type)).toEqual(['EMPLOYMENT', 'FREIBERUF', 'GEWERBE']);
  });

  it('should load entries on init', () => {
    const fixture = TestBed.createComponent(IncomeComponent);
    fixture.detectChanges();
    expect(mockIncomeService.list).toHaveBeenCalled();
    expect(fixture.componentInstance.entries().length).toBe(4);
  });

  it('should filter entries by selected stream', () => {
    const fixture = TestBed.createComponent(IncomeComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    // Default tab is EMPLOYMENT (index 0)
    expect(component.filteredEntries().length).toBe(1);

    // Switch to FREIBERUF (index 1)
    component.onTabChange(1);
    expect(component.filteredEntries().length).toBe(2);

    // Switch to GEWERBE (index 2)
    component.onTabChange(2);
    expect(component.filteredEntries().length).toBe(1);
  });

  it('should compute stream total correctly', () => {
    const fixture = TestBed.createComponent(IncomeComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    // EMPLOYMENT total
    expect(component.streamTotal()).toBe(3000);

    // FREIBERUF total
    component.onTabChange(1);
    expect(component.streamTotal()).toBe(7000);

    // GEWERBE total
    component.onTabChange(2);
    expect(component.streamTotal()).toBe(8000);
  });

  it('should toggle form visibility', () => {
    const fixture = TestBed.createComponent(IncomeComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    expect(component.showForm()).toBe(false);
    component.toggleForm();
    expect(component.showForm()).toBe(true);
    component.toggleForm();
    expect(component.showForm()).toBe(false);
  });

  it('should render tab labels in DOM', () => {
    const fixture = TestBed.createComponent(IncomeComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const tabLabels = compiled.querySelectorAll('.mdc-tab__text-label');
    expect(tabLabels.length).toBe(3);
  });

  it('should validate form requires amount and date', () => {
    const fixture = TestBed.createComponent(IncomeComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    expect(component.entryForm.valid).toBe(false);

    component.entryForm.patchValue({ amount: 100, entryDate: new Date() });
    expect(component.entryForm.valid).toBe(true);
  });
});
