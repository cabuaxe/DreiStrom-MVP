export type PeriodType = 'MONTHLY' | 'QUARTERLY' | 'ANNUAL';
export type VatReturnStatus = 'DRAFT' | 'SUBMITTED' | 'ACCEPTED' | 'CORRECTED';

export interface VatReturn {
  id: number;
  year: number;
  periodType: PeriodType;
  periodNumber: number;
  outputVat: number;
  inputVat: number;
  netPayable: number;
  status: VatReturnStatus;
  submissionDate: string | null;
  createdAt: string;
  updatedAt: string;
}
