import { describe, expect, it } from 'vitest';
import {
  adminHttp,
  buildBasicAuthHeader,
  createAuthHeaders,
  http,
  isForbidden,
  isUnauthorized,
  publicHttp,
} from '@/api/http';

describe('http helpers', () => {
  it('builds basic auth header from explicit credentials only', () => {
    expect(buildBasicAuthHeader('admin', 'secret')).toBe(`Basic ${btoa('admin:secret')}`);
  });

  it('omits Authorization header when auth header is empty', () => {
    expect(createAuthHeaders('')).toBeUndefined();
    expect(createAuthHeaders('Basic abc')).toEqual({ Authorization: 'Basic abc' });
  });

  it('detects 401 and 403 responses', () => {
    expect(isUnauthorized({ response: { status: 401 } })).toBe(true);
    expect(isForbidden({ response: { status: 403 } })).toBe(true);
  });

  it('separates public and admin axios clients', () => {
    expect(publicHttp.defaults.baseURL).toBe('/api/public');
    expect(adminHttp).toBe(http);
  });
});
