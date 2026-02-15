import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AbfaerbungStatus } from '../models/abfaerbung-status.model';
import { KleinunternehmerStatus } from '../models/kleinunternehmer-status.model';

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
}
