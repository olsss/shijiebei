import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  buildPrematchWorkbenchIntegrityPath,
  buildPrematchWorkbenchMatchesPath,
  buildPrematchWorkbenchMatchPath,
  getPublicPrematchWorkbenchIntegrity,
  getPublicPrematchWorkbenchMatch,
  listPublicPrematchWorkbenchMatches,
} from '@/api/prematchWorkbench';
import { publicHttp } from '@/api/http';

const okResponse = { data: { success: true, data: [], message: '', timestamp: '' } };

describe('prematch workbench api helpers', () => {
  afterEach(() => vi.restoreAllMocks());

  it('builds prematch workbench paths', () => {
    expect(buildPrematchWorkbenchMatchesPath()).toBe('/prematch-workbench/matches');
    expect(buildPrematchWorkbenchMatchPath(6)).toBe('/prematch-workbench/matches/6');
    expect(buildPrematchWorkbenchIntegrityPath(6)).toBe('/prematch-workbench/matches/6/integrity');
  });

  it('calls public prematch endpoints without Authorization', async () => {
    const getSpy = vi.spyOn(publicHttp, 'get').mockResolvedValue(okResponse);

    await listPublicPrematchWorkbenchMatches();
    await getPublicPrematchWorkbenchMatch(6);
    await getPublicPrematchWorkbenchIntegrity(6);

    expect(getSpy).toHaveBeenNthCalledWith(1, '/prematch-workbench/matches');
    expect(getSpy).toHaveBeenNthCalledWith(2, '/prematch-workbench/matches/6');
    expect(getSpy).toHaveBeenNthCalledWith(3, '/prematch-workbench/matches/6/integrity');
  });
});
