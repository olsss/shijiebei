import { describe, expect, it } from 'vitest';
import {
  buildBookmakersPath,
  buildMarketsPath,
  buildMatchOddsPath,
} from '@/api/odds';

describe('odds api helpers', () => {
  it('builds odds center paths', () => {
    expect(buildMatchOddsPath(6)).toBe('/odds/matches/6');
    expect(buildBookmakersPath()).toBe('/odds/bookmakers');
    expect(buildMarketsPath()).toBe('/odds/markets');
  });
});
