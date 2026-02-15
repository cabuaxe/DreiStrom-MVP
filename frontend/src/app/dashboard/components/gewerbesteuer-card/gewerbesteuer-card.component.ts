import { Component, inject, signal, computed, OnInit, OnDestroy } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Subscription } from 'rxjs';
import { DashboardService } from '../../services/dashboard.service';
import { SseService } from '../../../common/services/sse.service';
import { GewerbesteuerThreshold } from '../../models/gewerbesteuer-threshold.model';

@Component({
  selector: 'app-gewerbesteuer-card',
  imports: [
    CurrencyPipe,
    MatCardModule,
    MatIconModule,
    MatProgressBarModule,
  ],
  templateUrl: './gewerbesteuer-card.component.html',
  styleUrl: './gewerbesteuer-card.component.scss',
})
export class GewerbesteuerCardComponent implements OnInit, OnDestroy {
  private readonly dashboardService = inject(DashboardService);
  private readonly sseService = inject(SseService);
  private sseSub?: Subscription;

  readonly status = signal<GewerbesteuerThreshold | null>(null);
  readonly loading = signal(true);

  readonly freibetragProgress = computed(() => {
    const s = this.status();
    if (!s) return 0;
    return Math.min((s.gewerbeProfit / s.freibetrag) * 100, 100);
  });

  readonly revenueProgress = computed(() => {
    const s = this.status();
    if (!s) return 0;
    return Math.min((s.gewerbeRevenue / s.bilanzierungRevenueThreshold) * 100, 100);
  });

  readonly hasAnyWarning = computed(() => {
    const s = this.status();
    return s ? (s.freibetragExceeded || s.bilanzierungRevenueExceeded || s.bilanzierungProfitExceeded) : false;
  });

  ngOnInit(): void {
    this.loadStatus();
    this.sseSub = this.sseService.connect<GewerbesteuerThreshold>('/api/v1/dashboard/events')
        .subscribe({
          next: (event) => this.status.set(event.data),
        });
  }

  ngOnDestroy(): void {
    this.sseSub?.unsubscribe();
  }

  private loadStatus(): void {
    this.loading.set(true);
    this.dashboardService.getGewerbesteuerThreshold().subscribe({
      next: (data) => {
        this.status.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
