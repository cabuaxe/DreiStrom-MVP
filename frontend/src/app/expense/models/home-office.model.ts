export type HomeOfficeMethod = 'ARBEITSZIMMER' | 'PAUSCHALE';

export interface HomeOfficeResult {
  method: HomeOfficeMethod;
  deduction: number;
}

/** Client-side calculation — mirrors backend HomeOfficeService logic */
export const PAUSCHALE_PER_DAY = 6;
export const PAUSCHALE_MAX_YEAR = 1260;
export const PAUSCHALE_MAX_DAYS = 210;
