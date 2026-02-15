export type DocumentType =
  | 'INVOICE'
  | 'RECEIPT'
  | 'CONTRACT'
  | 'CORRESPONDENCE'
  | 'TAX_NOTICE'
  | 'BANK_STATEMENT'
  | 'PAYSLIP'
  | 'OTHER';

export interface DocumentEntry {
  id: number;
  fileName: string;
  contentType: string;
  fileSize: number;
  sha256Hash: string;
  documentType: DocumentType;
  retentionYears: number;
  retentionUntil: string;
  deletionLocked: boolean;
  description: string | null;
  uploadedAt: string;
}

export const DOCUMENT_TYPE_LABELS: Record<DocumentType, string> = {
  INVOICE: 'Rechnung',
  RECEIPT: 'Beleg',
  CONTRACT: 'Vertrag',
  CORRESPONDENCE: 'Korrespondenz',
  TAX_NOTICE: 'Steuerbescheid',
  BANK_STATEMENT: 'Kontoauszug',
  PAYSLIP: 'Gehaltsabrechnung',
  OTHER: 'Sonstiges',
};
