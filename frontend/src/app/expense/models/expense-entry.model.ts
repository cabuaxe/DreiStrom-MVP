export interface AllocationRuleSummary {
  id: number;
  name: string;
  freiberufPct: number;
  gewerbePct: number;
  personalPct: number;
}

export interface ExpenseEntryResponse {
  id: number;
  amount: number;
  currency: string;
  category: string;
  entryDate: string;
  receiptDocId?: number;
  allocationRule?: AllocationRuleSummary;
  description?: string;
  gwg: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateExpenseEntryRequest {
  amount: number;
  category: string;
  entryDate: string;
  allocationRuleId?: number;
  receiptDocId?: number;
  description?: string;
}

export interface UpdateExpenseEntryRequest {
  amount: number;
  category: string;
  entryDate: string;
  allocationRuleId?: number;
  receiptDocId?: number;
  description?: string;
}
