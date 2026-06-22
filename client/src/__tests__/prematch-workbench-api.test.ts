import { describe, expect, it } from 'vitest';
import {
  buildPrematchWorkbenchIntegrityPath,
  buildPrematchWorkbenchMatchesPath,
  buildPrematchWorkbenchMatchPath,
} from '@/api/prematchWorkbench';

describe('prematch workbench api helpers', () => {
  it('builds prematch workbench paths', () => {
    expect(buildPrematchWorkbenchMatchesPath()).toBe('/prematch-workbench/matches');
    expect(buildPrematchWorkbenchMatchPath(6)).toBe('/prematch-workbench/matches/6');
    expect(buildPrematchWorkbenchIntegrityPath(6)).toBe('/prematch-workbench/matches/6/integrity');
  });
});
