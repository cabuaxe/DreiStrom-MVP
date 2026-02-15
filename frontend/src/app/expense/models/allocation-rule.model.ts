export interface AllocationRuleResponse {
  id: number;
  name: string;
  freiberufPct: number;
  gewerbePct: number;
  personalPct: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateAllocationRuleRequest {
  name: string;
  freiberufPct: number;
  gewerbePct: number;
  personalPct: number;
}

export interface UpdateAllocationRuleRequest {
  name: string;
  freiberufPct: number;
  gewerbePct: number;
  personalPct: number;
}
