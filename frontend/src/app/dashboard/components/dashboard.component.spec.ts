import { TestBed } from '@angular/core/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { LOCALE_ID, signal } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeDe from '@angular/common/locales/de';
import { of, EMPTY } from 'rxjs';
import { DashboardComponent } from './dashboard.component';
import { DashboardService } from '../services/dashboard.service';
import { SseService } from '../../common/services/sse.service';
import { FeatureFlagService } from '../../common/services/feature-flag.service';

registerLocaleData(localeDe);

describe('DashboardComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideAnimationsAsync(),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: DashboardService,
          useValue: {
            getAbfaerbungStatus: vi.fn().mockReturnValue(of({
              ratio: 0.01,
              gewerbeRevenue: 500,
              selfEmployedRevenue: 50000,
              thresholdExceeded: false,
              year: 2026,
            })),
            getKleinunternehmerStatus: vi.fn().mockReturnValue(EMPTY),
            getSocialInsuranceStatus: vi.fn().mockReturnValue(EMPTY),
            getGewerbesteuerThreshold: vi.fn().mockReturnValue(EMPTY),
            getMandatoryFilingStatus: vi.fn().mockReturnValue(EMPTY),
            getArbZGStatus: vi.fn().mockReturnValue(EMPTY),
          },
        },
        {
          provide: SseService,
          useValue: { on: vi.fn().mockReturnValue(EMPTY), connect: vi.fn().mockReturnValue(EMPTY) },
        },
        {
          provide: FeatureFlagService,
          useValue: {
            flags: signal(null),
            loaded: signal(true),
            initialize: vi.fn(),
            destroy: vi.fn(),
          },
        },
        { provide: LOCALE_ID, useValue: 'de-DE' },
      ],
    }).compileComponents();
  });

  it('should create the component', () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render the dashboard heading', () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h2')?.textContent).toContain('Dashboard');
  });

  it('should render the abfaerbung card', () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-abfaerbung-card')).toBeTruthy();
  });
});
