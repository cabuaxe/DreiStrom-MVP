import { TestBed } from '@angular/core/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { LOCALE_ID } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeDe from '@angular/common/locales/de';
import { of, EMPTY } from 'rxjs';
import { AbfaerbungCardComponent } from './abfaerbung-card.component';
import { DashboardService } from '../../services/dashboard.service';
import { SseService } from '../../../common/services/sse.service';
import { AbfaerbungStatus } from '../../models/abfaerbung-status.model';

registerLocaleData(localeDe);

describe('AbfaerbungCardComponent', () => {
  const mockStatus: AbfaerbungStatus = {
    ratio: 0.0450,
    gewerbeRevenue: 30000,
    selfEmployedRevenue: 66666.67,
    thresholdExceeded: true,
    year: 2026,
  };

  const safeStatus: AbfaerbungStatus = {
    ratio: 0.0100,
    gewerbeRevenue: 1000,
    selfEmployedRevenue: 100000,
    thresholdExceeded: false,
    year: 2026,
  };

  let mockDashboardService: Partial<DashboardService>;
  let mockSseService: Partial<SseService>;

  beforeEach(async () => {
    mockDashboardService = {
      getAbfaerbungStatus: vi.fn().mockReturnValue(of(mockStatus)),
    };
    mockSseService = {
      on: vi.fn().mockReturnValue(EMPTY),
      connect: vi.fn().mockReturnValue(EMPTY),
    };

    await TestBed.configureTestingModule({
      imports: [AbfaerbungCardComponent],
      providers: [
        provideAnimationsAsync(),
        { provide: DashboardService, useValue: mockDashboardService },
        { provide: SseService, useValue: mockSseService },
        { provide: LOCALE_ID, useValue: 'de-DE' },
      ],
    }).compileComponents();
  });

  it('should create the component', () => {
    const fixture = TestBed.createComponent(AbfaerbungCardComponent);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should load abfaerbung status on init', () => {
    const fixture = TestBed.createComponent(AbfaerbungCardComponent);
    fixture.detectChanges();
    expect(mockDashboardService.getAbfaerbungStatus).toHaveBeenCalled();
    expect(fixture.componentInstance.status()).toEqual(mockStatus);
    expect(fixture.componentInstance.loading()).toBe(false);
  });

  it('should subscribe to SSE events on init', () => {
    const fixture = TestBed.createComponent(AbfaerbungCardComponent);
    fixture.detectChanges();
    expect(mockSseService.on).toHaveBeenCalledWith('abfaerbung');
  });

  it('should compute ratio percent correctly', () => {
    const fixture = TestBed.createComponent(AbfaerbungCardComponent);
    fixture.detectChanges();
    // 0.0450 * 100 = 4.50
    expect(fixture.componentInstance.ratioPercent()).toBeCloseTo(4.5, 1);
  });

  it('should compute progress value scaled to 10%', () => {
    const fixture = TestBed.createComponent(AbfaerbungCardComponent);
    fixture.detectChanges();
    // 4.5% * 10 = 45
    expect(fixture.componentInstance.progressValue()).toBeCloseTo(45, 0);
  });

  it('should return warn color when threshold exceeded', () => {
    const fixture = TestBed.createComponent(AbfaerbungCardComponent);
    fixture.detectChanges();
    expect(fixture.componentInstance.progressColor()).toBe('warn');
  });

  it('should return primary color when below threshold', () => {
    (mockDashboardService.getAbfaerbungStatus as ReturnType<typeof vi.fn>)
      .mockReturnValue(of(safeStatus));
    const fixture = TestBed.createComponent(AbfaerbungCardComponent);
    fixture.detectChanges();
    expect(fixture.componentInstance.progressColor()).toBe('primary');
  });

  it('should render warning card class when threshold exceeded', () => {
    const fixture = TestBed.createComponent(AbfaerbungCardComponent);
    fixture.detectChanges();
    const card = fixture.nativeElement.querySelector('mat-card');
    expect(card.classList.contains('warning')).toBe(true);
  });

  it('should render safe card class when below threshold', () => {
    (mockDashboardService.getAbfaerbungStatus as ReturnType<typeof vi.fn>)
      .mockReturnValue(of(safeStatus));
    const fixture = TestBed.createComponent(AbfaerbungCardComponent);
    fixture.detectChanges();
    const card = fixture.nativeElement.querySelector('mat-card');
    expect(card.classList.contains('safe')).toBe(true);
  });

  it('should update status from SSE event', () => {
    const updatedStatus: AbfaerbungStatus = { ...mockStatus, ratio: 0.06, thresholdExceeded: true };
    (mockSseService.on as ReturnType<typeof vi.fn>)
      .mockReturnValue(of({ type: 'abfaerbung', data: updatedStatus }));

    const fixture = TestBed.createComponent(AbfaerbungCardComponent);
    fixture.detectChanges();
    expect(fixture.componentInstance.status()).toEqual(updatedStatus);
  });
});
