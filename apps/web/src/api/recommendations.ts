export type ApiResult<T> = {
  code: number;
  data?: T;
  error_message?: string;
};

export type Recommendation = {
  id: number;
  ticker: string;
  market: string;
  term: "SHORT" | "LONG" | string;
  entryPrice: number;
  targetPrice: number;
  stopPrice: number;
  expectedExitAt: string;
  confidence: number;
  signalsJson: string;
  modelVersion: string;
  generatedAt: string;
  status: "OPEN" | "CLOSED" | "EXPIRED" | string;
};

export type DevGenerateResult = {
  market: string;
  sourceInstrumentCount: number;
  generatedPredictionCount: number;
  generatedRecommendationCount: number;
  predictionIds: number[];
  recommendationIds: number[];
};

export type RecommendationStatus = "OPEN" | "CLOSED" | "EXPIRED" | string;

export type RecommendationQuery = {
  status?: RecommendationStatus;
  ticker?: string;
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

export function getRecommendations(query: RecommendationQuery = {}) {
  const params = new URLSearchParams();

  if (query.status && query.status !== "ALL") {
    params.set("status", query.status);
  }

  if (query.ticker?.trim()) {
    params.set("ticker", query.ticker.trim().toUpperCase());
  }

  const queryString = params.toString();

  return request<Recommendation[]>(`/api/recommendations${queryString ? `?${queryString}` : ""}`);
}

export function generateDevRecommendations(market = "KOSPI", shortCount = 3, longCount = 3) {
  const params = new URLSearchParams({
    market,
    shortCount: String(shortCount),
    longCount: String(longCount)
  });

  return request<DevGenerateResult>(`/api/dev/recommendations/generate?${params.toString()}`, {
    method: "POST"
  });
}

export function updateRecommendationStatus(id: number, status: RecommendationStatus) {
  return request<Recommendation>(`/api/recommendations/${id}/status`, {
    method: "PUT",
    body: JSON.stringify({ status })
  });
}
