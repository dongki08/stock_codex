export type ApiResult<T> = {
  code: number;
  data?: T;
  error_message?: string;
};

export async function unwrapResult<T>(response: Response): Promise<T> {
  const result = (await response.json()) as ApiResult<T>;

  if (!response.ok || result.code !== 200) {
    throw new Error(result.error_message ?? "API 요청에 실패했습니다.");
  }

  return result.data as T;
}
