import { Component, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDividerModule } from '@angular/material/divider';

interface ReminderSetting {
  label: string;
  icon: string;
  enabled: boolean;
}

interface ReminderInterval {
  days: number;
  label: string;
  enabled: boolean;
}

@Component({
  selector: 'app-reminder-panel',
  imports: [
    MatCardModule,
    MatIconModule,
    MatSlideToggleModule,
    MatCheckboxModule,
    MatDividerModule,
  ],
  templateUrl: './reminder-panel.component.html',
  styleUrl: './reminder-panel.component.scss',
})
export class ReminderPanelComponent {
  readonly channels = signal<ReminderSetting[]>([
    { label: 'In-App Benachrichtigungen', icon: 'notifications', enabled: true },
    { label: 'E-Mail Erinnerungen', icon: 'email', enabled: true },
    { label: 'Push-Benachrichtigungen', icon: 'phone_android', enabled: false },
  ]);

  readonly intervals = signal<ReminderInterval[]>([
    { days: 14, label: '14 Tage vorher', enabled: true },
    { days: 7, label: '7 Tage vorher', enabled: true },
    { days: 3, label: '3 Tage vorher', enabled: true },
    { days: 1, label: '1 Tag vorher', enabled: true },
  ]);

  toggleChannel(index: number): void {
    const updated = [...this.channels()];
    updated[index] = { ...updated[index], enabled: !updated[index].enabled };
    this.channels.set(updated);
  }

  toggleInterval(index: number): void {
    const updated = [...this.intervals()];
    updated[index] = { ...updated[index], enabled: !updated[index].enabled };
    this.intervals.set(updated);
  }
}
