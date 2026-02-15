import { Component } from '@angular/core';
import { AbfaerbungCardComponent } from './abfaerbung-card/abfaerbung-card.component';
import { KleinunternehmerCardComponent } from './kleinunternehmer-card/kleinunternehmer-card.component';

@Component({
  selector: 'app-dashboard',
  imports: [AbfaerbungCardComponent, KleinunternehmerCardComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent {}
