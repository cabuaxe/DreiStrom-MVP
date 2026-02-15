import { Component, inject, signal, computed, OnInit, OnDestroy } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Subscription } from 'rxjs';
import { DashboardService } from '../../services/dashboard.service';
import { SseService } from '../../../common/services/sse.service';
import { MandatoryFilingStatus } from '../../models/mandatory-filing-status.model';

@Component({
  selector: 'app-mandatory-filing-card',
  imports: [
    CurrencyPipe,
    MatCardModule,
    MatIconModule,
    MatProgressBarModule,
  ],
  templateUrl: './mandatory-filing-card.component.html',
  styleUrl: './mandatory-filing-card.component.scss',
})
export class MandatoryFilingCardComponent implements OnInit, OnDestroy {
  private readonly dashboardService = inject(DashboardService);
  private readonly sseService = inject(SseService);
  private sseSub?: Subscription;

  readonly status = signal<MandatoryFilingStatus | null>(null);
  readonly loading = signal(true);

  readonly progressValue = computed(() => {
    const s = this.status();
    if (!s) return 0;
    return Math.min((s.nebeneinkuenfte / s.threshold) * 100, 100);
  });

  ngOnInit(): void {
    this.loadStatus();
    this.sseSub = this.sseService.connect<MandatoryFilingStatus>('/api/v1/dashboard/events', 'mandatory-filing')
      .subscribe({
        next: (event) => this.status.set(event.data),
      });
  }

  ngOnDestroy(): void {
    this.sseSub?.unsubscribe();
  }

  private loadStatus(): void {
    this.loading.set(true);
    this.dashboardService.getMandatoryFilingStatus().subscribe({
      next: (data) => {
        this.status.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
