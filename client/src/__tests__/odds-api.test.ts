import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  buildBookmakersPath,
  buildMarketsPath,
  buildMatchOddsPath,
  getPublicMatchOdds,
  listPublicBookmakers,
  listPublicOddsMarkets,
  listPublicOddsOverview,
} from '@/api/odds';
import { publicHttp } from '@/api/http';

const okResponse = { data: { success: true, data: [], message: '', timestamp: '' } };

describe('odds api helpers', () => {
  afterEach(() => vi.restoreAllMocks());

  it('builds odds center paths', () => {
    expect(buildMatchOddsPath(6)).toBe('/odds/matches/6');
    expect(buildBookmakersPath()).toBe('/odds/bookmakers');
    expect(buildMarketsPath()).toBe('/odds/markets');
  });

  it('calls public odds endpoints without Authorization', async () => {
    const getSpy = vi.spyOn(publicHttp, 'get').mockResolvedValue(okResponse);

    await listPublicOddsOverview();
    await getPublicMatchOdds(6);
    await listPublicBookmakers();
    await listPublicOddsMarkets();

    expect(getSpy).toHaveBeenNthCalledWith(1, '/odds');
    expect(getSpy).toHaveBeenNthCalledWith(2, '/odds/matches/6');
    expect(getSpy).toHaveBeenNthCalledWith(3, '/odds/bookmakers');
    expect(getSpy).toHaveBeenNthCalledWith(4, '/odds/markets');
  });
});
