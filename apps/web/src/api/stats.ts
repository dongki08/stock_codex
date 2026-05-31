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
  totalPnlPct: number;
  cumulativePnlPct: number;
}

export interface StatsStrategyResponse {
  modelVersion: string;
  count: number;
  hitRate: number;
  avgPnlPct: number;
}

export interface PaperTradingPosition {
  recommendationId: number;
  ticker: string;
  market: string;
  term: string;
  entryPrice: number;
  currentPrice: number | null;
  currentTradeDate: string | null;
  targetPrice: number;
  stopPrice: number;
  confidence: number;
  positionWeightPct: number;
  unrealizedPnlPct: number | null;
  weightedPnlPct: number | null;
  distanceToTargetPct: number | null;
  distanceToStopPct: number | null;
  priceStatus: "OPEN" | "TARGET_TOUCHED" | "STOP_TOUCHED" | "NO_PRICE" | string;
}

export interface StatsPaperTradingResponse {
  openCount: number;
  pricedCount: number;
  avgUnrealizedPnlPct: number;
  weightedUnrealizedPnlPct: number;
  totalWeightPct: number;
  targetTouchCount: number;
  stopTouchCount: number;
  positions: PaperTradingPosition[];
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

export async function fetchStatsPaperTrading(): Promise<StatsPaperTradingResponse> {
  const res = await fetch("/api/stats/paper-trading");
  const body: ApiResult<StatsPaperTradingResponse> = await res.json();
  return body.data!;
}
