import { unwrapResult } from "./result";

export type MarketUniverseItem = {
  universeKey: string;
  ticker: string;
  market: string;
  name: string;
  sector: string | null;
  marketCap: number | null;
  avgTurnover: number | null;
  lastPrice: number | null;
  tradable: boolean;
  source: string;
  lastSyncedAt: string | null;
};

export type UniverseFeature = {
  universeKey: string;
  ticker: string;
  market: string;
  name: string;
  lastPrice: number | null;
  avgTurnover: number | null;
  liquidityScore: number;
  priceScore: number;
  movingAverageScore: number;
  rsiScore: number;
  volumeScore: number;
  technicalScore: number;
  dataQualityScore: number;
  priceHistoryCount: number;
  totalScore: number;
  featureJson: string;
};

export type UniverseSeedResult = {
  market: string;
  upsertedCount: number;
  universeKeys: string[];
};

export type UniverseSyncResult = {
  source: string;
  market: string;
  fetchedCount: number;
  upsertedCount: number;
  sampleUniverseKeys: string[];
};

export type UniverseQuery = {
  market?: string;
  tradable?: boolean;
  minMarketCap?: string;
  minAvgTurnover?: string;
  minLastPrice?: string;
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

  return unwrapResult<T>(response);
}

export function getUniverse(query: UniverseQuery = {}) {
  const params = new URLSearchParams();

  if (query.market && query.market !== "ALL") {
    params.set("market", query.market);
  }

  if (typeof query.tradable === "boolean") {
    params.set("tradable", String(query.tradable));
  }

  if (query.minMarketCap?.trim()) {
    params.set("minMarketCap", query.minMarketCap.trim());
  }

  if (query.minAvgTurnover?.trim()) {
    params.set("minAvgTurnover", query.minAvgTurnover.trim());
  }

  if (query.minLastPrice?.trim()) {
    params.set("minLastPrice", query.minLastPrice.trim());
  }

  const queryString = params.toString();

  return request<MarketUniverseItem[]>(`/api/universe${queryString ? `?${queryString}` : ""}`);
}

export function getUniverseFeatures(market = "ALL", limit = "500") {
  const params = new URLSearchParams();

  if (market !== "ALL") {
    params.set("market", market);
  }

  if (limit.trim()) {
    params.set("limit", limit.trim());
  }

  const queryString = params.toString();

  return request<UniverseFeature[]>(`/api/features/universe${queryString ? `?${queryString}` : ""}`);
}

export function seedDevUniverse(market = "ALL") {
  const query = market !== "ALL" ? `?market=${encodeURIComponent(market)}` : "";

  return request<UniverseSeedResult>(`/api/dev/universe/seed${query}`, {
    method: "POST"
  });
}

export function syncUsSymbols(market = "ALL") {
  const query = market !== "ALL" ? `?market=${encodeURIComponent(market)}` : "";

  return request<UniverseSyncResult>(`/api/universe/sync/us-symbols${query}`, {
    method: "POST"
  });
}

export function syncKrSymbols(market = "ALL") {
  const query = market !== "ALL" ? `?market=${encodeURIComponent(market)}` : "";

  return request<UniverseSyncResult>(`/api/universe/sync/kr-symbols${query}`, {
    method: "POST"
  });
}

export function syncUsPrices(market = "ALL", limit = "50") {
  const params = new URLSearchParams();

  if (market !== "ALL") {
    params.set("market", market);
  }

  if (limit.trim()) {
    params.set("limit", limit.trim());
  }

  const queryString = params.toString();

  return request<UniverseSyncResult>(`/api/universe/sync/us-prices${queryString ? `?${queryString}` : ""}`, {
    method: "POST"
  });
}
