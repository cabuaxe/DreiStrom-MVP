import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  InvoiceResponse,
  CreateInvoiceRequest,
  UpdateInvoiceRequest,
  UpdateInvoiceStatusRequest,
  InvoiceStream,
  InvoiceStatus,
} from '../models/invoice.model';

@Injectable({ providedIn: 'root' })
export class InvoicingService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/invoices`;

  list(
    streamType?: InvoiceStream,
    status?: InvoiceStatus,
    clientId?: number,
    fromDate?: string,
    toDate?: string,
  ): Observable<InvoiceResponse[]> {
    let params = new HttpParams();
    if (streamType) params = params.set('streamType', streamType);
    if (status) params = params.set('status', status);
    if (clientId) params = params.set('clientId', clientId.toString());
    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);
    return this.http.get<InvoiceResponse[]>(this.baseUrl, { params });
  }

  getById(id: number): Observable<InvoiceResponse> {
    return this.http.get<InvoiceResponse>(`${this.baseUrl}/${id}`);
  }

  create(request: CreateInvoiceRequest): Observable<InvoiceResponse> {
    return this.http.post<InvoiceResponse>(this.baseUrl, request);
  }

  update(id: number, request: UpdateInvoiceRequest): Observable<InvoiceResponse> {
    return this.http.put<InvoiceResponse>(`${this.baseUrl}/${id}`, request);
  }

  updateStatus(id: number, request: UpdateInvoiceStatusRequest): Observable<InvoiceResponse> {
    return this.http.patch<InvoiceResponse>(`${this.baseUrl}/${id}/status`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  downloadPdf(id: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${id}/pdf`, { responseType: 'blob' });
  }
}
