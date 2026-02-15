import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DocumentEntry, DocumentType } from '../models/document.model';

@Injectable({ providedIn: 'root' })
export class DocumentService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/documents`;

  list(type?: DocumentType): Observable<DocumentEntry[]> {
    const params: Record<string, string> = {};
    if (type) params['type'] = type;
    return this.http.get<DocumentEntry[]>(this.baseUrl, { params });
  }

  getById(id: number): Observable<DocumentEntry> {
    return this.http.get<DocumentEntry>(`${this.baseUrl}/${id}`);
  }

  upload(file: File, documentType: DocumentType, description?: string): Observable<DocumentEntry> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('documentType', documentType);
    if (description) formData.append('description', description);
    return this.http.post<DocumentEntry>(this.baseUrl, formData);
  }

  updateMetadata(id: number, description?: string, tags?: string): Observable<DocumentEntry> {
    const params: Record<string, string> = {};
    if (description) params['description'] = description;
    if (tags) params['tags'] = tags;
    return this.http.patch<DocumentEntry>(`${this.baseUrl}/${id}`, null, { params });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  getDownloadUrl(id: number): string {
    return `${this.baseUrl}/${id}/download`;
  }
}
