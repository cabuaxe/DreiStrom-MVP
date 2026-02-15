import { Component, inject, signal, OnInit } from '@angular/core';
import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { VorauszahlungSchedule } from '../../models/tax.model';

@Component({
  selector: 'app-vorauszahlungen',
  imports: [
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
    MatCardModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatSelectModule,
    MatFormFieldModule,
  ],
  templateUrl: './vorauszahlungen.component.html',
  styleUrl: './vorauszahlungen.component.scss',
})
export class VorauszahlungenComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1`;

  readonly schedule = signal<VorauszahlungSchedule | null>(null);
  readonly loading = signal(true);
  readonly selectedYear = signal(new Date().getFullYear());

  get yearOptions(): number[] {
    const current = new Date().getFullYear();
    return [current - 1, current, current + 1];
  }

  ngOnInit(): void {
    this.loadSchedule();
  }

  onYearChange(year: number): void {
    this.selectedYear.set(year);
    this.loadSchedule();
  }

  statusLabel(status: string): string {
    switch (status) {
      case 'PAID': return 'Bezahlt';
      case 'OVERDUE': return 'Überfällig';
      default: return 'Ausstehend';
    }
  }

  statusIcon(status: string): string {
    switch (status) {
      case 'PAID': return 'check_circle';
      case 'OVERDUE': return 'error';
      default: return 'schedule';
    }
  }

  quarterLabel(quarter: number): string {
    switch (quarter) {
      case 1: return 'Q1 (10. März)';
      case 2: return 'Q2 (10. Juni)';
      case 3: return 'Q3 (10. Sept.)';
      case 4: return 'Q4 (10. Dez.)';
      default: return `Q${quarter}`;
    }
  }

  private loadSchedule(): void {
    this.loading.set(true);
    this.http
      .get<VorauszahlungSchedule>(`${this.baseUrl}/tax/vorauszahlungen`, {
        params: { year: this.selectedYear().toString() },
      })
      .subscribe({
        next: (data) => {
          this.schedule.set(data);
          this.loading.set(false);
        },
        error: () => {
          this.schedule.set(null);
          this.loading.set(false);
        },
      });
  }
}
