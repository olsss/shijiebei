import { afterEach, describe, expect, it, vi } from 'vitest';
import { buildBasicAuthHeader, http } from '@/api/http';
import { fetchSystemSettings } from '@/api/system';

describe('http helpers', () => {
  afterEach(() => vi.restoreAllMocks());

  it('builds a basic auth header', () => {
    expect(buildBasicAuthHeader('admin', 'admin123456')).toBe('Basic YWRtaW46YWRtaW4xMjM0NTY=');
  });

  it('omits Authorization for system settings when auth header is empty', async () => {
    const getSpy = vi.spyOn(http, 'get').mockResolvedValueOnce({
      data: { success: true, data: {}, message: '', timestamp: '' },
    });

    await fetchSystemSettings('');

    expect(getSpy).toHaveBeenCalledWith('/system/settings', { headers: undefined });
  });
});
