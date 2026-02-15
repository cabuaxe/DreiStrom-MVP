import { Component, inject, signal, computed, OnInit, OnDestroy } from '@angular/core';
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Subscription } from 'rxjs';
import { DashboardService } from '../../services/dashboard.service';
import { SseService } from '../../../common/services/sse.service';
import { AbfaerbungStatus } from '../../models/abfaerbung-status.model';

@Component({
  selector: 'app-abfaerbung-card',
  imports: [
    CurrencyPipe,
    DecimalPipe,
    MatCardModule,
    MatIconModule,
    MatProgressBarModule,
  ],
  templateUrl: './abfaerbung-card.component.html',
  styleUrl: './abfaerbung-card.component.scss',
})
export class AbfaerbungCardComponent implements OnInit, OnDestroy {
  private readonly dashboardService = inject(DashboardService);
  private readonly sseService = inject(SseService);
  private sseSub?: Subscription;

  readonly status = signal<AbfaerbungStatus | null>(null);
  readonly loading = signal(true);

  readonly ratioPercent = computed(() => {
    const s = this.status();
    return s ? s.ratio * 100 : 0;
  });

  readonly progressValue = computed(() => {
    // Scale: 10% ratio = 100% bar. Threshold (3%) shows at 30%.
    return Math.min(this.ratioPercent() * 10, 100);
  });

  readonly progressColor = computed(() =>
    this.status()?.thresholdExceeded ? 'warn' : 'primary'
  );

  ngOnInit(): void {
    this.loadStatus();
    this.sseSub = this.sseService.connect<AbfaerbungStatus>('/api/v1/dashboard/events', 'abfaerbung')
      .subscribe({
        next: (event) => this.status.set(event.data),
      });
  }

  ngOnDestroy(): void {
    this.sseSub?.unsubscribe();
  }

  private loadStatus(): void {
    this.loading.set(true);
    this.dashboardService.getAbfaerbungStatus().subscribe({
      next: (data) => {
        this.status.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }
}
