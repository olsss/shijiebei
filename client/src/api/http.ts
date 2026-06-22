import axios from 'axios';

export const http = axios.create({ baseURL: '/api', timeout: 10000 });
export const publicHttp = axios.create({ baseURL: '/api/public', timeout: 10000 });
export const adminHttp = http;

export function buildBasicAuthHeader(username: string, password: string): string {
  const token = btoa(`${username}:${password}`);
  return `Basic ${token}`;
}

export function createAuthHeaders(authHeader: string): { Authorization: string } | undefined {
  return authHeader ? { Authorization: authHeader } : undefined;
}

export function isUnauthorized(error: unknown): boolean {
  return Boolean((error as { response?: { status?: number } }).response?.status === 401);
}

export function isForbidden(error: unknown): boolean {
  return Boolean((error as { response?: { status?: number } }).response?.status === 403);
}
