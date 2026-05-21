export type ApiResult<T> = {
  code: number;
  data?: T;
  error_message?: string;
};

export type AdminSetting = {
  key: string;
  valueJson: string;
  description: string;
  updatedBy: string;
};

export type AuditLog = {
  id: number;
  actor: string;
  action: string;
  beforeJson: string;
  afterJson: string;
};

import { getAuthHeader } from "./auth";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const authHeader = getAuthHeader();
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...(authHeader ? { Authorization: authHeader } : {}),
      ...options?.headers
    },
    ...options
  });

  if (response.status === 401) {
    throw new Error("UNAUTHORIZED");
  }

  const result = (await response.json()) as ApiResult<T>;

  if (!response.ok || result.code !== 200) {
    throw new Error(result.error_message ?? "API 요청에 실패했습니다.");
  }

  return result.data as T;
}

export function getAdminSettings() {
  return request<AdminSetting[]>("/api/admin/settings");
}

export function resetAdminSettings() {
  return request<AdminSetting[]>("/api/admin/settings/reset", {
    method: "POST"
  });
}

export function updateAdminSetting(key: string, valueJson: string, actor = "admin") {
  return request<AdminSetting>(`/api/admin/settings/${encodeURIComponent(key)}`, {
    method: "PUT",
    body: JSON.stringify({ valueJson, actor })
  });
}

export function getAuditLogs() {
  return request<AuditLog[]>("/api/admin/audit-logs");
}
