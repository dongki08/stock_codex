export type ApiResult<T> = {
  code: number;
  data?: T;
  error_message?: string;
};

export type paths = Record<string, never>;
