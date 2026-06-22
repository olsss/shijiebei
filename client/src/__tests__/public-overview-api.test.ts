import { describe, expect, it, vi } from 'vitest';
import { fetchPublicOverview } from '@/api/publicOverview';
import { publicHttp } from '@/api/http';

describe('public overview api', () => {
  it('calls public overview without Authorization', async () => {
    const getSpy = vi.spyOn(publicHttp, 'get').mockResolvedValueOnce({
      data: { success: true, data: {}, message: '', timestamp: '' },
    });

    await fetchPublicOverview();

    expect(getSpy).toHaveBeenCalledWith('/overview');
  });
});
