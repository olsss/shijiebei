import { createPinia, setActivePinia } from 'pinia';
import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import MatchCenterView from '@/views/MatchCenterView.vue';
import OddsCenterView from '@/views/OddsCenterView.vue';
import SentimentCenterView from '@/views/SentimentCenterView.vue';
import TeamProfilesView from '@/views/TeamProfilesView.vue';
import PlayerProfilesView from '@/views/PlayerProfilesView.vue';
import matchCenterSource from '@/views/MatchCenterView.vue?raw';
import oddsCenterSource from '@/views/OddsCenterView.vue?raw';
import sentimentCenterSource from '@/views/SentimentCenterView.vue?raw';
import teamProfilesSource from '@/views/TeamProfilesView.vue?raw';
import playerProfilesSource from '@/views/PlayerProfilesView.vue?raw';
import {
  getMatchDetail,
  getPublicMatchDetail,
  listMatches,
  listPublicMatches,
} from '@/api/matches';
import {
  getMatchOdds,
  getPublicMatchOdds,
  listBookmakers,
  listOddsMarkets,
  listOddsOverview,
  listPublicBookmakers,
  listPublicOddsMarkets,
  listPublicOddsOverview,
} from '@/api/odds';
import {
  getMatchSentiment,
  getPublicMatchSentiment,
  listPublicSentimentCategories,
  listPublicSentimentOverview,
  listPublicSentimentRiskTypes,
  listSentimentCategories,
  listSentimentOverview,
  listSentimentRiskTypes,
} from '@/api/sentiment';
import {
  approveCollectionItem,
  getPlayerProfile,
  getPublicPlayerProfile,
  getPublicTeamProfile,
  getTeamProfile,
  listCollectionItems,
  listPlayerProfiles,
  listPublicPlayerProfiles,
  listPublicTeamProfiles,
  listTeamProfiles,
  rejectCollectionItem,
} from '@/api/profiles';

vi.mock('@/api/matches', () => ({
  listPublicMatches: vi.fn(),
  getPublicMatchDetail: vi.fn(),
  listMatches: vi.fn(),
  getMatchDetail: vi.fn(),
}));

vi.mock('@/api/odds', () => ({
  listPublicOddsOverview: vi.fn(),
  getPublicMatchOdds: vi.fn(),
  listPublicBookmakers: vi.fn(),
  listPublicOddsMarkets: vi.fn(),
  listOddsOverview: vi.fn(),
  getMatchOdds: vi.fn(),
  listBookmakers: vi.fn(),
  listOddsMarkets: vi.fn(),
}));

vi.mock('@/api/sentiment', () => ({
  listPublicSentimentOverview: vi.fn(),
  getPublicMatchSentiment: vi.fn(),
  listPublicSentimentCategories: vi.fn(),
  listPublicSentimentRiskTypes: vi.fn(),
  listSentimentOverview: vi.fn(),
  getMatchSentiment: vi.fn(),
  listSentimentCategories: vi.fn(),
  listSentimentRiskTypes: vi.fn(),
}));

vi.mock('@/api/profiles', () => ({
  listPublicTeamProfiles: vi.fn(),
  getPublicTeamProfile: vi.fn(),
  listPublicPlayerProfiles: vi.fn(),
  getPublicPlayerProfile: vi.fn(),
  listTeamProfiles: vi.fn(),
  getTeamProfile: vi.fn(),
  listPlayerProfiles: vi.fn(),
  getPlayerProfile: vi.fn(),
  listCollectionItems: vi.fn(),
  approveCollectionItem: vi.fn(),
  rejectCollectionItem: vi.fn(),
}));

const apiOk = <T,>(data: T) => ({ success: true, data, message: '', timestamp: '' });

const matchSummary = {
  id: 7,
  matchKey: 'france-brazil',
  matchName: 'France vs Brazil',
  matchday: '2026-06-22',
  jcCode: '001',
  competition: 'World Cup',
  stage: 'Group',
  venue: 'New York',
  kickoffTime: '2026-06-22T20:00:00',
  status: 'SCHEDULED',
  resultStatus: 'PENDING',
  homeTeamName: 'France',
  awayTeamName: 'Brazil',
  eventCount: 1,
  lineupCount: 1,
  evidenceCount: 1,
  conflictCount: 1,
};

const matchDetail = {
  summary: matchSummary,
  externalFactors: '高温天气',
  lineups: [{ id: 1, matchId: 7, teamName: 'France', playerName: 'Mbappe', role: '核心', position: 'FW', starter: true }],
  events: [{ id: 2, matchId: 7, eventMinute: 12, eventType: 'GOAL', teamName: 'France', playerName: 'Mbappe' }],
  teamStats: [{ id: 3, matchId: 7, teamName: 'France', statsType: 'RECENT', goalsFor: 2, goalsAgainst: 0, scoringMinutes: '12,55' }],
  playerStats: [{ id: 4, matchId: 7, playerName: 'Mbappe', teamName: 'France', minutesPlayed: 90, goals: 1, assists: 0 }],
  evidence: [{ id: 5, sourceType: 'OFFICIAL', sourceName: 'FIFA', summary: '官方赛程确认', reliabilityScore: 0.9 }],
  conflicts: [{ id: 6, conflictType: 'KICKOFF', fieldName: 'kickoffTime', resolutionStatus: 'PENDING' }],
};

const oddsOverview = [{
  id: 10,
  matchId: 7,
  matchName: 'France vs Brazil',
  matchday: '2026-06-22',
  jcCode: '001',
  bookmaker: 'Pinnacle',
  marketCode: 'HAD',
  marketName: '胜平负',
  snapshotType: 'LIVE',
  lineValue: '0',
  capturedAt: '2026-06-22T12:00:00',
  selectionCount: 2,
}];

const oddsDetail = {
  matchId: 7,
  matchName: 'France vs Brazil',
  jcCode: '001',
  markets: [{
    ...oddsOverview[0],
    sourceRef: 'public-feed',
    selections: [
      { id: 1, marketSnapshotId: 10, selectionCode: 'H', selectionName: '主胜', oddsValue: 1.8, impliedProbability: 0.55, selectionStatus: 'OPEN' },
    ],
  }],
};

const sentimentOverview = [{
  id: 20,
  matchId: 7,
  matchName: 'France vs Brazil',
  matchday: '2026-06-22',
  jcCode: '001',
  factorCategory: 'WEATHER',
  factorType: 'HEAT',
  title: '高温影响体能',
  summary: '高温可能影响压迫强度',
  impactDirection: 'NEGATIVE',
  evidenceLevel: 'MEDIUM',
  sourceName: 'Weather',
  stale: false,
  riskCount: 1,
  highestRiskLevel: 'MEDIUM',
}];

const sentimentDetail = {
  matchId: 7,
  matchName: 'France vs Brazil',
  jcCode: '001',
  factors: [{ ...sentimentOverview[0], confidenceScore: 0.7, reliabilityScore: 0.8 }],
  risks: [{ id: 21, matchId: 7, factorId: undefined, riskType: 'PACE', riskLevel: 'MEDIUM', riskScore: 61, title: '节奏下降', suggestedAction: 'MONITOR' }],
};

const teamSummary = {
  id: 30,
  teamKey: 'france',
  displayName: 'France',
  fifaCode: 'FRA',
  countryRegion: 'Europe',
  styleTags: '高位压迫',
  attackProfile: '边路速度优势',
  defenseProfile: '中路保护',
  publicSentiment: '阵容稳定',
  playerCount: 23,
  factCount: 1,
  latestProfileUpdate: '2026-06-22T10:00:00',
};

const teamDetail = {
  team: teamSummary,
  facts: [{ id: 31, factType: 'STYLE', title: '边路速度优势', summary: '左路推进效率高', sourceName: 'Scout', reliabilityScore: 0.8 }],
  players: [{ id: 32, playerKey: 'mbappe', displayName: 'Mbappe', shirtNumber: 10, position: 'FW', status: 'FIT', injuryStatus: '健康', cardStatus: '无' }],
  lineups: [{ matchId: 7, matchName: 'France vs Brazil', playerName: 'Mbappe', role: '核心', position: 'FW', starter: true }],
  scoringPatterns: [{ matchId: 7, matchName: 'France vs Brazil', goalsFor: 2, goalsAgainst: 0, scoringMinutes: '12,55' }],
  externalFactors: [{ matchId: 7, matchName: 'France vs Brazil', externalFactors: '高温天气' }],
  matchHistory: [{ matchId: 7, matchName: 'France vs Brazil', stage: 'Group', goalsFor: 2, goalsAgainst: 0, scoringMinutes: '12,55' }],
  evidenceCount: 2,
  conflictCount: 0,
};

const playerSummary = {
  id: 40,
  playerKey: 'mbappe',
  teamName: 'France',
  displayName: 'Mbappe',
  shirtNumber: 10,
  position: 'FW',
  status: 'FIT',
  injuryStatus: '健康',
  cardStatus: '无',
  lockerRoomStatus: '稳定',
  factCount: 1,
  latestProfileUpdate: '2026-06-22T10:00:00',
};

const playerDetail = {
  player: playerSummary,
  facts: [{ id: 41, factType: 'FORM', title: '冲刺状态良好', summary: '训练速度正常', sourceName: 'Training', reliabilityScore: 0.8 }],
};

const mountPublicView = async (component: unknown) => {
  const wrapper = mount(component, {
    global: {
      stubs: {
        RouterLink: {
          props: ['to'],
          template: '<a :href="typeof to === `string` ? to : to.path"><slot /></a>',
        },
      },
    },
  });
  await flushPromises();
  await flushPromises();
  return wrapper;
};

function resetAllMocks() {
  vi.mocked(listPublicMatches).mockReset().mockResolvedValue(apiOk([matchSummary]));
  vi.mocked(getPublicMatchDetail).mockReset().mockResolvedValue(apiOk(matchDetail));
  vi.mocked(listMatches).mockReset();
  vi.mocked(getMatchDetail).mockReset();

  vi.mocked(listPublicOddsOverview).mockReset().mockResolvedValue(apiOk(oddsOverview));
  vi.mocked(getPublicMatchOdds).mockReset().mockResolvedValue(apiOk(oddsDetail));
  vi.mocked(listPublicBookmakers).mockReset().mockResolvedValue(apiOk(['Pinnacle']));
  vi.mocked(listPublicOddsMarkets).mockReset().mockResolvedValue(apiOk([{ marketCode: 'HAD', marketName: '胜平负' }]));
  vi.mocked(listOddsOverview).mockReset();
  vi.mocked(getMatchOdds).mockReset();
  vi.mocked(listBookmakers).mockReset();
  vi.mocked(listOddsMarkets).mockReset();

  vi.mocked(listPublicSentimentOverview).mockReset().mockResolvedValue(apiOk(sentimentOverview));
  vi.mocked(getPublicMatchSentiment).mockReset().mockResolvedValue(apiOk(sentimentDetail));
  vi.mocked(listPublicSentimentCategories).mockReset().mockResolvedValue(apiOk(['WEATHER']));
  vi.mocked(listPublicSentimentRiskTypes).mockReset().mockResolvedValue(apiOk(['PACE']));
  vi.mocked(listSentimentOverview).mockReset();
  vi.mocked(getMatchSentiment).mockReset();
  vi.mocked(listSentimentCategories).mockReset();
  vi.mocked(listSentimentRiskTypes).mockReset();

  vi.mocked(listPublicTeamProfiles).mockReset().mockResolvedValue(apiOk([teamSummary]));
  vi.mocked(getPublicTeamProfile).mockReset().mockResolvedValue(apiOk(teamDetail));
  vi.mocked(listPublicPlayerProfiles).mockReset().mockResolvedValue(apiOk([playerSummary]));
  vi.mocked(getPublicPlayerProfile).mockReset().mockResolvedValue(apiOk(playerDetail));
  vi.mocked(listTeamProfiles).mockReset();
  vi.mocked(getTeamProfile).mockReset();
  vi.mocked(listPlayerProfiles).mockReset();
  vi.mocked(getPlayerProfile).mockReset();
  vi.mocked(listCollectionItems).mockReset();
  vi.mocked(approveCollectionItem).mockReset();
  vi.mocked(rejectCollectionItem).mockReset();
}

describe('Evidence center public views', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    resetAllMocks();
  });

  it('renders MatchCenter from public APIs without raw payload fields', async () => {
    const wrapper = await mountPublicView(MatchCenterView);

    expect(listPublicMatches).toHaveBeenCalledTimes(1);
    expect(getPublicMatchDetail).toHaveBeenCalledWith(7);
    expect(listMatches).not.toHaveBeenCalled();
    expect(getMatchDetail).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain('比赛中心');
    expect(wrapper.text()).toContain('France vs Brazil');
    expect(wrapper.text()).toContain('官方赛程确认');
    expect(wrapper.text()).not.toMatch(/payload|rawPayload|原始字段|当前值|新值/i);
  });

  it('renders OddsCenter from public APIs without raw odds payload', async () => {
    const wrapper = await mountPublicView(OddsCenterView);

    expect(listPublicOddsOverview).toHaveBeenCalledTimes(1);
    expect(listPublicBookmakers).toHaveBeenCalledTimes(1);
    expect(listPublicOddsMarkets).toHaveBeenCalledTimes(1);
    expect(getPublicMatchOdds).toHaveBeenCalledWith(7);
    expect(listOddsOverview).not.toHaveBeenCalled();
    expect(getMatchOdds).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain('赔率中心');
    expect(wrapper.text()).toContain('Pinnacle');
    expect(wrapper.text()).toContain('主胜');
    expect(wrapper.text()).not.toMatch(/rawPayload|原始 JSON|原始字段/i);
  });

  it('renders SentimentCenter as public cards and keeps match-level risks', async () => {
    const wrapper = await mountPublicView(SentimentCenterView);

    expect(listPublicSentimentOverview).toHaveBeenCalledTimes(1);
    expect(getPublicMatchSentiment).toHaveBeenCalledWith(7);
    expect(listSentimentOverview).not.toHaveBeenCalled();
    expect(getMatchSentiment).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain('舆情与外部因素中心');
    expect(wrapper.text()).toContain('高温影响体能');
    expect(wrapper.text()).toContain('比赛级风险');
    expect(wrapper.text()).toContain('节奏下降');
    expect(wrapper.text()).not.toMatch(/rawPayload|原始字段/i);
  });

  it('renders TeamProfiles from public APIs without review controls', async () => {
    const wrapper = await mountPublicView(TeamProfilesView);

    expect(listPublicTeamProfiles).toHaveBeenCalledTimes(1);
    expect(getPublicTeamProfile).toHaveBeenCalledWith(30);
    expect(listTeamProfiles).not.toHaveBeenCalled();
    expect(listCollectionItems).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain('球队画像中心');
    expect(wrapper.text()).toContain('France');
    expect(wrapper.text()).toContain('边路速度优势');
    expect(wrapper.text()).not.toMatch(/待审核|批准|驳回|reviewedBy|approvedBy/i);
  });

  it('renders PlayerProfiles from public APIs without review controls', async () => {
    const wrapper = await mountPublicView(PlayerProfilesView);

    expect(listPublicPlayerProfiles).toHaveBeenCalledTimes(1);
    expect(getPublicPlayerProfile).toHaveBeenCalledWith(40);
    expect(listPlayerProfiles).not.toHaveBeenCalled();
    expect(listCollectionItems).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain('球员画像中心');
    expect(wrapper.text()).toContain('Mbappe');
    expect(wrapper.text()).toContain('冲刺状态良好');
    expect(wrapper.text()).not.toMatch(/待审核|批准|驳回|reviewedBy|approvedBy/i);
  });

  it('declares H5 card layouts for all evidence pages', () => {
    for (const source of [matchCenterSource, oddsCenterSource, sentimentCenterSource, teamProfilesSource, playerProfilesSource]) {
      expect(source).not.toContain('<el-table');
      expect(source).toContain('@media (max-width: 640px)');
      expect(source).toContain('grid-template-columns: 1fr');
      expect(source).toContain('min-width: 0');
      expect(source).not.toMatch(/min-width:\s*(?:[4-9]\d{2,}|\d{4,})px/);
    }
  });
});
