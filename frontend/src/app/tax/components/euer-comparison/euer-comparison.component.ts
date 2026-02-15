import { Component, inject, signal, OnInit } from '@angular/core';
import { CurrencyPipe, NgTemplateOutlet } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { TaxService } from '../../services/tax.service';
import { DualStreamEuer } from '../../models/tax.model';

@Component({
  selector: 'app-euer-comparison',
  imports: [
    CurrencyPipe,
    NgTemplateOutlet,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatDividerModule,
    MatSelectModule,
    MatFormFieldModule,
  ],
  templateUrl: './euer-comparison.component.html',
  styleUrl: './euer-comparison.component.scss',
})
export class EuerComparisonComponent implements OnInit {
  private readonly taxService = inject(TaxService);

  readonly dualEuer = signal<DualStreamEuer | null>(null);
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

  downloadPdf(stream: 'FREIBERUF' | 'GEWERBE'): void {
    window.open(this.taxService.getEuerPdfUrl(stream, this.selectedYear()), '_blank');
  }

  downloadDualPdf(): void {
    window.open(this.taxService.getDualEuerPdfUrl(this.selectedYear()), '_blank');
  }

  profitClass(profit: number): string {
    return profit >= 0 ? 'profit-positive' : 'profit-negative';
  }

  private loadData(): void {
    this.loading.set(true);
    this.taxService.getDualEuer(this.selectedYear()).subscribe({
      next: (data) => {
        this.dualEuer.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.dualEuer.set(null);
        this.loading.set(false);
      },
    });
  }
}
