import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  buildMatchSentimentPath,
  buildSentimentCategoriesPath,
  buildSentimentRiskTypesPath,
  getMatchSentiment,
  getPublicMatchSentiment,
  listPublicSentimentCategories,
  listPublicSentimentOverview,
  listPublicSentimentRiskTypes,
  listSentimentOverview,
} from '@/api/sentiment';
import { http, publicHttp } from '@/api/http';

const okResponse = { data: { success: true, data: [], message: '', timestamp: '' } };
const authHeaders = { Authorization: 'Basic abc' };

describe('sentiment api helpers', () => {
  afterEach(() => vi.restoreAllMocks());

  it('builds sentiment center paths', () => {
    expect(buildMatchSentimentPath(6)).toBe('/sentiment/matches/6');
    expect(buildSentimentCategoriesPath()).toBe('/sentiment/categories');
    expect(buildSentimentRiskTypesPath()).toBe('/sentiment/risk-types');
  });

  it('calls public sentiment endpoints without Authorization', async () => {
    const getSpy = vi.spyOn(publicHttp, 'get').mockResolvedValue(okResponse);

    await listPublicSentimentOverview();
    await getPublicMatchSentiment(6);
    await listPublicSentimentCategories();
    await listPublicSentimentRiskTypes();

    expect(getSpy).toHaveBeenNthCalledWith(1, '/sentiment');
    expect(getSpy).toHaveBeenNthCalledWith(2, '/sentiment/matches/6');
    expect(getSpy).toHaveBeenNthCalledWith(3, '/sentiment/categories');
    expect(getSpy).toHaveBeenNthCalledWith(4, '/sentiment/risk-types');
  });

  it('calls admin sentiment endpoints with Authorization headers', async () => {
    const getSpy = vi.spyOn(http, 'get').mockResolvedValue(okResponse);

    await listSentimentOverview('Basic abc');
    await getMatchSentiment('Basic abc', 6);

    expect(getSpy).toHaveBeenNthCalledWith(1, '/sentiment', { headers: authHeaders });
    expect(getSpy).toHaveBeenNthCalledWith(2, '/sentiment/matches/6', { headers: authHeaders });
  });
});
