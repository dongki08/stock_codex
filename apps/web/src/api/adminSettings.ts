import { getAuthHeader } from "./auth";
import { unwrapResult } from "./result";

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

  return unwrapResult<T>(response);
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
