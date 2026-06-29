import { describe, expect, it } from 'vitest';
import { mount } from '@vue/test-utils';
import ScoreboardCard from '@/components/football/ScoreboardCard.vue';
import coverageDonutSource from '@/components/football/CoverageDonut.vue?raw';
import {
  flagEmojiForFifaCode,
  normalizeTeamVisual,
  scoreboardFallback,
  scoreboardMetaStatus,
} from '@/utils/football-visuals';

describe('football-visuals', () => {
  it('infers flags from common Chinese national team names when FIFA code is missing', () => {
    const bosnia = normalizeTeamVisual({ teamName: '波黑' });
    const qatar = normalizeTeamVisual({ teamName: '卡塔尔国家队待同步' });
    const algeria = normalizeTeamVisual({ teamName: '阿尔及利亚' });
    const curacao = normalizeTeamVisual({ teamName: '库拉索' });
    const ivoryCoast = normalizeTeamVisual({ teamName: '科特迪瓦' });

    expect(bosnia.fifaCode).toBe('BIH');
    expect(bosnia.flagUrl).toContain('/ba.png');
    expect(qatar.fifaCode).toBe('QAT');
    expect(qatar.flagUrl).toContain('/qa.png');
    expect(algeria.fifaCode).toBe('ALG');
    expect(algeria.flagUrl).toContain('/dz.png');
    expect(curacao.fifaCode).toBe('CUW');
    expect(curacao.flagUrl).toContain('/cw.png');
    expect(ivoryCoast.fifaCode).toBe('CIV');
    expect(ivoryCoast.flagUrl).toContain('/ci.png');
  });

  it('keeps scheduled Chinese status readable as a pre-match scoreboard state', () => {
    const scoreboard = scoreboardFallback({ matchName: '波黑 vs 卡塔尔', status: '待开球' }, '待开球');

    expect(scoreboard.scoreDisplay).toBe('待开球');
    expect(scoreboard.resultText).toBe('赛前');
  });

  it('does not show pre-match status when a score result is already available', () => {
    const scoreboard = { homeScore: 3, awayScore: 0, scoreDisplay: '3 - 0', winnerSide: 'HOME', resultText: '主队胜' };

    expect(scoreboardMetaStatus('待开球', scoreboard)).toBe('比分记录');
    expect(scoreboardMetaStatus('SCHEDULED', scoreboard)).toBe('比分记录');

    const wrapper = mount(ScoreboardCard, {
      props: {
        matchName: '瑞士 vs 加拿大',
        status: '待开球',
        meta: '2026-06-24',
        scoreboard,
        homeTeam: { teamName: '瑞士', fifaCode: 'SUI' },
        awayTeam: { teamName: '加拿大', fifaCode: 'CAN' },
        compact: true,
      },
    });
    const text = wrapper.text();

    expect(text).toContain('比分记录');
    expect(text).toContain('3 - 0');
    expect(text).toContain('主队胜');
    expect(text).not.toContain('待开球');
  });

  it('renders offline flag emoji fallback from FIFA or ISO codes', () => {
    expect(flagEmojiForFifaCode('USA')).toBe('🇺🇸');
    expect(flagEmojiForFifaCode('fr')).toBe('🇫🇷');
    expect(flagEmojiForFifaCode('ENG')).toBeNull();
  });

  it('renders coverage as readable meter cards instead of dominant donut rings', () => {
    expect(coverageDonutSource).toContain('display: none;');
    expect(coverageDonutSource).toContain('grid-column: 1 / -1;');
    expect(coverageDonutSource).toContain('display: block;');
    expect(coverageDonutSource).toContain('width: var(--coverage-percent);');
  });
});
