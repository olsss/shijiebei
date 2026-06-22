import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  buildMatchSentimentPath,
  buildSentimentCategoriesPath,
  buildSentimentRiskTypesPath,
  getPublicMatchSentiment,
  listPublicSentimentCategories,
  listPublicSentimentOverview,
  listPublicSentimentRiskTypes,
} from '@/api/sentiment';
import { publicHttp } from '@/api/http';

const okResponse = { data: { success: true, data: [], message: '', timestamp: '' } };

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
});
