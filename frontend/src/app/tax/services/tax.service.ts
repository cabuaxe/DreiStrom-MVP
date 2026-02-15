import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  TaxCalculationResult,
  GewerbesteuerResult,
  TaxReserveRecommendation,
  EuerResult,
  DualStreamEuer,
} from '../models/tax.model';

@Injectable({ providedIn: 'root' })
export class TaxService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1`;

  getTaxAssessment(year: number): Observable<TaxCalculationResult> {
    return this.http.get<TaxCalculationResult>(`${this.baseUrl}/tax/assessment`, {
      params: { year: year.toString() },
    });
  }

  getGewerbesteuer(year: number): Observable<GewerbesteuerResult> {
    return this.http.get<GewerbesteuerResult>(`${this.baseUrl}/tax/gewerbesteuer`, {
      params: { year: year.toString() },
    });
  }

  getTaxReserve(year: number, alreadyReserved: number = 0): Observable<TaxReserveRecommendation> {
    return this.http.get<TaxReserveRecommendation>(`${this.baseUrl}/tax/reserve`, {
      params: { year: year.toString(), alreadyReserved: alreadyReserved.toString() },
    });
  }

  getEuer(stream: 'FREIBERUF' | 'GEWERBE', year: number): Observable<EuerResult> {
    return this.http.get<EuerResult>(`${this.baseUrl}/bookkeeping/eur/${stream}/${year}`);
  }

  getDualEuer(year: number): Observable<DualStreamEuer> {
    return this.http.get<DualStreamEuer>(`${this.baseUrl}/bookkeeping/eur/dual/${year}`);
  }

  getEuerPdfUrl(stream: 'FREIBERUF' | 'GEWERBE', year: number): string {
    return `${this.baseUrl}/bookkeeping/eur/${stream}/${year}/pdf`;
  }

  getDualEuerPdfUrl(year: number): string {
    return `${this.baseUrl}/bookkeeping/eur/dual/${year}/pdf`;
  }
}
