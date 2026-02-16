import { Component, input, output, computed } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ComplianceEvent, CalendarDay, CalendarWeek } from '../../models/calendar.model';
import { EventBadgeComponent } from '../event-badge/event-badge.component';

@Component({
  selector: 'app-month-view',
  imports: [MatIconModule, MatTooltipModule, EventBadgeComponent],
  templateUrl: './month-view.component.html',
  styleUrl: './month-view.component.scss',
})
export class MonthViewComponent {
  readonly currentDate = input.required<Date>();
  readonly events = input.required<ComplianceEvent[]>();
  readonly eventClicked = output<ComplianceEvent>();

  readonly weekdays = ['Mo', 'Di', 'Mi', 'Do', 'Fr', 'Sa', 'So'];

  readonly weeks = computed<CalendarWeek[]>(() => {
    const date = this.currentDate();
    const allEvents = this.events();
    const year = date.getFullYear();
    const month = date.getMonth();

    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);

    // Find Monday of the first week
    const start = new Date(firstDay);
    const dayOfWeek = start.getDay();
    const diff = dayOfWeek === 0 ? -6 : 1 - dayOfWeek;
    start.setDate(start.getDate() + diff);

    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const weeks: CalendarWeek[] = [];
    const current = new Date(start);

    while (current <= lastDay || current.getDay() !== 1) {
      const days: CalendarDay[] = [];
      for (let i = 0; i < 7; i++) {
        const dayDate = new Date(current);
        const dateStr = this.formatDate(dayDate);
        days.push({
          date: dayDate,
          isCurrentMonth: dayDate.getMonth() === month,
          isToday: dayDate.getTime() === today.getTime(),
          events: allEvents.filter(e => e.dueDate === dateStr),
        });
        current.setDate(current.getDate() + 1);
      }
      weeks.push({ days });
      if (current.getMonth() !== month && current.getDay() === 1) break;
    }

    return weeks;
  });

  onEventClick(event: ComplianceEvent): void {
    this.eventClicked.emit(event);
  }

  private formatDate(d: Date): string {
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
