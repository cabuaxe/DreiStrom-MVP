import { Component, input } from '@angular/core';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ComplianceEvent, ComplianceEventType } from '../../models/calendar.model';

@Component({
  selector: 'app-event-badge',
  imports: [MatTooltipModule],
  templateUrl: './event-badge.component.html',
  styleUrl: './event-badge.component.scss',
})
export class EventBadgeComponent {
  readonly event = input.required<ComplianceEvent>();

  get colorClass(): string {
    return this.eventTypeColorMap[this.event().eventType] ?? 'custom';
  }

  get shortLabel(): string {
    return this.eventTypeLabels[this.event().eventType] ?? this.event().title;
  }

  get statusIcon(): string {
    switch (this.event().status) {
      case 'COMPLETED': return 'done';
      case 'OVERDUE': return 'error';
      case 'DUE': return 'warning';
      default: return '';
    }
  }

  private readonly eventTypeColorMap: Record<ComplianceEventType, string> = {
    UST_VA: 'tax',
    EST_VORAUSZAHLUNG: 'tax',
    GEWST_VORAUSZAHLUNG: 'tax',
    EUER_FILING: 'tax',
    EST_DECLARATION: 'tax',
    GEWST_DECLARATION: 'tax',
    UST_DECLARATION: 'vat',
    ZM_REPORT: 'vat',
    SOCIAL_INSURANCE: 'social',
    CUSTOM: 'custom',
  };

  private readonly eventTypeLabels: Record<ComplianceEventType, string> = {
    UST_VA: 'USt-VA',
    EST_VORAUSZAHLUNG: 'ESt-VA',
    GEWST_VORAUSZAHLUNG: 'GewSt-VA',
    EUER_FILING: 'EÜR',
    EST_DECLARATION: 'ESt',
    GEWST_DECLARATION: 'GewSt',
    UST_DECLARATION: 'USt',
    ZM_REPORT: 'ZM',
    SOCIAL_INSURANCE: 'SV',
    CUSTOM: 'Termin',
  };
}
