export type ComplianceEventType =
  | 'UST_VA'
  | 'EST_VORAUSZAHLUNG'
  | 'GEWST_VORAUSZAHLUNG'
  | 'EUER_FILING'
  | 'EST_DECLARATION'
  | 'GEWST_DECLARATION'
  | 'UST_DECLARATION'
  | 'ZM_REPORT'
  | 'SOCIAL_INSURANCE'
  | 'CUSTOM';

export type ComplianceEventStatus =
  | 'UPCOMING'
  | 'DUE'
  | 'OVERDUE'
  | 'COMPLETED'
  | 'CANCELLED';

export interface ComplianceEvent {
  id: number;
  eventType: ComplianceEventType;
  title: string;
  description: string;
  dueDate: string;
  status: ComplianceEventStatus;
  taxPeriodId: number | null;
  completedAt: string | null;
}

export interface ReminderConfig {
  channels: string[];
  daysBefore: number[];
}

export interface CalendarDay {
  date: Date;
  isCurrentMonth: boolean;
  isToday: boolean;
  events: ComplianceEvent[];
}

export interface CalendarWeek {
  days: CalendarDay[];
}
