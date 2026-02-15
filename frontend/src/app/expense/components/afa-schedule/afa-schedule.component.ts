import { Component, input } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { DepreciationYearEntry } from '../../models/depreciation.model';

@Component({
  selector: 'app-afa-schedule',
  imports: [CurrencyPipe, MatTableModule, MatCardModule, MatIconModule],
  templateUrl: './afa-schedule.component.html',
  styleUrl: './afa-schedule.component.scss',
})
export class AfaScheduleComponent {
  readonly schedule = input.required<DepreciationYearEntry[]>();
  readonly assetName = input<string>('');

  readonly displayedColumns = ['year', 'depreciation', 'remainingBookValue'];
}
