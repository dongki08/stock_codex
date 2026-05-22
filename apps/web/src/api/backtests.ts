export type ApiResult<T> = {
  code: number;
  data?: T;
  error_message?: string;
};

export type BacktestRun = {
  id: number;
  strategy: string;
  periodFrom: string;
  periodTo: string;
  metricsJson: string;
};

export type BacktestSimulationInput = {
  strategy: string;
  market: string;
  periodFrom: string;
  periodTo: string;
  maxTickers: string;
  holdingDays: string;
  targetPct: string;
  stopPct: string;
};

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...options?.headers
    },
    ...options
  });

  const result = (await response.json()) as ApiResult<T>;

  if (!response.ok || result.code !== 200) {
    throw new Error(result.error_message ?? "API 요청에 실패했습니다.");
  }

  return result.data as T;
}

export function fetchBacktests() {
  return request<BacktestRun[]>("/api/backtests");
}

export function simulateBacktest(input: BacktestSimulationInput) {
  return request<BacktestRun>("/api/backtests/simulate", {
    body: JSON.stringify({
      strategy: input.strategy.trim() || undefined,
      market: input.market,
      periodFrom: input.periodFrom,
      periodTo: input.periodTo,
      maxTickers: input.maxTickers.trim() ? Number(input.maxTickers) : undefined,
      holdingDays: input.holdingDays.trim() ? Number(input.holdingDays) : undefined,
      targetPct: input.targetPct.trim() ? Number(input.targetPct) : undefined,
      stopPct: input.stopPct.trim() ? Number(input.stopPct) : undefined
    }),
    method: "POST"
  });
}
