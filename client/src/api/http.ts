import axios from 'axios';

export const http = axios.create({
  baseURL: '/api',
  timeout: 10000,
});

export function buildBasicAuthHeader(username: string, password: string): string {
  const token = btoa(`${username}:${password}`);
  return `Basic ${token}`;
}
