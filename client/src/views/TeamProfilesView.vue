<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import {
  getPublicTeamProfile,
  listPublicTeamProfiles,
  type PublicTeamProfileDetail,
  type PublicTeamMatchHistory,
  type PublicTeamScoringPattern,
  type PublicTeamProfileSummary,
} from '@/api/profiles';
import CoverageDonut from '@/components/football/CoverageDonut.vue';
import FlagTeamName from '@/components/football/FlagTeamName.vue';
import MetricBar from '@/components/football/MetricBar.vue';
import {
  enumLabel,
  factTypeLabel,
  lineupRoleLabel,
  matchStatusLabel,
  positionLabel,
  readablePublicText,
} from '@/utils/display-labels';

const loading = ref(false);
const detailLoading = ref(false);
const error = ref('');
const detailError = ref('');
const teams = ref<PublicTeamProfileSummary[]>([]);
const selected = ref<PublicTeamProfileDetail | null>(null);
const selectedTeamId = ref<number | null>(null);
const teamSearchQuery = ref('');
const teamGroupFilter = ref('ALL');
const teamDataFilter = ref('ALL');
const teamPage = ref(1);
const TEAM_PAGE_SIZE = 12;

const stats = computed(() => ({
  teams: teams.value.length,
  facts: teams.value.reduce((sum, team) => sum + team.factCount, 0),
  players: teams.value.reduce((sum, team) => sum + team.playerCount, 0),
  evidence: selected.value?.evidenceCount ?? 0,
}));

const teamOverviewRings = computed(() => {
  const nationalContextCount = teams.value.filter(teamHasNationalContext).length;
  const standingCount = teams.value.filter(teamHasStanding).length;
  const selectedEvidence = selected.value?.evidenceCount ?? 0;

  return [
    {
      label: '公开球队',
      value: stats.value.teams,
      max: Math.max(1, stats.value.teams),
      unit: '支',
      tone: stats.value.teams ? 'success' : 'info',
      caption: `${stats.value.teams} 支球队画像`,
    },
    {
      label: '名单球员',
      value: stats.value.players,
      max: Math.max(1, stats.value.players),
      unit: '人',
      tone: stats.value.players ? 'accent' : 'info',
      caption: `${stats.value.players} 名公开球员`,
    },
    {
      label: '画像事实',
      value: stats.value.facts,
      max: Math.max(1, stats.value.facts),
      unit: '条',
      tone: stats.value.facts ? 'info' : 'warning',
      caption: `${stats.value.facts} 条球队画像事实`,
    },
    {
      label: '国家上下文',
      value: nationalContextCount,
      max: Math.max(1, stats.value.teams),
      unit: '支',
      tone: nationalContextCount === stats.value.teams && stats.value.teams ? 'success' : nationalContextCount ? 'warning' : 'info',
      caption: `${nationalContextCount} 支球队有国旗、FIFA code 或小组`,
    },
    {
      label: '积分态势',
      value: standingCount,
      max: Math.max(1, stats.value.teams),
      unit: '支',
      tone: standingCount === stats.value.teams && stats.value.teams ? 'success' : standingCount ? 'warning' : 'info',
      caption: `${standingCount} 支球队有小组排名、积分和净胜球`,
    },
    {
      label: '当前证据',
      value: selectedEvidence,
      max: Math.max(1, selectedEvidence),
      unit: '条',
      tone: selectedEvidence ? 'accent' : 'info',
      caption: `${selectedEvidence} 条当前球队证据`,
    },
  ];
});

const teamDataOverview = computed(() => {
  const total = Math.max(1, teams.value.length);
  const withFacts = teams.value.filter((team) => team.factCount > 0).length;
  const withRoster = teams.value.filter((team) => team.playerCount >= 23).length;
  const withAttackDefense = teams.value.filter((team) => team.attackProfile && team.defenseProfile).length;
  const withNationalContext = teams.value.filter(teamHasNationalContext).length;
  const withStanding = teams.value.filter(teamHasStanding).length;
  const missingFacts = Math.max(0, teams.value.length - withFacts);
  const missingRoster = Math.max(0, teams.value.length - withRoster);
  const missingAttackDefense = Math.max(0, teams.value.length - withAttackDefense);
  const missingNationalContext = Math.max(0, teams.value.length - withNationalContext);
  const missingStanding = Math.max(0, teams.value.length - withStanding);
  const readinessScore = Math.round(((withFacts / total) * 0.28 + (withRoster / total) * 0.22 + (withAttackDefense / total) * 0.20 + (withNationalContext / total) * 0.16 + (withStanding / total) * 0.14) * 100);
  const priorityGaps = [
    missingFacts ? `主要缺口：${missingFacts} 支球队缺战术风格、近期状态、伤停停赛、教练发布会和新闻事实。` : '画像事实已覆盖主要球队。',
    missingRoster ? `还有 ${missingRoster} 支球队名单少于 23 人，大名单、预计首发和关键替补缺口。` : '大名单覆盖已达到世界杯资料门槛。',
    missingAttackDefense ? `还有 ${missingAttackDefense} 支球队缺进攻/防守画像，补 xG/xGA、PPDA、定位球和转换速度。` : '攻防画像资料较齐。',
    missingNationalContext ? `还有 ${missingNationalContext} 支球队缺国旗/FIFA code/小组上下文，国家元数据缺口。` : '国旗、FIFA code 和小组上下文资料较齐。',
    missingStanding ? `还有 ${missingStanding} 支球队缺小组积分、排名和净胜球态势。` : '小组积分、排名和净胜球态势已覆盖。',
  ];
  const priorityActions = [
    {
      id: 'facts',
      title: missingFacts ? '筛缺画像事实球队' : '画像事实已覆盖',
      body: missingFacts ? `${missingFacts} 支球队缺战术、近期状态、伤停或新闻事实。` : '来源时效和第二来源已覆盖。',
      filter: missingFacts ? 'NO_FACTS' : 'HAS_FACTS',
      cta: missingFacts ? '筛缺事实' : '看已有事实',
    },
    {
      id: 'attack-defense',
      title: missingAttackDefense ? '筛缺攻防画像球队' : '攻防画像可读',
      body: missingAttackDefense ? `${missingAttackDefense} 支球队缺 xG/xGA、PPDA、定位球或转换速度。` : '攻防画像已能支撑指标覆盖。',
      filter: missingAttackDefense ? 'NO_ATTACK_DEFENSE' : 'ALL',
      cta: missingAttackDefense ? '筛攻防缺口' : '看全部球队',
    },
    {
      id: 'national-context',
      title: missingNationalContext ? '筛国家上下文缺口' : '国家上下文已可读',
      body: missingNationalContext ? `${missingNationalContext} 支球队缺国旗、FIFA code 或小组。` : 'FIFA code 与小组足够支撑国旗和分组展示。',
      filter: missingNationalContext ? 'METADATA_GAP' : 'FULL_ROSTER',
      cta: missingNationalContext ? '筛国家缺口' : '看名单较齐',
    },
  ];

  return {
    score: teams.value.length ? readinessScore : 0,
    level: readinessScore >= 80 ? '准备充分' : readinessScore >= 55 ? '基础可读' : '偏薄弱',
    missingFacts,
    missingRoster,
    missingAttackDefense,
    missingNationalContext,
    missingStanding,
    bars: [
      { label: '画像事实覆盖', value: withFacts, max: total, tone: withFacts / total >= 0.7 ? 'success' : 'danger', caption: `${missingFacts} 支球队缺战术、状态、伤停或新闻事实。` },
      { label: '名单准备度', value: withRoster, max: total, tone: withRoster / total >= 0.8 ? 'success' : 'warning', caption: `${missingRoster} 支球队名单少于 23 人，首发/轮换资料会偏弱。` },
      { label: '攻防画像', value: withAttackDefense, max: total, tone: withAttackDefense / total >= 0.7 ? 'success' : 'warning', caption: `${missingAttackDefense} 支球队缺进攻或防守画像。` },
      { label: '国旗小组上下文', value: withNationalContext, max: total, tone: withNationalContext / total >= 0.9 ? 'success' : 'warning', caption: `${missingNationalContext} 支球队缺国旗、FIFA code 或小组信息。` },
      { label: '小组积分态势', value: withStanding, max: total, tone: withStanding / total >= 0.9 ? 'success' : 'warning', caption: `${missingStanding} 支球队缺排名、积分或净胜球。` },
    ],
    priorityGaps,
    priorityActions,
  };
});

const teamCoverageBars = computed(() => {
  if (!selected.value) {
    return [];
  }
  return [
    { label: '球员名单', value: selected.value.players.length, max: 26, tone: selected.value.players.length >= 18 ? 'success' : 'warning', caption: '世界杯名单通常 23-26 人' },
    { label: '画像事实', value: selected.value.facts.length, max: Math.max(6, selected.value.facts.length), tone: selected.value.facts.length >= 6 ? 'success' : 'warning', caption: '需覆盖战术、状态、伤停、新闻' },
    { label: '历史比赛', value: selected.value.matchHistory.length, max: Math.max(5, selected.value.matchHistory.length), tone: selected.value.matchHistory.length >= 5 ? 'success' : 'warning', caption: '近五场比分与赛果记录' },
    { label: '证据链', value: selected.value.evidenceCount, max: Math.max(5, selected.value.evidenceCount), tone: selected.value.evidenceCount >= 5 ? 'success' : 'warning', caption: '公开来源数量与交叉校验材料' },
  ];
});
const teamReadiness = computed(() => selected.value?.readiness ?? {
  score: 0,
  level: 'UNKNOWN',
  summary: '球队资料准备度待同步。',
  strengths: [],
  missingDimensions: ['球队资料准备度待同步'],
  nextActions: ['公开只读评分待同步'],
});
const teamReadinessRings = computed(() => {
  if (!selected.value) {
    return [];
  }
  const detail = selected.value;
  const conflictClarity = Math.max(0, 100 - detail.conflictCount * 25);
  return [
    {
      label: '资料准备度',
      value: teamReadiness.value.score,
      max: 100,
      unit: '%',
      tone: thresholdTone(teamReadiness.value.score, 80, 55),
      caption: enumLabel('profileReadinessLevel', teamReadiness.value.level, '待评估'),
    },
    {
      label: '名单覆盖',
      value: detail.players.length,
      max: 26,
      unit: '人',
      tone: detail.players.length >= 23 ? 'success' : detail.players.length >= 18 ? 'warning' : 'danger',
      caption: `${detail.players.length} 名球员`,
    },
    {
      label: '画像事实',
      value: detail.facts.length,
      max: Math.max(6, detail.facts.length),
      unit: '条',
      tone: detail.facts.length >= 6 ? 'success' : detail.facts.length > 0 ? 'warning' : 'danger',
      caption: '战术、状态、伤停、新闻事实',
    },
    {
      label: '证据链',
      value: detail.evidenceCount,
      max: Math.max(5, detail.evidenceCount),
      unit: '条',
      tone: detail.evidenceCount >= 5 ? 'success' : detail.evidenceCount > 0 ? 'warning' : 'danger',
      caption: '公开来源数量',
    },
    {
      label: '冲突清晰度',
      value: conflictClarity,
      max: 100,
      unit: '%',
      tone: thresholdTone(conflictClarity, 80, 55),
      caption: `${detail.conflictCount} 个公开冲突`,
    },
  ];
});
const teamMetricRows = computed(() => {
  const metric = selected.value?.latestMetric;
  if (!metric) {
    return [];
  }
  return [
    { label: 'xG 预期进球', value: metric.xg, max: 3, tone: 'success', caption: '射门机会质量' },
    { label: 'xGA 预期失球', value: metric.xga, max: 3, tone: 'danger', caption: '防线承压指标' },
    { label: 'PPDA 压迫强度', value: metric.ppda, max: 20, tone: 'warning', caption: '前场压迫指标' },
    { label: '状态分', value: metric.formScore, max: 100, tone: 'accent', caption: '按已入库指标综合展示' },
  ].filter((row) => row.value != null);
});
const teamMetricRings = computed(() => {
  if (!selected.value) {
    return [];
  }
  const metric = selected.value.latestMetric;
  const attackThreat = metric?.xg == null ? null : Math.min(100, Math.round((metric.xg / 3) * 100));
  const defensivePressure = metric?.xga == null ? null : Math.min(100, Math.round((metric.xga / 3) * 100));
  const pressingIntensity = metric?.ppda == null ? null : Math.max(0, Math.min(100, Math.round(((20 - metric.ppda) / 20) * 100)));
  return [
    {
      label: '进攻威胁',
      value: attackThreat,
      tone: thresholdTone(attackThreat, 60, 35),
      caption: `xG ${metricText(metric?.xg)} · 射门 ${metric?.shots ?? '待同步'}`,
    },
    {
      label: '防守压力',
      value: defensivePressure,
      tone: pressureTone(defensivePressure, 35, 60),
      caption: `xGA ${metricText(metric?.xga)} · 失球风险`,
    },
    {
      label: '压迫强度',
      value: pressingIntensity,
      tone: thresholdTone(pressingIntensity, 60, 35),
      caption: `PPDA ${metricText(metric?.ppda)}`,
    },
    {
      label: '状态分',
      value: metric?.formScore ?? null,
      tone: thresholdTone(metric?.formScore, 80, 60),
      caption: `控球 ${metricText(metric?.possessionPct, '%')} · 渐进传球 ${metric?.progressivePasses ?? '待同步'}`,
    },
  ];
});

type TeamScoreTone = 'win' | 'draw' | 'loss' | 'unknown';

interface TeamScoreContextBar {
  label: string;
  value: number;
  max: number;
  tone: string;
  caption: string;
}

interface TeamScoreContextRing {
  label: string;
  value: number;
  max: number;
  tone: string;
  caption: string;
}

interface TeamScoreContextMatch {
  matchId: number;
  matchName: string;
  stage?: string;
  venue?: string;
  matchday?: string;
  resultStatus?: string;
  goalsFor?: number;
  goalsAgainst?: number;
  scoreDisplay: string;
  resultLabel: string;
  resultTone: TeamScoreTone;
  firstGoalText: string;
  scoringMinutes: string;
  hasScore: boolean;
}

function firstNumber(...values: Array<number | null | undefined>): number | undefined {
  return values.find((value): value is number => typeof value === 'number' && Number.isFinite(value));
}

function scoreDisplay(goalsFor?: number, goalsAgainst?: number): string {
  if (goalsFor == null || goalsAgainst == null) {
    return '比分待同步';
  }
  return `${goalsFor} - ${goalsAgainst}`;
}

function scoreTone(goalsFor?: number, goalsAgainst?: number): TeamScoreTone {
  if (goalsFor == null || goalsAgainst == null) {
    return 'unknown';
  }
  if (goalsFor > goalsAgainst) {
    return 'win';
  }
  if (goalsFor < goalsAgainst) {
    return 'loss';
  }
  return 'draw';
}

function scoreResultLabel(goalsFor?: number, goalsAgainst?: number, resultStatus?: string): string {
  const tone = scoreTone(goalsFor, goalsAgainst);
  if (tone === 'win') {
    return '胜';
  }
  if (tone === 'loss') {
    return '负';
  }
  if (tone === 'draw') {
    return '平';
  }
  const status = matchStatusLabel(resultStatus);
  return status === '已完赛' ? '比分待校验' : status;
}

function formatScoringMinutes(value?: string | null): string {
  const trimmed = value?.trim();
  if (!trimmed || trimmed === '[]') {
    return '进球分钟待同步';
  }
  if (trimmed.startsWith('[') && trimmed.endsWith(']')) {
    try {
      const parsed = JSON.parse(trimmed);
      if (Array.isArray(parsed)) {
        const items = parsed.map((item) => String(item).trim()).filter(Boolean);
        return items.length ? items.join('、') : '进球分钟待同步';
      }
    } catch {
      // 使用兜底清洗，避免公开页暴露 JSON 数组符号。
    }
    const cleaned = trimmed
      .replace(/^\[/, '')
      .replace(/\]$/, '')
      .split(',')
      .map((item) => item.replace(/^['"]|['"]$/g, '').trim())
      .filter(Boolean);
    return cleaned.length ? cleaned.join('、') : '进球分钟待同步';
  }
  return trimmed.split(/[，,、]+/).map((item) => item.trim()).filter(Boolean).join('、') || '进球分钟待同步';
}

function toScoreContextMatch(
  matchId: number,
  history?: PublicTeamMatchHistory,
  pattern?: PublicTeamScoringPattern,
): TeamScoreContextMatch {
  const goalsFor = firstNumber(history?.goalsFor, pattern?.goalsFor);
  const goalsAgainst = firstNumber(history?.goalsAgainst, pattern?.goalsAgainst);
  const firstGoalMinute = firstNumber(pattern?.firstGoalMinute);
  const hasScore = goalsFor != null && goalsAgainst != null;
  return {
    matchId,
    matchName: history?.matchName || pattern?.matchName || '比赛待同步',
    stage: history?.stage,
    venue: history?.venue,
    matchday: history?.matchday || pattern?.matchday,
    resultStatus: history?.resultStatus,
    goalsFor,
    goalsAgainst,
    scoreDisplay: scoreDisplay(goalsFor, goalsAgainst),
    resultLabel: scoreResultLabel(goalsFor, goalsAgainst, history?.resultStatus),
    resultTone: scoreTone(goalsFor, goalsAgainst),
    firstGoalText: firstGoalMinute == null ? '待同步' : `${firstGoalMinute} 分钟`,
    scoringMinutes: formatScoringMinutes(history?.scoringMinutes || pattern?.scoringMinutes),
    hasScore,
  };
}

const teamScoreContext = computed(() => {
  if (!selected.value) {
    return {
      matches: [] as TeamScoreContextMatch[],
      bars: [] as TeamScoreContextBar[],
      resultSummary: '近期赛果待同步',
      headline: '近期比分待同步',
      primaryScore: '待同步',
      primaryResultLabel: '比分待同步',
      primaryResultTone: 'unknown' as TeamScoreTone,
      explanation: '选择球队后展示近期比分、胜平负和进失球对比。',
    };
  }

  const historyById = new Map(selected.value.matchHistory.map((match) => [match.matchId, match]));
  const patternById = new Map(selected.value.scoringPatterns.map((pattern) => [pattern.matchId, pattern]));
  const matchIds = [
    ...selected.value.matchHistory.map((match) => match.matchId),
    ...selected.value.scoringPatterns
      .filter((pattern) => !historyById.has(pattern.matchId))
      .map((pattern) => pattern.matchId),
  ];
  const matches = matchIds
    .map((matchId) => toScoreContextMatch(matchId, historyById.get(matchId), patternById.get(matchId)))
    .slice(0, 5);
  const scoredMatches = matches.filter((match) => match.hasScore);
  const wins = scoredMatches.filter((match) => match.resultTone === 'win').length;
  const draws = scoredMatches.filter((match) => match.resultTone === 'draw').length;
  const losses = scoredMatches.filter((match) => match.resultTone === 'loss').length;
  const goalsFor = scoredMatches.reduce((sum, match) => sum + (match.goalsFor ?? 0), 0);
  const goalsAgainst = scoredMatches.reduce((sum, match) => sum + (match.goalsAgainst ?? 0), 0);
  const goalBalance = goalsFor - goalsAgainst;
  const maxGoals = Math.max(1, goalsFor, goalsAgainst);
  const primary = matches[0];

  if (!primary) {
    return {
      matches,
      bars: [] as TeamScoreContextBar[],
      resultSummary: '近期赛果待同步',
      headline: '近期比分待同步',
      primaryScore: '待同步',
      primaryResultLabel: '比分待同步',
      primaryResultTone: 'unknown' as TeamScoreTone,
      explanation: '这支球队还没有近期比赛记录，近五场比分、胜平负、首球时间和进球分钟缺口。',
    };
  }

  return {
    matches,
    bars: [
      {
        label: '比分覆盖',
        value: scoredMatches.length,
        max: Math.max(1, matches.length),
        tone: scoredMatches.length === matches.length ? 'success' : 'warning',
        caption: `${scoredMatches.length} / ${matches.length} 场已有明确比分。`,
      },
      {
        label: '胜场',
        value: wins,
        max: Math.max(1, scoredMatches.length),
        tone: wins >= losses ? 'success' : 'warning',
        caption: `近 ${scoredMatches.length || matches.length} 场可读记录：${wins}胜 ${draws}平 ${losses}负。`,
      },
      {
        label: '进球',
        value: goalsFor,
        max: maxGoals,
        tone: 'success',
        caption: `已入库近期记录合计进球 ${goalsFor}。`,
      },
      {
        label: '失球',
        value: goalsAgainst,
        max: maxGoals,
        tone: goalsAgainst <= goalsFor ? 'warning' : 'danger',
        caption: `已入库近期记录合计失球 ${goalsAgainst}。`,
      },
    ],
    resultSummary: scoredMatches.length ? `${wins}胜 ${draws}平 ${losses}负` : '比分待同步',
    headline: scoredMatches.length ? `近 ${scoredMatches.length} 场：${wins}胜 ${draws}平 ${losses}负` : '近期比分待同步',
    primaryScore: primary.scoreDisplay,
    primaryResultLabel: primary.resultLabel,
    primaryResultTone: primary.resultTone,
    explanation: scoredMatches.length
      ? `净胜球 ${goalBalance >= 0 ? '+' : ''}${goalBalance}；比分、胜平负、进球分钟与证据链已展示。`
      : '近期比赛已有记录但缺少明确比分，比分来源仍为空。',
  };
});

const teamRecentResultRings = computed<TeamScoreContextRing[]>(() => {
  const matches = teamScoreContext.value.matches;
  const scoredMatches = matches.filter((match) => match.hasScore);
  const max = Math.max(1, scoredMatches.length);
  const captionBase = scoredMatches.length ? scoredMatches.length : matches.length;
  const captionSuffix = captionBase ? `${captionBase} 场记录` : '暂无记录';
  const wins = scoredMatches.filter((match) => match.resultTone === 'win').length;
  const draws = scoredMatches.filter((match) => match.resultTone === 'draw').length;
  const losses = scoredMatches.filter((match) => match.resultTone === 'loss').length;
  const scored = scoredMatches.filter((match) => (match.goalsFor ?? 0) > 0).length;
  const cleanSheets = scoredMatches.filter((match) => match.goalsAgainst === 0).length;

  return [
    {
      label: '胜场',
      value: wins,
      max,
      tone: wins >= losses ? 'success' : 'warning',
      caption: `${wins} / ${captionSuffix}`,
    },
    {
      label: '平局',
      value: draws,
      max,
      tone: draws ? 'info' : 'accent',
      caption: `${draws} / ${captionSuffix}`,
    },
    {
      label: '失利',
      value: losses,
      max,
      tone: losses ? 'danger' : 'success',
      caption: `${losses} / ${captionSuffix}`,
    },
    {
      label: '有进球',
      value: scored,
      max,
      tone: scored === scoredMatches.length && scoredMatches.length ? 'success' : 'warning',
      caption: `${scored} / ${captionSuffix}`,
    },
    {
      label: '零封',
      value: cleanSheets,
      max,
      tone: cleanSheets ? 'success' : 'info',
      caption: `${cleanSheets} / ${captionSuffix}`,
    },
  ];
});

const teamGroupOptions = computed(() => {
  const groups = new Set<string>();
  for (const team of teams.value) {
    const groupName = teamGroupName(team);
    if (groupName) {
      groups.add(groupName);
    }
  }
  return [...groups].sort((left, right) => left.localeCompare(right, 'zh-Hans-CN'));
});

const filteredTeams = computed(() => {
  const query = teamSearchQuery.value.trim().toLowerCase();
  return teams.value.filter((team) => {
    const groupName = teamGroupName(team);
    const readableTags = formatStyleTags(team.styleTags);
    const searchText = [
      team.displayName,
      team.teamKey,
      team.fifaCode,
      team.countryRegion,
      team.countryIso2,
      team.flagAssetKey,
      team.confederation,
      groupName,
      standingLabel(team),
      team.groupStandingSummary,
      readableTags,
      team.attackProfile,
      team.defenseProfile,
      team.publicSentiment,
    ].filter(Boolean).join(' ').toLowerCase();
    const matchesSearch = !query || searchText.includes(query);
    const matchesGroup = teamGroupFilter.value === 'ALL' || groupName === teamGroupFilter.value;
    const matchesData = teamDataFilter.value === 'ALL'
      || (teamDataFilter.value === 'NO_FACTS' && team.factCount === 0)
      || (teamDataFilter.value === 'HAS_FACTS' && team.factCount > 0)
      || (teamDataFilter.value === 'FULL_ROSTER' && team.playerCount >= 23)
      || (teamDataFilter.value === 'NO_ATTACK_DEFENSE' && !(team.attackProfile && team.defenseProfile))
      || (teamDataFilter.value === 'METADATA_GAP' && !teamHasNationalContext(team));
    return matchesSearch && matchesGroup && matchesData;
  });
});

const teamPageCount = computed(() => Math.max(1, Math.ceil(filteredTeams.value.length / TEAM_PAGE_SIZE)));
const pagedTeams = computed(() => {
  const start = (teamPage.value - 1) * TEAM_PAGE_SIZE;
  return filteredTeams.value.slice(start, start + TEAM_PAGE_SIZE);
});
const teamFilterActive = computed(() => Boolean(teamSearchQuery.value.trim()) || teamGroupFilter.value !== 'ALL' || teamDataFilter.value !== 'ALL');

function reliabilityLabel(value?: number): string {
  return value == null ? '未评分' : `${Number(value).toFixed(1).replace(/\.0$/, '')} / 10`;
}

function formatDateTime(value?: string): string {
  return value ? value.replace('T', ' ').slice(0, 16) : '待同步';
}

function metricText(value?: number, unit = ''): string {
  if (value == null) {
    return '待同步';
  }
  return `${Number(value).toFixed(2).replace(/\.00$/, '').replace(/0$/, '')}${unit}`;
}

function thresholdTone(value: number | null | undefined, high: number, mid: number): 'success' | 'warning' | 'danger' | 'info' {
  if (value == null) {
    return 'info';
  }
  if (value >= high) {
    return 'success';
  }
  if (value >= mid) {
    return 'warning';
  }
  return 'danger';
}

function pressureTone(value: number | null | undefined, low: number, mid: number): 'success' | 'warning' | 'danger' | 'info' {
  if (value == null) {
    return 'info';
  }
  if (value <= low) {
    return 'success';
  }
  if (value <= mid) {
    return 'warning';
  }
  return 'danger';
}

function styleTagParts(value?: string): string[] {
  if (!value) {
    return [];
  }
  const trimmed = value.trim();
  if (!trimmed) {
    return [];
  }
  if (trimmed.startsWith('[') && trimmed.endsWith(']')) {
    try {
      const parsed = JSON.parse(trimmed);
      if (Array.isArray(parsed)) {
        return parsed.map((item) => String(item).trim()).filter(Boolean);
      }
    } catch {
      // 使用兜底清洗，避免公开页把 JSON 原样展示给用户。
    }
    return trimmed
      .replace(/^\[/, '')
      .replace(/\]$/, '')
      .split(',')
      .map((item) => item.replace(/^['"]|['"]$/g, '').trim())
      .filter(Boolean);
  }
  return trimmed.split(/[、,，/|]+/).map((item) => item.trim()).filter(Boolean);
}

function formatStyleTags(value?: string): string {
  const parts = styleTagParts(value);
  return parts.length ? parts.join(' · ') : '';
}

function teamGroupName(team: PublicTeamProfileSummary): string {
  if (team.groupName) {
    return team.groupName;
  }
  return styleTagParts(team.styleTags).find((item) => /^[A-ZＡ-Ｚ]组$/i.test(item) || /^Group\s+[A-Z]$/i.test(item)) || '';
}

function teamHasStanding(team: PublicTeamProfileSummary): boolean {
  return team.groupStandingRank != null && team.groupStandingPoints != null;
}

function standingRankText(team: PublicTeamProfileSummary): string {
  return team.groupStandingRank != null ? `第 ${team.groupStandingRank} 名` : '待同步';
}

function standingLabel(team: PublicTeamProfileSummary): string {
  const group = teamGroupName(team) || '小组';
  if (!teamHasStanding(team)) {
    return `${group}积分待同步`;
  }
  return `${group}第${team.groupStandingRank} · ${team.groupStandingPoints}分`;
}

function goalDiffText(value?: number): string {
  if (value == null) {
    return '待同步';
  }
  return value > 0 ? `+${value}` : String(value);
}

function teamHasNationalContext(team: PublicTeamProfileSummary): boolean {
  return Boolean(team.fifaCode && teamGroupName(team));
}

function resetTeamFilters() {
  teamSearchQuery.value = '';
  teamGroupFilter.value = 'ALL';
  teamDataFilter.value = 'ALL';
  teamPage.value = 1;
}


function applyTeamDataFilter(filter: string) {
  teamSearchQuery.value = '';
  teamGroupFilter.value = 'ALL';
  teamDataFilter.value = filter;
  teamPage.value = 1;
}

function goTeamPage(direction: 'PREV' | 'NEXT') {
  if (direction === 'PREV') {
    teamPage.value = Math.max(1, teamPage.value - 1);
    return;
  }
  teamPage.value = Math.min(teamPageCount.value, teamPage.value + 1);
}

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const response = await listPublicTeamProfiles();
    teams.value = response.data;
    const nextTeam = selectedTeamId.value
      ? teams.value.find((team) => team.id === selectedTeamId.value) ?? teams.value[0]
      : teams.value[0];
    if (nextTeam) {
      await openTeam(nextTeam);
    } else {
      selected.value = null;
      selectedTeamId.value = null;
    }
  } catch (cause) {
    teams.value = [];
    selected.value = null;
    selectedTeamId.value = null;
    error.value = cause instanceof Error ? cause.message : '无法读取公开球队画像数据。';
  } finally {
    loading.value = false;
  }
}

async function openTeam(team: PublicTeamProfileSummary) {
  selectedTeamId.value = team.id;
  detailLoading.value = true;
  detailError.value = '';
  try {
    const response = await getPublicTeamProfile(team.id);
    selected.value = response.data;
  } catch (cause) {
    selected.value = null;
    detailError.value = cause instanceof Error ? cause.message : '无法读取公开球队详情。';
  } finally {
    detailLoading.value = false;
  }
}

onMounted(load);

watch([teamSearchQuery, teamGroupFilter, teamDataFilter], () => {
  teamPage.value = 1;
});

watch(teamPageCount, (next) => {
  if (teamPage.value > next) {
    teamPage.value = next;
  }
});
</script>

<template>
  <section class="page-shell evidence-page profile-page" aria-labelledby="team-profile-title">
    <section class="page-content profile-page__content">
      <header class="evidence-hero">
        <div>
          <p class="eyebrow">证据 · 球队</p>
          <h1 id="team-profile-title">球队画像中心</h1>
        </div>
        <button class="action-button" type="button" :disabled="loading" @click="load">
          {{ loading ? '刷新中' : '刷新公开数据' }}
        </button>
      </header>

      <section class="stat-grid" aria-label="球队画像统计" tabindex="0">
        <article class="stat-card"><span>球队</span><strong>{{ stats.teams }}</strong><small>公开画像</small></article>
        <article class="stat-card"><span>球员</span><strong>{{ stats.players }}</strong><small>名单覆盖</small></article>
        <article class="stat-card"><span>事实</span><strong>{{ stats.facts }}</strong><small>画像事实</small></article>
        <article class="stat-card"><span>证据</span><strong>{{ stats.evidence }}</strong><small>当前球队链路</small></article>
      </section>

      <section class="team-overview-structure" data-test="team-overview-rings" aria-label="球队画像结构" tabindex="0">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">画像结构</p>
            <h2>球队画像结构</h2>
          </div>
          <span class="count-pill">{{ stats.teams }} 支球队</span>
        </div>
        <div class="team-overview-rings" tabindex="0" aria-label="球队画像结构环形图">
          <CoverageDonut
            v-for="ring in teamOverviewRings"
            :key="ring.label"
            :label="ring.label"
            :value="ring.value"
            :max="ring.max"
            :unit="ring.unit"
            :tone="ring.tone"
            size="compact"
            :caption="ring.caption"
          />
        </div>
      </section>

      <section class="team-overview-board" data-test="team-data-overview" aria-label="球队资料准备度总览" tabindex="0">
        <article class="overview-score-card">
          <p class="eyebrow">重点</p>
          <h2>球队资料准备度总览</h2>
          <CoverageDonut
            label="球队资料度"
            :value="teamDataOverview.score"
            unit="%"
            :tone="teamDataOverview.score >= 80 ? 'success' : teamDataOverview.score >= 55 ? 'warning' : 'danger'"
            :caption="teamDataOverview.level"
          />
        </article>
        <div class="overview-bars" aria-label="球队数据覆盖条形图" tabindex="0">
          <MetricBar
            v-for="bar in teamDataOverview.bars"
            :key="bar.label"
            :label="bar.label"
            :value="bar.value"
            :max="bar.max"
            :tone="bar.tone"
            :caption="bar.caption"
          />
        </div>
        <article class="overview-gap-card">
          <p class="eyebrow">数据缺口</p>
          <h3>球队数据缺口</h3>
          <div class="gap-action-list" aria-label="球队数据缺口筛选" tabindex="0">
            <button
              v-for="action in teamDataOverview.priorityActions"
              :key="action.id"
              class="gap-action-button"
              type="button"
              @click="applyTeamDataFilter(action.filter)"
            >
              <span>{{ action.title }}</span>
              <small>{{ action.body }}</small>
              <strong>{{ action.cta }}</strong>
            </button>
          </div>
          <ol>
            <li v-for="gap in teamDataOverview.priorityGaps" :key="gap">{{ gap }}</li>
          </ol>
        </article>
      </section>

      <div v-if="error" class="alert-panel" role="alert">{{ error }}</div>

      <section class="evidence-grid">
        <aside class="side-panel" aria-label="球队列表" tabindex="0">
          <div class="panel-heading">
            <div><p class="eyebrow">球队</p><h2>球队列表</h2></div>
            <span class="count-pill">{{ teams.length }}</span>
          </div>
          <section class="team-filter-panel" aria-label="球队检索与筛选">
            <label class="filter-field">
              <span>搜索球队</span>
              <input
                v-model="teamSearchQuery"
                type="search"
                aria-label="搜索球队名、FIFA code、小组或风格"
                placeholder="如 巴西 / BRA / K组"
              />
            </label>
            <div class="filter-row">
              <label class="filter-field">
                <span>小组</span>
                <select v-model="teamGroupFilter" aria-label="按小组筛选球队">
                  <option value="ALL">全部小组</option>
                  <option v-for="group in teamGroupOptions" :key="group" :value="group">{{ group }}</option>
                </select>
              </label>
              <label class="filter-field">
                <span>数据</span>
                <select v-model="teamDataFilter" aria-label="按球队数据覆盖筛选">
                  <option value="ALL">全部球队</option>
                  <option value="NO_FACTS">缺画像事实</option>
                  <option value="HAS_FACTS">已有画像事实</option>
                  <option value="FULL_ROSTER">名单较齐</option>
                  <option value="NO_ATTACK_DEFENSE">缺攻防画像</option>
                  <option value="METADATA_GAP">国家上下文待同步</option>
                </select>
              </label>
            </div>
            <div class="filter-summary">
              <span>已筛出 {{ filteredTeams.length }} / {{ teams.length }} 支球队</span>
              <button v-if="teamFilterActive" class="ghost-button" type="button" @click="resetTeamFilters">清除筛选</button>
            </div>
          </section>
          <p v-if="loading && !teams.length" class="empty-copy">正在加载公开球队...</p>
          <p v-else-if="!teams.length" class="empty-copy">暂无公开球队画像。</p>
          <div v-else-if="filteredTeams.length" class="team-list-scroll" tabindex="0" aria-label="球队筛选结果">
            <button
              v-for="team in pagedTeams"
              :key="team.id"
              class="list-card"
              :class="{ 'list-card--active': team.id === selectedTeamId }"
              type="button"
              @click="openTeam(team)"
            >
              <FlagTeamName
                :team="{ teamId: team.id, teamName: team.displayName, fifaCode: team.fifaCode, countryIso2: team.countryIso2, flagUrl: team.flagAssetKey, countryRegion: team.countryRegion }"
                compact
              />
              <small>{{ teamGroupName(team) || team.countryRegion || '小组待同步' }}</small>
              <small>{{ standingLabel(team) }}</small>
              <small>{{ formatStyleTags(team.styleTags) || '风格待同步' }}</small>
              <small>{{ team.playerCount }} 名球员 · {{ team.factCount }} 条事实</small>
            </button>
          </div>
          <div v-else class="empty-filter-state">
            <strong>没有找到匹配球队</strong>
            <p>换一个国家名、FIFA code、小组，或清除筛选后查看。</p>
            <button class="ghost-button" type="button" @click="resetTeamFilters">清除筛选</button>
          </div>
          <nav v-if="filteredTeams.length > TEAM_PAGE_SIZE" class="team-pager" aria-label="球队列表分页">
            <button type="button" :disabled="teamPage <= 1" @click="goTeamPage('PREV')">上一页</button>
            <span>第 {{ teamPage }} / {{ teamPageCount }} 页</span>
            <button type="button" :disabled="teamPage >= teamPageCount" @click="goTeamPage('NEXT')">下一页</button>
          </nav>
        </aside>

        <article class="detail-panel" tabindex="0">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">球队详情</p>
              <h2>{{ selected?.team.displayName || '球队详情' }}</h2>
            </div>
            <span v-if="selected" class="status-pill">证据 {{ selected.evidenceCount }} · 冲突 {{ selected.conflictCount }}</span>
          </div>

          <div v-if="detailError" class="alert-panel" role="alert">{{ detailError }}</div>
          <p v-else-if="detailLoading && !selected" class="empty-copy">正在加载球队详情...</p>
          <p v-else-if="!selected" class="empty-copy">当前未选中球队。</p>

          <template v-else>
            <section class="team-identity-card" aria-label="球队身份与资料状态">
              <FlagTeamName
                :team="{
                  teamId: selected.team.id,
                  teamName: selected.team.displayName,
                  fifaCode: selected.team.fifaCode,
                  countryIso2: selected.team.countryIso2,
                  flagUrl: selected.team.flagAssetKey,
                  countryRegion: selected.team.countryRegion,
                }"
              />
              <div>
                <strong>{{ formatStyleTags(selected.team.styleTags) || '风格标签待同步' }}</strong>
              </div>
            </section>

            <section class="score-context-board" data-test="team-standing-board" aria-label="小组积分态势" tabindex="0">
              <div class="panel-heading">
                <div>
                  <p class="eyebrow">小组积分</p>
                  <h3>{{ standingLabel(selected.team) }}</h3>
                </div>
                <span class="status-pill">{{ selected.team.groupStandingPoints ?? '待同步' }} 分</span>
              </div>
              <div class="summary-grid">
                <div><span>小组</span><strong>{{ teamGroupName(selected.team) || '待同步' }}</strong></div>
                <div><span>排名</span><strong>{{ standingRankText(selected.team) }}</strong></div>
                <div><span>积分</span><strong>{{ selected.team.groupStandingPoints ?? '待同步' }}</strong></div>
                <div><span>战绩</span><strong>{{ selected.team.groupStandingRecord || '待同步' }}</strong></div>
                <div><span>净胜球</span><strong>{{ goalDiffText(selected.team.groupGoalDifference) }}</strong></div>
              </div>
              <p class="coverage-note">
                {{ selected.team.groupStandingSummary || '小组积分、排名、净胜球和战绩待同步。' }}
              </p>
            </section>

            <section class="readiness-board" aria-label="球队资料准备度结论">
              <article class="readiness-score">
                <p class="eyebrow">资料判断</p>
                <strong>{{ teamReadiness.score }}</strong>
                <span>{{ enumLabel('profileReadinessLevel', teamReadiness.level, '待评估') }}</span>
                <MetricBar
                  label="资料准备度"
                  :value="teamReadiness.score"
                  :max="100"
                  unit="%"
                  :tone="teamReadiness.score >= 80 ? 'success' : teamReadiness.score >= 55 ? 'warning' : 'danger'"
                  :caption="teamReadiness.summary"
                />
              </article>
              <article class="readiness-ring-card" aria-label="球队资料准备度环形图" tabindex="0">
                <CoverageDonut
                  v-for="ring in teamReadinessRings"
                  :key="ring.label"
                  :label="ring.label"
                  :value="ring.value"
                  :max="ring.max"
                  :unit="ring.unit"
                  :tone="ring.tone"
                  size="compact"
                  :caption="ring.caption"
                />
              </article>
              <article class="readiness-list readiness-list--good">
                <strong>已具备</strong>
                <ul>
                  <li v-for="item in teamReadiness.strengths.slice(0, 5)" :key="item">{{ item }}</li>
                  <li v-if="!teamReadiness.strengths.length">暂无明确优势维度。</li>
                </ul>
              </article>
              <article class="readiness-list readiness-list--missing">
                <strong>缺口维度</strong>
                <ul>
                  <li v-for="item in teamReadiness.missingDimensions.slice(0, 5)" :key="item">{{ item }}</li>
                  <li v-if="!teamReadiness.missingDimensions.length">关键维度已基本覆盖。</li>
                </ul>
              </article>
            </section>

            <section class="score-context-board" data-test="team-score-context" aria-label="球队近期比分与胜负上下文" tabindex="0">
              <div class="panel-heading">
                <div>
                  <p class="eyebrow">近期比分</p>
                  <h3>近期比分与胜负</h3>
                </div>
                <span class="status-pill">{{ teamScoreContext.resultSummary }}</span>
              </div>
              <div v-if="teamScoreContext.matches.length" class="score-context-grid" tabindex="0">
                <article class="score-headline-card">
                  <p class="eyebrow">查看结果</p>
                  <h4>{{ teamScoreContext.headline }}</h4>
                  <strong>{{ teamScoreContext.primaryScore }}</strong>
                  <span class="result-chip" :class="`result-chip--${teamScoreContext.primaryResultTone}`">
                    {{ teamScoreContext.primaryResultLabel }}
                  </span>
                  <p>{{ teamScoreContext.explanation }}</p>
                </article>
                <div class="score-bars" aria-label="球队近期进失球条形图" tabindex="0">
                  <MetricBar
                    v-for="bar in teamScoreContext.bars"
                    :key="bar.label"
                    :label="bar.label"
                    :value="bar.value"
                    :max="bar.max"
                    :tone="bar.tone"
                    :caption="bar.caption"
                  />
                </div>
                <div class="recent-result-rings" aria-label="近期赛果结构环形图" tabindex="0">
                  <CoverageDonut
                    v-for="ring in teamRecentResultRings"
                    :key="ring.label"
                    :label="ring.label"
                    :value="ring.value"
                    :max="ring.max"
                    unit="场"
                    :tone="ring.tone"
                    :caption="ring.caption"
                    size="compact"
                  />
                </div>
                <div class="recent-result-list" aria-label="近期比赛比分列表" tabindex="0">
                  <article v-for="match in teamScoreContext.matches" :key="match.matchId" class="recent-result-card">
                    <div class="recent-result-top">
                      <strong>{{ match.matchName }}</strong>
                      <span class="result-chip" :class="`result-chip--${match.resultTone}`">{{ match.resultLabel }}</span>
                    </div>
                    <div class="recent-scoreline">
                      <strong>{{ match.scoreDisplay }}</strong>
                      <small>{{ matchStatusLabel(match.resultStatus) }}</small>
                    </div>
                    <small>
                      {{ match.stage || '阶段待同步' }} · {{ match.venue || '场地待同步' }} · 首球 {{ match.firstGoalText }}
                    </small>
                    <small>进球分钟：{{ match.scoringMinutes }}</small>
                  </article>
                </div>
              </div>
              <p v-else class="empty-copy">
                暂无近期比赛比分；近五场比分、胜平负、首球时间和进球分钟为空。
              </p>
            </section>

            <section class="summary-grid" aria-label="球队摘要">
              <div><span>FIFA</span><strong>{{ selected.team.fifaCode || '-' }}</strong></div>
              <div><span>国旗/ISO</span><strong>{{ selected.team.countryIso2 || selected.team.flagAssetKey || '待同步' }}</strong></div>
              <div><span>洲别</span><strong>{{ selected.team.confederation || '待同步' }}</strong></div>
              <div><span>小组</span><strong>{{ teamGroupName(selected.team) || '待同步' }}</strong></div>
              <div><span>地区</span><strong>{{ selected.team.countryRegion || '-' }}</strong></div>
              <div><span>攻击画像</span><strong>{{ selected.team.attackProfile || '-' }}</strong></div>
              <div><span>防守画像</span><strong>{{ selected.team.defenseProfile || '-' }}</strong></div>
              <div><span>元数据来源</span><strong>{{ selected.team.metadataSourceRef || '待同步' }}</strong></div>
            </section>

            <section class="coverage-board" aria-label="球队数据缺口情况">
              <div class="panel-heading">
                <div><p class="eyebrow">资料缺口</p><h3>球队资料条形图</h3></div>
              </div>
              <div class="coverage-grid" tabindex="0">
                <MetricBar
                  v-for="bar in teamCoverageBars"
                  :key="bar.label"
                  :label="bar.label"
                  :value="bar.value"
                  :max="bar.max"
                  :tone="bar.tone"
                  :caption="bar.caption"
                />
              </div>
              <p class="coverage-note">
                持续缺口：近五场强弱、xG/xGA、PPDA、定位球、伤停停赛、赛程疲劳、旅行距离、天气场地和赔率/市场价格快照。
              </p>
            </section>

            <section class="metric-board" data-test="team-metric-board" aria-label="球队高阶指标">
              <div class="panel-heading">
                <div><p class="eyebrow">高阶指标</p><h3>xG / PPDA / 状态分</h3></div>
                <span class="status-pill">{{ selected.latestMetric?.sourceName || '指标待同步' }}</span>
              </div>
              <div v-if="teamMetricRings.length" class="team-metric-rings" aria-label="球队攻防态势环形图" tabindex="0">
                <CoverageDonut
                  v-for="ring in teamMetricRings"
                  :key="ring.label"
                  :label="ring.label"
                  :value="ring.value"
                  unit="%"
                  :tone="ring.tone"
                  size="compact"
                  :caption="ring.caption"
                />
              </div>
              <div v-if="teamMetricRows.length" class="metric-grid" tabindex="0">
                <MetricBar
                  v-for="row in teamMetricRows"
                  :key="row.label"
                  :label="row.label"
                  :value="row.value"
                  :max="row.max"
                  :tone="row.tone"
                  :caption="row.caption"
                />
              </div>
              <div class="metric-summary">
                <span>xG {{ metricText(selected.latestMetric?.xg) }}</span>
                <span>xGA {{ metricText(selected.latestMetric?.xga) }}</span>
                <span>射门 {{ selected.latestMetric?.shots ?? '待同步' }}</span>
                <span>控球 {{ metricText(selected.latestMetric?.possessionPct, '%') }}</span>
                <span>更新时间 {{ formatDateTime(selected.latestMetric?.capturedAt) }}</span>
              </div>
              <p v-if="!selected.latestMetric" class="empty-copy">暂无高阶指标；xG、xGA、npxG、PPDA、射门、控球、渐进传球和定位球 xG 缺口。</p>
            </section>

            <section class="info-card">
              <p class="eyebrow">公开情绪</p>
              <h3>公开情绪与风格</h3>
              <p>{{ selected.team.publicSentiment || '暂无公开情绪摘要' }}</p>
              <small>{{ formatStyleTags(selected.team.styleTags) || '暂无风格标签' }}</small>
            </section>

            <section class="card-grid" aria-label="球队画像内容" tabindex="0">
              <article class="info-card">
                <div class="mini-card-heading">
                  <div><p class="eyebrow">事实</p><h3>画像事实</h3></div>
                  <span class="count-pill">{{ selected.facts.length }} 条</span>
                </div>
                <div v-if="selected.facts.length" class="stack-scroll" tabindex="0" aria-label="球队画像事实明细">
                  <div v-for="fact in selected.facts" :key="fact.id" class="stack-item">
                    <strong>{{ fact.title }} <small>{{ factTypeLabel(fact.factType) }}</small></strong>
                    <span>{{ readablePublicText(fact.summary) }}</span>
                    <small>{{ fact.sourceName }} · 可信度 {{ reliabilityLabel(fact.reliabilityScore) }} · {{ formatDateTime(fact.capturedAt) }}</small>
                  </div>
                </div>
                <p v-if="!selected.facts.length" class="empty-copy">暂无画像事实；战术风格、近期状态、核心伤停、教练发布会和赔率/市场价格快照缺口。</p>
              </article>

              <article class="info-card">
                <div class="mini-card-heading">
                  <div><p class="eyebrow">球员</p><h3>球员名单</h3></div>
                  <span class="count-pill">{{ selected.players.length }} 人</span>
                </div>
                <div v-if="selected.players.length" class="stack-scroll" tabindex="0" aria-label="球员名单明细">
                  <div v-for="player in selected.players" :key="player.id" class="stack-item">
                    <strong>{{ player.displayName }} <small>#{{ player.shirtNumber ?? '-' }}</small></strong>
                    <span>{{ positionLabel(player.position) }} · {{ enumLabel('playerStatus', player.status, '状态待同步') }}</span>
                    <small>伤病 {{ player.injuryStatus || '-' }} · 牌面 {{ player.cardStatus || '-' }}</small>
                  </div>
                </div>
                <p v-if="!selected.players.length" class="empty-copy">暂无球员名单；大名单、预计首发、关键替补和伤停/停赛信息缺口。</p>
              </article>

              <article class="info-card">
                <div class="mini-card-heading">
                  <div><p class="eyebrow">阵容</p><h3>上阵人员 / 首发阵容</h3></div>
                  <span class="count-pill">{{ selected.lineups.length }} 条</span>
                </div>
                <div v-if="selected.lineups.length" class="stack-scroll" tabindex="0" aria-label="阵容记录明细">
                  <div v-for="lineup in selected.lineups" :key="`${lineup.matchId}-${lineup.playerName}`" class="stack-item">
                    <strong>{{ lineup.playerName }} <small>{{ positionLabel(lineup.position) }}</small></strong>
                    <span>{{ lineup.matchName }} · {{ lineupRoleLabel(lineup.role) }}</span>
                    <small>{{ lineup.starter ? '首发' : '替补/上阵' }}</small>
                  </div>
                </div>
                <p v-if="!selected.lineups.length" class="empty-copy">暂无阵容数据。</p>
              </article>

              <article class="info-card">
                <div class="mini-card-heading">
                  <div><p class="eyebrow">进球</p><h3>历史进球时间点</h3></div>
                  <span class="count-pill">{{ selected.scoringPatterns.length }} 场</span>
                </div>
                <div v-if="selected.scoringPatterns.length" class="stack-scroll" tabindex="0" aria-label="历史进球时间点明细">
                  <div v-for="pattern in selected.scoringPatterns" :key="pattern.matchId" class="stack-item">
                    <strong>{{ pattern.matchName }} <small>{{ pattern.goalsFor ?? '-' }} / {{ pattern.goalsAgainst ?? '-' }}</small></strong>
                    <span>首球 {{ pattern.firstGoalMinute ?? '-' }} 分钟</span>
                    <small>{{ formatScoringMinutes(pattern.scoringMinutes) }}</small>
                  </div>
                </div>
                <p v-if="!selected.scoringPatterns.length" class="empty-copy">暂无进球时间数据。</p>
              </article>

              <article class="info-card">
                <div class="mini-card-heading">
                  <div><p class="eyebrow">外部</p><h3>外部因素</h3></div>
                  <span class="count-pill">{{ selected.externalFactors.length }} 条</span>
                </div>
                <div v-if="selected.externalFactors.length" class="stack-scroll" tabindex="0" aria-label="外部因素明细">
                  <div v-for="factor in selected.externalFactors" :key="factor.matchId" class="stack-item">
                    <strong>{{ factor.matchName }}</strong>
                    <span>{{ readablePublicText(factor.externalFactors) }}</span>
                    <small>{{ formatDateTime(factor.matchday) }}</small>
                  </div>
                </div>
                <p v-if="!selected.externalFactors.length" class="empty-copy">暂无外部因素。</p>
              </article>

              <article class="info-card">
                <div class="mini-card-heading">
                  <div><p class="eyebrow">历史</p><h3>历史比赛</h3></div>
                  <span class="count-pill">{{ selected.matchHistory.length }} 场</span>
                </div>
                <div v-if="selected.matchHistory.length" class="stack-scroll" tabindex="0" aria-label="历史比赛明细">
                  <div v-for="match in selected.matchHistory" :key="match.matchId" class="stack-item">
                    <strong>{{ match.matchName }} <small>{{ match.stage || '阶段待定' }}</small></strong>
                    <span>{{ match.competition || '赛事待定' }} · {{ match.venue || '场地待同步' }} · {{ matchStatusLabel(match.resultStatus) }}</span>
                    <small>进失球 {{ match.goalsFor ?? '-' }} / {{ match.goalsAgainst ?? '-' }} · {{ formatScoringMinutes(match.scoringMinutes) }}</small>
                  </div>
                </div>
                <p v-if="!selected.matchHistory.length" class="empty-copy">暂无历史比赛。</p>
              </article>
            </section>
          </template>
        </article>
      </section>
    </section>
  </section>
</template>

<style scoped>
.evidence-page { max-width: 100%; overflow-x: hidden; }
.profile-page__content { display: grid; gap: 18px; min-width: 0; }
.evidence-hero, .stat-card, .team-overview-structure, .team-overview-board, .overview-score-card, .overview-gap-card, .side-panel, .detail-panel, .team-identity-card, .readiness-board, .score-context-board, .coverage-board, .metric-board, .info-card, .summary-grid div, .alert-panel {
  background: var(--wc-glass);
  border: 1px solid var(--wc-border);
  border-radius: var(--wc-radius-lg);
  color: var(--wc-text);
}
.evidence-hero {
  align-items: center;
  display: grid;
  gap: 18px;
  grid-template-columns: minmax(0, 1fr) auto;
  padding: clamp(20px, 4vw, 38px);
}
.evidence-hero h1 {
  font-family: var(--wc-font-display);
  font-size: clamp(34px, 6vw, 68px);
  line-height: 1;
  margin: 0 0 12px;
}
.evidence-hero p:not(.eyebrow), .empty-copy, .list-card span, .list-card small, .stack-item span, .stack-item small, .summary-grid span, .info-card p, .team-identity-card p, .coverage-note, .readiness-list li, .overview-score-card p, .overview-gap-card li {
  color: var(--wc-text-muted);
}
.eyebrow {
  color: var(--wc-warning);
  font-family: var(--wc-font-mono);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: .08em;
  margin: 0 0 8px;
  text-transform: uppercase;
}
.action-button {
  background: var(--wc-accent);
  border: 0;
  border-radius: 999px;
  color: var(--wc-on-accent);
  cursor: pointer;
  font-weight: 800;
  min-height: 44px;
  padding: 0 16px;
}
.stat-grid, .summary-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}
.stat-card, .overview-score-card, .overview-gap-card, .side-panel, .detail-panel, .team-identity-card, .readiness-board, .score-context-board, .coverage-board, .metric-board, .info-card, .alert-panel, .summary-grid div {
  display: grid;
  gap: 12px;
  min-width: 0;
  padding: 18px;
}
.stat-card strong {
  color: var(--wc-primary);
  font-family: var(--wc-font-mono);
  font-size: 36px;
}
.team-overview-structure {
  display: grid;
  gap: 14px;
  min-width: 0;
  padding: 18px;
}
.team-overview-rings {
  display: grid;
  gap: 13px;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  min-width: 0;
}
.team-overview-board {
  display: grid;
  gap: 16px;
  grid-template-columns: minmax(0, .9fr) minmax(0, 1.35fr) minmax(0, .8fr);
  min-width: 0;
  padding: 18px;
}
.overview-score-card {
  background:
    radial-gradient(circle at 16% 0%, rgba(96, 165, 250, .14), transparent 30%),
    rgba(15, 23, 42, .5);
}
.overview-score-card h2,
.overview-gap-card h3 {
  font-family: var(--wc-font-display);
  font-size: clamp(22px, 2.45vw, 30px);
  line-height: 1.12;
  margin: 0;
}
.overview-score-card strong {
  color: var(--wc-primary);
  font-family: var(--wc-font-mono);
  font-size: clamp(36px, 4.2vw, 54px);
  line-height: 1;
}
.overview-score-card span {
  color: var(--wc-warning);
  font-weight: 900;
}
.overview-score-card p,
.overview-gap-card li {
  line-height: 1.65;
  margin: 0;
}
.overview-bars {
  display: grid;
  gap: 12px;
  max-height: 520px;
  min-width: 0;
  overflow: auto;
  padding-right: 2px;
}

.gap-action-list {
  display: grid;
  gap: 10px;
  max-height: 360px;
  min-width: 0;
  overflow: auto;
  padding-right: 2px;
}

.gap-action-button {
  background: rgba(15, 23, 42, .48);
  border: 1px solid rgba(217, 119, 6, .26);
  border-radius: var(--wc-radius-md);
  color: var(--wc-text);
  cursor: pointer;
  display: grid;
  gap: 5px;
  min-height: 72px;
  padding: 12px;
  text-align: left;
  transition: border-color 180ms ease, background 180ms ease, transform 180ms ease;
}

.gap-action-button:hover {
  background: rgba(30, 41, 59, .72);
  border-color: rgba(217, 119, 6, .52);
}

.gap-action-button:focus-visible {
  box-shadow: var(--wc-focus-ring);
  outline: none;
}

.gap-action-button span,
.gap-action-button strong {
  font-weight: 900;
}

.gap-action-button small {
  color: var(--wc-text-muted);
  line-height: 1.5;
}

.gap-action-button strong {
  color: var(--wc-warning);
  font-size: 12px;
}
.overview-gap-card ol {
  display: grid;
  gap: 9px;
  margin: 0;
  padding-left: 20px;
}
.evidence-grid {
  align-items: start;
  display: grid;
  gap: 16px;
  grid-template-columns: minmax(0, 340px) minmax(0, 1fr);
  min-width: 0;
}
.team-overview-structure,
.team-overview-rings,
.team-overview-board,
.overview-bars,
.gap-action-list,
.side-panel,
.detail-panel,
.readiness-ring-card,
.score-context-board,
.score-context-grid,
.score-bars,
.recent-result-rings,
.recent-result-list,
.coverage-grid,
.team-metric-rings,
.metric-grid,
.card-grid {
  scrollbar-color: rgba(217, 119, 6, .42) rgba(15, 23, 42, .28);
  scrollbar-gutter: stable both-edges;
}
.detail-panel, .readiness-board, .score-context-board, .coverage-board, .metric-board { align-content: start; }
.side-panel {
  max-height: min(76dvh, 680px);
  overflow: auto;
}
.detail-panel {
  max-height: min(78dvh, 700px);
  overflow: auto;
}
.panel-heading {
  align-items: center;
  display: flex;
  gap: 12px;
  justify-content: space-between;
}
.panel-heading h2, .info-card h3 { margin: 0; }
.team-filter-panel {
  background: rgba(15, 23, 42, .38);
  border: 1px solid rgba(147, 197, 253, .14);
  border-radius: var(--wc-radius-md);
  display: grid;
  gap: 12px;
  padding: 12px;
}
.filter-row {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}
.filter-field {
  color: var(--wc-text-muted);
  display: grid;
  font-size: 12px;
  font-weight: 800;
  gap: 6px;
}
.filter-field input,
.filter-field select {
  background: rgba(15, 23, 42, .72);
  border: 1px solid rgba(147, 197, 253, .22);
  border-radius: 14px;
  color: var(--wc-text);
  font: inherit;
  min-height: 44px;
  min-width: 0;
  padding: 0 12px;
}
.filter-field input:focus-visible,
.filter-field select:focus-visible,
.ghost-button:focus-visible,
.team-pager button:focus-visible,
.team-list-scroll:focus-visible,
.stat-grid:focus-visible,
.team-overview-structure:focus-visible,
.team-overview-rings:focus-visible,
.team-overview-board:focus-visible,
.overview-bars:focus-visible,
.gap-action-list:focus-visible,
.side-panel:focus-visible,
.detail-panel:focus-visible,
.readiness-ring-card:focus-visible,
.score-context-board:focus-visible,
.score-context-grid:focus-visible,
.score-bars:focus-visible,
.recent-result-rings:focus-visible,
.recent-result-list:focus-visible,
.coverage-grid:focus-visible,
.team-metric-rings:focus-visible,
.metric-grid:focus-visible,
.card-grid:focus-visible,
.list-card:focus-visible {
  outline: 3px solid rgba(217, 119, 6, .45);
  outline-offset: 2px;
}
.filter-summary {
  align-items: center;
  color: var(--wc-text-muted);
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: space-between;
}
.filter-summary span {
  font-family: var(--wc-font-mono);
  font-size: 12px;
  font-weight: 900;
}
.ghost-button {
  background: rgba(147, 197, 253, .1);
  border: 1px solid rgba(147, 197, 253, .22);
  border-radius: 999px;
  color: var(--wc-primary);
  cursor: pointer;
  font-weight: 900;
  min-height: 44px;
  padding: 0 14px;
}
.team-list-scroll {
  display: grid;
  gap: 10px;
  max-height: min(62dvh, 560px);
  min-width: 0;
  overflow: auto;
  padding-right: 2px;
}
.list-card, .stack-item {
  background: rgba(15, 23, 42, .5);
  border: 1px solid rgba(147, 197, 253, .18);
  border-radius: var(--wc-radius-md);
  color: var(--wc-text);
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 14px;
  text-align: left;
}
.list-card {
  cursor: pointer;
  transition: border-color 180ms ease, transform 180ms ease;
}
.list-card--active { border-color: rgba(217, 119, 6, .62); }
.mini-card-heading {
  align-items: center;
  display: flex;
  gap: 12px;
  justify-content: space-between;
  min-width: 0;
}
.mini-card-heading h3,
.mini-card-heading .eyebrow {
  margin: 0;
}
.stack-scroll {
  display: grid;
  gap: 10px;
  max-height: 460px;
  min-width: 0;
  overflow: auto;
  padding-right: 4px;
  overscroll-behavior: contain;
}
.stack-scroll:focus-visible {
  box-shadow: var(--wc-focus-ring);
  outline: none;
}
.empty-filter-state {
  background: rgba(15, 23, 42, .36);
  border: 1px dashed rgba(245, 158, 11, .38);
  border-radius: var(--wc-radius-md);
  color: var(--wc-text-muted);
  display: grid;
  gap: 10px;
  padding: 14px;
}
.empty-filter-state strong {
  color: var(--wc-text);
}
.empty-filter-state p {
  line-height: 1.58;
  margin: 0;
}
.team-pager {
  align-items: center;
  display: grid;
  gap: 10px;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
}
.team-pager button {
  background: rgba(147, 197, 253, .1);
  border: 1px solid rgba(147, 197, 253, .22);
  border-radius: 999px;
  color: var(--wc-text);
  cursor: pointer;
  font-weight: 900;
  min-height: 44px;
}
.team-pager button:disabled {
  cursor: not-allowed;
  opacity: .45;
}
.team-pager span {
  color: var(--wc-text-muted);
  font-family: var(--wc-font-mono);
  font-size: 12px;
  font-weight: 900;
  text-align: center;
}
.count-pill, .status-pill {
  background: rgba(147, 197, 253, .12);
  border-radius: 999px;
  color: var(--wc-primary);
  font-family: var(--wc-font-mono);
  font-size: 12px;
  font-weight: 800;
  padding: 6px 9px;
}
.team-identity-card {
  align-items: center;
  grid-template-columns: auto minmax(0, 1fr);
}
.team-identity-card strong {
  color: var(--wc-text);
}
.team-identity-card p,
.coverage-note {
  line-height: 1.62;
  margin: 0;
}
.readiness-board {
  grid-template-columns: minmax(0, .9fr) minmax(0, 1fr) minmax(0, 1fr);
}
.readiness-score, .readiness-list, .readiness-ring-card {
  background: rgba(15, 23, 42, .45);
  border: 1px solid rgba(147, 197, 253, .16);
  border-radius: var(--wc-radius-md);
  display: grid;
  gap: 10px;
  min-width: 0;
  padding: 14px;
}
.readiness-ring-card {
  grid-column: 2 / -1;
  grid-template-columns: repeat(auto-fit, minmax(178px, 1fr));
  max-height: 520px;
  overflow: auto;
}
.readiness-score > strong {
  color: var(--wc-primary);
  font-family: var(--wc-font-mono);
  font-size: clamp(42px, 6vw, 72px);
  line-height: .95;
}
.readiness-score > span {
  color: var(--wc-warning);
  font-weight: 900;
}
.readiness-list strong {
  color: var(--wc-text);
  font-size: 16px;
}
.readiness-list ul {
  display: grid;
  gap: 8px;
  margin: 0;
  padding-left: 18px;
}
.readiness-list--good { border-color: rgba(34, 197, 94, .26); }
.readiness-list--missing { border-color: rgba(245, 158, 11, .28); }
.score-context-board {
  background:
    radial-gradient(circle at 10% 0%, rgba(34, 197, 94, .12), transparent 30%),
    rgba(15, 23, 42, .56);
}
.score-context-grid {
  align-items: start;
  display: grid;
  gap: 14px;
  grid-template-columns: minmax(0, .86fr) minmax(0, 1fr) minmax(0, 1.1fr);
  min-width: 0;
}
.score-headline-card,
.recent-result-card {
  background: rgba(15, 23, 42, .48);
  border: 1px solid rgba(147, 197, 253, .16);
  border-radius: var(--wc-radius-md);
  display: grid;
  gap: 10px;
  min-width: 0;
  padding: 14px;
}
.score-headline-card h4 {
  font-family: var(--wc-font-display);
  font-size: clamp(18px, 2vw, 24px);
  line-height: 1.14;
  margin: 0;
  overflow-wrap: anywhere;
}
.score-headline-card > strong {
  color: var(--wc-primary);
  font-family: var(--wc-font-mono);
  font-size: clamp(34px, 4.2vw, 52px);
  line-height: .96;
}
.score-headline-card p,
.recent-result-card small {
  color: var(--wc-text-muted);
  line-height: 1.58;
  margin: 0;
}
.score-bars,
.recent-result-list {
  display: grid;
  gap: 12px;
  min-width: 0;
}
.score-bars {
  max-height: 420px;
  overflow: auto;
  padding-right: 2px;
}
.recent-result-list {
  max-height: 520px;
  overflow: auto;
  padding-right: 2px;
}
.recent-result-rings {
  display: grid;
  gap: 12px;
  grid-column: 1 / -1;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  max-height: 520px;
  min-width: 0;
  overflow: auto;
  padding-right: 2px;
}
.recent-result-rings :deep(.coverage-donut) {
  background: rgba(15, 23, 42, .4);
  border: 1px solid rgba(147, 197, 253, .16);
  border-radius: var(--wc-radius-md);
  padding: 12px;
}
.recent-result-top,
.recent-scoreline {
  align-items: center;
  display: flex;
  gap: 10px;
  justify-content: space-between;
  min-width: 0;
}
.recent-result-top strong {
  overflow-wrap: anywhere;
}
.recent-scoreline strong {
  color: var(--wc-text);
  font-family: var(--wc-font-mono);
  font-size: 24px;
}
.result-chip {
  align-self: start;
  border: 1px solid rgba(148, 163, 184, .24);
  border-radius: 999px;
  color: var(--wc-text-muted);
  display: inline-flex;
  font-family: var(--wc-font-mono);
  font-size: 12px;
  font-weight: 900;
  min-height: 28px;
  padding: 5px 10px;
  width: max-content;
}
.result-chip--win {
  background: rgba(34, 197, 94, .14);
  border-color: rgba(34, 197, 94, .32);
  color: #bbf7d0;
}
.result-chip--draw {
  background: rgba(147, 197, 253, .14);
  border-color: rgba(147, 197, 253, .32);
  color: var(--wc-primary);
}
.result-chip--loss {
  background: rgba(248, 113, 113, .14);
  border-color: rgba(248, 113, 113, .32);
  color: #fecaca;
}
.result-chip--unknown {
  background: rgba(245, 158, 11, .12);
  border-color: rgba(245, 158, 11, .3);
  color: var(--wc-warning);
}
.coverage-grid {
  display: grid;
  gap: 13px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  max-height: 520px;
  min-width: 0;
  overflow: auto;
  padding-right: 2px;
}
.team-metric-rings {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  max-height: 520px;
  min-width: 0;
  overflow: auto;
  padding-right: 2px;
}
.metric-grid {
  display: grid;
  gap: 13px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  max-height: 460px;
  min-width: 0;
  overflow: auto;
  padding-right: 2px;
}
.metric-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.metric-summary span {
  background: rgba(147, 197, 253, .1);
  border: 1px solid rgba(147, 197, 253, .16);
  border-radius: 999px;
  color: var(--wc-text-muted);
  font-family: var(--wc-font-mono);
  font-size: 12px;
  font-weight: 800;
  padding: 7px 10px;
}
.card-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  min-width: 0;
}
@media (max-width: 1024px) {
  .evidence-hero, .team-overview-board, .evidence-grid, .summary-grid, .team-identity-card, .readiness-board, .score-context-grid { grid-template-columns: 1fr; }
  .readiness-ring-card { grid-column: auto; }
  .stat-grid, .coverage-grid, .card-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 640px) {
  .evidence-hero, .team-overview-board, .evidence-grid, .team-identity-card, .readiness-board, .score-context-grid, .card-grid { grid-template-columns: 1fr; }
  .stat-grid, .team-overview-rings, .overview-bars, .gap-action-list, .coverage-grid, .team-metric-rings, .readiness-ring-card, .metric-grid, .summary-grid, .recent-result-rings { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .panel-heading { align-items: stretch; flex-direction: column; }
  .mini-card-heading { align-items: stretch; flex-direction: column; }
  .filter-row, .team-pager { grid-template-columns: 1fr; }
  .evidence-hero p:not(.eyebrow),
  .team-identity-card p,
  .overview-gap-card ol,
  .gap-action-button small,
  .overview-bars :deep(.metric-bar small),
  .readiness-score :deep(.metric-bar small),
  .score-bars :deep(.metric-bar small),
  .coverage-grid :deep(.metric-bar small),
  .metric-grid :deep(.metric-bar small),
  .team-overview-rings :deep(.coverage-donut__copy small),
  .readiness-ring-card :deep(.coverage-donut__copy small),
  .recent-result-rings :deep(.coverage-donut__copy small),
  .team-metric-rings :deep(.coverage-donut__copy small) {
    display: none;
  }
  .evidence-hero,
  .stat-card,
  .overview-score-card,
  .overview-gap-card,
  .side-panel,
  .detail-panel,
  .team-identity-card,
  .readiness-board,
  .score-context-board,
  .coverage-board,
  .metric-board,
  .info-card,
  .summary-grid div {
    padding: 14px;
  }
  .side-panel,
  .detail-panel {
    max-height: none;
    overflow: visible;
    scrollbar-gutter: auto;
  }
  .team-list-scroll {
    max-height: min(48dvh, 420px);
    overflow: auto;
    padding-right: 0;
    scrollbar-gutter: auto;
    scrollbar-width: none;
  }
  .team-list-scroll::-webkit-scrollbar {
    display: none;
  }
  .overview-bars,
  .gap-action-list,
  .readiness-ring-card,
  .score-bars,
  .recent-result-rings,
  .recent-result-list,
  .coverage-grid,
  .team-metric-rings,
  .metric-grid {
    max-height: none;
    overflow: visible;
    padding-right: 0;
    scrollbar-gutter: auto;
  }
  .stack-scroll {
    max-height: 380px;
    padding-right: 0;
    scrollbar-gutter: auto;
    scrollbar-width: none;
  }
  .stack-scroll::-webkit-scrollbar {
    display: none;
  }
  .score-headline-card > strong { font-size: clamp(34px, 14vw, 50px); }
  .recent-scoreline strong { font-size: 20px; }
  .team-overview-rings :deep(.coverage-donut),
  .readiness-ring-card :deep(.coverage-donut),
  .recent-result-rings :deep(.coverage-donut),
  .team-metric-rings :deep(.coverage-donut) {
    gap: 8px;
  }
  .team-overview-rings :deep(.coverage-donut__ring),
  .readiness-ring-card :deep(.coverage-donut__ring),
  .recent-result-rings :deep(.coverage-donut__ring),
  .team-metric-rings :deep(.coverage-donut__ring) {
    width: 76px;
  }
  .team-overview-rings :deep(.coverage-donut__ring span),
  .readiness-ring-card :deep(.coverage-donut__ring span),
  .recent-result-rings :deep(.coverage-donut__ring span),
  .team-metric-rings :deep(.coverage-donut__ring span) {
    font-size: 24px;
  }
  .ghost-button, .team-pager button { width: 100%; }
  .action-button { width: 100%; }
}
@media (prefers-reduced-motion: reduce) {
  .list-card { transition: none; }
}
</style>
