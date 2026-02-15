import { Component, inject, input, output, signal } from '@angular/core';
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { OnboardingService } from '../../services/onboarding.service';
import {
  DecisionChoice,
  DecisionPointResponse,
  KurDecisionResponse,
} from '../../models/onboarding.model';

@Component({
  selector: 'app-decision-panel',
  imports: [
    CurrencyPipe,
    DecimalPipe,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatDividerModule,
  ],
  templateUrl: './decision-panel.component.html',
  styleUrl: './decision-panel.component.scss',
})
export class DecisionPanelComponent {
  private readonly onboardingService = inject(OnboardingService);

  readonly decisionPoint = input.required<DecisionPointResponse>();
  readonly decided = output<DecisionChoice>();

  readonly evaluation = signal<KurDecisionResponse | null>(null);
  readonly evaluating = signal(false);

  loadEvaluation(): void {
    this.evaluating.set(true);
    this.onboardingService.evaluateKleinunternehmerFromData().subscribe({
      next: (res) => {
        this.evaluation.set(res);
        this.evaluating.set(false);
      },
      error: () => this.evaluating.set(false),
    });
  }

  selectOption(choice: DecisionChoice): void {
    this.decided.emit(choice);
  }
}
