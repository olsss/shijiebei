import { createPinia, setActivePinia } from 'pinia';
import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import MatchCenterView from '@/views/MatchCenterView.vue';
import EvidenceCenterView from '@/views/EvidenceCenterView.vue';
import OddsCenterView from '@/views/OddsCenterView.vue';
import SentimentCenterView from '@/views/SentimentCenterView.vue';
import TeamProfilesView from '@/views/TeamProfilesView.vue';
import PlayerProfilesView from '@/views/PlayerProfilesView.vue';
import matchCenterSource from '@/views/MatchCenterView.vue?raw';
import oddsCenterSource from '@/views/OddsCenterView.vue?raw';
import sentimentCenterSource from '@/views/SentimentCenterView.vue?raw';
import teamProfilesSource from '@/views/TeamProfilesView.vue?raw';
import playerProfilesSource from '@/views/PlayerProfilesView.vue?raw';
import evidenceCenterSource from '@/views/EvidenceCenterView.vue?raw';
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
  status: 'FINISHED',
  resultStatus: 'FINAL',
  homeTeamName: 'France',
  awayTeamName: 'Brazil',
  homeTeam: { teamName: '法国', fifaCode: 'FRA', countryIso2: 'FR' },
  awayTeam: { teamName: '巴西', fifaCode: 'BRA', countryIso2: 'BR' },
  scoreboard: { homeScore: 2, awayScore: 0, scoreDisplay: '2 - 0', winnerSide: 'HOME', resultText: '主队胜', scoreSource: 'TEAM_STATS' },
  eventCount: 1,
  lineupCount: 1,
  evidenceCount: 3,
  conflictCount: 1,
};

const matchSummaryAlt = {
  id: 8,
  matchKey: 'spain-japan',
  matchName: 'Spain vs Japan',
  matchday: '2026-06-23',
  jcCode: '002',
  competition: 'World Cup',
  stage: 'Group',
  venue: 'Los Angeles',
  kickoffTime: '2026-06-23T18:00:00',
  status: 'SCHEDULED',
  resultStatus: 'PENDING',
  homeTeamName: 'Spain',
  awayTeamName: 'Japan',
  homeTeam: { teamName: '西班牙', fifaCode: 'ESP', countryIso2: 'ES' },
  awayTeam: { teamName: '日本', fifaCode: 'JPN', countryIso2: 'JP' },
  eventCount: 0,
  lineupCount: 0,
  evidenceCount: 1,
  conflictCount: 0,
};

const matchDetail = {
  summary: matchSummary,
  externalFactors: '高温天气',
  lineups: [{ id: 1, matchId: 7, teamName: 'France', playerName: 'Mbappe', role: '核心', position: 'FW', starter: true }],
  events: [{ id: 2, matchId: 7, eventMinute: 12, eventType: 'GOAL', teamName: 'France', playerName: 'Mbappe' }],
  teamStats: [
    { id: 3, matchId: 7, teamName: '法国', statsType: 'RECENT', goalsFor: 2, goalsAgainst: 0, firstGoalMinute: 12, scoringMinutes: '12,55' },
    { id: 33, matchId: 7, teamName: '巴西', statsType: 'RECENT', goalsFor: 0, goalsAgainst: 2, scoringMinutes: '' },
  ],
  playerStats: [{ id: 4, matchId: 7, playerName: 'Mbappe', teamName: 'France', minutesPlayed: 90, goals: 1, assists: 0 }],
  evidence: [{
    id: 5,
    sourceType: 'OFFICIAL',
    sourceName: 'FIFA',
    summary: '官方赛程确认',
    reliabilityScore: 0.9,
    qualityLevel: 'HIGH',
    freshnessStatus: 'FRESH',
    supportsConclusion: '赛程 / 官方确认',
    suggestedAction: '可作为核心证据，但仍需与比分/阵容交叉核对',
  }],
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

const sentimentOverview = [
  {
    id: 20,
    matchId: 7,
    matchName: 'France vs Brazil',
    matchday: '2026-06-22',
    jcCode: '001',
    homeTeam: { teamName: 'France', fifaCode: 'FRA', countryIso2: 'FR' },
    awayTeam: { teamName: 'Brazil', fifaCode: 'BRA', countryIso2: 'BR' },
    scoreboard: { homeScore: 2, awayScore: 0, scoreDisplay: '2 - 0', winnerSide: 'HOME', resultText: '主队胜', scoreSource: 'TEAM_STATS' },
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
  },
  {
    id: 25,
    matchId: 7,
    matchName: 'France vs Brazil',
    matchday: '2026-06-22',
    jcCode: '001',
    homeTeam: { teamName: 'France', fifaCode: 'FRA', countryIso2: 'FR' },
    awayTeam: { teamName: 'Brazil', fifaCode: 'BRA', countryIso2: 'BR' },
    scoreboard: { homeScore: 2, awayScore: 0, scoreDisplay: '2 - 0', winnerSide: 'HOME', resultText: '主队胜', scoreSource: 'TEAM_STATS' },
    factorCategory: 'ATTENDANCE',
    factorType: 'CROWD',
    title: '上座人数',
    summary: 'ESPN 记录现场上座 68000 人，仅作比赛环境事实。',
    impactDirection: 'NEUTRAL',
    evidenceLevel: 'STRUCTURED_API',
    sourceName: 'ESPN Scoreboard API',
    stale: false,
    riskCount: 0,
    highestRiskLevel: 'LOW',
  },
  {
    id: 26,
    matchId: 7,
    matchName: 'France vs Brazil',
    matchday: '2026-06-22',
    jcCode: '001',
    homeTeam: { teamName: 'France', fifaCode: 'FRA', countryIso2: 'FR' },
    awayTeam: { teamName: 'Brazil', fifaCode: 'BRA', countryIso2: 'BR' },
    scoreboard: { homeScore: 2, awayScore: 0, scoreDisplay: '2 - 0', winnerSide: 'HOME', resultText: '主队胜', scoreSource: 'TEAM_STATS' },
    factorCategory: 'OTHER',
    factorType: 'BROADCAST',
    title: '转播平台',
    summary: 'ESPN Scoreboard 记录转播平台 FOX、Peacock。',
    impactDirection: 'NEUTRAL',
    evidenceLevel: 'STRUCTURED_API',
    sourceName: 'ESPN Scoreboard API',
    stale: false,
    riskCount: 0,
    highestRiskLevel: 'LOW',
  },
  {
    id: 22,
    matchId: 7,
    matchName: 'France vs Brazil',
    matchday: '2026-06-22',
    jcCode: '001',
    homeTeam: { teamName: 'France', fifaCode: 'FRA', countryIso2: 'FR' },
    awayTeam: { teamName: 'Brazil', fifaCode: 'BRA', countryIso2: 'BR' },
    scoreboard: { homeScore: 2, awayScore: 0, scoreDisplay: '2 - 0', winnerSide: 'HOME', resultText: '主队胜', scoreSource: 'TEAM_STATS' },
    factorCategory: 'SCORING',
    factorType: 'GOAL_TIME_BUCKET',
    title: '进球时间段',
    summary: '法国赛前进球时间段集中在 61-75 分钟。',
    impactDirection: 'NEUTRAL',
    evidenceLevel: 'DERIVED',
    sourceName: 'Score events',
    stale: false,
    riskCount: 0,
    highestRiskLevel: 'LOW',
  },
  {
    id: 24,
    matchId: 7,
    matchName: 'France vs Brazil',
    matchday: '2026-06-22',
    jcCode: '001',
    homeTeam: { teamName: 'France', fifaCode: 'FRA', countryIso2: 'FR' },
    awayTeam: { teamName: 'Brazil', fifaCode: 'BRA', countryIso2: 'BR' },
    scoreboard: { homeScore: 2, awayScore: 0, scoreDisplay: '2 - 0', winnerSide: 'HOME', resultText: '主队胜', scoreSource: 'TEAM_STATS' },
    factorCategory: 'REFEREE',
    factorType: 'OFFICIAL',
    title: '裁判信息',
    summary: 'ESPN gameInfo 记录本场主裁判；仅用于身份与来源追溯。',
    impactDirection: 'NEUTRAL',
    evidenceLevel: 'STRUCTURED_API',
    sourceName: 'ESPN Summary API / gameInfo',
    stale: false,
    riskCount: 0,
    highestRiskLevel: 'LOW',
  },
  {
    id: 23,
    matchId: 7,
    matchName: 'France vs Brazil',
    matchday: '2026-06-22',
    jcCode: '001',
    homeTeam: { teamName: 'France', fifaCode: 'FRA', countryIso2: 'FR' },
    awayTeam: { teamName: 'Brazil', fifaCode: 'BRA', countryIso2: 'BR' },
    scoreboard: { homeScore: 2, awayScore: 0, scoreDisplay: '2 - 0', winnerSide: 'HOME', resultText: '主队胜', scoreSource: 'TEAM_STATS' },
    factorCategory: 'TECHNICAL_STYLE',
    factorType: 'PASSING_PROFILE',
    title: '控球传球画像',
    summary: '法国平均控球 58%，传球成功率约 88%。',
    impactDirection: 'NEUTRAL',
    evidenceLevel: 'DERIVED',
    sourceName: 'Boxscore',
    stale: false,
    riskCount: 0,
    highestRiskLevel: 'LOW',
  },
  {
    id: 27,
    matchId: 7,
    matchName: 'France vs Brazil',
    matchday: '2026-06-22',
    jcCode: '001',
    homeTeam: { teamName: 'France', fifaCode: 'FRA', countryIso2: 'FR' },
    awayTeam: { teamName: 'Brazil', fifaCode: 'BRA', countryIso2: 'BR' },
    scoreboard: { homeScore: 2, awayScore: 0, scoreDisplay: '2 - 0', winnerSide: 'HOME', resultText: '主队胜', scoreSource: 'TEAM_STATS' },
    factorCategory: 'ATTACK_CONTRIBUTION',
    factorType: 'KEY_PLAYER_CONTRIBUTION',
    title: '赛前关键球员进球参与：France',
    summary: 'France赛前本届球员统计记录3名球员有进球或助攻参与，主要参与者：Mbappe 2球1助；Griezmann 0球2助。',
    impactDirection: 'NEUTRAL',
    evidenceLevel: 'DERIVED',
    sourceName: '正式库球员进球/助攻统计派生',
    stale: false,
    riskCount: 0,
    highestRiskLevel: 'LOW',
  },
  {
    id: 28,
    matchId: 7,
    matchName: 'France vs Brazil',
    matchday: '2026-06-22',
    jcCode: '001',
    homeTeam: { teamName: 'France', fifaCode: 'FRA', countryIso2: 'FR' },
    awayTeam: { teamName: 'Brazil', fifaCode: 'BRA', countryIso2: 'BR' },
    scoreboard: { homeScore: 2, awayScore: 0, scoreDisplay: '2 - 0', winnerSide: 'HOME', resultText: '主队胜', scoreSource: 'TEAM_STATS' },
    factorCategory: 'GOALKEEPING',
    factorType: 'SAVE_PRESSURE_PROFILE',
    title: '赛前门将扑救与承压画像：France',
    summary: 'France赛前本届门将统计扑救10次、失球1球、零封2场；本资料只来自正式库基础统计。',
    impactDirection: 'NEUTRAL',
    evidenceLevel: 'DERIVED',
    sourceName: '正式库 ESPN 基础技术/球员统计派生',
    stale: false,
    riskCount: 0,
    highestRiskLevel: 'LOW',
  },
  {
    id: 31,
    matchId: 7,
    matchName: 'France vs Brazil',
    matchday: '2026-06-22',
    jcCode: '001',
    homeTeam: { teamName: 'France', fifaCode: 'FRA', countryIso2: 'FR' },
    awayTeam: { teamName: 'Brazil', fifaCode: 'BRA', countryIso2: 'BR' },
    scoreboard: { homeScore: 2, awayScore: 0, scoreDisplay: '2 - 0', winnerSide: 'HOME', resultText: '主队胜', scoreSource: 'TEAM_STATS' },
    factorCategory: 'INFRACTION_PROFILE',
    factorType: 'FOUL_OFFSIDE_PENALTY_PROFILE',
    title: '赛前犯规/越位基础画像：France',
    summary: 'France赛前本届 ESPN 技术样本犯规/越位基础画像：完整技术样本2场，犯规18次、越位3次；点球尝试1次、命中1次。该画像只描述已入库基础技术统计样本。',
    impactDirection: 'NEUTRAL',
    evidenceLevel: 'DERIVED',
    sourceName: '正式库 ESPN 基础技术统计派生',
    stale: false,
    riskCount: 0,
    highestRiskLevel: 'LOW',
  },
  {
    id: 29,
    matchId: 7,
    matchName: 'France vs Brazil',
    matchday: '2026-06-22',
    jcCode: '001',
    homeTeam: { teamName: 'France', fifaCode: 'FRA', countryIso2: 'FR' },
    awayTeam: { teamName: 'Brazil', fifaCode: 'BRA', countryIso2: 'BR' },
    scoreboard: { homeScore: 2, awayScore: 0, scoreDisplay: '2 - 0', winnerSide: 'HOME', resultText: '主队胜', scoreSource: 'TEAM_STATS' },
    factorCategory: 'SQUAD_PROFILE',
    factorType: 'TEAM_SIDE_ROSTER_BASELINE',
    title: '赛前阵容年龄经验画像：France',
    summary: 'France赛前官方名单阵容结构画像：名单26人，门将3人、后卫8人、中场7人、前锋8人；年龄按2026-06-24计算，平均27.1岁。该画像只描述官方名单基础字段，不作优劣解读。',
    impactDirection: 'NEUTRAL',
    evidenceLevel: 'DERIVED',
    sourceName: 'FIFA 官方 Squad List PDF',
    stale: false,
    riskCount: 0,
    highestRiskLevel: 'LOW',
  },
  {
    id: 30,
    matchId: 7,
    matchName: 'France vs Brazil',
    matchday: '2026-06-22',
    jcCode: '001',
    homeTeam: { teamName: 'France', fifaCode: 'FRA', countryIso2: 'FR' },
    awayTeam: { teamName: 'Brazil', fifaCode: 'BRA', countryIso2: 'BR' },
    scoreboard: { homeScore: 2, awayScore: 0, scoreDisplay: '2 - 0', winnerSide: 'HOME', resultText: '主队胜', scoreSource: 'TEAM_STATS' },
    factorCategory: 'LINEUP_STRUCTURE',
    factorType: 'CONFIRMED_STARTING_XI_STRUCTURE',
    title: '赛前已确认首发位置结构：France',
    summary: 'France赛前本届已确认首发位置结构画像：已入库阵容样本3场、确认首发33人次；按位置编码归并：后卫线12次、中场线11次、前锋线7次、门将3次。',
    impactDirection: 'NEUTRAL',
    evidenceLevel: 'DERIVED',
    sourceName: '正式库 ESPN Summary API 阵容记录派生',
    stale: false,
    riskCount: 0,
    highestRiskLevel: 'LOW',
  },
];

const sentimentDetail = {
  matchId: 7,
  matchName: 'France vs Brazil',
  jcCode: '001',
  homeTeam: { teamName: 'France', fifaCode: 'FRA', countryIso2: 'FR' },
  awayTeam: { teamName: 'Brazil', fifaCode: 'BRA', countryIso2: 'BR' },
  scoreboard: { homeScore: 2, awayScore: 0, scoreDisplay: '2 - 0', winnerSide: 'HOME', resultText: '主队胜', scoreSource: 'TEAM_STATS' },
  factors: [
    { ...sentimentOverview[0], confidenceScore: 0.7, reliabilityScore: 0.8 },
    { ...sentimentOverview[1], confidenceScore: 0.73, reliabilityScore: 0.82 },
    { ...sentimentOverview[2], confidenceScore: 0.73, reliabilityScore: 0.82 },
    { ...sentimentOverview[3], confidenceScore: 0.74, reliabilityScore: 0.74 },
    { ...sentimentOverview[4], confidenceScore: 0.76, reliabilityScore: 0.8 },
    { ...sentimentOverview[5], confidenceScore: 0.75, reliabilityScore: 0.75 },
    { ...sentimentOverview[6], confidenceScore: 0.74, reliabilityScore: 0.76 },
    { ...sentimentOverview[7], confidenceScore: 0.74, reliabilityScore: 0.76 },
    { ...sentimentOverview[8], confidenceScore: 0.82, reliabilityScore: 0.86 },
    { ...sentimentOverview[9], confidenceScore: 0.77, reliabilityScore: 0.78 },
    { ...sentimentOverview[10], confidenceScore: 0.77, reliabilityScore: 0.78 },
  ],
  risks: [{ id: 21, matchId: 7, factorId: undefined, riskType: 'PACE', riskLevel: 'MEDIUM', riskScore: 61, title: '节奏下降', suggestedAction: 'MONITOR' }],
};

const teamSummary = {
  id: 30,
  teamKey: 'france',
  displayName: 'France',
  fifaCode: 'FRA',
  countryRegion: 'Europe',
  groupName: 'A组',
  styleTags: '高位压迫',
  attackProfile: '边路速度优势',
  defenseProfile: '中路保护',
  publicSentiment: '阵容稳定',
  playerCount: 23,
  factCount: 1,
  technicalMetricCount: 1,
  advancedMetricCount: 0,
  groupStandingRank: 1,
  groupStandingPoints: 7,
  groupStandingRecord: '3场2胜1平0负',
  groupGoalDifference: 4,
  groupStandingSummary: 'France当前A组第1/4名，3场2胜1平0负，进5球失1球，净胜球+4，积分7。',
  latestProfileUpdate: '2026-06-22T10:00:00',
};

const teamSummaryAlt = {
  id: 31,
  teamKey: 'brazil',
  displayName: 'Brazil',
  fifaCode: 'BRA',
  countryRegion: 'South America',
  groupName: 'B组',
  styleTags: '["世界杯2026参赛队","B组"]',
  attackProfile: '',
  defenseProfile: '',
  publicSentiment: '',
  playerCount: 26,
  factCount: 0,
  technicalMetricCount: 1,
  advancedMetricCount: 0,
  groupStandingRank: 2,
  groupStandingPoints: 4,
  groupStandingRecord: '3场1胜1平1负',
  groupGoalDifference: 1,
  groupStandingSummary: 'Brazil当前B组第2/4名，3场1胜1平1负，进4球失3球，净胜球+1，积分4。',
  latestProfileUpdate: '2026-06-22T10:00:00',
};

const teamDetail = {
  team: teamSummary,
  facts: [
    { id: 31, factType: 'STYLE', title: '边路速度优势', summary: '左路推进效率高', sourceName: 'Scout', reliabilityScore: 0.8 },
    { id: 34, factType: 'SCORING', title: '进球时间段与方式画像', summary: '已确认进球时间 2/2 球，特殊进球方式待核对。', sourceName: 'Score events', reliabilityScore: 0.74 },
    { id: 35, factType: 'TOURNAMENT_TECHNICAL_SUMMARY', title: '世界杯基础技术统计摘要', summary: '场均射门 13.7、射正 6.3、控球 54.0%、角球 5.7。', sourceName: 'Boxscore', reliabilityScore: 0.76 },
    { id: 36, factType: 'TECHNICAL_STYLE', title: '基础技术风格画像', summary: '传球成功率约 88%，场均传中 15 次，防守活动中等。', sourceName: 'Boxscore', reliabilityScore: 0.75 },
    { id: 37, factType: 'GROUP_QUALIFICATION', title: '小组出线状态', summary: '以小组第2名身份进入32强赛；不代表战意强弱、轮换意图或胜率结论。', sourceName: 'DB派生：小组积分与32强赛程', reliabilityScore: 0.78 },
    { id: 38, factType: 'TOURNAMENT_FORM', title: '本届赛果走势', summary: '本届已完赛3场，2胜1平0负，近3场为胜-平-胜；仅描述已完赛结果。', sourceName: 'DB派生：正式比分统计', reliabilityScore: 0.78 },
    { id: 39, factType: 'GOAL_CONTRIBUTION', title: '本届进球参与贡献分布', summary: '球员统计记录3名球员有进球或助攻参与，主要参与者：Mbappe 2球1助；Griezmann 0球2助。', sourceName: '正式库球员进球/助攻统计派生', reliabilityScore: 0.76 },
    { id: 40, factType: 'GOALKEEPING_SAVE_PROFILE', title: '本届门将扑救与防守承压画像', summary: '门将统计扑救10次、失球1球、零封2场；本资料只来自正式库基础统计。', sourceName: '正式库 ESPN 基础技术/球员统计派生', reliabilityScore: 0.76 },
    { id: 43, factType: 'FOUL_OFFSIDE_PENALTY_PROFILE', title: '犯规/越位/点球尝试基础画像', summary: 'ESPN 技术样本犯规18次、越位3次；点球尝试1次、命中1次。', sourceName: '正式库 ESPN 基础技术统计派生', reliabilityScore: 0.76 },
    { id: 41, factType: 'SQUAD_PROFILE', title: '阵容结构与年龄经验基础画像', summary: '官方名单阵容结构画像：名单26人，门将3人、后卫8人、中场7人、前锋8人；年龄按2026-06-24计算，平均27.1岁。', sourceName: 'FIFA 官方 Squad List PDF', reliabilityScore: 0.86 },
    { id: 42, factType: 'CONFIRMED_STARTING_XI_STRUCTURE', title: '已确认首发位置结构画像', summary: '已入库阵容样本3场、确认首发33人次；按位置编码归并：后卫线12次、中场线11次、前锋线7次、门将3次。', sourceName: '正式库 ESPN Summary API 阵容记录派生', reliabilityScore: 0.78 },
  ],
  players: [{ id: 32, playerKey: 'mbappe', displayName: 'Mbappe', shirtNumber: 10, position: 'FW', status: 'FIT', injuryStatus: '健康', cardStatus: '无' }],
  lineups: [{ matchId: 7, matchName: 'France vs Brazil', playerName: 'Mbappe', role: '核心', position: 'FW', starter: true }],
  scoringPatterns: [{ matchId: 7, matchName: 'France vs Brazil', goalsFor: 2, goalsAgainst: 0, scoringMinutes: '12,55' }],
  externalFactors: [{ matchId: 7, matchName: 'France vs Brazil', externalFactors: '高温天气' }],
  matchHistory: [{ matchId: 7, matchName: 'France vs Brazil', stage: 'Group', goalsFor: 2, goalsAgainst: 0, scoringMinutes: '12,55' }],
  evidenceCount: 2,
  conflictCount: 0,
  readiness: {
    score: 72,
    level: 'PARTIAL',
    summary: '画像可用于初步了解，但仍有关键数据待补采。',
    strengths: ['已有进攻画像', '球员名单覆盖较好'],
    missingDimensions: ['缺 xG、xGA、PPDA、射门等高阶指标'],
    nextActions: ['补 xG/xGA/PPDA、射门、控球、定位球等高阶指标'],
  },
  latestMetric: { xg: 1.8, xga: 0.9, ppda: 9.2, shots: 13, possessionPct: 58.5, formScore: 76, sourceName: 'Scout', capturedAt: '2026-06-22T10:00:00' },
};

const playerSummary = {
  id: 40,
  playerKey: 'mbappe',
  teamName: 'France',
  team: { teamId: 30, teamName: 'France', fifaCode: 'FRA', countryIso2: 'FR' },
  displayName: 'Mbappe',
  shirtNumber: 10,
  position: 'FW',
  status: 'FIT',
  injuryStatus: '健康',
  cardStatus: '无',
  lockerRoomStatus: '稳定',
  factCount: 3,
  performanceMetricCount: 1,
  advancedMetricCount: 0,
  latestProfileUpdate: '2026-06-22T10:00:00',
};

const playerSummaryAlt = {
  id: 41,
  playerKey: 'alisson',
  teamName: 'Brazil',
  team: { teamId: 31, teamName: 'Brazil', fifaCode: 'BRA', countryIso2: 'BR' },
  displayName: 'Alisson',
  shirtNumber: 1,
  position: 'GK',
  status: 'DOUBTFUL',
  injuryStatus: '轻微不适',
  cardStatus: '无',
  lockerRoomStatus: '稳定',
  factCount: 0,
  performanceMetricCount: 1,
  advancedMetricCount: 0,
  latestProfileUpdate: '2026-06-21T09:30:00',
};

const playerDetail = {
  player: playerSummary,
  facts: [
    { id: 41, factType: 'FORM', title: '冲刺状态良好', summary: '训练速度正常', sourceName: 'Training', reliabilityScore: 0.8 },
    { id: 42, factType: 'DISCIPLINE', title: '牌面纪律记录', summary: '累计黄牌 1 张；不等于官方停赛名单。', sourceName: 'Stats', reliabilityScore: 0.74 },
    { id: 43, factType: 'WORKLOAD', title: '阵容负荷与首发连续性', summary: '连续首发 2 场；不等于真实疲劳。', sourceName: 'Lineup', reliabilityScore: 0.71 },
    { id: 44, factType: 'SCORING', title: '进球参与与时间段', summary: '正式球员统计记录进球 1 球、助攻 0 次。', sourceName: 'Score events', reliabilityScore: 0.75 },
    { id: 45, factType: 'SHOT_PROFILE', title: '基础射门画像', summary: '射门 4 次、射正 2 次，射正率 50%。', sourceName: 'Boxscore', reliabilityScore: 0.73 },
    { id: 46, factType: 'GOALKEEPER_SAVE_PROFILE', title: '本届门将扑救画像', summary: '出场记录3次，扑救10次，门将失球1球。', sourceName: '正式库 ESPN 球员比赛统计派生', reliabilityScore: 0.76 },
  ],
  readiness: {
    score: 78,
    level: 'PARTIAL',
    summary: '球员画像可用于初步了解。',
    strengths: ['号码和位置可读', '伤病状态已同步'],
    missingDimensions: ['缺预计首发概率'],
    nextActions: ['补近三场出场时间、xG/xA、训练负荷'],
  },
  latestMetric: { minutesPlayed: 86, xg: 0.7, xa: 0.2, expectedStartingProbability: 0.82, availabilityScore: 88, sourceName: 'Training', capturedAt: '2026-06-22T10:00:00' },
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
  vi.mocked(listPublicMatches).mockReset().mockResolvedValue(apiOk([matchSummary, matchSummaryAlt]));
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
  vi.mocked(listPublicSentimentCategories).mockReset().mockResolvedValue(apiOk(['WEATHER', 'ATTENDANCE', 'REFEREE', 'ATTACK_CONTRIBUTION', 'SCORING', 'TECHNICAL_STYLE', 'GOALKEEPING', 'INFRACTION_PROFILE', 'SQUAD_PROFILE', 'LINEUP_STRUCTURE']));
  vi.mocked(listPublicSentimentRiskTypes).mockReset().mockResolvedValue(apiOk(['PACE']));
  vi.mocked(listSentimentOverview).mockReset();
  vi.mocked(getMatchSentiment).mockReset();
  vi.mocked(listSentimentCategories).mockReset();
  vi.mocked(listSentimentRiskTypes).mockReset();

  vi.mocked(listPublicTeamProfiles).mockReset().mockResolvedValue(apiOk([teamSummary]));
  vi.mocked(getPublicTeamProfile).mockReset().mockResolvedValue(apiOk(teamDetail));
  vi.mocked(listPublicPlayerProfiles).mockReset().mockResolvedValue(apiOk([playerSummary, playerSummaryAlt]));
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
    window.history.pushState({}, '', '/');
    setActivePinia(createPinia());
    resetAllMocks();
  });

  it('renders EvidenceCenter overview as a data-first hub', async () => {
    const wrapper = await mountPublicView(EvidenceCenterView);

    expect(listPublicMatches).toHaveBeenCalledTimes(1);
    expect(listPublicOddsOverview).toHaveBeenCalledTimes(1);
    expect(listPublicSentimentOverview).toHaveBeenCalledTimes(1);
    expect(listPublicTeamProfiles).toHaveBeenCalledTimes(1);
    expect(listPublicPlayerProfiles).toHaveBeenCalledTimes(1);
    expect(getPublicMatchDetail).not.toHaveBeenCalled();
    expect(getPublicMatchOdds).not.toHaveBeenCalled();
    expect(getPublicMatchSentiment).not.toHaveBeenCalled();
    expect(getPublicTeamProfile).not.toHaveBeenCalled();
    expect(getPublicPlayerProfile).not.toHaveBeenCalled();

    expect(wrapper.text()).toContain('证据中心总览');
    expect(wrapper.text()).toContain('比赛状态');
    expect(wrapper.find('[data-test="evidence-hub-score"] .coverage-donut').exists()).toBe(true);
    expect(wrapper.find('[data-test="evidence-hub-score"]').attributes('tabindex')).toBe('0');
    expect(wrapper.findAll('[data-test="evidence-catalog-rings"] .coverage-donut')).toHaveLength(6);
    expect(wrapper.find('[data-test="evidence-catalog-rings"]').attributes('tabindex')).toBe('0');
    expect(wrapper.find('[data-test="evidence-catalog-rings"]').text()).toContain('证据目录结构');
    expect(wrapper.find('[data-test="evidence-catalog-rings"]').text()).toContain('公开比赛');
    expect(wrapper.find('[data-test="evidence-catalog-rings"]').text()).toContain('来源证据');
    expect(wrapper.find('[data-test="evidence-catalog-rings"]').text()).toContain('市场快照');
    expect(wrapper.find('[data-test="evidence-catalog-rings"]').text()).toContain('外部因素');
    expect(wrapper.find('[data-test="evidence-catalog-rings"]').text()).toContain('画像资料');
    expect(wrapper.find('[data-test="evidence-catalog-rings"]').text()).toContain('材料合计');
    expect(wrapper.text()).toContain('比赛中心');
    expect(wrapper.text()).toContain('赔率中心');
    expect(wrapper.text()).toContain('舆情与外部因素');
    expect(wrapper.text()).toContain('球队画像');
    expect(wrapper.text()).toContain('球员画像');
    expect(wrapper.text()).toContain('AI 足球分析数据维度');
    expect(wrapper.findAll('.coverage-panel--data-map .ai-data-ring-grid .coverage-donut')).toHaveLength(5);
    expect(wrapper.text()).toContain('专业高阶指标');
    expect(wrapper.text()).toContain('伤停训练/球员状态');
    expect(wrapper.text()).toContain('市场价格/公众热度');
    expect(wrapper.text()).toContain('天气裁判/赛程旅行');
    expect(wrapper.text()).toContain('来源一致性');
    expect(wrapper.text()).toContain('公众热度/市场倾向');
    expect(wrapper.text()).toContain('外因缺口');
    const commandBoard = wrapper.find('[data-test="evidence-command-board"]');
    expect(commandBoard.exists()).toBe(true);
    expect(commandBoard.text()).toContain('PC 证据态势');
    expect(commandBoard.text()).toContain('主要数据缺口');
    expect(commandBoard.text()).toContain('风险排序');
    expect(commandBoard.text()).toContain('证据冲突');
    expect(commandBoard.text()).toContain('来源冲突');
    expect(commandBoard.text()).toContain('外部因素');
    expect(commandBoard.text()).toContain('场外变量缺口');
    expect(commandBoard.text()).toContain('五类证据覆盖');
    expect(commandBoard.text()).toContain('证据维度覆盖');
    expect(commandBoard.findAll('.readiness-ring-grid .coverage-donut')).toHaveLength(5);
    expect(commandBoard.find('.readiness-ring-grid').attributes('tabindex')).toBe('0');
    expect(commandBoard.text()).toContain('比分/赛果');
    expect(commandBoard.text()).toContain('胜负与比分记录');
    expect(commandBoard.text()).toContain('市场快照');
    expect(commandBoard.text()).toContain('外部风险');
    expect(commandBoard.text()).toContain('球队资料');
    expect(commandBoard.text()).toContain('球员可用性');
    expect(commandBoard.text()).toContain('关键球员可用性');
    expect(commandBoard.findAll('a')).toHaveLength(3);
    expect(commandBoard.attributes('tabindex')).toBe('0');
    expect(wrapper.find('[data-test="evidence-quality-board"]').exists()).toBe(true);
    expect(wrapper.find('[data-test="evidence-quality-board"]').attributes('tabindex')).toBe('0');
    expect(wrapper.find('[data-test="evidence-quality-board"]').text()).toContain('证据质量速览');
    expect(wrapper.find('[data-test="evidence-quality-board"]').text()).toContain('比分记录');
    expect(wrapper.find('[data-test="evidence-quality-board"]').text()).toContain('市场目录');
    expect(wrapper.find('[data-test="evidence-quality-board"]').text()).toContain('外因覆盖');
    expect(wrapper.find('[data-test="evidence-quality-board"]').text()).toContain('球队画像');
    expect(wrapper.find('[data-test="evidence-quality-board"]').text()).toContain('球员状态');
    expect(wrapper.find('[data-test="evidence-quality-board"]').text()).toContain('公开缺口');
    expect(wrapper.find('[data-test="evidence-quality-board"] .quality-bars').attributes('tabindex')).toBe('0');
    expect(wrapper.findAll('[data-test="evidence-quality-board"] .quality-gap-rings .coverage-donut')).toHaveLength(5);
    expect(wrapper.find('[data-test="evidence-quality-board"]').text()).toContain('比分缺口');
    expect(wrapper.find('[data-test="evidence-quality-board"]').text()).toContain('证据冲突');
    expect(wrapper.find('[data-test="evidence-quality-board"]').text()).toContain('画像事实缺口');
    expect(wrapper.find('[data-test="evidence-quality-board"]').text()).toContain('市场缺口');
    expect(wrapper.find('[data-test="evidence-data-literacy-board"]').exists()).toBe(true);
    expect(wrapper.find('[data-test="evidence-data-literacy-board"] .data-literacy-bars').attributes('tabindex')).toBe('0');
    expect(wrapper.find('[data-test="evidence-data-literacy-board"]').text()).toContain('数据覆盖矩阵');
    expect(wrapper.find('[data-test="evidence-data-literacy-board"]').text()).toContain('六类证据覆盖情况');
    expect(wrapper.find('[data-test="evidence-data-literacy-board"]').text()).toContain('比分与胜负');
    expect(wrapper.find('[data-test="evidence-data-literacy-board"]').text()).toContain('球队基础画像');
    expect(wrapper.find('[data-test="evidence-data-literacy-board"]').text()).toContain('关键球员状态');
    expect(wrapper.find('[data-test="beginner-evidence-path"]').exists()).toBe(false);
    expect(wrapper.text()).not.toMatch(/证据路径|证据覆盖概览|本场核查点|证据中心术语对照/);
    expect(wrapper.find('[data-test="evidence-data-literacy-board"]').text()).toContain('市场价格');
    expect(wrapper.find('[data-test="evidence-data-literacy-board"]').text()).toContain('来源一致性');
    const supplementBoard = wrapper.find('[data-test="evidence-supplement-priority"]');
    expect(supplementBoard.exists()).toBe(true);
    expect(supplementBoard.attributes('tabindex')).toBe('0');
    expect(supplementBoard.text()).toContain('AI 分析补采优先级');
    expect(supplementBoard.text()).toContain('xG/xGA/PPDA 与攻防画像');
    expect(supplementBoard.text()).toContain('专业 xG/PPDA 仍需补采');
    expect(supplementBoard.text()).toContain('伤停、牌面、训练与预计首发');
    expect(supplementBoard.text()).toContain('开盘、当前与价格变化时间线');
    expect(supplementBoard.text()).toContain('国旗、洲别、小组与来源追踪');
    expect(supplementBoard.findAll('.supplement-priority-card')).toHaveLength(6);
    expect(supplementBoard.findAll('a')).toHaveLength(6);
    expect(evidenceCenterSource).toContain('Opta/StatsBomb');
    expect(evidenceCenterSource).toContain('max-height: min(42dvh, 340px)');
    expect(evidenceCenterSource).toContain('supplement-priority-board:focus-visible');
    expect(evidenceCenterSource).toContain('supplement-priority-card__link');
    expect(evidenceCenterSource).toContain('min-height: 44px');
    expect(evidenceCenterSource).toContain('公开证据资料较齐');
    expect(wrapper.text()).toContain('证据覆盖评分');
    expect(wrapper.findAll('[data-test="evidence-route-grid"] a')).toHaveLength(5);
    expect(wrapper.find('[data-test="evidence-route-grid"]').attributes('tabindex')).toBe('0');
    expect(wrapper.find('a[href="/evidence/matches"]').exists()).toBe(true);
    expect(wrapper.find('a[href="/evidence/odds"]').exists()).toBe(true);
    expect(wrapper.find('a[href="/evidence/sentiment"]').exists()).toBe(true);
    expect(wrapper.find('a[href="/evidence/teams"]').exists()).toBe(true);
    expect(wrapper.find('a[href="/evidence/players"]').exists()).toBe(true);
    expect(wrapper.text()).not.toMatch(/是否健壮|较完整|完整度/);
    expect(evidenceCenterSource).not.toContain('max-height: min(52dvh, 420px)');
    expect(evidenceCenterSource).toContain('max-height: min(44dvh, 340px)');
    expect(evidenceCenterSource).not.toContain('max-height: min(38dvh, 280px)');
    expect(evidenceCenterSource).toContain('max-height: min(32dvh, 240px)');
    expect(evidenceCenterSource).toContain('min-height: 44px');
    expect(evidenceCenterSource).toContain('grid-template-columns: repeat(2, minmax(0, 1fr));');
    expect(wrapper.text()).not.toMatch(/rawPayload|原始 JSON|票号|投注指令|stake|ticketNo/i);
  });

  it('renders MatchCenter from public APIs without raw payload fields', async () => {
    const wrapper = await mountPublicView(MatchCenterView);

    expect(listPublicMatches).toHaveBeenCalledTimes(1);
    expect(getPublicMatchDetail).toHaveBeenCalledWith(7);
    expect(listMatches).not.toHaveBeenCalled();
    expect(getMatchDetail).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain('比赛中心');
    expect(wrapper.text()).toContain('France vs Brazil');
    expect(wrapper.findAll('[data-test="match-catalog-rings"] .coverage-donut')).toHaveLength(5);
    expect(wrapper.find('[data-test="match-catalog-rings"]').attributes('tabindex')).toBe('0');
    expect(wrapper.find('[data-test="match-catalog-rings"]').text()).toContain('比赛目录结构');
    expect(wrapper.find('[data-test="match-catalog-rings"]').text()).toContain('公开比赛');
    expect(wrapper.find('[data-test="match-catalog-rings"]').text()).toContain('比分记录');
    expect(wrapper.find('[data-test="match-catalog-rings"]').text()).toContain('事件材料');
    expect(wrapper.find('[data-test="match-catalog-rings"]').text()).toContain('阵容材料');
    expect(wrapper.find('[data-test="match-catalog-rings"]').text()).toContain('冲突状态');
    expect(wrapper.find('[data-test="match-score-overview"]').exists()).toBe(true);
    expect(wrapper.find('[data-test="match-score-overview"]').attributes('tabindex')).toBe('0');
    expect(wrapper.find('[data-test="match-score-overview"] .coverage-donut').exists()).toBe(true);
    expect(wrapper.find('[data-test="match-score-overview"]').text()).toContain('比分状态总览');
    expect(wrapper.find('[data-test="match-score-overview"]').text()).toContain('胜负强对比已关联');
    expect(wrapper.find('[data-test="match-score-overview"]').text()).toContain('有比分');
    expect(wrapper.find('[data-test="match-score-overview"]').text()).toContain('待开球');
    expect(wrapper.find('[data-test="match-score-overview"]').text()).toContain('完赛缺比分');
    expect(wrapper.find('[data-test="match-score-overview"]').text()).toContain('只看有比分');
    expect(wrapper.find('[data-test="match-score-overview"]').text()).toContain('筛完赛缺比分');
    expect(wrapper.find('[data-test="match-result-contrast"]').text()).toContain('胜负强对比');
    expect(wrapper.find('[data-test="match-result-contrast"]').text()).toContain('法国 胜出');
    expect(wrapper.find('[data-test="match-result-contrast"]').text()).toContain('2 - 0');
    expect(wrapper.find('[data-test="match-result-contrast"]').text()).toContain('两队强弱条');
    expect(wrapper.find('[data-test="match-result-contrast"]').text()).toContain('首球时间');
    expect(wrapper.find('[data-test="match-result-contrast"]').text()).toContain('证据可读度');
    expect(wrapper.find('.side-panel').attributes('tabindex')).toBe('0');
    expect(wrapper.find('.detail-panel').attributes('tabindex')).toBe('0');
    expect(wrapper.find('.match-visual-grid').attributes('tabindex')).toBe('0');
    expect(wrapper.find('.evidence-quality-panel').attributes('tabindex')).toBe('0');
    expect(wrapper.findAll('[data-test="match-result-contrast"] .match-evidence-ring-grid .coverage-donut')).toHaveLength(4);
    expect(wrapper.find('[data-test="match-result-contrast"]').text()).not.toContain('核查顺序');
    expect(wrapper.text()).toContain('已筛出 2 / 2 场比赛');
    expect(wrapper.text()).toContain('证据质量面板');
    expect(wrapper.findAll('.evidence-quality-panel .quality-rings .coverage-donut')).toHaveLength(5);
    expect(wrapper.text()).toContain('高可信');
    expect(wrapper.text()).toContain('中可信');
    expect(wrapper.text()).toContain('低可信');
    expect(wrapper.text()).toContain('未评分');
    expect(wrapper.text()).toContain('时效风险');
    expect(wrapper.text()).toContain('赛程 / 官方确认');
    expect(wrapper.text()).toContain('核心证据');
    expect(wrapper.text()).toContain('官方赛程确认');
    expect(matchCenterSource).toContain('市场价格/舆情变化');
    expect(matchCenterSource).toContain('max-height: min(88dvh, 860px)');
    expect(matchCenterSource).toMatch(/\.result-contrast-panel\s*\{\s*max-height: none;\s*overflow: visible;/s);
    expect(matchCenterSource).toMatch(/\.detail-panel\s*\{\s*max-height: none;\s*overflow: visible;/s);
    expect(matchCenterSource).toMatch(/\.evidence-quality-layout\s*\{[^}]*max-height: none;[^}]*overflow: visible;/s);
    expect(matchCenterSource).toMatch(/\.card-grid > \.info-card\s*\{\s*max-height: none;\s*overflow: visible;/s);
    expect(matchCenterSource).toMatch(/\.match-list-scroll\s*\{\s*max-height: min\(48dvh, 420px\);\s*overflow: auto;[^}]*scrollbar-width: none;/s);
    expect(matchCenterSource).toContain('min-height: 44px');
    expect(matchCenterSource).toContain('grid-template-columns: repeat(2, minmax(0, 1fr));');
    expect(matchCenterSource).not.toContain('后续盘口/舆情变化');
    expect(wrapper.text()).not.toMatch(/payload|rawPayload|原始字段|当前值|新值/i);
  });

  it('applies match center route filters from evidence workbench CTAs', async () => {
    window.history.pushState({}, '', '/evidence/matches?filter=CONFLICT');

    const wrapper = await mountPublicView(MatchCenterView);

    expect(wrapper.find('select[aria-label="按比分、冲突或证据筛选"]').element).toMatchObject({ value: 'CONFLICT' });
    expect(wrapper.text()).toContain('已筛出 1 / 2 场比赛');
    expect(wrapper.text()).toContain('清除筛选');
    expect(wrapper.text()).toContain('France vs Brazil');
    expect(wrapper.text()).not.toContain('Spain vs Japan');
  });

  it('filters, recovers and paginates the public match schedule list', async () => {
    const longMatchList = Array.from({ length: 12 }, (_, index) => ({
      ...matchSummaryAlt,
      id: 100 + index,
      matchKey: `generated-${index}`,
      matchName: index === 10 ? 'Germany vs Japan' : `Generated ${index} vs Team`,
      jcCode: `${200 + index}`,
      homeTeam: index === 10 ? { teamName: '德国', fifaCode: 'GER', countryIso2: 'DE' } : matchSummaryAlt.homeTeam,
      awayTeam: index === 10 ? { teamName: '日本', fifaCode: 'JPN', countryIso2: 'JP' } : matchSummaryAlt.awayTeam,
      conflictCount: index === 2 ? 1 : 0,
    }));
    vi.mocked(listPublicMatches).mockResolvedValueOnce(apiOk([matchSummary, ...longMatchList]));

    const wrapper = await mountPublicView(MatchCenterView);

    expect(wrapper.text()).toContain('已筛出 13 / 13 场比赛');
    expect(wrapper.findAll('.side-panel .list-card')).toHaveLength(10);
    expect(wrapper.text()).toContain('第 1 / 2 页');

    await wrapper.find('input[aria-label="搜索比赛、球队、竞彩编号或场地"]').setValue('Germany');
    expect(wrapper.text()).toContain('已筛出 1 / 13 场比赛');
    expect(wrapper.findAll('.side-panel .list-card')).toHaveLength(1);
    expect(wrapper.find('.side-panel .list-card').text()).toContain('Germany vs Japan');

    await wrapper.find('select[aria-label="按比分、冲突或证据筛选"]').setValue('CONFLICT');
    expect(wrapper.text()).toContain('没有找到匹配比赛');

    await wrapper.find('.side-panel .ghost-button').trigger('click');
    expect(wrapper.text()).toContain('已筛出 13 / 13 场比赛');

    const quickButtons = wrapper.findAll('[data-test="match-score-overview"] button');
    await quickButtons.find((button) => button.text() === '只看待开球')!.trigger('click');
    expect(wrapper.text()).toContain('已筛出 12 / 13 场比赛');
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
    expect(wrapper.text()).toContain('已筛出 1 / 1 个市场快照');
    expect(wrapper.find('[data-test="odds-reading-board"]').exists()).toBe(false);
    expect(wrapper.findAll('[data-test="odds-overview-rings"] .coverage-donut')).toHaveLength(5);
    expect(wrapper.find('[data-test="odds-overview-rings"] .odds-overview-rings').attributes('tabindex')).toBe('0');
    expect(wrapper.text()).toContain('公开市场结构');
    expect(wrapper.text()).toContain('市场快照');
    expect(wrapper.text()).toContain('覆盖比赛');
    expect(wrapper.text()).toContain('来源公司');
    expect(wrapper.text()).toContain('玩法字典');
    expect(wrapper.text()).toContain('选项总数');
    const timelineAudit = wrapper.find('[data-test="market-timeline-audit"]');
    expect(timelineAudit.exists()).toBe(true);
    expect(timelineAudit.attributes('tabindex')).toBe('0');
    expect(timelineAudit.text()).toContain('单点快照 vs 价格变化时间线');
    expect(timelineAudit.text()).toContain('真实价格变化仍需开盘、当前和至少一个中间时点');
    expect(timelineAudit.text()).toContain('UNKNOWN');
    expect(oddsCenterSource).toContain('market-timeline-audit:focus-visible');
    expect(wrapper.text()).toContain('市场信号');
    expect(wrapper.findAll('.odds-visual-card .coverage-donut')).toHaveLength(2);
    expect(wrapper.text()).toContain('市场倾向峰值');
    expect(wrapper.text()).toContain('概率覆盖');
    expect(wrapper.text()).toContain('比赛市场结构');
    expect(wrapper.findAll('.market-structure-rings .coverage-donut')).toHaveLength(5);
    expect(wrapper.text()).toContain('玩法覆盖');
    expect(wrapper.text()).toContain('即时快照');
    expect(wrapper.text()).toContain('赛前快照');
    expect(wrapper.text()).toContain('选项密度');
    expect(wrapper.text()).toContain('全场概率覆盖');
    expect(wrapper.find('.side-panel').attributes('tabindex')).toBe('0');
    expect(wrapper.find('.detail-panel').attributes('tabindex')).toBe('0');
    expect(oddsCenterSource).toMatch(/\.side-panel\s*\{\s*max-height: none;\s*overflow: visible;/s);
    expect(oddsCenterSource).toMatch(/\.detail-panel\s*\{\s*max-height: none;\s*overflow: visible;/s);
    expect(oddsCenterSource).toMatch(/\.odds-list-scroll\s*\{\s*max-height: min\(48dvh, 420px\);\s*overflow: auto;[^}]*scrollbar-width: none;/s);
    expect(oddsCenterSource).toContain('overflow-wrap: anywhere');
    expect(oddsCenterSource).toContain('grid-template-columns: repeat(2, minmax(0, 1fr));');
    expect(wrapper.text()).not.toMatch(/赔率数据结构|查看比赛，并查看赔率|术语速懂|盘口（专业叫法）/);
    expect(wrapper.text()).not.toMatch(/真实胜率|条形越长|不是操作方向/);
    expect(wrapper.text()).toContain('市场快照');
    expect(wrapper.text()).not.toContain('已筛出 1 / 1 个盘口');
    expect(wrapper.text()).not.toMatch(/rawPayload|原始 JSON|原始字段/i);
    expect(wrapper.text()).not.toMatch(/票号|投注金额|资金|ROI|CLV|stake|budget|profit|loss/i);
  });

  it('filters, recovers and paginates odds market snapshots', async () => {
    const generatedOdds = Array.from({ length: 14 }, (_, index) => ({
      ...oddsOverview[0],
      id: 200 + index,
      matchId: 300 + index,
      matchName: index === 13 ? 'Brazil vs Germany' : `测试比赛${index + 1}`,
      jcCode: `${index + 1}`.padStart(3, '0'),
      bookmaker: index % 2 === 0 ? 'Pinnacle' : 'DraftKings',
      marketCode: index % 2 === 0 ? 'HAD' : 'OU',
      marketName: index % 2 === 0 ? '胜平负' : '总进球',
      snapshotType: index % 3 === 0 ? 'LIVE' : 'PRE_MATCH',
      lineValue: index % 2 === 0 ? '0' : '2.5',
      homeTeam: { teamName: index === 13 ? 'Brazil' : `主队${index + 1}`, fifaCode: index === 13 ? 'BRA' : `H${index}` },
      awayTeam: { teamName: index === 13 ? 'Germany' : `客队${index + 1}`, fifaCode: index === 13 ? 'GER' : `A${index}` },
      scoreboard: { scoreDisplay: index === 13 ? '1 - 1' : '待开球', winnerSide: 'UNKNOWN', resultText: index === 13 ? '平局' : '待开球', scoreSource: 'TEAM_STATS' },
    }));
    vi.mocked(listPublicOddsOverview).mockResolvedValue(apiOk(generatedOdds));
    vi.mocked(listPublicBookmakers).mockResolvedValue(apiOk(['Pinnacle', 'DraftKings']));
    vi.mocked(listPublicOddsMarkets).mockResolvedValue(apiOk([
      { marketCode: 'HAD', marketName: '胜平负' },
      { marketCode: 'OU', marketName: '总进球' },
    ]));
    vi.mocked(getPublicMatchOdds).mockResolvedValue(apiOk({ ...oddsDetail, matchId: 300, matchName: '测试比赛1' }));

    const wrapper = await mountPublicView(OddsCenterView);

    expect(wrapper.text()).toContain('已筛出 14 / 14 个市场快照');
    expect(wrapper.text()).toContain('第 1 / 2 页');
    expect(wrapper.findAll('.side-panel .list-card')).toHaveLength(12);

    await wrapper.find('input[aria-label="搜索比赛、球队、玩法、市场线或竞彩编号"]').setValue('Brazil');
    expect(wrapper.text()).toContain('已筛出 1 / 14 个市场快照');
    expect(wrapper.findAll('.side-panel .list-card')).toHaveLength(1);
    expect(wrapper.find('.side-panel .list-card').text()).toContain('Brazil vs Germany');

    await wrapper.find('select[aria-label="按快照类型筛选市场快照"]').setValue('LIVE');
    expect(wrapper.text()).toContain('没有找到匹配市场快照');

    await wrapper.find('.empty-filter-state .ghost-button').trigger('click');
    expect(wrapper.text()).toContain('已筛出 14 / 14 个市场快照');
  });

  it('renders SentimentCenter as public cards and keeps match-level risks', async () => {
    const wrapper = await mountPublicView(SentimentCenterView);

    expect(listPublicSentimentOverview).toHaveBeenCalledTimes(1);
    expect(getPublicMatchSentiment).toHaveBeenCalledWith(7);
    expect(listSentimentOverview).not.toHaveBeenCalled();
    expect(getMatchSentiment).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain('舆情与外部因素中心');
    expect(wrapper.text()).toContain('高温影响体能');
    expect(wrapper.text()).toContain('2 - 0');
    expect(wrapper.text()).toContain('AI 分析的 16 类外部因素');
    expect(wrapper.find('[data-test="sentiment-command-board"]').exists()).toBe(true);
    expect(wrapper.find('[data-test="sentiment-command-board"]').attributes('tabindex')).toBe('0');
    expect(wrapper.find('.side-panel').attributes('tabindex')).toBe('0');
    expect(wrapper.find('.detail-panel').attributes('tabindex')).toBe('0');
    expect(wrapper.findAll('.coverage-donut').length).toBeGreaterThanOrEqual(2);
    expect(wrapper.find('[data-test="sentiment-command-board"]').text()).toContain('外因覆盖与缺口');
    expect(wrapper.find('[data-test="sentiment-command-board"]').text()).toContain('主要外因缺口');
    expect(wrapper.text()).toContain('外部情报准备度');
    expect(wrapper.text()).toContain('赔率/市场价格');
    expect(wrapper.text()).toContain('缺口等级');
    expect(wrapper.text()).toContain('已筛出 11 / 11 条因素');
    expect(wrapper.text()).toContain('天气与场地');
    expect(wrapper.text()).toContain('上座/观众');
    expect(wrapper.text()).toContain('现场上座');
    expect(wrapper.text()).toContain('转播信息');
    expect(wrapper.text()).toContain('伤停与训练');
    expect(wrapper.text()).toContain('裁判与判罚');
    expect(wrapper.text()).toContain('公众热度');
    expect(wrapper.text()).toContain('进球方式与时间');
    expect(wrapper.text()).toContain('进球时间段');
    expect(wrapper.text()).toContain('基础技术风格');
    expect(wrapper.text()).toContain('传球画像');
    expect(wrapper.text()).toContain('关键球员进球参与');
    expect(wrapper.text()).toContain('主要参与者');
    expect(wrapper.text()).toContain('门将/防守承压');
    expect(wrapper.text()).toContain('扑救与承压画像');
    expect(wrapper.text()).toContain('门将统计扑救10次');
    expect(wrapper.text()).toContain('犯规/越位样本');
    expect(wrapper.text()).toContain('犯规/越位/点球尝试画像');
    expect(wrapper.text()).toContain('点球尝试1次');
    expect(wrapper.text()).toContain('阵容结构/年龄经验');
    expect(wrapper.text()).toContain('球队名单结构画像');
    expect(wrapper.text()).toContain('赛前阵容年龄经验画像');
    expect(wrapper.text()).toContain('名单26人');
    expect(wrapper.text()).toContain('裁判与判罚');
    expect(wrapper.text()).toContain('裁判信息');
    expect(wrapper.text()).toContain('身份与来源追溯');
    expect(wrapper.findAll('.factor-state-rings .coverage-donut')).toHaveLength(4);
    expect(wrapper.text()).toContain('风险评分');
    expect(wrapper.text()).toContain('证据把握');
    expect(wrapper.text()).toContain('来源可靠');
    expect(wrapper.text()).toContain('时效状态');
    expect(wrapper.text()).toContain('比赛级风险');
    expect(wrapper.findAll('.match-risk-rings .coverage-donut')).toHaveLength(5);
    expect(wrapper.text()).toContain('高风险');
    expect(wrapper.text()).toContain('中风险');
    expect(wrapper.text()).toContain('低风险');
    expect(wrapper.text()).toContain('未定风险');
    expect(wrapper.text()).toContain('风险峰值');
    expect(wrapper.text()).toContain('节奏下降');
    expect(wrapper.text()).not.toContain('外部情报完整度');
    expect(wrapper.text()).not.toMatch(/不是胜率|不等于胜率|这个分数不是/);
    expect(wrapper.text()).not.toMatch(/新手|读法|先按|先确认|不要只看|提示/);
    expect(wrapper.text()).not.toMatch(/rawPayload|原始字段|票号|投注金额|公众投注|资金|ROI|CLV|REDUCE_STAKE|ATTACK_CONTRIBUTION|KEY_PLAYER_CONTRIBUTION|GOAL_CONTRIBUTION|TOP_SCORER_CONTRIBUTION|ASSIST_CONTRIBUTION|CONTRIBUTION_DISTRIBUTION|SCORING|GOAL_TIME_BUCKET|SPECIAL_SCORING_PROFILE|TECHNICAL_STYLE|PASSING_PROFILE|SHOT_PROFILE|DEFENSIVE_ACTION_PROFILE|GOALKEEPING|SAVE_PRESSURE_PROFILE|GOALKEEPING_SAVE_PROFILE|GOALKEEPER_SAVE_PROFILE|INFRACTION_PROFILE|FOUL_OFFSIDE_PENALTY_PROFILE|FOUL_OFFSIDE_PENALTY_PROFILE_DERIVED|SQUAD_PROFILE|TEAM_SIDE_ROSTER_BASELINE|SQUAD_LIST|LINEUP_STRUCTURE|CONFIRMED_STARTING_XI_STRUCTURE|LINEUP_STRUCTURE_PROFILE|LINEUP_STRUCTURE_DERIVED|stake|budget|profit|loss/i);
  });

  it('renders TeamProfiles from public APIs without review controls', async () => {
    const wrapper = await mountPublicView(TeamProfilesView);

    expect(listPublicTeamProfiles).toHaveBeenCalledTimes(1);
    expect(getPublicTeamProfile).toHaveBeenCalledWith(30);
    expect(listTeamProfiles).not.toHaveBeenCalled();
    expect(listCollectionItems).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain('球队画像中心');
    expect(wrapper.findAll('[data-test="team-overview-rings"] .coverage-donut')).toHaveLength(6);
    expect(wrapper.find('[data-test="team-overview-rings"]').text()).toContain('球队画像结构');
    expect(wrapper.find('[data-test="team-overview-rings"]').text()).toContain('公开球队');
    expect(wrapper.find('[data-test="team-overview-rings"]').text()).toContain('名单球员');
    expect(wrapper.find('[data-test="team-overview-rings"]').text()).toContain('画像事实');
    expect(wrapper.find('[data-test="team-overview-rings"]').text()).toContain('国家上下文');
    expect(wrapper.find('[data-test="team-overview-rings"]').text()).toContain('积分态势');
    expect(wrapper.find('[data-test="team-overview-rings"]').text()).toContain('当前证据');
    expect(wrapper.find('[data-test="team-data-overview"]').exists()).toBe(true);
    expect(wrapper.find('[data-test="team-data-overview"]').text()).toContain('球队资料准备度总览');
    expect(wrapper.find('[data-test="team-data-overview"] .coverage-donut').exists()).toBe(true);
    expect(wrapper.find('[data-test="team-data-overview"]').text()).toContain('球队数据缺口');
    expect(wrapper.find('[data-test="team-data-overview"]').text()).not.toContain('不是球队强弱');
    expect(wrapper.find('[data-test="team-data-overview"]').text()).toContain('名单准备度');
    expect(wrapper.find('[data-test="team-data-overview"] .gap-action-list').exists()).toBe(true);
    expect(wrapper.find('[data-test="team-data-overview"]').text()).toContain('国家上下文已可读');
    expect(wrapper.text()).toContain('France');
    expect(wrapper.text()).toContain('边路速度优势');
    expect(wrapper.text()).toContain('进球时间段与方式画像');
    expect(wrapper.text()).toContain('进球模式');
    expect(wrapper.text()).toContain('世界杯基础技术统计摘要');
    expect(wrapper.text()).toContain('世界杯基础技术统计');
    expect(wrapper.text()).toContain('基础技术风格画像');
    expect(wrapper.text()).toContain('小组出线状态');
    expect(wrapper.text()).toContain('本届赛果走势');
    expect(wrapper.text()).toContain('本届进球参与贡献分布');
    expect(wrapper.text()).toContain('进球参与贡献');
    expect(wrapper.text()).toContain('本届门将扑救与防守承压画像');
    expect(wrapper.text()).toContain('门将扑救画像');
    expect(wrapper.text()).toContain('犯规/越位/点球尝试基础画像');
    expect(wrapper.text()).toContain('犯规/越位/点球尝试画像');
    expect(wrapper.text()).toContain('阵容结构与年龄经验基础画像');
    expect(wrapper.text()).toContain('阵容结构与年龄经验');
    expect(wrapper.text()).toContain('已确认首发位置结构画像');
    expect(wrapper.text()).toContain('首发位置结构画像');
    expect(wrapper.text()).toContain('主要参与者');
    expect(wrapper.text()).toContain('基础技术风格');
    expect(wrapper.find('[data-test="team-standing-board"]').exists()).toBe(true);
    expect(wrapper.find('[data-test="team-standing-board"]').text()).toContain('小组积分');
    expect(wrapper.find('[data-test="team-standing-board"]').text()).toContain('A组第1 · 7分');
    expect(wrapper.find('[data-test="team-standing-board"]').text()).toContain('3场2胜1平0负');
    expect(wrapper.find('[data-test="team-standing-board"]').text()).toContain('+4');
    expect(wrapper.text()).toContain('资料判断');
    expect(wrapper.text()).toContain('资料准备度');
    expect(wrapper.findAll('.readiness-ring-card .coverage-donut')).toHaveLength(5);
    expect(wrapper.text()).toContain('名单覆盖');
    expect(wrapper.text()).toContain('冲突清晰度');
    expect(wrapper.text()).toContain('球队资料条形图');
    expect(wrapper.text()).toContain('赔率/市场价格');
    expect(wrapper.text()).toContain('xG / PPDA / 状态分');
    expect(wrapper.find('[data-test="team-metric-board"]').exists()).toBe(true);
    expect(wrapper.findAll('[data-test="team-metric-board"] .coverage-donut')).toHaveLength(4);
    expect(wrapper.find('[data-test="team-metric-board"]').text()).toContain('进攻威胁');
    expect(wrapper.find('[data-test="team-metric-board"]').text()).toContain('防守压力');
    expect(wrapper.find('[data-test="team-metric-board"]').text()).toContain('压迫强度');
    expect(wrapper.text()).toContain('缺 xG、xGA、PPDA、射门等高阶指标');
    expect(wrapper.find('[data-test="team-score-context"]').exists()).toBe(true);
    expect(wrapper.find('[data-test="team-score-context"]').text()).toContain('近期比分与胜负');
    expect(wrapper.find('[data-test="team-score-context"]').text()).toContain('近 1 场：1胜 0平 0负');
    expect(wrapper.find('[data-test="team-score-context"]').text()).toContain('2 - 0');
    expect(wrapper.find('[data-test="team-score-context"]').text()).toContain('胜');
    expect(wrapper.find('[data-test="team-score-context"]').text()).toContain('比分覆盖');
    expect(wrapper.find('[data-test="team-score-context"]').text()).toContain('进球分钟：12、55');
    expect(wrapper.findAll('[data-test="team-score-context"] .recent-result-rings .coverage-donut')).toHaveLength(5);
    expect(wrapper.find('[data-test="team-score-context"]').text()).toContain('平局');
    expect(wrapper.find('[data-test="team-score-context"]').text()).toContain('失利');
    expect(wrapper.find('[data-test="team-score-context"]').text()).toContain('有进球');
    expect(wrapper.find('[data-test="team-score-context"]').text()).toContain('零封');
    expect(wrapper.findAll('.card-grid .stack-scroll').length).toBeGreaterThanOrEqual(5);
    expect(wrapper.find('.card-grid').text()).toContain('球员名单');
    expect(wrapper.find('.card-grid').text()).toContain('1 人');
    expect(wrapper.find('.card-grid').text()).toContain('1 条');
    expect(wrapper.find('[data-test="team-data-overview"]').attributes('tabindex')).toBe('0');
    expect(wrapper.find('.side-panel').attributes('tabindex')).toBe('0');
    expect(wrapper.find('.detail-panel').attributes('tabindex')).toBe('0');
    expect(wrapper.find('[data-test="team-score-context"]').attributes('tabindex')).toBe('0');
    expect(wrapper.find('.card-grid').attributes('tabindex')).toBe('0');
    expect(teamProfilesSource).toContain('max-height: 460px');
    expect(teamProfilesSource).toContain('overscroll-behavior: contain');
    expect(teamProfilesSource).not.toContain('max-height: min(70dvh, 580px)');
    expect(teamProfilesSource).not.toContain('max-height: min(34dvh, 250px)');
    expect(teamProfilesSource).not.toContain('.team-overview-structure { max-height: min(46dvh, 360px); }');
    expect(teamProfilesSource).not.toContain('.team-overview-board { max-height: min(54dvh, 440px); overflow: auto; }');
    expect(teamProfilesSource).not.toContain('max-height: min(70dvh, 600px)');
    expect(teamProfilesSource).not.toContain('.card-grid { max-height: min(54dvh, 440px); }');
    expect(teamProfilesSource).not.toContain('.score-context-board { max-height: min(58dvh, 470px); }');
    expect(teamProfilesSource).toMatch(/\.side-panel,\s*\.detail-panel\s*\{\s*max-height: none;\s*overflow: visible;/s);
    expect(teamProfilesSource).toMatch(/\.team-list-scroll\s*\{\s*max-height: min\(48dvh, 420px\);\s*overflow: auto;[^}]*scrollbar-width: none;/s);
    expect(teamProfilesSource).toContain('grid-template-columns: repeat(2, minmax(0, 1fr));');
    expect(teamProfilesSource).not.toContain('国旗、队名、攻击/防守画像、资料条形图和公开来源状态同步展示。');
    expect(wrapper.text()).toContain('已筛出 1 / 1 支球队');
    expect(wrapper.text()).not.toMatch(/待审核|批准|驳回|reviewedBy|approvedBy/i);
    expect(wrapper.text()).not.toMatch(/球队画像健壮度|画像健壮性|名单完整度|名单较完整|盘口变化|赔率异动|不等于真实进球|条形图偏短|越高说明|越低通常/);
    expect(teamProfilesSource).toContain('名单准备度');
    expect(teamProfilesSource).toContain('赔率/市场价格快照');
    expect(teamProfilesSource).not.toMatch(/球队画像健壮度|画像健壮性|名单完整度|名单较完整|盘口变化|赔率异动|不等于真实进球|条形图偏短|越高说明|越低通常/);
  });

  it('filters, cleans tags and paginates team profile list', async () => {
    const generatedTeams = Array.from({ length: 14 }, (_, index) => ({
      ...teamSummaryAlt,
      id: 100 + index,
      teamKey: `team-${index}`,
      displayName: index === 13 ? 'Brazil' : `测试球队${index + 1}`,
      fifaCode: index === 13 ? 'BRA' : `T${index}`,
      groupName: `${String.fromCharCode(65 + (index % 4))}组`,
      styleTags: `["世界杯2026参赛队","${String.fromCharCode(65 + (index % 4))}组"]`,
      factCount: index % 3 === 0 ? 1 : 0,
      groupStandingSummary: `${index === 13 ? 'Brazil' : `测试球队${index + 1}`}当前${String.fromCharCode(65 + (index % 4))}组第2/4名，3场1胜1平1负，进4球失3球，净胜球+1，积分4。`,
    }));
    vi.mocked(listPublicTeamProfiles).mockResolvedValue(apiOk(generatedTeams));
    vi.mocked(getPublicTeamProfile).mockResolvedValue(apiOk({ ...teamDetail, team: generatedTeams[0] }));

    const wrapper = await mountPublicView(TeamProfilesView);

    expect(wrapper.text()).toContain('已筛出 14 / 14 支球队');
    expect(wrapper.text()).toContain('第 1 / 2 页');
    expect(wrapper.findAll('.side-panel .list-card')).toHaveLength(12);
    expect(wrapper.text()).not.toContain('["世界杯2026参赛队"');
    expect(wrapper.text()).toContain('世界杯2026参赛队 · A组');

    await wrapper.find('input[aria-label="搜索球队名、FIFA code、小组或风格"]').setValue('Brazil');
    expect(wrapper.text()).toContain('已筛出 1 / 14 支球队');
    expect(wrapper.findAll('.side-panel .list-card')).toHaveLength(1);
    expect(wrapper.find('.side-panel .list-card').text()).toContain('Brazil');

    await wrapper.find('input[aria-label="搜索球队名、FIFA code、小组或风格"]').setValue('不存在的球队');
    expect(wrapper.text()).toContain('没有找到匹配球队');

    await wrapper.find('.side-panel .ghost-button').trigger('click');
    expect(wrapper.text()).toContain('已筛出 14 / 14 支球队');

    const teamGapButtons = wrapper.findAll('[data-test="team-data-overview"] .gap-action-button');
    expect(teamGapButtons).toHaveLength(3);
    await teamGapButtons[0].trigger('click');
    expect(wrapper.find('select[aria-label="按球队数据覆盖筛选"]').element).toMatchObject({ value: 'NO_FACTS' });
    expect(wrapper.text()).toContain('清除筛选');

    await wrapper.find('select[aria-label="按球队数据覆盖筛选"]').setValue('NO_ATTACK_DEFENSE');
    expect(wrapper.text()).toContain('已筛出 14 / 14 支球队');
  });

  it('renders PlayerProfiles from public APIs without review controls', async () => {
    const wrapper = await mountPublicView(PlayerProfilesView);

    expect(listPublicPlayerProfiles).toHaveBeenCalledTimes(1);
    expect(getPublicPlayerProfile).toHaveBeenCalledWith(40);
    expect(listPlayerProfiles).not.toHaveBeenCalled();
    expect(listCollectionItems).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain('球员画像中心');
    expect(wrapper.findAll('[data-test="player-overview-rings"] .coverage-donut')).toHaveLength(5);
    expect(wrapper.find('[data-test="player-overview-rings"]').text()).toContain('球员画像结构');
    expect(wrapper.find('[data-test="player-overview-rings"]').text()).toContain('公开球员');
    expect(wrapper.find('[data-test="player-overview-rings"]').text()).toContain('覆盖球队');
    expect(wrapper.find('[data-test="player-overview-rings"]').text()).toContain('画像事实');
    expect(wrapper.find('[data-test="player-overview-rings"]').text()).toContain('可用性关注');
    expect(wrapper.find('[data-test="player-overview-rings"]').text()).toContain('当前事实');
    expect(wrapper.find('[data-test="player-data-overview"]').exists()).toBe(true);
    expect(wrapper.find('[data-test="player-data-overview"]').text()).toContain('球员资料准备度总览');
    expect(wrapper.find('[data-test="player-data-overview"] .coverage-donut').exists()).toBe(true);
    expect(wrapper.find('[data-test="player-data-overview"]').text()).not.toContain('不是球员强弱');
    expect(wrapper.find('[data-test="player-data-overview"]').text()).toContain('球员数据缺口');
    expect(wrapper.find('[data-test="player-data-overview"] .gap-action-list').exists()).toBe(true);
    expect(wrapper.find('[data-test="player-data-overview"]').text()).toContain('筛缺事实球员');
    expect(wrapper.find('[data-test="player-data-overview"]').text()).toContain('按国家队关联');
    expect(wrapper.find('[data-test="player-team-context"]').exists()).toBe(true);
    expect(wrapper.find('[data-test="player-team-context"]').text()).toContain('国家队上下文');
    expect(wrapper.find('[data-test="player-team-context"]').text()).toContain('国家队上下文');
    expect(wrapper.find('[data-test="player-team-context"]').text()).toContain('France');
    expect(wrapper.find('[data-test="player-team-context"]').text()).toContain('Brazil');
    expect(wrapper.findAll('[data-test="player-team-context"] .team-context-rings .coverage-donut')).toHaveLength(5);
    expect(wrapper.find('[data-test="player-team-context"]').text()).toContain('国家队卡片');
    expect(wrapper.find('[data-test="player-team-context"]').text()).toContain('位置覆盖');
    expect(wrapper.find('[data-test="player-team-context"]').text()).toContain('画像事实');
    expect(wrapper.find('[data-test="player-team-context"]').text()).toContain('可用性线索');
    expect(wrapper.find('[data-test="player-team-context"]').text()).toContain('可用性缺口');
    expect(wrapper.find('[data-test="player-impact-board"]').exists()).toBe(true);
    expect(wrapper.find('[data-test="player-impact-board"]').text()).toContain('球员影响指标');
    expect(wrapper.find('[data-test="player-impact-board"]').text()).toContain('出场可能性');
    expect(wrapper.find('[data-test="player-impact-board"]').text()).toContain('进球威胁');
    expect(wrapper.find('[data-test="player-impact-board"]').text()).toContain('创造机会');
    expect(wrapper.find('[data-test="player-impact-board"]').text()).toContain('可用性稳定度');
    expect(wrapper.findAll('[data-test="player-impact-board"] .coverage-donut')).toHaveLength(4);
    expect(wrapper.find('[data-test="player-impact-board"]').text()).toContain('出场概率');
    expect(wrapper.text()).toContain('Mbappe');
    expect(wrapper.text()).toContain('冲刺状态良好');
    expect(wrapper.text()).toContain('牌面纪律记录');
    expect(wrapper.text()).toContain('纪律/牌面');
    expect(wrapper.text()).toContain('阵容负荷与首发连续性');
    expect(wrapper.text()).toContain('出场负荷');
    expect(wrapper.text()).toContain('进球参与与时间段');
    expect(wrapper.text()).toContain('进球模式');
    expect(wrapper.text()).toContain('基础射门画像');
    expect(wrapper.text()).toContain('射门画像');
    expect(wrapper.text()).toContain('球员名片');
    expect(wrapper.text()).toContain('资料准备度');
    expect(wrapper.findAll('.readiness-ring-card .coverage-donut')).toHaveLength(5);
    expect(wrapper.text()).toContain('事实支撑');
    expect(wrapper.text()).toContain('身份上下文');
    expect(wrapper.text()).toContain('指标覆盖');
    expect(wrapper.text()).toContain('出场 / xG / 可用性');
    expect(wrapper.text()).toContain('缺预计首发概率');
    expect(wrapper.text()).toContain('已筛出 2 / 2 名球员');
    const playerGapButtons = wrapper.findAll('[data-test="player-data-overview"] .gap-action-button');
    expect(playerGapButtons).toHaveLength(3);
    await playerGapButtons[0].trigger('click');
    expect(wrapper.find('select[aria-label="按球员资料缺口筛选"]').element).toMatchObject({ value: 'NO_FACTS' });
    expect(wrapper.text()).toContain('已筛出 1 / 2 名球员');
    expect(wrapper.find('.list-card').text()).toContain('Alisson');
    await wrapper.find('input[aria-label="搜索球员姓名、球队或号码"]').setValue('Alisson');
    expect(wrapper.text()).toContain('已筛出 1 / 2 名球员');
    expect(wrapper.findAll('.list-card')).toHaveLength(1);
    expect(wrapper.find('.list-card').text()).toContain('Alisson');
    expect(wrapper.text()).not.toMatch(/待审核|批准|驳回|reviewedBy|approvedBy/i);
    expect(wrapper.text()).not.toMatch(/球员画像健壮度|球员画像健壮性|画像健壮性|较健壮|越完整/);
    expect(playerProfilesSource).toContain('球员资料准备度总览');
    expect(wrapper.find('[data-test="player-data-overview"]').attributes('tabindex')).toBe('0');
    expect(wrapper.find('[data-test="player-team-context"]').attributes('tabindex')).toBe('0');
    expect(wrapper.find('.side-panel').attributes('tabindex')).toBe('0');
    expect(wrapper.find('.detail-panel').attributes('tabindex')).toBe('0');
    expect(wrapper.find('[data-test="player-impact-board"]').attributes('tabindex')).toBe('0');
    expect(playerProfilesSource).not.toContain('max-height: min(70dvh, 580px)');
    expect(playerProfilesSource).not.toContain('max-height: min(36dvh, 300px)');
    expect(playerProfilesSource).not.toContain('.player-overview-structure { max-height: min(46dvh, 360px); }');
    expect(playerProfilesSource).not.toContain('.player-overview-board { max-height: min(54dvh, 440px); }');
    expect(playerProfilesSource).not.toContain('max-height: min(70dvh, 620px)');
    expect(playerProfilesSource).not.toContain('max-height: min(46dvh, 360px)');
    expect(playerProfilesSource).not.toMatch(/\.player-team-context,\s*\.impact-board,\s*\.readiness-board\s*\{\s*max-height: min\(58dvh, 470px\);/s);
    expect(playerProfilesSource).toMatch(/\.side-panel\s*\{\s*max-height: none;\s*overflow: visible;/s);
    expect(playerProfilesSource).toMatch(/\.player-list-scroll\s*\{[^}]*max-height: min\(48dvh, 420px\);[^}]*overflow: auto;[^}]*scrollbar-width: none;/s);
    expect(playerProfilesSource).toMatch(/\.detail-panel\s*\{\s*max-height: none;\s*overflow: visible;/s);
    expect(playerProfilesSource).toMatch(/\.impact-board,\s*\.readiness-board\s*\{\s*max-height: none;\s*overflow: visible;/s);
    expect(playerProfilesSource).toMatch(/\.card-grid\s*\{\s*max-height: none;\s*overflow: visible;/s);
    expect(playerProfilesSource).toContain('grid-template-columns: repeat(2, minmax(0, 1fr));');
    expect(playerProfilesSource).not.toContain('球队国旗、位置、状态、伤病、训练、停赛和预计首发概率同步展示。');
    expect(playerProfilesSource).not.toMatch(/球员画像健壮度|球员画像健壮性|画像健壮性|较健壮|越完整/);
  });

  it('falls back to teamName flags when player API has no team visual object', async () => {
    vi.mocked(listPublicPlayerProfiles).mockResolvedValue(apiOk([
      {
        ...playerSummary,
        id: 90,
        teamName: '阿尔及利亚',
        team: undefined,
        displayName: 'ABADA Achref',
      },
    ]));
    vi.mocked(getPublicPlayerProfile).mockResolvedValue(apiOk({
      ...playerDetail,
      player: {
        ...playerSummary,
        id: 90,
        teamName: '阿尔及利亚',
        team: undefined,
        displayName: 'ABADA Achref',
      },
    }));

    const wrapper = await mountPublicView(PlayerProfilesView);

    expect(wrapper.find('[data-test="player-team-context"]').text()).toContain('阿尔及利亚');
    expect(wrapper.find('[data-test="player-team-context"]').text()).toContain('ALG');
    expect(wrapper.find('.player-identity-card').text()).toContain('阿尔及利亚');
    expect(wrapper.find('.player-identity-card').text()).toContain('ALG');
  });

  it('declares H5 card layouts for all evidence pages', () => {
    for (const source of [
      evidenceCenterSource,
      matchCenterSource,
      oddsCenterSource,
      sentimentCenterSource,
      teamProfilesSource,
      playerProfilesSource,
    ]) {
      expect(source).not.toContain('<el-table');
      expect(source).toContain('@media (max-width: 640px)');
      expect(source).toContain('grid-template-columns: 1fr');
      expect(source).toContain('min-width: 0');
      expect(source).not.toMatch(/min-width:\s*(?:[4-9]\d{2,}|\d{4,})px/);
    }
  });
});
