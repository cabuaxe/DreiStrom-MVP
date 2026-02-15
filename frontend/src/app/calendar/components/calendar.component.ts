import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatMenuModule } from '@angular/material/menu';
import { CalendarService } from '../services/calendar.service';
import { ComplianceEvent } from '../models/calendar.model';
import { MonthViewComponent } from './month-view/month-view.component';
import { DeadlineListComponent } from './deadline-list/deadline-list.component';
import { ReminderPanelComponent } from './reminder-panel/reminder-panel.component';

@Component({
  selector: 'app-calendar',
  imports: [
    MatButtonModule,
    MatIconModule,
    MatButtonToggleModule,
    MatMenuModule,
    MonthViewComponent,
    DeadlineListComponent,
    ReminderPanelComponent,
  ],
  templateUrl: './calendar.component.html',
  styleUrl: './calendar.component.scss',
})
export class CalendarComponent implements OnInit {
  private readonly calendarService = inject(CalendarService);

  readonly events = signal<ComplianceEvent[]>([]);
  readonly loading = signal(true);
  readonly currentDate = signal(new Date());
  readonly viewMode = signal<'month' | 'list'>('month');

  readonly currentMonthLabel = computed(() => {
    const d = this.currentDate();
    return d.toLocaleDateString('de-DE', { month: 'long', year: 'numeric' });
  });

  readonly currentYear = computed(() => this.currentDate().getFullYear());

  ngOnInit(): void {
    this.loadEvents();
  }

  onPrevMonth(): void {
    const d = this.currentDate();
    this.currentDate.set(new Date(d.getFullYear(), d.getMonth() - 1, 1));
    this.loadEvents();
  }

  onNextMonth(): void {
    const d = this.currentDate();
    this.currentDate.set(new Date(d.getFullYear(), d.getMonth() + 1, 1));
    this.loadEvents();
  }

  onToday(): void {
    this.currentDate.set(new Date());
    this.loadEvents();
  }

  onViewChange(mode: 'month' | 'list'): void {
    this.viewMode.set(mode);
  }

  onEventClicked(event: ComplianceEvent): void {
    // Could open detail dialog — for now, log
    console.log('Event clicked:', event);
  }

  onCompleteEvent(event: ComplianceEvent): void {
    this.calendarService.completeEvent(event.id).subscribe({
      next: (updated) => {
        const list = this.events().map(e => e.id === updated.id ? updated : e);
        this.events.set(list);
      },
    });
  }

  onGenerateEvents(): void {
    this.calendarService.generateYear(this.currentYear()).subscribe({
      next: () => this.loadEvents(),
    });
  }

  onDownloadICal(): void {
    window.open(this.calendarService.getICalUrl(this.currentYear()), '_blank');
  }

  private loadEvents(): void {
    this.loading.set(true);
    this.calendarService.getEvents(this.currentYear()).subscribe({
      next: (data) => {
        this.events.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.events.set([]);
        this.loading.set(false);
      },
    });
  }
}
