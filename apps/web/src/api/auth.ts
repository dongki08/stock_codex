let encoded: string | null = null;

export function setCredentials(username: string, password: string) {
  encoded = btoa(`${username}:${password}`);
}

export function getAuthHeader(): string | undefined {
  return encoded ? `Basic ${encoded}` : undefined;
}

export function clearCredentials() {
  encoded = null;
}

export function hasCredentials() {
  return encoded !== null;
}
