import { Component, inject, signal, OnInit } from '@angular/core';
import { CurrencyPipe, PercentPipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { TaxService } from '../../services/tax.service';
import { TaxCalculationResult, GewerbesteuerResult } from '../../models/tax.model';

@Component({
  selector: 'app-tax-estimator',
  imports: [
    CurrencyPipe,
    PercentPipe,
    MatCardModule,
    MatSelectModule,
    MatFormFieldModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatIconModule,
  ],
  templateUrl: './tax-estimator.component.html',
  styleUrl: './tax-estimator.component.scss',
})
export class TaxEstimatorComponent implements OnInit {
  private readonly taxService = inject(TaxService);

  readonly taxResult = signal<TaxCalculationResult | null>(null);
  readonly gewerbeResult = signal<GewerbesteuerResult | null>(null);
  readonly loading = signal(true);
  readonly selectedYear = signal(new Date().getFullYear());

  get yearOptions(): number[] {
    const current = new Date().getFullYear();
    return [current - 1, current, current + 1];
  }

  ngOnInit(): void {
    this.loadData();
  }

  onYearChange(year: number): void {
    this.selectedYear.set(year);
    this.loadData();
  }

  get totalBurden(): number {
    const tax = this.taxResult();
    const gew = this.gewerbeResult();
    if (!tax) return 0;
    return tax.totalTax + (gew?.netGewerbesteuer ?? 0);
  }

  private loadData(): void {
    this.loading.set(true);
    const year = this.selectedYear();

    this.taxService.getTaxAssessment(year).subscribe({
      next: (result) => {
        this.taxResult.set(result);
        this.loading.set(false);
      },
      error: () => {
        this.taxResult.set(null);
        this.loading.set(false);
      },
    });

    this.taxService.getGewerbesteuer(year).subscribe({
      next: (result) => this.gewerbeResult.set(result),
      error: () => this.gewerbeResult.set(null),
    });
  }
}
