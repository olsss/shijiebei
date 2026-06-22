import { describe, expect, it } from 'vitest';
import {
  buildApproveCollectionItemPath,
  buildCollectionItemsPath,
  buildPlayerDetailPath,
  buildRejectCollectionItemPath,
  buildTeamDetailPath,
  buildTeamPlayersPath,
} from '@/api/profiles';

describe('profiles api helpers', () => {
  it('builds collection review paths', () => {
    expect(buildCollectionItemsPath('PENDING_REVIEW')).toBe('/profiles/collections/items?status=PENDING_REVIEW');
    expect(buildCollectionItemsPath()).toBe('/profiles/collections/items');
    expect(buildApproveCollectionItemPath(15)).toBe('/profiles/collections/items/15/approve');
    expect(buildRejectCollectionItemPath(15)).toBe('/profiles/collections/items/15/reject');
  });

  it('builds profile detail paths', () => {
    expect(buildTeamDetailPath(3)).toBe('/profiles/teams/3');
    expect(buildTeamPlayersPath(3)).toBe('/profiles/teams/3/players');
    expect(buildPlayerDetailPath(8)).toBe('/profiles/players/8');
  });
});
