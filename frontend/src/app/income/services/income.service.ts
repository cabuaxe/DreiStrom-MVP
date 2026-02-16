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
    return this.api.listIncomeEntries(streamType);
  }

  create(request: CreateIncomeEntryRequest): Observable<IncomeEntryResponse> {
    return this.api.createIncomeEntry(request);
  }

  update(id: number, request: UpdateIncomeEntryRequest): Observable<IncomeEntryResponse> {
    return this.api.updateIncomeEntry(id, request);
  }

  delete(id: number): Observable<void> {
    return this.api.deleteIncomeEntry(id) as Observable<void>;
  }
}
