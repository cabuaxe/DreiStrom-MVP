import { Component, ViewChild } from '@angular/core';
import { DocumentUploadComponent } from './document-upload/document-upload.component';
import { DocumentListComponent } from './document-list/document-list.component';

@Component({
  selector: 'app-document',
  imports: [DocumentUploadComponent, DocumentListComponent],
  templateUrl: './document.component.html',
  styleUrl: './document.component.scss',
})
export class DocumentComponent {
  @ViewChild(DocumentListComponent) documentList!: DocumentListComponent;

  onUploaded(): void {
    this.documentList.refreshList();
  }
}
