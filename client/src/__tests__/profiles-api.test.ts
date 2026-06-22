import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  approveCollectionItem,
  buildApproveCollectionItemPath,
  buildCollectionItemsPath,
  buildPlayerDetailPath,
  buildRejectCollectionItemPath,
  buildTeamDetailPath,
  buildTeamPlayersPath,
  getPublicPlayerProfile,
  getPublicTeamProfile,
  listCollectionItems,
  listPublicPlayerProfiles,
  listPublicTeamPlayers,
  listPublicTeamProfiles,
  listTeamProfiles,
} from '@/api/profiles';
import { http, publicHttp } from '@/api/http';

const okResponse = { data: { success: true, data: [], message: '', timestamp: '' } };
const authHeaders = { Authorization: 'Basic abc' };

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

  it('calls admin profile endpoints with Authorization headers', async () => {
    const getSpy = vi.spyOn(http, 'get').mockResolvedValue(okResponse);
    const postSpy = vi.spyOn(http, 'post').mockResolvedValue(okResponse);

    await listCollectionItems('Basic abc', 'PENDING_REVIEW');
    await listTeamProfiles('Basic abc');
    await approveCollectionItem('Basic abc', 15);

    expect(getSpy).toHaveBeenNthCalledWith(1, '/profiles/collections/items', {
      headers: authHeaders,
      params: { status: 'PENDING_REVIEW' },
    });
    expect(getSpy).toHaveBeenNthCalledWith(2, '/profiles/teams', { headers: authHeaders });
    expect(postSpy).toHaveBeenCalledWith('/profiles/collections/items/15/approve', undefined, {
      headers: authHeaders,
    });
  });
});
