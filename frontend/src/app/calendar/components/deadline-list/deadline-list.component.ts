import { Component, input, output, computed } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { ComplianceEvent } from '../../models/calendar.model';

@Component({
  selector: 'app-deadline-list',
  imports: [DatePipe, MatCardModule, MatIconModule, MatButtonModule, MatChipsModule],
  templateUrl: './deadline-list.component.html',
  styleUrl: './deadline-list.component.scss',
})
export class DeadlineListComponent {
  readonly events = input.required<ComplianceEvent[]>();
  readonly loading = input(false);
  readonly completeClicked = output<ComplianceEvent>();

  readonly sortedEvents = computed(() => {
    return [...this.events()]
      .filter(e => e.status !== 'COMPLETED' && e.status !== 'CANCELLED')
      .sort((a, b) => a.dueDate.localeCompare(b.dueDate));
  });

  daysUntil(dueDate: string): number {
    const due = new Date(dueDate + 'T00:00:00');
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return Math.ceil((due.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
  }

  countdownLabel(dueDate: string): string {
    const days = this.daysUntil(dueDate);
    if (days < 0) return `${Math.abs(days)} Tage überfällig`;
    if (days === 0) return 'Heute fällig';
    if (days === 1) return 'Morgen fällig';
    return `${days} Tage`;
  }

  urgencyClass(dueDate: string): string {
    const days = this.daysUntil(dueDate);
    if (days < 0) return 'overdue';
    if (days <= 3) return 'urgent';
    if (days <= 7) return 'soon';
    return 'normal';
  }

  statusIcon(status: string): string {
    switch (status) {
      case 'OVERDUE': return 'error';
      case 'DUE': return 'warning';
      case 'UPCOMING': return 'schedule';
      default: return 'event';
    }
  }

  statusColor(status: string): string {
    switch (status) {
      case 'OVERDUE': return 'warn';
      case 'DUE': return 'accent';
      default: return 'primary';
    }
  }

  eventTypeLabel(type: string): string {
    const labels: Record<string, string> = {
      UST_VA: 'USt-VA',
      EST_VORAUSZAHLUNG: 'ESt-Vorauszahlung',
      GEWST_VORAUSZAHLUNG: 'GewSt-Vorauszahlung',
      EUER_FILING: 'EÜR-Abgabe',
      EST_DECLARATION: 'Einkommensteuererklärung',
      GEWST_DECLARATION: 'Gewerbesteuererklärung',
      UST_DECLARATION: 'Umsatzsteuererklärung',
      ZM_REPORT: 'Zusammenfassende Meldung',
      SOCIAL_INSURANCE: 'Sozialversicherung',
      CUSTOM: 'Termin',
    };
    return labels[type] ?? type;
  }

  onComplete(event: ComplianceEvent): void {
    this.completeClicked.emit(event);
  }
}
