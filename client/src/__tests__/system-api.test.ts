import { describe, expect, it } from 'vitest';
import { buildBasicAuthHeader } from '@/api/http';

describe('http helpers', () => {
  it('builds a basic auth header', () => {
    expect(buildBasicAuthHeader('admin', 'admin123456')).toBe('Basic YWRtaW46YWRtaW4xMjM0NTY=');
  });
});
