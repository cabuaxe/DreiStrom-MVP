import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { DashboardService } from '../../services/dashboard.service';
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
export class KleinunternehmerCardComponent implements OnInit {
  private readonly dashboardService = inject(DashboardService);

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
