import { describe, expect, it } from 'vitest';
import { formatMarketLine } from '@/utils/odds-format';

describe('odds formatting helpers', () => {
  it('keeps zero handicap lines instead of treating them as empty', () => {
    expect(formatMarketLine(undefined, 0)).toBe('0');
    expect(formatMarketLine('', -1)).toBe('-1');
    expect(formatMarketLine('2.5', 2.5)).toBe('2.5');
    expect(formatMarketLine(undefined, undefined)).toBe('-');
  });
});
