export type RiskLevel = 'SAFE' | 'WARNING' | 'CRITICAL';

export interface MonthlyBreakdown {
  month: number;
  employmentHoursWeekly: number;
  selfEmployedHoursWeekly: number;
  employmentIncome: number;
  selfEmployedIncome: number;
}

export interface SocialInsuranceStatus {
  year: number;
  avgEmploymentHoursWeekly: number;
  avgSelfEmployedHoursWeekly: number;
  totalEmploymentIncome: number;
  totalSelfEmployedIncome: number;
  hoursRiskFlag: boolean;
  incomeRiskFlag: boolean;
  riskLevel: RiskLevel;
  riskMessage: string;
  monthlyData: MonthlyBreakdown[];
}
