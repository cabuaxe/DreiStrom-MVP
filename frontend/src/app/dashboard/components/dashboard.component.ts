import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { AbfaerbungCardComponent } from './abfaerbung-card/abfaerbung-card.component';
import { KleinunternehmerCardComponent } from './kleinunternehmer-card/kleinunternehmer-card.component';
import { SocialInsuranceCardComponent } from './social-insurance-card/social-insurance-card.component';
import { GewerbesteuerCardComponent } from './gewerbesteuer-card/gewerbesteuer-card.component';
import { MandatoryFilingCardComponent } from './mandatory-filing-card/mandatory-filing-card.component';
import { ArbZGCardComponent } from './arbzg-card/arbzg-card.component';
import { FeatureFlagService } from '../../common/services/feature-flag.service';

@Component({
  selector: 'app-dashboard',
  imports: [
    AbfaerbungCardComponent,
    KleinunternehmerCardComponent,
    SocialInsuranceCardComponent,
    GewerbesteuerCardComponent,
    MandatoryFilingCardComponent,
    ArbZGCardComponent,
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit, OnDestroy {
  private readonly featureFlagService = inject(FeatureFlagService);
  readonly flags = this.featureFlagService.flags;
  readonly loaded = this.featureFlagService.loaded;

  ngOnInit(): void {
    this.featureFlagService.initialize();
  }

  ngOnDestroy(): void {
    this.featureFlagService.destroy();
  }
}
