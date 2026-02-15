import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CurrencyPipe, PercentPipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { TaxService } from '../../services/tax.service';
import { TaxReserveRecommendation } from '../../models/tax.model';

@Component({
  selector: 'app-tax-reserve',
  imports: [
    CurrencyPipe,
    PercentPipe,
    MatCardModule,
    MatIconModule,
    MatProgressBarModule,
  ],
  templateUrl: './tax-reserve.component.html',
  styleUrl: './tax-reserve.component.scss',
})
export class TaxReserveComponent implements OnInit {
  private readonly taxService = inject(TaxService);

  readonly reserve = signal<TaxReserveRecommendation | null>(null);
  readonly loading = signal(true);

  readonly progressPercent = computed(() => {
    const r = this.reserve();
    if (!r || r.annualReserve === 0) return 0;
    return Math.min((r.alreadyReserved / r.annualReserve) * 100, 100);
  });

  readonly onTrack = computed(() => this.progressPercent() >= 50);

  ngOnInit(): void {
    this.loadReserve();
  }

  private loadReserve(): void {
    this.loading.set(true);
    this.taxService.getTaxReserve(new Date().getFullYear()).subscribe({
      next: (data) => {
        this.reserve.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
