import { describe, expect, it } from 'vitest';
import { buildImportItemPath } from '@/api/importReview';

describe('import review api helpers', () => {
  it('builds item detail path', () => {
    expect(buildImportItemPath(12)).toBe('/import-items/12');
  });
});
