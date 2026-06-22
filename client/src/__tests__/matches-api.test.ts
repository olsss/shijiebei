import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  buildMatchDetailPath,
  buildMatchEventsPath,
  buildMatchLineupsPath,
  buildMatchPlayerStatsPath,
  buildMatchTeamStatsPath,
  getPublicMatchDetail,
  listPublicMatchEvents,
  listPublicMatchLineups,
  listPublicMatches,
  listPublicMatchPlayerStats,
  listPublicMatchTeamStats,
} from '@/api/matches';
import { publicHttp } from '@/api/http';

const okResponse = { data: { success: true, data: [], message: '', timestamp: '' } };

describe('matches api helpers', () => {
  afterEach(() => vi.restoreAllMocks());

  it('builds match center paths', () => {
    expect(buildMatchDetailPath(6)).toBe('/matches/6');
    expect(buildMatchLineupsPath(6)).toBe('/matches/6/lineups');
    expect(buildMatchEventsPath(6)).toBe('/matches/6/events');
    expect(buildMatchTeamStatsPath(6)).toBe('/matches/6/team-stats');
    expect(buildMatchPlayerStatsPath(6)).toBe('/matches/6/player-stats');
  });

  it('calls public match endpoints without Authorization', async () => {
    const getSpy = vi.spyOn(publicHttp, 'get').mockResolvedValue(okResponse);

    await listPublicMatches();
    await getPublicMatchDetail(6);
    await listPublicMatchLineups(6);
    await listPublicMatchEvents(6);
    await listPublicMatchTeamStats(6);
    await listPublicMatchPlayerStats(6);

    expect(getSpy).toHaveBeenNthCalledWith(1, '/matches');
    expect(getSpy).toHaveBeenNthCalledWith(2, '/matches/6');
    expect(getSpy).toHaveBeenNthCalledWith(3, '/matches/6/lineups');
    expect(getSpy).toHaveBeenNthCalledWith(4, '/matches/6/events');
    expect(getSpy).toHaveBeenNthCalledWith(5, '/matches/6/team-stats');
    expect(getSpy).toHaveBeenNthCalledWith(6, '/matches/6/player-stats');
  });
});
