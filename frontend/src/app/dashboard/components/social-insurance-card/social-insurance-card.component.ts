import { Component, inject, signal, computed, OnInit, OnDestroy } from '@angular/core';
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Subscription } from 'rxjs';
import { DashboardService } from '../../services/dashboard.service';
import { SseService } from '../../../common/services/sse.service';
import { SocialInsuranceStatus, RiskLevel } from '../../models/social-insurance-status.model';

@Component({
  selector: 'app-social-insurance-card',
  imports: [
    CurrencyPipe,
    DecimalPipe,
    MatCardModule,
    MatIconModule,
    MatProgressBarModule,
  ],
  templateUrl: './social-insurance-card.component.html',
  styleUrl: './social-insurance-card.component.scss',
})
export class SocialInsuranceCardComponent implements OnInit, OnDestroy {
  private readonly dashboardService = inject(DashboardService);
  private readonly sseService = inject(SseService);
  private sseSub?: Subscription;

  readonly status = signal<SocialInsuranceStatus | null>(null);
  readonly loading = signal(true);

  readonly employmentBarWidth = computed(() => {
    const s = this.status();
    if (!s) return 0;
    return Math.min((s.avgEmploymentHoursWeekly / 40) * 100, 100);
  });

  readonly selfEmployedBarWidth = computed(() => {
    const s = this.status();
    if (!s) return 0;
    return Math.min((s.avgSelfEmployedHoursWeekly / 40) * 100, 100);
  });

  /** SE hours as % of 20h threshold -> progress bar */
  readonly hoursProgress = computed(() => {
    const s = this.status();
    if (!s) return 0;
    return Math.min((s.avgSelfEmployedHoursWeekly / 20) * 100, 100);
  });

  /** Income ratio: SE / (SE + Employment) as percentage */
  readonly incomeRatio = computed(() => {
    const s = this.status();
    if (!s) return 0;
    const total = s.totalEmploymentIncome + s.totalSelfEmployedIncome;
    return total > 0 ? (s.totalSelfEmployedIncome / total) * 100 : 0;
  });

  readonly riskIcon = computed(() => {
    const level = this.status()?.riskLevel;
    switch (level) {
      case 'CRITICAL': return 'error';
      case 'WARNING': return 'warning';
      default: return 'verified_user';
    }
  });

  readonly riskColor = computed((): string => {
    const level = this.status()?.riskLevel;
    switch (level) {
      case 'CRITICAL': return 'critical';
      case 'WARNING': return 'warning';
      default: return 'safe';
    }
  });

  readonly riskLabel = computed(() => {
    const level = this.status()?.riskLevel;
    switch (level) {
      case 'CRITICAL': return 'Umstufungsrisiko';
      case 'WARNING': return 'Grenzbereich';
      default: return 'Hauptberuflich angestellt';
    }
  });

  ngOnInit(): void {
    this.loadStatus();
    this.sseSub = this.sseService.connect<SocialInsuranceStatus>('/api/v1/dashboard/events', 'social-insurance')
      .subscribe({
        next: (event) => this.status.set(event.data),
      });
  }

  ngOnDestroy(): void {
    this.sseSub?.unsubscribe();
  }

  private loadStatus(): void {
    this.loading.set(true);
    this.dashboardService.getSocialInsuranceStatus().subscribe({
      next: (data) => {
        this.status.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
