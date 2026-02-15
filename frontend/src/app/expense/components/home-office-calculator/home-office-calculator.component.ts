import { Component, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { HomeOfficeMethod, PAUSCHALE_PER_DAY, PAUSCHALE_MAX_YEAR, PAUSCHALE_MAX_DAYS } from '../../models/home-office.model';

@Component({
  selector: 'app-home-office-calculator',
  imports: [
    FormsModule,
    CurrencyPipe,
    DecimalPipe,
    MatFormFieldModule,
    MatInputModule,
    MatButtonToggleModule,
    MatCardModule,
    MatIconModule,
  ],
  templateUrl: './home-office-calculator.component.html',
  styleUrl: './home-office-calculator.component.scss',
})
export class HomeOfficeCalculatorComponent {
  readonly selectedMethod = signal<HomeOfficeMethod>('PAUSCHALE');

  // Pauschale inputs
  readonly homeOfficeDays = signal(120);

  // Arbeitszimmer inputs
  readonly monthlyRent = signal(800);
  readonly totalArea = signal(80);
  readonly officeArea = signal(12);
  readonly monthlyUtilities = signal(200);

  // Pauschale calculation
  readonly pauschaleDeduction = computed(() => {
    const days = Math.min(this.homeOfficeDays(), PAUSCHALE_MAX_DAYS);
    return Math.min(days * PAUSCHALE_PER_DAY, PAUSCHALE_MAX_YEAR);
  });

  // Arbeitszimmer calculation
  readonly officeRatio = computed(() => {
    const total = this.totalArea();
    const office = this.officeArea();
    if (total <= 0 || office <= 0) return 0;
    return office / total;
  });

  readonly arbeitszimmerDeduction = computed(() => {
    const ratio = this.officeRatio();
    const annualRent = this.monthlyRent() * 12;
    const annualUtilities = this.monthlyUtilities() * 12;
    return (annualRent + annualUtilities) * ratio;
  });

  readonly recommendedMethod = computed<HomeOfficeMethod>(() =>
    this.arbeitszimmerDeduction() > this.pauschaleDeduction() ? 'ARBEITSZIMMER' : 'PAUSCHALE'
  );

  readonly currentDeduction = computed(() =>
    this.selectedMethod() === 'PAUSCHALE'
      ? this.pauschaleDeduction()
      : this.arbeitszimmerDeduction()
  );

  onMethodChange(method: HomeOfficeMethod): void {
    this.selectedMethod.set(method);
  }
}
