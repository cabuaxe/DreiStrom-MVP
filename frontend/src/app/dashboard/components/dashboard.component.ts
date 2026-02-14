import { Component } from '@angular/core';
import { AbfaerbungCardComponent } from './abfaerbung-card/abfaerbung-card.component';

@Component({
  selector: 'app-dashboard',
  imports: [AbfaerbungCardComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent {}
