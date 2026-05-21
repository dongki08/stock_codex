export type ApiResult<T> = {
  code: number;
  data?: T;
  error_message?: string;
};

export type Instrument = {
  ticker: string;
  market: string;
  name: string;
  sector: string | null;
  enabled: boolean;
};

export type InstrumentPayload = {
  ticker: string;
  market: string;
  name: string;
  sector: string;
  enabled: boolean;
};

export type InstrumentUpdatePayload = Omit<InstrumentPayload, "ticker">;

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

export function getInstruments(market?: string) {
  const query = market && market !== "ALL" ? `?market=${encodeURIComponent(market)}` : "";

  return request<Instrument[]>(`/api/instruments${query}`);
}

export function createInstrument(payload: InstrumentPayload) {
  return request<Instrument>("/api/instruments", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function updateInstrument(ticker: string, payload: InstrumentUpdatePayload) {
  return request<Instrument>(`/api/instruments/${encodeURIComponent(ticker)}`, {
    method: "PUT",
    body: JSON.stringify(payload)
  });
}
