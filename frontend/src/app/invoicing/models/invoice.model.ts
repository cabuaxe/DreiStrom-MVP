export type InvoiceStream = 'FREIBERUF' | 'GEWERBE';
export type InvoiceStatus = 'DRAFT' | 'SENT' | 'PAID' | 'OVERDUE' | 'CANCELLED';
export type VatTreatment = 'REGULAR' | 'REVERSE_CHARGE' | 'SMALL_BUSINESS' | 'INTRA_EU' | 'THIRD_COUNTRY';

export interface LineItem {
  description: string;
  quantity: number;
  unitPrice: number;
  vatRate: number;
}

export interface ClientSummary {
  id: number;
  name: string;
  clientType: string;
}

export interface InvoiceResponse {
  id: number;
  streamType: InvoiceStream;
  number: string;
  client: ClientSummary;
  invoiceDate: string;
  dueDate?: string;
  lineItems: LineItem[];
  netTotal: number;
  vat: number;
  grossTotal: number;
  currency: string;
  vatTreatment: VatTreatment;
  status: InvoiceStatus;
  notes?: string;
  zmReportable: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface LineItemRequest {
  description: string;
  quantity: number;
  unitPrice: number;
  vatRate: number;
}

export interface CreateInvoiceRequest {
  streamType: InvoiceStream;
  clientId: number;
  invoiceDate: string;
  dueDate?: string;
  lineItems: LineItemRequest[];
  netTotal: number;
  vat: number;
  grossTotal: number;
  vatTreatment?: VatTreatment;
  notes?: string;
}

export interface UpdateInvoiceRequest {
  clientId: number;
  invoiceDate: string;
  dueDate?: string;
  lineItems: LineItemRequest[];
  netTotal: number;
  vat: number;
  grossTotal: number;
  vatTreatment?: VatTreatment;
  notes?: string;
}

export interface UpdateInvoiceStatusRequest {
  status: InvoiceStatus;
}
