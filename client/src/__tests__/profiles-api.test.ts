import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  buildApproveCollectionItemPath,
  buildCollectionItemsPath,
  buildPlayerDetailPath,
  buildRejectCollectionItemPath,
  buildTeamDetailPath,
  buildTeamPlayersPath,
  getPublicPlayerProfile,
  getPublicTeamProfile,
  listPublicPlayerProfiles,
  listPublicTeamPlayers,
  listPublicTeamProfiles,
} from '@/api/profiles';
import { publicHttp } from '@/api/http';

const okResponse = { data: { success: true, data: [], message: '', timestamp: '' } };

describe('profiles api helpers', () => {
  afterEach(() => vi.restoreAllMocks());

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

  it('calls public profile endpoints without Authorization', async () => {
    const getSpy = vi.spyOn(publicHttp, 'get').mockResolvedValue(okResponse);

    await listPublicTeamProfiles();
    await getPublicTeamProfile(3);
    await listPublicTeamPlayers(3);
    await listPublicPlayerProfiles();
    await getPublicPlayerProfile(8);

    expect(getSpy).toHaveBeenNthCalledWith(1, '/profiles/teams');
    expect(getSpy).toHaveBeenNthCalledWith(2, '/profiles/teams/3');
    expect(getSpy).toHaveBeenNthCalledWith(3, '/profiles/teams/3/players');
    expect(getSpy).toHaveBeenNthCalledWith(4, '/profiles/players');
    expect(getSpy).toHaveBeenNthCalledWith(5, '/profiles/players/8');
  });
});
