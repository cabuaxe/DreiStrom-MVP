import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { IncomeEntriesApiService } from '../../api/generated/api/income-entries.service';
import { IncomeEntryResponse } from '../../api/generated/model/income-entry-response.model';
import { CreateIncomeEntryRequest } from '../../api/generated/model/create-income-entry-request.model';
import { UpdateIncomeEntryRequest } from '../../api/generated/model/update-income-entry-request.model';

export type StreamType = 'EMPLOYMENT' | 'FREIBERUF' | 'GEWERBE';

@Injectable({ providedIn: 'root' })
export class IncomeService {
  private readonly api = inject(IncomeEntriesApiService);

  list(streamType?: StreamType): Observable<IncomeEntryResponse[]> {
    return this.api.list(streamType);
  }

  create(request: CreateIncomeEntryRequest): Observable<IncomeEntryResponse> {
    return this.api.create(request);
  }

  update(id: number, request: UpdateIncomeEntryRequest): Observable<IncomeEntryResponse> {
    return this.api.update(id, request);
  }

  delete(id: number): Observable<void> {
    return this.api._delete(id) as Observable<void>;
  }
}
