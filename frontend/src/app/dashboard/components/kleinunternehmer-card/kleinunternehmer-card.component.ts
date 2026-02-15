import { Component, inject, signal, computed, OnInit, OnDestroy } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Subscription } from 'rxjs';
import { DashboardService } from '../../services/dashboard.service';
import { SseService } from '../../../common/services/sse.service';
import { KleinunternehmerStatus } from '../../models/kleinunternehmer-status.model';

@Component({
  selector: 'app-kleinunternehmer-card',
  imports: [
    CurrencyPipe,
    MatCardModule,
    MatIconModule,
    MatProgressBarModule,
  ],
  templateUrl: './kleinunternehmer-card.component.html',
  styleUrl: './kleinunternehmer-card.component.scss',
})
export class KleinunternehmerCardComponent implements OnInit, OnDestroy {
  private readonly dashboardService = inject(DashboardService);
  private readonly sseService = inject(SseService);
  private sseSub?: Subscription;

  readonly status = signal<KleinunternehmerStatus | null>(null);
  readonly loading = signal(true);

  readonly currentPercent = computed(() => {
    const s = this.status();
    return s ? s.currentRatio * 100 : 0;
  });

  readonly projectedPercent = computed(() => {
    const s = this.status();
    return s ? s.projectedRatio * 100 : 0;
  });

  readonly currentBarValue = computed(() =>
    Math.min(this.currentPercent(), 100)
  );

  readonly projectedBarValue = computed(() =>
    Math.min(this.projectedPercent(), 100)
  );

  readonly hasWarning = computed(() => {
    const s = this.status();
    return s ? s.currentExceeded || s.projectedExceeded : false;
  });

  ngOnInit(): void {
    this.loadStatus();
    this.sseSub = this.sseService.on<KleinunternehmerStatus>('kleinunternehmer')
      .subscribe({
        next: (event) => this.status.set(event.data),
      });
  }

  ngOnDestroy(): void {
    this.sseSub?.unsubscribe();
  }

  private loadStatus(): void {
    this.loading.set(true);
    this.dashboardService.getKleinunternehmerStatus().subscribe({
      next: (data) => {
        this.status.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
