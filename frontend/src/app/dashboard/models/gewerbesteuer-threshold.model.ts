export interface GewerbesteuerThreshold {
  year: number;
  gewerbeProfit: number;
  freibetrag: number;
  freibetragExceeded: boolean;
  gewerbeRevenue: number;
  bilanzierungRevenueThreshold: number;
  bilanzierungRevenueExceeded: boolean;
  bilanzierungProfitThreshold: number;
  bilanzierungProfitExceeded: boolean;
}
