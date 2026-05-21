import type { ApiResult } from "./recommendations";

export interface TermStats {
  count: number;
  hitRate: number;
  avgPnlPct: number;
}

export interface StatsSummaryResponse {
  total: number;
  closed: number;
  open: number;
  expired: number;
  hitRate: number;
  avgPnlPct: number;
  totalPnlPct: number;
  maxDrawdownPct: number;
  byTerm: Record<string, TermStats>;
  byExitReason: Record<string, number>;
}

export interface StatsDailyResponse {
  date: string;
  count: number;
  hitCount: number;
  avgPnlPct: number;
}

export interface StatsStrategyResponse {
  modelVersion: string;
  count: number;
  hitRate: number;
  avgPnlPct: number;
}

export async function fetchStatsSummary(): Promise<StatsSummaryResponse> {
  const res = await fetch("/api/stats/summary");
  const body: ApiResult<StatsSummaryResponse> = await res.json();
  return body.data!;
}

export async function fetchStatsDaily(): Promise<StatsDailyResponse[]> {
  const res = await fetch("/api/stats/daily");
  const body: ApiResult<StatsDailyResponse[]> = await res.json();
  return body.data!;
}

export async function fetchStatsByStrategy(): Promise<StatsStrategyResponse[]> {
  const res = await fetch("/api/stats/by-strategy");
  const body: ApiResult<StatsStrategyResponse[]> = await res.json();
  return body.data!;
}
