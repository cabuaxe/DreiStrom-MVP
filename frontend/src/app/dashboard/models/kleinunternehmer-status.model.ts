export interface KleinunternehmerStatus {
  year: number;
  currentRevenue: number;
  currentYearLimit: number;
  currentRatio: number;
  projectedRevenue: number;
  projectedYearLimit: number;
  projectedRatio: number;
  currentExceeded: boolean;
  projectedExceeded: boolean;
}
