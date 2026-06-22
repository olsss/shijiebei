import { describe, expect, it } from 'vitest';
import {
  buildMatchDetailPath,
  buildMatchEventsPath,
  buildMatchLineupsPath,
  buildMatchPlayerStatsPath,
  buildMatchTeamStatsPath,
} from '@/api/matches';

describe('matches api helpers', () => {
  it('builds match center paths', () => {
    expect(buildMatchDetailPath(6)).toBe('/matches/6');
    expect(buildMatchLineupsPath(6)).toBe('/matches/6/lineups');
    expect(buildMatchEventsPath(6)).toBe('/matches/6/events');
    expect(buildMatchTeamStatsPath(6)).toBe('/matches/6/team-stats');
    expect(buildMatchPlayerStatsPath(6)).toBe('/matches/6/player-stats');
  });
});
