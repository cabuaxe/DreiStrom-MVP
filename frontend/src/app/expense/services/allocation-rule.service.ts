import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AllocationRuleResponse,
  CreateAllocationRuleRequest,
  UpdateAllocationRuleRequest,
} from '../models/allocation-rule.model';

@Injectable({ providedIn: 'root' })
export class AllocationRuleService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/allocation-rules`;

  list(): Observable<AllocationRuleResponse[]> {
    return this.http.get<AllocationRuleResponse[]>(this.baseUrl);
  }

  getById(id: number): Observable<AllocationRuleResponse> {
    return this.http.get<AllocationRuleResponse>(`${this.baseUrl}/${id}`);
  }

  create(request: CreateAllocationRuleRequest): Observable<AllocationRuleResponse> {
    return this.http.post<AllocationRuleResponse>(this.baseUrl, request);
  }

  update(id: number, request: UpdateAllocationRuleRequest): Observable<AllocationRuleResponse> {
    return this.http.put<AllocationRuleResponse>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
