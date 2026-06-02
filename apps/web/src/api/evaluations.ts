import { unwrapResult } from "./result";

export type Evaluation = {
  id: number;
  recommendationId: number;
  actualExitPrice: number | null;
  exitReason: "TARGET_HIT" | "STOP_HIT" | "TIME_OUT" | "MANUAL_CLOSE" | string;
  pnlPct: number;
  drawdownPct: number | null;
  hitTarget: boolean;
  evaluatedAt: string;
};

export type EvaluationCreatePayload = {
  recommendationId: number;
  actualExitPrice?: number | null;
  exitReason: string;
  pnlPct: number;
  drawdownPct?: number | null;
  hitTarget: boolean;
  evaluatedAt?: string | null;
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

export function getEvaluations(recommendationId?: number) {
  const query = recommendationId ? `?recommendationId=${recommendationId}` : "";

  return request<Evaluation[]>(`/api/evaluations${query}`);
}

export function createEvaluation(payload: EvaluationCreatePayload) {
  return request<Evaluation>("/api/evaluations", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}
