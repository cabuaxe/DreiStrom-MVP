import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AbfaerbungStatus } from '../models/abfaerbung-status.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/dashboard`;

  getAbfaerbungStatus(year?: number): Observable<AbfaerbungStatus> {
    const params: Record<string, string> = {};
    if (year) {
      params['year'] = year.toString();
    }
    return this.http.get<AbfaerbungStatus>(`${this.baseUrl}/abfaerbung`, { params });
  }
}
