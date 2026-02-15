import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AbfaerbungStatus } from '../models/abfaerbung-status.model';
import { KleinunternehmerStatus } from '../models/kleinunternehmer-status.model';
import { SocialInsuranceStatus } from '../models/social-insurance-status.model';
import { GewerbesteuerThreshold } from '../models/gewerbesteuer-threshold.model';
import { MandatoryFilingStatus } from '../models/mandatory-filing-status.model';
import { ArbZGStatus } from '../models/arbzg-status.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1`;

  getAbfaerbungStatus(year?: number): Observable<AbfaerbungStatus> {
    const params: Record<string, string> = {};
    if (year) {
      params['year'] = year.toString();
    }
    return this.http.get<AbfaerbungStatus>(`${this.baseUrl}/dashboard/abfaerbung`, { params });
  }

  getKleinunternehmerStatus(year?: number): Observable<KleinunternehmerStatus> {
    const params: Record<string, string> = {};
    if (year) {
      params['year'] = year.toString();
    }
    return this.http.get<KleinunternehmerStatus>(`${this.baseUrl}/vat/kleinunternehmer`, { params });
  }

  getSocialInsuranceStatus(year?: number): Observable<SocialInsuranceStatus> {
    const params: Record<string, string> = {};
    if (year) {
      params['year'] = year.toString();
    }
    return this.http.get<SocialInsuranceStatus>(`${this.baseUrl}/social-insurance/status`, { params });
  }

  getGewerbesteuerThreshold(year?: number): Observable<GewerbesteuerThreshold> {
    const params: Record<string, string> = {};
    if (year) {
      params['year'] = year.toString();
    }
    return this.http.get<GewerbesteuerThreshold>(`${this.baseUrl}/dashboard/gewerbesteuer`, { params });
  }

  getMandatoryFilingStatus(year?: number): Observable<MandatoryFilingStatus> {
    const params: Record<string, string> = {};
    if (year) {
      params['year'] = year.toString();
    }
    return this.http.get<MandatoryFilingStatus>(`${this.baseUrl}/dashboard/mandatory-filing`, { params });
  }

  getArbZGStatus(year?: number): Observable<ArbZGStatus> {
    const params: Record<string, string> = {};
    if (year) {
      params['year'] = year.toString();
    }
    return this.http.get<ArbZGStatus>(`${this.baseUrl}/dashboard/arbzg`, { params });
  }
}
