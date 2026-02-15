import { Component } from '@angular/core';
import { AbfaerbungCardComponent } from './abfaerbung-card/abfaerbung-card.component';
import { KleinunternehmerCardComponent } from './kleinunternehmer-card/kleinunternehmer-card.component';
import { SocialInsuranceCardComponent } from './social-insurance-card/social-insurance-card.component';

@Component({
  selector: 'app-dashboard',
  imports: [AbfaerbungCardComponent, KleinunternehmerCardComponent, SocialInsuranceCardComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent {}
