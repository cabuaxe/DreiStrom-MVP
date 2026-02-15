import { Component, inject, signal, output } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { FormsModule } from '@angular/forms';
import { DocumentService } from '../../services/document.service';
import { DocumentType, DocumentEntry, DOCUMENT_TYPE_LABELS } from '../../models/document.model';

@Component({
  selector: 'app-document-upload',
  imports: [
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatSelectModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressBarModule,
    FormsModule,
  ],
  templateUrl: './document-upload.component.html',
  styleUrl: './document-upload.component.scss',
})
export class DocumentUploadComponent {
  private readonly documentService = inject(DocumentService);

  readonly uploaded = output<DocumentEntry>();

  readonly selectedFile = signal<File | null>(null);
  readonly documentType = signal<DocumentType>('INVOICE');
  readonly description = signal('');
  readonly uploading = signal(false);
  readonly dragOver = signal(false);
  readonly error = signal('');

  readonly typeOptions: { value: DocumentType; label: string }[] = (
    Object.entries(DOCUMENT_TYPE_LABELS) as [DocumentType, string][]
  ).map(([value, label]) => ({ value, label }));

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(true);
  }

  onDragLeave(): void {
    this.dragOver.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(false);
    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.selectedFile.set(files[0]);
    }
  }

  onFileSelect(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile.set(input.files[0]);
    }
  }

  onUpload(): void {
    const file = this.selectedFile();
    if (!file) return;

    this.uploading.set(true);
    this.error.set('');

    this.documentService.upload(file, this.documentType(), this.description() || undefined)
        .subscribe({
          next: (doc) => {
            this.uploaded.emit(doc);
            this.selectedFile.set(null);
            this.description.set('');
            this.uploading.set(false);
          },
          error: (err) => {
            this.error.set(err.error?.message || 'Upload fehlgeschlagen');
            this.uploading.set(false);
          },
        });
  }

  formatFileSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  }
}
