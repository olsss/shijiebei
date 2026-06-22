import { describe, expect, it } from 'vitest';
import {
  buildMatchSentimentPath,
  buildSentimentCategoriesPath,
  buildSentimentRiskTypesPath,
} from '@/api/sentiment';

describe('sentiment api helpers', () => {
  it('builds sentiment center paths', () => {
    expect(buildMatchSentimentPath(6)).toBe('/sentiment/matches/6');
    expect(buildSentimentCategoriesPath()).toBe('/sentiment/categories');
    expect(buildSentimentRiskTypesPath()).toBe('/sentiment/risk-types');
  });
});
