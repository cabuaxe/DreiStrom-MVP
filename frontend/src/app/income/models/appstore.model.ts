export type PayoutPlatform = 'APPLE' | 'GOOGLE';

export interface PayoutEntry {
  id: number;
  platform: PayoutPlatform;
  reportDate: string;
  region: string;
  currency: string;
  grossRevenue: number;
  commission: number;
  netRevenue: number;
  vat: number;
  productId: string;
  productName: string;
  quantity: number;
  importBatchId: string;
}

export interface ImportSummary {
  platform: PayoutPlatform;
  totalRecords: number;
  totalGross: number;
  totalCommission: number;
  totalNet: number;
  entries: PayoutEntry[];
}
