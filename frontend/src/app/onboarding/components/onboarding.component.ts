import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DecisionPanelComponent } from './decision-panel/decision-panel.component';
import { OnboardingService } from '../services/onboarding.service';
import {
  DecisionChoice,
  OnboardingProgressResponse,
  Responsible,
  StepResponse,
  StepStatus,
} from '../models/onboarding.model';

@Component({
  selector: 'app-onboarding',
  imports: [
    MatExpansionModule,
    MatProgressBarModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    DecisionPanelComponent,
  ],
  templateUrl: './onboarding.component.html',
  styleUrl: './onboarding.component.scss',
})
export class OnboardingComponent implements OnInit {
  private readonly onboardingService = inject(OnboardingService);
  private readonly snackBar = inject(MatSnackBar);

  readonly progress = signal<OnboardingProgressResponse | null>(null);
  readonly loading = signal(true);
  readonly actionLoading = signal<number | null>(null);
  readonly expandedStep = signal<number | null>(null);

  readonly completedCount = computed(() => this.progress()?.completedSteps ?? 0);
  readonly totalCount = computed(() => this.progress()?.totalSteps ?? 0);
  readonly progressPercent = computed(() => this.progress()?.progressPercent ?? 0);

  ngOnInit(): void {
    this.loadProgress();
  }

  onStepOpened(stepNumber: number): void {
    this.expandedStep.set(stepNumber);
  }

  onStepClosed(stepNumber: number): void {
    if (this.expandedStep() === stepNumber) {
      this.expandedStep.set(null);
    }
  }

  startStep(step: StepResponse): void {
    this.actionLoading.set(step.stepNumber);
    this.onboardingService.startStep(step.stepNumber).subscribe({
      next: () => {
        this.actionLoading.set(null);
        this.loadProgress();
      },
      error: (err) => {
        this.actionLoading.set(null);
        const msg = err.error?.message || 'Schritt konnte nicht gestartet werden';
        this.snackBar.open(msg, 'OK', { duration: 4000 });
      },
    });
  }

  completeStep(step: StepResponse): void {
    this.actionLoading.set(step.stepNumber);
    this.onboardingService.completeStep(step.stepNumber).subscribe({
      next: () => {
        this.actionLoading.set(null);
        this.snackBar.open(`„${step.title}" abgeschlossen`, 'OK', { duration: 2000 });
        this.loadProgress();
      },
      error: () => {
        this.actionLoading.set(null);
        this.snackBar.open('Fehler beim Abschließen', 'OK', { duration: 3000 });
      },
    });
  }

  onDecision(step: StepResponse, choice: DecisionChoice): void {
    const dp = step.decisionPoints?.[0];
    if (!dp) return;
    this.actionLoading.set(step.stepNumber);
    this.onboardingService.makeDecision(dp.id, choice).subscribe({
      next: () => {
        this.actionLoading.set(null);
        this.snackBar.open('Entscheidung gespeichert', 'OK', { duration: 2000 });
        this.loadProgress();
      },
      error: () => {
        this.actionLoading.set(null);
        this.snackBar.open('Fehler beim Speichern der Entscheidung', 'OK', { duration: 3000 });
      },
    });
  }

  generateDocument(type: string): void {
    this.snackBar.open(
      `${type}-Generierung wird in einem zukünftigen Update verfügbar sein.`,
      'OK',
      { duration: 4000 },
    );
  }

  parseDependencies(deps: string | null): number[] {
    if (!deps) return [];
    try {
      const parsed = JSON.parse(deps);
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }

  areDependenciesMet(step: StepResponse): boolean {
    const deps = this.parseDependencies(step.dependencies);
    if (deps.length === 0) return true;
    const steps = this.progress()?.steps ?? [];
    return deps.every((dep) => {
      const depStep = steps.find((s) => s.stepNumber === dep);
      return depStep?.status === 'COMPLETED';
    });
  }

  getBlockingStepTitles(step: StepResponse): string[] {
    const deps = this.parseDependencies(step.dependencies);
    const steps = this.progress()?.steps ?? [];
    return deps
      .map((dep) => steps.find((s) => s.stepNumber === dep))
      .filter((s) => s && s.status !== 'COMPLETED')
      .map((s) => `Schritt ${s!.stepNumber}: ${s!.title}`);
  }

  canStart(step: StepResponse): boolean {
    return step.status === 'NOT_STARTED' && this.areDependenciesMet(step);
  }

  canComplete(step: StepResponse): boolean {
    return step.status === 'IN_PROGRESS';
  }

  statusIcon(status: StepStatus): string {
    switch (status) {
      case 'COMPLETED': return 'check_circle';
      case 'IN_PROGRESS': return 'pending';
      case 'BLOCKED': return 'lock';
      default: return 'radio_button_unchecked';
    }
  }

  statusLabel(status: StepStatus): string {
    switch (status) {
      case 'COMPLETED': return 'Abgeschlossen';
      case 'IN_PROGRESS': return 'In Bearbeitung';
      case 'BLOCKED': return 'Blockiert';
      default: return 'Offen';
    }
  }

  responsibleLabel(responsible: Responsible): string {
    switch (responsible) {
      case 'USER': return 'Du';
      case 'SYSTEM': return 'System';
      case 'STEUERBERATER': return 'Steuerberater';
    }
  }

  responsibleIcon(responsible: Responsible): string {
    switch (responsible) {
      case 'USER': return 'person';
      case 'SYSTEM': return 'settings';
      case 'STEUERBERATER': return 'person_search';
    }
  }

  hasDecisionPoint(step: StepResponse): boolean {
    return (step.decisionPoints?.length ?? 0) > 0;
  }

  isDocumentStep(stepNumber: number): boolean {
    return stepNumber === 8 || stepNumber === 9;
  }

  documentLabel(stepNumber: number): string {
    return stepNumber === 8
      ? 'Arbeitgeber-Mitteilung'
      : 'Krankenkassen-Brief';
  }

  private loadProgress(): void {
    this.loading.set(true);
    this.onboardingService.getProgress().subscribe({
      next: (data) => {
        this.progress.set(data);
        this.loading.set(false);
      },
      error: () => {
        // First load failed — likely no checklist yet, initialize
        this.onboardingService.initialize().subscribe({
          next: (data) => {
            this.progress.set(data);
            this.loading.set(false);
          },
          error: () => {
            this.loading.set(false);
            this.snackBar.open('Onboarding konnte nicht geladen werden', 'OK', { duration: 4000 });
          },
        });
      },
    });
  }
}
