import { Component, inject, signal, computed, OnInit, OnDestroy } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Subscription } from 'rxjs';
import { DashboardService } from '../../services/dashboard.service';
import { SseService } from '../../../common/services/sse.service';
import { ArbZGStatus } from '../../models/arbzg-status.model';

@Component({
  selector: 'app-arbzg-card',
  imports: [
    DecimalPipe,
    MatCardModule,
    MatIconModule,
    MatProgressBarModule,
  ],
  templateUrl: './arbzg-card.component.html',
  styleUrl: './arbzg-card.component.scss',
})
export class ArbZGCardComponent implements OnInit, OnDestroy {
  private readonly dashboardService = inject(DashboardService);
  private readonly sseService = inject(SseService);
  private sseSub?: Subscription;

  readonly status = signal<ArbZGStatus | null>(null);
  readonly loading = signal(true);

  readonly progressValue = computed(() => {
    const s = this.status();
    if (!s) return 0;
    return Math.min((s.avgTotalHoursWeekly / s.maxAllowedHoursWeekly) * 100, 100);
  });

  readonly employmentBarWidth = computed(() => {
    const s = this.status();
    if (!s || s.avgTotalHoursWeekly === 0) return 0;
    return (s.avgEmploymentHoursWeekly / s.maxAllowedHoursWeekly) * 100;
  });

  readonly selfEmployedBarWidth = computed(() => {
    const s = this.status();
    if (!s || s.avgTotalHoursWeekly === 0) return 0;
    return (s.avgSelfEmployedHoursWeekly / s.maxAllowedHoursWeekly) * 100;
  });

  ngOnInit(): void {
    this.loadStatus();
    this.sseSub = this.sseService.connect<ArbZGStatus>('/api/v1/dashboard/events', 'social-insurance')
      .subscribe({
        next: (event) => this.status.set(event.data as ArbZGStatus),
      });
  }

  ngOnDestroy(): void {
    this.sseSub?.unsubscribe();
  }

  private loadStatus(): void {
    this.loading.set(true);
    this.dashboardService.getArbZGStatus().subscribe({
      next: (data) => {
        this.status.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
