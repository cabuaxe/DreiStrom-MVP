import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ComplianceEvent } from '../models/calendar.model';

@Injectable({ providedIn: 'root' })
export class CalendarService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/calendar`;

  getEvents(year: number): Observable<ComplianceEvent[]> {
    return this.http.get<ComplianceEvent[]>(`${this.baseUrl}/events`, {
      params: { year: year.toString() },
    });
  }

  getUpcoming(): Observable<ComplianceEvent[]> {
    return this.http.get<ComplianceEvent[]>(`${this.baseUrl}/upcoming`);
  }

  generateYear(year: number): Observable<ComplianceEvent[]> {
    return this.http.post<ComplianceEvent[]>(`${this.baseUrl}/generate`, null, {
      params: { year: year.toString() },
    });
  }

  completeEvent(id: number): Observable<ComplianceEvent> {
    return this.http.post<ComplianceEvent>(`${this.baseUrl}/events/${id}/complete`, null);
  }

  getICalUrl(year: number): string {
    return `${this.baseUrl}/export.ics?year=${year}`;
  }
}
