export interface UserFeatureFlags {
  year: number;

  // Stream activity
  hasEmploymentIncome: boolean;
  hasFreiberufIncome: boolean;
  hasGewerbeIncome: boolean;
  hasMultipleStreams: boolean;

  // Dashboard cards
  showKleinunternehmerCard: boolean;
  showAbfaerbungCard: boolean;
  showGewerbesteuerCard: boolean;
  showBilanzierungWarnings: boolean;
  showSocialInsuranceCard: boolean;
  showMandatoryFilingCard: boolean;
  showArbZGCard: boolean;

  // Module visibility
  showInvoicingModule: boolean;
  showExpenseAllocation: boolean;
  showVorauszahlungen: boolean;
  showTaxEstimation: boolean;

  // Advanced features
  showOssRules: boolean;
  showZmReporting: boolean;

  // 1=micro, 2=small, 3=medium
  complexityLevel: number;
}
