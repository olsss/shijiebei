import { describe, expect, it } from 'vitest';
import { buildCoreDataImportPath, buildCoreDataMappingsPath } from '@/api/coreData';

describe('core data api helpers', () => {
  it('builds import and mapping paths', () => {
    expect(buildCoreDataImportPath(7)).toBe('/core-data/import-items/7/import');
    expect(buildCoreDataMappingsPath(7)).toBe('/core-data/import-items/7/mappings');
  });
});
