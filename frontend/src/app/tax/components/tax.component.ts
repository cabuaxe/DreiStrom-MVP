import { Component } from '@angular/core';
import { VatListComponent } from './vat-list/vat-list.component';

@Component({
  selector: 'app-tax',
  imports: [VatListComponent],
  template: `
    <h2>Steuer</h2>
    <app-vat-list />
  `,
  styles: `
    :host { display: block; }
    h2 { margin: 0 0 16px; font-size: 1.5rem; font-weight: 500; }
  `,
})
export class TaxComponent {}
