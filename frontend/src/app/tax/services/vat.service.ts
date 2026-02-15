import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { VatReturn } from '../models/vat-return.model';

@Injectable({ providedIn: 'root' })
export class VatService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1`;

  listReturns(year: number): Observable<VatReturn[]> {
    return this.http.get<VatReturn[]>(`${this.baseUrl}/vat/returns`, {
      params: { year: year.toString() },
    });
  }

  getReturn(id: number): Observable<VatReturn> {
    return this.http.get<VatReturn>(`${this.baseUrl}/vat/returns/${id}`);
  }

  submitReturn(id: number): Observable<VatReturn> {
    return this.http.post<VatReturn>(`${this.baseUrl}/vat/returns/${id}/submit`, {});
  }

  getElsterXmlUrl(periodId: number): string {
    return `${this.baseUrl}/tax/export/elster/vat/${periodId}`;
  }

  getElsterCsvUrl(periodId: number): string {
    return `${this.baseUrl}/tax/export/elster/vat/${periodId}/csv`;
  }
}
