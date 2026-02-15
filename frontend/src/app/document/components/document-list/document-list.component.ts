import { Component, inject, signal, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DocumentService } from '../../services/document.service';
import { DocumentEntry, DocumentType, DOCUMENT_TYPE_LABELS } from '../../models/document.model';

@Component({
  selector: 'app-document-list',
  imports: [
    DatePipe,
    MatCardModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatSelectModule,
    MatFormFieldModule,
    MatMenuModule,
    MatTooltipModule,
  ],
  templateUrl: './document-list.component.html',
  styleUrl: './document-list.component.scss',
})
export class DocumentListComponent implements OnInit {
  private readonly documentService = inject(DocumentService);

  readonly documents = signal<DocumentEntry[]>([]);
  readonly loading = signal(true);
  readonly filterType = signal<DocumentType | ''>('');
  readonly displayedColumns = ['type', 'fileName', 'size', 'retention', 'uploadedAt', 'actions'];

  readonly typeOptions: { value: DocumentType | ''; label: string }[] = [
    { value: '', label: 'Alle' },
    ...(Object.entries(DOCUMENT_TYPE_LABELS) as [DocumentType, string][])
        .map(([value, label]) => ({ value, label })),
  ];

  ngOnInit(): void {
    this.loadDocuments();
  }

  onFilterChange(type: DocumentType | ''): void {
    this.filterType.set(type);
    this.loadDocuments();
  }

  typeLabel(type: DocumentType): string {
    return DOCUMENT_TYPE_LABELS[type] ?? type;
  }

  formatFileSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  }

  retentionDaysLeft(retentionUntil: string): number {
    const until = new Date(retentionUntil + 'T00:00:00');
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return Math.ceil((until.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
  }

  retentionLabel(doc: DocumentEntry): string {
    const days = this.retentionDaysLeft(doc.retentionUntil);
    if (days <= 0) return 'Abgelaufen';
    const years = Math.floor(days / 365);
    if (years > 0) return `${years}J ${days % 365}T`;
    return `${days} Tage`;
  }

  onDownload(doc: DocumentEntry): void {
    window.open(this.documentService.getDownloadUrl(doc.id), '_blank');
  }

  onDelete(doc: DocumentEntry): void {
    this.documentService.delete(doc.id).subscribe({
      next: () => this.loadDocuments(),
    });
  }

  refreshList(): void {
    this.loadDocuments();
  }

  private loadDocuments(): void {
    this.loading.set(true);
    const type = this.filterType();
    this.documentService.list(type || undefined).subscribe({
      next: (data) => {
        this.documents.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
