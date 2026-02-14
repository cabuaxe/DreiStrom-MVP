import { TestBed } from '@angular/core/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { LOCALE_ID } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeDe from '@angular/common/locales/de';
import { of, EMPTY } from 'rxjs';
import { DashboardComponent } from './dashboard.component';
import { DashboardService } from '../services/dashboard.service';
import { SseService } from '../../common/services/sse.service';

registerLocaleData(localeDe);

describe('DashboardComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideAnimationsAsync(),
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
          },
        },
        {
          provide: SseService,
          useValue: { connect: vi.fn().mockReturnValue(EMPTY) },
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
