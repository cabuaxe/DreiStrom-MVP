import { Component } from '@angular/core';
import { VatListComponent } from './vat-list/vat-list.component';
import { TaxEstimatorComponent } from './tax-estimator/tax-estimator.component';
import { TaxReserveComponent } from './tax-reserve/tax-reserve.component';
import { EuerComparisonComponent } from './euer-comparison/euer-comparison.component';
import { VorauszahlungenComponent } from './vorauszahlungen/vorauszahlungen.component';

@Component({
  selector: 'app-tax',
  imports: [
    VatListComponent,
    TaxEstimatorComponent,
    TaxReserveComponent,
    EuerComparisonComponent,
    VorauszahlungenComponent,
  ],
  templateUrl: './tax.component.html',
  styleUrl: './tax.component.scss',
})
export class TaxComponent {}
