export interface TaxCalculationResult {
  taxYear: number;
  employmentIncome: number;
  freiberufIncome: number;
  gewerbeIncome: number;
  totalGrossIncome: number;
  deductions: DeductionBreakdown;
  taxableIncome: number;
  incomeTax: number;
  solidaritaetszuschlag: number;
  totalTax: number;
  marginalRate: number;
  effectiveRate: number;
}

export interface DeductionBreakdown {
  businessExpensesFreiberuf: number;
  businessExpensesGewerbe: number;
  werbungskostenpauschale: number;
  sonderausgabenpauschale: number;
  totalDeductions: number;
}

export interface GewerbesteuerResult {
  gewerbeProfit: number;
  freibetrag: number;
  taxableProfit: number;
  steuermesszahl: number;
  steuermessbetrag: number;
  hebesatz: number;
  gewerbesteuer: number;
  paragraph35Credit: number;
  netGewerbesteuer: number;
}

export interface TaxReserveRecommendation {
  year: number;
  netSelfEmployedProfit: number;
  reserveRatePercent: number;
  monthlyReserve: number;
  annualReserve: number;
  alreadyReserved: number;
  remainingToReserve: number;
  monthsRemaining: number;
}

export interface VorauszahlungSchedule {
  year: number;
  assessmentBasis: number;
  quarterlyAmount: number;
  annualTotal: number;
  payments: QuarterPayment[];
  adjustmentSuggestion: AdjustmentSuggestion;
}

export interface QuarterPayment {
  quarter: number;
  dueDate: string;
  amount: number;
  paid: number;
  status: 'PENDING' | 'PAID' | 'OVERDUE';
}

export interface AdjustmentSuggestion {
  recommended: boolean;
  actualIncome: number;
  assessmentBasis: number;
  deviationPercent: number;
  suggestedQuarterlyAmount: number;
}

export interface EuerResult {
  taxYear: number;
  stream: 'FREIBERUF' | 'GEWERBE';
  totalIncome: number;
  directExpenses: number;
  allocatedSharedExpenses: number;
  depreciation: number;
  totalExpenses: number;
  profit: number;
}

export interface DualStreamEuer {
  freiberuf: EuerResult;
  gewerbe: EuerResult;
  combinedProfit: number;
}
