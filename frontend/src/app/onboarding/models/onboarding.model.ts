export type StepStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'BLOCKED';

export type Responsible = 'USER' | 'SYSTEM' | 'STEUERBERATER';

export type DecisionChoice = 'OPTION_A' | 'OPTION_B';

export interface DecisionPointResponse {
  id: number;
  stepId: number;
  question: string;
  optionA: string;
  optionB: string;
  recommendation: DecisionChoice;
  recommendationReason: string;
  userChoice: DecisionChoice | null;
  decidedAt: string | null;
}

export interface StepResponse {
  id: number;
  stepNumber: number;
  title: string;
  description: string;
  status: StepStatus;
  responsible: Responsible;
  dependencies: string;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
  decisionPoints: DecisionPointResponse[];
}

export interface OnboardingProgressResponse {
  totalSteps: number;
  completedSteps: number;
  inProgressSteps: number;
  blockedSteps: number;
  progressPercent: number;
  steps: StepResponse[];
}

export interface KurDecisionInput {
  projectedFreiberufRevenue: number;
  projectedGewerbeRevenue: number;
  projectedBusinessExpenses: number;
  b2bClientCount: number;
  b2cClientCount: number;
}

export interface KurDecisionResponse {
  recommendation: DecisionChoice;
  summary: string;
  projectedTotalRevenue: number;
  belowCurrentYearLimit: boolean;
  belowProjectedYearLimit: boolean;
  estimatedVorsteuerSavings: number;
  b2bRatio: number;
  prosKleinunternehmer: string[];
  consKleinunternehmer: string[];
  prosRegelbesteuerung: string[];
  consRegelbesteuerung: string[];
}
