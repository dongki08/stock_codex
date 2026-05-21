export type ApiResult<T> = {
  code: number;
  data?: T;
  error_message?: string;
};

export type PriceDailySyncResult = {
  market: string;
  candidateCount: number;
  fetchedCount: number;
  upsertedCount: number;
  samplePriceKeys: string[];
};

export type MarketDataCollectionSyncResult = {
  source: string;
  market: string | null;
  ticker: string | null;
  fetchedCount: number;
  savedCount: number;
  sampleKeys: string[];
};

export type NewsArticle = {
  articleKey: string;
  ticker: string | null;
  market: string | null;
  title: string;
  url: string;
  source: string;
  publishedAt: string | null;
  summary: string | null;
  sentimentScore: number | null;
};

export type DisclosureEvent = {
  disclosureKey: string;
  ticker: string | null;
  market: string | null;
  title: string;
  url: string | null;
  source: string;
  disclosureType: string | null;
  importanceScore: number | null;
  disclosedAt: string | null;
};

export type MacroObservation = {
  observationKey: string;
  seriesId: string;
  seriesName: string;
  observedDate: string;
  observedValue: number | null;
  source: string;
  fetchedAt: string;
};

export type FundamentalMetric = {
  metricKey: string;
  ticker: string;
  market: string;
  metricName: string;
  metricValue: number | null;
  unit: string | null;
  fiscalYear: number | null;
  fiscalPeriod: string | null;
  periodEnd: string | null;
  source: string;
  fetchedAt: string;
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

export function syncDailyPrices(market = "ALL", limit = "20", days = "120") {
  const params = new URLSearchParams();

  if (market !== "ALL") {
    params.set("market", market);
  }

  if (limit.trim()) {
    params.set("limit", limit.trim());
  }

  if (days.trim()) {
    params.set("days", days.trim());
  }

  const queryString = params.toString();

  return request<PriceDailySyncResult>(`/api/market-data/daily-prices/sync${queryString ? `?${queryString}` : ""}`, {
    method: "POST"
  });
}

function marketTickerParams(market = "ALL", ticker = "", limit = "20") {
  const params = new URLSearchParams();

  if (market !== "ALL") {
    params.set("market", market);
  }

  if (ticker.trim()) {
    params.set("ticker", ticker.trim().toUpperCase());
  }

  if (limit.trim()) {
    params.set("limit", limit.trim());
  }

  return params.toString();
}

export function getNewsArticles(market = "ALL", ticker = "", limit = "50") {
  const queryString = marketTickerParams(market, ticker, limit);
  return request<NewsArticle[]>(`/api/market-data/news${queryString ? `?${queryString}` : ""}`);
}

export function syncNewsArticles(market = "ALL", ticker = "", limit = "20") {
  const queryString = marketTickerParams(market, ticker, limit);
  return request<MarketDataCollectionSyncResult>(`/api/market-data/news/sync${queryString ? `?${queryString}` : ""}`, {
    method: "POST"
  });
}

export function getDisclosureEvents(market = "ALL", ticker = "", limit = "50") {
  const queryString = marketTickerParams(market, ticker, limit);
  return request<DisclosureEvent[]>(`/api/market-data/disclosures${queryString ? `?${queryString}` : ""}`);
}

export function syncDisclosureEvents(market = "ALL", ticker = "", limit = "20") {
  const queryString = marketTickerParams(market, ticker, limit);
  return request<MarketDataCollectionSyncResult>(`/api/market-data/disclosures/sync${queryString ? `?${queryString}` : ""}`, {
    method: "POST"
  });
}

export function getMacroObservations(seriesId = "", limit = "50") {
  const params = new URLSearchParams();

  if (seriesId.trim()) {
    params.set("seriesId", seriesId.trim().toUpperCase());
  }

  if (limit.trim()) {
    params.set("limit", limit.trim());
  }

  const queryString = params.toString();
  return request<MacroObservation[]>(`/api/market-data/macro-observations${queryString ? `?${queryString}` : ""}`);
}

export function syncMacroObservations(seriesId = "", limit = "5") {
  const params = new URLSearchParams();

  if (seriesId.trim()) {
    params.set("seriesId", seriesId.trim().toUpperCase());
  }

  if (limit.trim()) {
    params.set("limit", limit.trim());
  }

  const queryString = params.toString();
  return request<MarketDataCollectionSyncResult>(`/api/market-data/macro-observations/sync${queryString ? `?${queryString}` : ""}`, {
    method: "POST"
  });
}

export function getFundamentalMetrics(market = "ALL", ticker = "", limit = "50") {
  const queryString = marketTickerParams(market, ticker, limit);
  return request<FundamentalMetric[]>(`/api/market-data/fundamentals${queryString ? `?${queryString}` : ""}`);
}

export function syncFundamentalMetrics(market = "NASDAQ", ticker = "") {
  const params = new URLSearchParams();

  if (market !== "ALL") {
    params.set("market", market);
  }

  if (ticker.trim()) {
    params.set("ticker", ticker.trim().toUpperCase());
  }

  const queryString = params.toString();
  return request<MarketDataCollectionSyncResult>(`/api/market-data/fundamentals/sync${queryString ? `?${queryString}` : ""}`, {
    method: "POST"
  });
}
