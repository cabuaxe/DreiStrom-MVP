import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PayoutEntry, PayoutPlatform } from '../models/appstore.model';

@Injectable({ providedIn: 'root' })
export class AppStoreService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/appstore`;

  importAppleCsv(file: File, smallBusinessProgram: boolean): Observable<PayoutEntry[]> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('smallBusinessProgram', String(smallBusinessProgram));
    return this.http.post<PayoutEntry[]>(`${this.baseUrl}/apple/import`, formData);
  }

  importGoogleCsv(file: File, reducedFeeProgram: boolean): Observable<PayoutEntry[]> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('reducedFeeProgram', String(reducedFeeProgram));
    return this.http.post<PayoutEntry[]>(`${this.baseUrl}/google/import`, formData);
  }

  getPayouts(platform: PayoutPlatform, from: string, to: string): Observable<PayoutEntry[]> {
    return this.http.get<PayoutEntry[]>(`${this.baseUrl}/payouts`, {
      params: { platform, from, to },
    });
  }
}
