import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ExpenseEntryResponse,
  CreateExpenseEntryRequest,
  UpdateExpenseEntryRequest,
} from '../models/expense-entry.model';
import { DepreciationYearEntry } from '../models/depreciation.model';

@Injectable({ providedIn: 'root' })
export class ExpenseService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/expenses`;

  list(category?: string, fromDate?: string, toDate?: string): Observable<ExpenseEntryResponse[]> {
    let params = new HttpParams();
    if (category) params = params.set('category', category);
    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);
    return this.http.get<ExpenseEntryResponse[]>(this.baseUrl, { params });
  }

  getById(id: number): Observable<ExpenseEntryResponse> {
    return this.http.get<ExpenseEntryResponse>(`${this.baseUrl}/${id}`);
  }

  create(request: CreateExpenseEntryRequest): Observable<ExpenseEntryResponse> {
    return this.http.post<ExpenseEntryResponse>(this.baseUrl, request);
  }

  update(id: number, request: UpdateExpenseEntryRequest): Observable<ExpenseEntryResponse> {
    return this.http.put<ExpenseEntryResponse>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  getDepreciationSchedule(expenseId: number): Observable<DepreciationYearEntry[]> {
    return this.http.get<DepreciationYearEntry[]>(`${this.baseUrl}/${expenseId}/depreciation`);
  }
}
