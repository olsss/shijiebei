<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import {
  getPublicMatchDetail,
  listPublicMatches,
  type PublicMatchDetail,
  type PublicMatchSummary,
} from '@/api/matches';
import CoverageDonut from '@/components/football/CoverageDonut.vue';
import MetricBar from '@/components/football/MetricBar.vue';
import ScoreboardCard from '@/components/football/ScoreboardCard.vue';
import {
  enumLabel,
  fieldNameLabel,
  lineupRoleLabel,
  positionLabel,
  readablePublicText,
  sourceTypeLabel,
} from '@/utils/display-labels';
import {
  parseScoringMinutes,
  scoreTone,
  scoreboardFallback,
  scoreboardHasResult,
  statusLabel,
  isPlaceholderTeamName,
  teamNameFromMatchName,
} from '@/utils/football-visuals';

const loading = ref(false);
const detailLoading = ref(false);
const error = ref('');
const detailError = ref('');
const matches = ref<PublicMatchSummary[]>([]);
const selected = ref<PublicMatchDetail | null>(null);
const selectedMatchId = ref<number | null>(null);
const matchSearchQuery = ref('');
const matchStatusFilter = ref('ALL');
const matchIssueFilter = ref('ALL');
const matchPage = ref(1);
const MATCH_PAGE_SIZE = 10;

const stats = computed(() => ({
  matches: matches.value.length,
  events: matches.value.reduce((sum, match) => sum + match.eventCount, 0),
  lineups: matches.value.reduce((sum, match) => sum + match.lineupCount, 0),
  conflicts: matches.value.reduce((sum, match) => sum + match.conflictCount, 0),
}));
const scoreStatusSummary = computed(() => {
  const total = matches.value.length;
  const withScore = matches.value.filter(hasReadableScore).length;
  const scheduled = matches.value.filter((match) => statusGroup(match) === 'SCHEDULED').length;
  const finishedWithoutScore = matches.value.filter(
    (match) => statusGroup(match) === 'FINISHED' && !hasReadableScore(match),
  ).length;
  const withConflict = matches.value.filter((match) => match.conflictCount > 0).length;
  const scoreCoverage = total ? Math.round((withScore / total) * 100) : 0;
  const priority =
    finishedWithoutScore > 0
      ? `有 ${finishedWithoutScore} 场完赛但比分待同步，官方赛果缺口已标出。`
      : withScore > 0
        ? `已有 ${withScore} 场带比分，胜负强对比已关联。`
        : scheduled > 0
          ? `当前多为待开球，开球时间、阵容伤停和资料准备度已展示。`
          : '暂无可读比赛，等待正式赛程和比分入库。';

  return {
    total,
    withScore,
    scheduled,
    finishedWithoutScore,
    withConflict,
    scoreCoverage,
    priority,
  };
});
const scoreStatusRows = computed(() => {
  const summary = scoreStatusSummary.value;
  const max = Math.max(1, summary.total);
  return [
    {
      key: 'with-score',
      label: '有比分',
      value: summary.withScore,
      max,
      tone: 'success',
      caption: '胜负结果与比分对比已展示。',
    },
    {
      key: 'scheduled',
      label: '待开球',
      value: summary.scheduled,
      max,
      tone: 'info',
      caption: '赛果未产生，阵容、天气、场地和外部风险为当前关联资料。',
    },
    {
      key: 'missing-finished',
      label: '完赛缺比分',
      value: summary.finishedWithoutScore,
      max,
      tone: summary.finishedWithoutScore ? 'danger' : 'success',
      caption: '完赛无比分会降低页面可读性。',
    },
    {
      key: 'conflict',
      label: '有冲突',
      value: summary.withConflict,
      max,
      tone: summary.withConflict ? 'warning' : 'success',
      caption: '来源说法不一致，页面结论显示为未定稿。',
    },
  ];
});

const matchMaterialMax = computed(() => Math.max(1, stats.value.events, stats.value.lineups, stats.value.matches));
const matchCatalogRings = computed(() => [
  {
    label: '公开比赛',
    value: stats.value.matches,
    max: Math.max(1, stats.value.matches),
    unit: '场',
    tone: stats.value.matches ? 'success' : 'info',
    caption: `${stats.value.matches} 场公开赛程`,
  },
  {
    label: '比分记录',
    value: scoreStatusSummary.value.withScore,
    max: Math.max(1, stats.value.matches),
    unit: '场',
    tone: scoreStatusSummary.value.withScore ? 'success' : 'warning',
    caption: `${scoreStatusSummary.value.withScore} / ${stats.value.matches} 场有比分`,
  },
  {
    label: '事件材料',
    value: stats.value.events,
    max: matchMaterialMax.value,
    unit: '条',
    tone: stats.value.events ? 'accent' : 'info',
    caption: `${stats.value.events} 条进球、红黄牌等事件`,
  },
  {
    label: '阵容材料',
    value: stats.value.lineups,
    max: matchMaterialMax.value,
    unit: '条',
    tone: stats.value.lineups ? 'success' : 'info',
    caption: `${stats.value.lineups} 条首发与角色记录`,
  },
  {
    label: '冲突状态',
    value: stats.value.conflicts,
    max: Math.max(1, stats.value.conflicts, stats.value.matches),
    unit: '项',
    tone: stats.value.conflicts ? 'warning' : 'success',
    caption: `${stats.value.conflicts} 项公开冲突状态`,
  },
]);
const filteredMatches = computed(() => {
  const query = matchSearchQuery.value.trim().toLowerCase();
  return matches.value.filter((match) => {
    const searchText = [
      match.matchName,
      match.homeTeam?.teamName,
      match.awayTeam?.teamName,
      match.homeTeam?.fifaCode,
      match.awayTeam?.fifaCode,
      match.homeTeamName,
      match.awayTeamName,
      match.jcCode,
      match.venue,
      match.competition,
      match.stage,
    ]
      .filter(Boolean)
      .join(' ')
      .toLowerCase();
    const matchesSearch = !query || searchText.includes(query);
    const matchesStatus = matchStatusFilter.value === 'ALL' || statusGroup(match) === matchStatusFilter.value;
    const matchesIssue =
      matchIssueFilter.value === 'ALL' ||
      (matchIssueFilter.value === 'CONFLICT' && match.conflictCount > 0) ||
      (matchIssueFilter.value === 'NO_SCORE' && !hasReadableScore(match)) ||
      (matchIssueFilter.value === 'HAS_SCORE' && hasReadableScore(match)) ||
      (matchIssueFilter.value === 'ENOUGH_EVIDENCE' && match.evidenceCount >= 4);
    return matchesSearch && matchesStatus && matchesIssue;
  });
});
const matchPageCount = computed(() => Math.max(1, Math.ceil(filteredMatches.value.length / MATCH_PAGE_SIZE)));
const pagedMatches = computed(() => {
  const start = (matchPage.value - 1) * MATCH_PAGE_SIZE;
  return filteredMatches.value.slice(start, start + MATCH_PAGE_SIZE);
});
const matchFilterActive = computed(
  () => Boolean(matchSearchQuery.value.trim()) || matchStatusFilter.value !== 'ALL' || matchIssueFilter.value !== 'ALL',
);

const selectedSummary = computed(() => selected.value?.summary ?? null);
const selectedScoreboard = computed(() => scoreboardFallback(selectedSummary.value));
const matchEvidenceReadiness = computed(() =>
  selected.value?.summary.evidenceCount ? Math.min(100, selected.value.summary.evidenceCount * 12) : 0,
);
const matchEvidenceRings = computed(() => {
  const summary = selected.value?.summary;
  const scoreboard = selectedScoreboard.value;
  const hasScore = scoreboard.homeScore != null && scoreboard.awayScore != null;
  const scoreAnchor = summary ? (hasScore ? 100 : 45) : 0;
  const evidenceCover = Math.min(100, (summary?.evidenceCount ?? 0) * 12);
  const conflictClarity = summary ? Math.max(0, 100 - (summary.conflictCount ?? 0) * 25) : 0;
  return [
    {
      label: '证据可读度',
      value: matchEvidenceReadiness.value,
      tone: scoreTone(matchEvidenceReadiness.value),
      caption: '公开证据、比分、冲突、来源时效综合',
    },
    {
      label: '比分记录',
      value: scoreAnchor,
      tone: scoreTone(scoreAnchor),
      caption: hasScore ? (scoreboard.scoreDisplay || `${scoreboard.homeScore} - ${scoreboard.awayScore}`) : '比分待同步',
    },
    {
      label: '公开证据',
      value: evidenceCover,
      tone: scoreTone(evidenceCover),
      caption: `${summary?.evidenceCount ?? 0} 条证据`,
    },
    {
      label: '冲突清晰度',
      value: conflictClarity,
      tone: scoreTone(conflictClarity),
      caption: `${summary?.conflictCount ?? 0} 个公开冲突`,
    },
  ];
});
const teamComparisonRows = computed(() => {
  const rows = selected.value?.teamStats ?? [];
  const maxGoals = Math.max(1, ...rows.map((row) => row.goalsFor ?? 0), ...rows.map((row) => row.goalsAgainst ?? 0));
  return rows.map((row) => ({
    id: row.id,
    teamName: row.teamName || '球队待定',
    goalsFor: row.goalsFor ?? 0,
    goalsAgainst: row.goalsAgainst ?? 0,
    firstGoalMinute: row.firstGoalMinute,
    scoringMinutes: parseScoringMinutes(row.scoringMinutes),
    maxGoals,
  }));
});
const duelTeams = computed(() => {
  const summary = selected.value?.summary;
  if (!summary) {
    return [];
  }
  const statsRows = selected.value?.teamStats ?? [];
  const homeName = teamDisplayName(summary, 'HOME');
  const awayName = teamDisplayName(summary, 'AWAY');
  const homeStats = findStatsForTeam(statsRows, summary.homeTeamId, homeName);
  const awayStats = findStatsForTeam(statsRows, summary.awayTeamId, awayName);
  const scoreboard = selectedScoreboard.value;
  const homeScore = scoreboard.homeScore ?? homeStats?.goalsFor ?? null;
  const awayScore = scoreboard.awayScore ?? awayStats?.goalsFor ?? null;

  return [
    {
      side: 'HOME',
      label: '主队',
      teamName: homeName,
      score: homeScore,
      goalsFor: homeStats?.goalsFor ?? homeScore,
      goalsAgainst: homeStats?.goalsAgainst ?? awayScore,
      firstGoalMinute: homeStats?.firstGoalMinute ?? null,
      scoringMinutes: parseScoringMinutes(homeStats?.scoringMinutes),
      isWinner: scoreboard.winnerSide === 'HOME',
    },
    {
      side: 'AWAY',
      label: '客队',
      teamName: awayName,
      score: awayScore,
      goalsFor: awayStats?.goalsFor ?? awayScore,
      goalsAgainst: awayStats?.goalsAgainst ?? homeScore,
      firstGoalMinute: awayStats?.firstGoalMinute ?? null,
      scoringMinutes: parseScoringMinutes(awayStats?.scoringMinutes),
      isWinner: scoreboard.winnerSide === 'AWAY',
    },
  ];
});
const duelMetricRows = computed(() => {
  const [home, away] = duelTeams.value;
  if (!home || !away) {
    return [];
  }
  const goalMax = Math.max(1, home.goalsFor ?? 0, away.goalsFor ?? 0);
  const concededMax = Math.max(1, home.goalsAgainst ?? 0, away.goalsAgainst ?? 0);
  const firstGoalMax = Math.max(1, home.firstGoalMinute ?? 0, away.firstGoalMinute ?? 0);
  return [
    {
      code: 'goals',
      label: '比分 / 进球',
      homeValue: home.goalsFor,
      awayValue: away.goalsFor,
      max: goalMax,
      unit: '球',
      lowerBetter: false,
      caption: '进球更多的一方在结果上更占优；未开赛时显示待同步。',
    },
    {
      code: 'conceded',
      label: '失球压力',
      homeValue: home.goalsAgainst,
      awayValue: away.goalsAgainst,
      max: concededMax,
      unit: '球',
      lowerBetter: true,
      caption: '失球数据与防守承压记录；缺数据时显示统计缺口。',
    },
    {
      code: 'first-goal',
      label: '首球时间',
      homeValue: home.firstGoalMinute,
      awayValue: away.firstGoalMinute,
      max: firstGoalMax,
      unit: '分钟',
      lowerBetter: true,
      caption: '首球分钟记录；没有首球记录显示事件缺口。',
    },
  ];
});
const resultVerdict = computed(() => {
  const [home, away] = duelTeams.value;
  const scoreboard = selectedScoreboard.value;
  if (!home || !away) {
    return { title: '等待比赛数据', tone: 'info', body: '当前未选中比赛，胜负强对比为空。' };
  }
  if (scoreboard.winnerSide === 'HOME') {
    return {
      title: `${home.teamName} 胜出`,
      tone: 'success',
      body: `比分 ${scoreboard.scoreDisplay || '待同步'}。主队进球优势、证据质量与冲突状态已展示。`,
    };
  }
  if (scoreboard.winnerSide === 'AWAY') {
    return {
      title: `${away.teamName} 胜出`,
      tone: 'success',
      body: `比分 ${scoreboard.scoreDisplay || '待同步'}。客队进球优势、证据质量与冲突状态已展示。`,
    };
  }
  if (scoreboard.winnerSide === 'DRAW') {
    return {
      title: '双方打平',
      tone: 'warning',
      body: `比分 ${scoreboard.scoreDisplay || '待同步'}。两队进球、红黄牌、伤停和市场价格/舆情变化已展示。`,
    };
  }
  if ((scoreboard.scoreDisplay || '').includes('待开球')) {
    return {
      title: '待开球',
      tone: 'info',
      body: '还没有赛果；开球时间、阵容伤停、天气场地和资料准备度已展示。',
    };
  }
  return {
    title: scoreboard.scoreDisplay || '比分待同步',
    tone: 'warning',
    body: '比分仍未同步；官方比分、事件、球队统计和证据链存在缺口。',
  };
});
const evidenceQualitySummary = computed(() => {
  const evidence = selected.value?.evidence ?? [];
  const conflicts = selected.value?.conflicts ?? [];
  const qualityCounts = evidence.reduce<Record<string, number>>((acc, item) => {
    const level = (item.qualityLevel || 'UNRATED').toUpperCase();
    const key = ['HIGH', 'MEDIUM', 'LOW'].includes(level) ? level : 'UNRATED';
    acc[key] = (acc[key] ?? 0) + 1;
    return acc;
  }, {});
  const staleCount = evidence.filter((item) => ['STALE', 'AGING', 'UNKNOWN'].includes((item.freshnessStatus || 'UNKNOWN').toUpperCase())).length;
  const highCount = qualityCounts.HIGH ?? 0;
  const reviewCount = (qualityCounts.LOW ?? 0) + (qualityCounts.UNRATED ?? 0) + staleCount + conflicts.length;
  return {
    total: evidence.length,
    highCount,
    staleCount,
    reviewCount,
    conflictCount: conflicts.length,
    qualityCounts,
  };
});
const evidenceQualityRows = computed(() => {
  const max = Math.max(1, evidenceQualitySummary.value.total);
  return [
    { code: 'HIGH', label: '高可信证据', tone: 'success', caption: '支撑核心判断，并与比分/阵容关联展示' },
    { code: 'MEDIUM', label: '中可信证据', tone: 'warning', caption: '辅助判断资料，第二来源覆盖较低' },
    { code: 'LOW', label: '低可信证据', tone: 'danger', caption: '弱参考资料，第二来源状态同步展示' },
    { code: 'UNRATED', label: '未评分证据', tone: 'info', caption: '缺少可信度评分' },
  ].map((row) => ({
    ...row,
    count: evidenceQualitySummary.value.qualityCounts[row.code] ?? 0,
    max,
  }));
});
const evidenceQualityRings = computed(() => {
  const summary = evidenceQualitySummary.value;
  const total = Math.max(1, summary.total);
  const mediumCount = summary.qualityCounts.MEDIUM ?? 0;
  const lowCount = summary.qualityCounts.LOW ?? 0;
  const unratedCount = summary.qualityCounts.UNRATED ?? 0;
  return [
    {
      label: '高可信',
      value: summary.highCount,
      max: total,
      tone: summary.highCount ? 'success' : 'info',
      caption: `${summary.highCount} / ${summary.total} 条证据`,
    },
    {
      label: '中可信',
      value: mediumCount,
      max: total,
      tone: mediumCount ? 'warning' : 'info',
      caption: `${mediumCount} / ${summary.total} 条证据`,
    },
    {
      label: '低可信',
      value: lowCount,
      max: total,
      tone: lowCount ? 'danger' : 'success',
      caption: `${lowCount} / ${summary.total} 条证据`,
    },
    {
      label: '未评分',
      value: unratedCount,
      max: total,
      tone: unratedCount ? 'warning' : 'success',
      caption: `${unratedCount} / ${summary.total} 条证据`,
    },
    {
      label: '时效风险',
      value: summary.staleCount,
      max: total,
      tone: summary.staleCount ? 'danger' : 'success',
      caption: `${summary.staleCount} / ${summary.total} 条证据`,
    },
  ];
});
const evidenceSupportRows = computed(() => {
  const groups = new Map<string, { label: string; count: number; sources: Set<string> }>();
  for (const item of selected.value?.evidence ?? []) {
    const label = item.supportsConclusion || '背景证据';
    const group = groups.get(label) ?? { label, count: 0, sources: new Set<string>() };
    group.count += 1;
    if (item.sourceName) {
      group.sources.add(item.sourceName);
    }
    groups.set(label, group);
  }
  return Array.from(groups.values())
    .map((group) => ({
      label: group.label,
      count: group.count,
      sources: Array.from(group.sources).slice(0, 2).join(' / ') || '来源待同步',
    }))
    .sort((left, right) => right.count - left.count || left.label.localeCompare(right.label));
});
const evidenceReviewInsight = computed(() => {
  const summary = evidenceQualitySummary.value;
  if (!summary.total) {
    return '这场比赛还没有公开证据，比分、阵容或外部因素可靠性为空。';
  }
  if (summary.conflictCount) {
    return `有 ${summary.conflictCount} 个冲突待处理，结论区显示未定稿。`;
  }
  if (summary.reviewCount) {
    return `有 ${summary.reviewCount} 个证据质量或时效风险点，官方/第二来源存在缺口。`;
  }
  return '证据质量较稳定：比分、结果与高可信证据支撑关系已展示。';
});

function publicEvidenceAction(value?: string | null): string {
  const text = readablePublicText(value, '来源复验待完成')
    .replace(/可作为/g, '')
    .replace(/但仍需与/g, '；与')
    .replace(/仍需与/g, '与')
    .replace(/交叉核对/g, '交叉校验')
    .replace(/建议/g, '')
    .replace(/请人工/g, '')
    .trim();
  return text || '来源复验待完成';
}

function matchTitle(match?: PublicMatchSummary | null): string {
  if (!match) {
    return '比赛中心';
  }
  return `${teamDisplayName(match, 'HOME')} vs ${teamDisplayName(match, 'AWAY')}`;
}

function teamDisplayName(match: PublicMatchSummary, side: 'HOME' | 'AWAY'): string {
  const visualName = side === 'HOME' ? match.homeTeam?.teamName : match.awayTeam?.teamName;
  const legacyName = side === 'HOME' ? match.homeTeamName : match.awayTeamName;
  const candidate = !isPlaceholderTeamName(visualName) ? visualName : legacyName;
  if (!isPlaceholderTeamName(candidate)) {
    return candidate?.trim() || teamNameFromMatchName(match.matchName, side);
  }
  return teamNameFromMatchName(match.matchName, side);
}

function statusGroup(match: PublicMatchSummary): string {
  if (scoreboardHasResult(scoreboardFallback(match as unknown as Record<string, unknown>, match.status || match.resultStatus))) {
    return 'FINISHED';
  }
  const status = String(match.status || match.resultStatus || '').toUpperCase();
  if (status.includes('FINISHED') || status.includes('FINAL') || status.includes('COMPLETED')) {
    return 'FINISHED';
  }
  if (status.includes('LIVE') || status.includes('IN_PLAY')) {
    return 'LIVE';
  }
  if (status.includes('POSTPONED') || status.includes('CANCELLED')) {
    return 'EXCEPTION';
  }
  return 'SCHEDULED';
}

function hasReadableScore(match: PublicMatchSummary): boolean {
  const scoreboard = scoreboardFallback(match as unknown as Record<string, unknown>, match.status || match.resultStatus);
  return scoreboardHasResult(scoreboard);
}

function formatDateTime(value?: string): string {
  return value ? value.replace('T', ' ').slice(0, 16) : '待同步';
}

function scoreText(value?: number): string {
  return value == null ? '-' : Number(value).toFixed(1).replace(/\.0$/, '');
}

function metricValueText(value?: number | null, unit = ''): string {
  return value == null ? '待同步' : `${Number(value).toFixed(0)}${unit}`;
}

function metricWidth(value?: number | null, max = 1): string {
  if (value == null || max <= 0) {
    return '0%';
  }
  const percent = Math.max(0, Math.min(100, (Number(value) / max) * 100));
  return `${percent}%`;
}

function metricBarStyle(value?: number | null, max = 1): Record<string, string> {
  const width = metricWidth(value, max);
  return {
    width,
    minWidth: value != null && Number(value) > 0 && width !== '0%' ? '3px' : '0',
  };
}

function metricLeaderClass(row: { homeValue?: number | null; awayValue?: number | null; lowerBetter: boolean }): string {
  if (row.homeValue == null || row.awayValue == null || row.homeValue === row.awayValue) {
    return 'duel-row--balanced';
  }
  const homeLeads = row.lowerBetter ? row.homeValue < row.awayValue : row.homeValue > row.awayValue;
  return homeLeads ? 'duel-row--home' : 'duel-row--away';
}

function findStatsForTeam(rows: PublicMatchDetail['teamStats'], teamId?: number, teamName?: string) {
  const normalizedName = teamName?.trim().toLowerCase();
  return rows.find((row) => {
    if (teamId && row.teamId === teamId) {
      return true;
    }
    return Boolean(normalizedName && row.teamName?.trim().toLowerCase() === normalizedName);
  });
}

function evidenceQualityTone(value?: string): string {
  switch ((value || '').toUpperCase()) {
    case 'HIGH':
      return 'success';
    case 'MEDIUM':
      return 'warning';
    case 'LOW':
      return 'danger';
    default:
      return 'info';
  }
}

function freshnessTone(value?: string): string {
  switch ((value || '').toUpperCase()) {
    case 'FRESH':
      return 'success';
    case 'AGING':
      return 'warning';
    case 'STALE':
      return 'danger';
    default:
      return 'info';
  }
}

function matchMeta(match?: PublicMatchSummary | null): string {
  if (!match) {
    return '世界杯 · 待定阶段';
  }
  return `${match.competition || '世界杯'} · ${match.stage || '待定阶段'} · ${formatDateTime(
    match.kickoffTime || match.matchday,
  )}`;
}

function resetMatchFilters() {
  matchSearchQuery.value = '';
  matchStatusFilter.value = 'ALL';
  matchIssueFilter.value = 'ALL';
  matchPage.value = 1;
}

function applyMatchFilterIntent(kind: string) {
  const normalized = kind.trim().toUpperCase();
  matchSearchQuery.value = '';
  if (normalized === 'SCHEDULED') {
    matchStatusFilter.value = 'SCHEDULED';
    matchIssueFilter.value = 'ALL';
    matchPage.value = 1;
    return;
  }
  if (['MISSING_FINISHED', 'MISSING_SCORE', 'NO_SCORE'].includes(normalized)) {
    matchStatusFilter.value = 'FINISHED';
    matchIssueFilter.value = 'NO_SCORE';
    matchPage.value = 1;
    return;
  }
  if (['HAS_SCORE', 'CONFLICT', 'ENOUGH_EVIDENCE'].includes(normalized)) {
    matchStatusFilter.value = 'ALL';
    matchIssueFilter.value = normalized;
    matchPage.value = 1;
  }
}

function applyInitialRouteFilter() {
  const params = new URLSearchParams(window.location.search);
  const filter = params.get('filter') || params.get('issue') || '';
  if (filter) {
    applyMatchFilterIntent(filter);
  }
}

function applyScoreQuickFilter(kind: 'HAS_SCORE' | 'MISSING_FINISHED' | 'CONFLICT' | 'SCHEDULED') {
  applyMatchFilterIntent(kind);
}

function goMatchPage(direction: 'PREV' | 'NEXT') {
  if (direction === 'PREV') {
    matchPage.value = Math.max(1, matchPage.value - 1);
    return;
  }
  matchPage.value = Math.min(matchPageCount.value, matchPage.value + 1);
}

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const response = await listPublicMatches();
    matches.value = response.data;
    const nextMatch = selectedMatchId.value
      ? matches.value.find((match) => match.id === selectedMatchId.value) ?? matches.value[0]
      : matches.value[0];
    if (nextMatch) {
      await openMatch(nextMatch);
    } else {
      selected.value = null;
      selectedMatchId.value = null;
    }
  } catch (cause) {
    matches.value = [];
    selected.value = null;
    selectedMatchId.value = null;
    error.value = cause instanceof Error ? cause.message : '无法读取公开比赛中心数据。';
  } finally {
    loading.value = false;
  }
}

async function openMatch(match: PublicMatchSummary) {
  selectedMatchId.value = match.id;
  detailLoading.value = true;
  detailError.value = '';
  try {
    const response = await getPublicMatchDetail(match.id);
    selected.value = response.data;
  } catch (cause) {
    selected.value = null;
    detailError.value = cause instanceof Error ? cause.message : '无法读取公开比赛详情。';
  } finally {
    detailLoading.value = false;
  }
}

onMounted(() => {
  applyInitialRouteFilter();
  void load();
});

watch([matchSearchQuery, matchStatusFilter, matchIssueFilter], () => {
  matchPage.value = 1;
});

watch(matchPageCount, (count) => {
  if (matchPage.value > count) {
    matchPage.value = count;
  }
});
</script>

<template>
  <section class="page-shell evidence-page" aria-labelledby="match-center-title">
    <section class="page-content evidence-page__content">
      <header class="evidence-hero">
        <div>
          <p class="eyebrow">证据 · 比赛</p>
          <h1 id="match-center-title">比赛中心</h1>
        </div>
        <button class="action-button" type="button" :disabled="loading" @click="load">
          {{ loading ? '刷新中' : '刷新公开数据' }}
        </button>
      </header>

      <section class="stat-grid" aria-label="比赛中心统计">
        <article class="stat-card"><span>比赛</span><strong>{{ stats.matches }}</strong><small>公开赛程</small></article>
        <article class="stat-card"><span>事件</span><strong>{{ stats.events }}</strong><small>进球 / 红黄牌等</small></article>
        <article class="stat-card"><span>阵容</span><strong>{{ stats.lineups }}</strong><small>首发与角色</small></article>
        <article class="stat-card"><span>冲突</span><strong>{{ stats.conflicts }}</strong><small>仅显示状态</small></article>
      </section>

      <section class="match-catalog-structure" data-test="match-catalog-rings" tabindex="0" aria-label="比赛目录结构">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">目录结构</p>
            <h2>比赛目录结构</h2>
          </div>
          <span class="status-pill">{{ stats.matches }} 场比赛</span>
        </div>
        <div class="match-catalog-rings" tabindex="0">
          <CoverageDonut
            v-for="ring in matchCatalogRings"
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

      <section class="score-overview-panel" aria-label="比分状态总览" data-test="match-score-overview" tabindex="0">
        <article class="score-overview-copy">
          <p class="eyebrow">比分状态</p>
          <h2>比分状态总览</h2>
          <strong>{{ scoreStatusSummary.priority }}</strong>
          <div class="quick-filter-row" aria-label="比分状态快速筛选">
            <button type="button" @click="applyScoreQuickFilter('HAS_SCORE')">只看有比分</button>
            <button type="button" @click="applyScoreQuickFilter('SCHEDULED')">只看待开球</button>
            <button type="button" @click="applyScoreQuickFilter('MISSING_FINISHED')">筛完赛缺比分</button>
            <button type="button" @click="applyScoreQuickFilter('CONFLICT')">筛有冲突</button>
          </div>
        </article>

        <article class="score-overview-bars" aria-label="比分状态分布" tabindex="0">
          <div class="score-coverage">
            <CoverageDonut
              label="比分覆盖"
              :value="scoreStatusSummary.scoreCoverage"
              :tone="scoreTone(scoreStatusSummary.scoreCoverage)"
              size="compact"
              :caption="`${scoreStatusSummary.withScore} / ${scoreStatusSummary.total} 场已有比分或赛果`"
            />
          </div>
          <MetricBar
            v-for="row in scoreStatusRows"
            :key="row.key"
            :label="row.label"
            :value="row.value"
            :max="row.max"
            unit="场"
            :tone="row.tone"
            :caption="row.caption"
          />
        </article>
      </section>

      <div v-if="error" class="alert-panel" role="alert">{{ error }}</div>

      <section class="evidence-grid">
        <aside class="side-panel" aria-label="赛程列表" tabindex="0">
          <div class="panel-heading">
            <div><p class="eyebrow">赛程</p><h2>赛程列表</h2></div>
          </div>

          <div class="match-filter-panel" aria-label="赛程搜索与筛选">
            <label class="filter-field">
              <span>搜索比赛</span>
              <input
                v-model="matchSearchQuery"
                aria-label="搜索比赛、球队、竞彩编号或场地"
                autocomplete="off"
                placeholder="球队 / 竞彩 / 场地"
                type="search"
              />
            </label>
            <div class="filter-row">
              <label class="filter-field">
                <span>状态</span>
                <select v-model="matchStatusFilter" aria-label="按比赛状态筛选">
                  <option value="ALL">全部状态</option>
                  <option value="SCHEDULED">待开球</option>
                  <option value="LIVE">进行中</option>
                  <option value="FINISHED">已完赛</option>
                  <option value="EXCEPTION">延期/取消</option>
                </select>
              </label>
              <label class="filter-field">
                <span>问题</span>
                <select v-model="matchIssueFilter" aria-label="按比分、冲突或证据筛选">
                  <option value="ALL">全部比赛</option>
                  <option value="CONFLICT">有冲突</option>
                  <option value="NO_SCORE">缺比分</option>
                  <option value="HAS_SCORE">有比分</option>
                  <option value="ENOUGH_EVIDENCE">证据较多</option>
                </select>
              </label>
            </div>
            <div class="filter-summary" aria-live="polite">
              <span>已筛出 {{ filteredMatches.length }} / {{ matches.length }} 场比赛</span>
              <button v-if="matchFilterActive" class="ghost-button" type="button" @click="resetMatchFilters">清除筛选</button>
            </div>
          </div>

          <p v-if="loading && !matches.length" class="empty-copy">正在加载公开赛程...</p>
          <p v-else-if="!matches.length" class="empty-copy">暂无公开比赛。</p>
          <div v-else-if="filteredMatches.length" class="match-list-scroll" tabindex="0" aria-label="筛选后的赛程列表">
            <button
              v-for="match in pagedMatches"
              :key="match.id"
              class="list-card"
              :class="{ 'list-card--active': match.id === selectedMatchId }"
              type="button"
              @click="openMatch(match)"
            >
              <ScoreboardCard
                compact
                :home-team="match.homeTeam"
                :away-team="match.awayTeam"
                :scoreboard="match.scoreboard"
                :match-name="match.matchName"
                :meta="matchMeta(match)"
                :status="statusLabel(match.status || match.resultStatus)"
              />
              <small>竞彩 {{ match.jcCode || '待定' }} · 证据 {{ match.evidenceCount }} · 冲突 {{ match.conflictCount }}</small>
            </button>
          </div>
          <div v-else class="empty-filter-state">
            <strong>没有找到匹配比赛</strong>
            <p>换一个球队名、竞彩编号或清除筛选。</p>
            <button class="ghost-button" type="button" @click="resetMatchFilters">清除筛选</button>
          </div>

          <div v-if="filteredMatches.length > MATCH_PAGE_SIZE" class="match-pager" aria-label="赛程分页">
            <button type="button" :disabled="matchPage <= 1" @click="goMatchPage('PREV')">上一页</button>
            <span>第 {{ matchPage }} / {{ matchPageCount }} 页</span>
            <button type="button" :disabled="matchPage >= matchPageCount" @click="goMatchPage('NEXT')">下一页</button>
          </div>
        </aside>

        <article class="detail-panel" tabindex="0">
          <div class="panel-heading">
            <div><p class="eyebrow">比赛详情</p><h2>{{ matchTitle(selected?.summary) }}</h2></div>
            <span v-if="selected" class="status-pill">证据 {{ selected.summary.evidenceCount }} · 冲突 {{ selected.summary.conflictCount }}</span>
          </div>

          <div v-if="detailError" class="alert-panel" role="alert">{{ detailError }}</div>
          <p v-else-if="detailLoading && !selected" class="empty-copy">正在加载比赛详情...</p>
          <p v-else-if="!selected" class="empty-copy">当前未选中比赛。</p>

          <template v-else>
            <section class="result-contrast-panel" tabindex="0" aria-label="胜负强对比" data-test="match-result-contrast">
              <article class="result-verdict" :class="`result-verdict--${resultVerdict.tone}`">
                <p class="eyebrow">胜负强对比</p>
                <h3>{{ resultVerdict.title }}</h3>
                <p>{{ resultVerdict.body }}</p>
                <div class="result-verdict__chips" aria-label="比赛结果摘要">
                  <span>{{ selectedScoreboard.scoreDisplay || '比分待同步' }}</span>
                  <span>{{ selectedScoreboard.resultText || '赛果待同步' }}</span>
                  <span>证据 {{ selected.summary.evidenceCount }}</span>
                  <span>冲突 {{ selected.summary.conflictCount }}</span>
                </div>
              </article>

              <article class="duel-chart" aria-label="两队关键指标对比">
                <header>
                  <span>{{ duelTeams[0]?.teamName || '主队' }}</span>
                  <strong>两队强弱条</strong>
                  <span>{{ duelTeams[1]?.teamName || '客队' }}</span>
                </header>
                <div
                  v-for="row in duelMetricRows"
                  :key="row.code"
                  class="duel-row"
                  :class="metricLeaderClass(row)"
                  :aria-label="`${row.label}：${duelTeams[0]?.teamName || '主队'} ${metricValueText(
                    row.homeValue,
                    row.unit,
                  )}，${duelTeams[1]?.teamName || '客队'} ${metricValueText(row.awayValue, row.unit)}`"
                >
                  <div class="duel-row__top">
                    <span>{{ row.label }}</span>
                    <strong>
                      {{ metricValueText(row.homeValue, row.unit) }}
                      <b>vs</b>
                      {{ metricValueText(row.awayValue, row.unit) }}
                    </strong>
                  </div>
                  <div class="duel-row__bars" aria-hidden="true">
                    <span class="duel-row__bar duel-row__bar--home" :style="metricBarStyle(row.homeValue, row.max)"></span>
                    <span class="duel-row__bar duel-row__bar--away" :style="metricBarStyle(row.awayValue, row.max)"></span>
                  </div>
                  <small>{{ row.caption }}</small>
                </div>
              </article>

              <article class="evidence-meter-card" aria-label="比赛证据可读度">
                <p class="eyebrow">证据可读度</p>
                <strong>{{ matchEvidenceReadiness }}%</strong>
                <div class="match-evidence-ring-grid" aria-label="比赛证据资料环形图">
                  <CoverageDonut
                    v-for="ring in matchEvidenceRings"
                    :key="ring.label"
                    :label="ring.label"
                    :value="ring.value"
                    unit="%"
                    :tone="ring.tone"
                    size="compact"
                    :caption="ring.caption"
                  />
                </div>
                <MetricBar
                  label="证据可读度"
                  :value="matchEvidenceReadiness"
                  :max="100"
                  unit="%"
                  :tone="scoreTone(matchEvidenceReadiness)"
                  caption="公开证据、比分、冲突和来源时效的综合展示"
                />
              </article>
            </section>

            <ScoreboardCard
              class="detail-scoreboard-card"
              :home-team="selected.summary.homeTeam"
              :away-team="selected.summary.awayTeam"
              :scoreboard="selectedScoreboard"
              :match-name="selected.summary.matchName"
              :meta="matchMeta(selected.summary)"
              :status="statusLabel(selected.summary.status || selected.summary.resultStatus)"
              :integrity-score="selected.summary.evidenceCount ? matchEvidenceReadiness : null"
              :risk-count="selected.summary.conflictCount"
              :evidence-count="selected.summary.evidenceCount"
            />

            <section class="summary-grid" aria-label="比赛摘要">
              <div><span>赛事</span><strong>{{ selected.summary.competition || '世界杯' }}</strong></div>
              <div><span>阶段</span><strong>{{ selected.summary.stage || '待定阶段' }}</strong></div>
              <div><span>场地</span><strong>{{ selected.summary.venue || '待同步' }}</strong></div>
              <div><span>开球</span><strong>{{ formatDateTime(selected.summary.kickoffTime) }}</strong></div>
            </section>

            <section class="match-visual-grid" aria-label="比分与事件图表" tabindex="0">
              <article class="info-card info-card--wide">
                <p class="eyebrow">时间线</p>
                <h3>进球 / 关键事件时间线</h3>
                <ol v-if="selected.events.length" class="event-timeline" tabindex="0">
                  <li v-for="event in selected.events" :key="event.id">
                    <span>{{ event.eventMinute ?? '-' }}'</span>
                    <strong>{{ enumLabel('eventType', event.eventType) }}</strong>
                    <small>{{ event.teamName || '球队待定' }} · {{ event.playerName || '球员待定' }}</small>
                  </li>
                </ol>
                <p v-else class="empty-copy">暂无公开事件；进球、红黄牌、换人与 VAR 等节点缺口。</p>
              </article>

              <article class="info-card">
                <p class="eyebrow">对比</p>
                <h3>进球与失球对比</h3>
                <div v-for="row in teamComparisonRows" :key="row.id" class="team-bars">
                  <strong>{{ row.teamName }}</strong>
                  <MetricBar label="进球" :value="row.goalsFor" :max="row.maxGoals" tone="success" />
                  <MetricBar label="失球" :value="row.goalsAgainst" :max="row.maxGoals" tone="danger" />
                  <small>
                    首球 {{ row.firstGoalMinute ?? '待同步' }}' · 进球分钟
                    {{ row.scoringMinutes.length ? row.scoringMinutes.join(' / ') : '待同步' }}
                  </small>
                </div>
                <p v-if="!teamComparisonRows.length" class="empty-copy">暂无球队统计；等待正式入库后展示柱状对比。</p>
              </article>

              <article class="info-card">
                <p class="eyebrow">解读</p>
                <h3>结果数据</h3>
                <p>
                  当前比分：{{ selectedScoreboard.scoreDisplay }}，{{ selectedScoreboard.resultText }}。
                  胜负、比分、证据数和冲突数已展示；冲突较多时公开结论显示未定稿。
                </p>
                <MetricBar
                  label="证据可读度"
                  :value="matchEvidenceReadiness"
                  :max="100"
                  unit="%"
                  :tone="scoreTone(matchEvidenceReadiness)"
                  caption="按公开证据数量临时估算，后端资料准备度分待接入"
                />
              </article>
            </section>

            <section class="evidence-quality-panel" aria-label="证据质量面板" tabindex="0">
              <div class="panel-heading">
                <div>
                  <p class="eyebrow">证据质量</p>
                  <h3>证据质量面板</h3>
                </div>
                <span class="status-pill">{{ evidenceQualitySummary.total }} 条证据 · {{ evidenceQualitySummary.reviewCount }} 个质量风险点</span>
              </div>

              <div class="evidence-quality-layout" tabindex="0">
                <article class="quality-insight">
                  <strong>证据摘要</strong>
                  <p>{{ evidenceReviewInsight }}</p>
                  <div class="quality-numbers" aria-label="证据质量摘要">
                    <span><b>{{ evidenceQualitySummary.highCount }}</b> 高可信</span>
                    <span><b>{{ evidenceQualitySummary.staleCount }}</b> 时效风险</span>
                    <span><b>{{ evidenceQualitySummary.conflictCount }}</b> 冲突</span>
                  </div>
                </article>

                <article class="quality-rings" tabindex="0" aria-label="证据质量结构环形图">
                  <CoverageDonut
                    v-for="ring in evidenceQualityRings"
                    :key="ring.label"
                    :label="ring.label"
                    :value="ring.value"
                    :max="ring.max"
                    unit="条"
                    :tone="ring.tone"
                    size="compact"
                    :caption="ring.caption"
                  />
                </article>

                <article class="quality-bars" aria-label="可信度分布" tabindex="0">
                  <MetricBar
                    v-for="row in evidenceQualityRows"
                    :key="row.code"
                    :label="row.label"
                    :value="row.count"
                    :max="row.max"
                    unit="条"
                    :tone="row.tone"
                    :caption="row.caption"
                  />
                </article>

                <article class="quality-supports" aria-label="证据支撑结论" tabindex="0">
                  <strong>证据支撑了什么结论</strong>
                  <div v-for="row in evidenceSupportRows" :key="row.label" class="support-row">
                    <span>{{ row.label }}</span>
                    <b>{{ row.count }} 条</b>
                    <small>{{ row.sources }}</small>
                  </div>
                  <p v-if="!evidenceSupportRows.length" class="empty-copy">暂无可归类证据；比分/阵容/场地/市场信号存在缺口。</p>
                </article>

                <article class="quality-checklist" aria-label="证据质量清单" tabindex="0">
                  <strong>证据质量状态</strong>
                  <ul>
                    <li>比分、胜负、阵容、伤停、场地、裁判、天气和市场信号。</li>
                    <li>低可信或未评分证据不单独支撑结论，第二来源覆盖状态同步展示。</li>
                    <li>存在冲突时，正式来源处理状态决定结论展示。</li>
                  </ul>
                </article>
              </div>
            </section>

            <section v-if="selected.externalFactors" class="info-card">
              <p class="eyebrow">外部因素</p>
              <h3>外部因素</h3>
              <p>{{ readablePublicText(selected.externalFactors) }}</p>
            </section>

            <section class="card-grid" tabindex="0" aria-label="比赛证据卡片">
              <article class="info-card" tabindex="0">
                <p class="eyebrow">阵容</p>
                <h3>阵容 / 首发</h3>
                <div v-for="lineup in selected.lineups" :key="lineup.id" class="stack-item">
                  <strong>{{ lineup.playerName || '球员待定' }} <small>{{ lineup.teamName || '' }}</small></strong>
                  <span>{{ positionLabel(lineup.position) }} · {{ lineupRoleLabel(lineup.role) }} · {{ lineup.starter ? '首发' : '替补' }}</span>
                </div>
              </article>

              <article class="info-card" tabindex="0">
                <p class="eyebrow">事件</p>
                <h3>比赛事件</h3>
                <div v-for="event in selected.events" :key="event.id" class="stack-item">
                  <strong>{{ enumLabel('eventType', event.eventType) }} <small>{{ event.eventMinute ?? '-' }}'</small></strong>
                  <span>{{ event.teamName || '球队待定' }} · {{ event.playerName || '球员待定' }}</span>
                </div>
              </article>

              <article class="info-card" tabindex="0">
                <p class="eyebrow">球队统计</p>
                <h3>球队统计 / 进球时间点</h3>
                <div v-for="stat in selected.teamStats" :key="stat.id" class="stack-item">
                  <strong>{{ stat.teamName || '球队待定' }} <small>{{ enumLabel('statsType', stat.statsType) }}</small></strong>
                  <span>进 {{ stat.goalsFor ?? '-' }} / 失 {{ stat.goalsAgainst ?? '-' }} · {{ stat.scoringMinutes || '进球分钟待同步' }}</span>
                </div>
              </article>

              <article class="info-card" tabindex="0">
                <p class="eyebrow">球员统计</p>
                <h3>球员统计</h3>
                <div v-for="stat in selected.playerStats" :key="stat.id" class="stack-item">
                  <strong>{{ stat.playerName || '球员待定' }} <small>{{ stat.teamName || '' }}</small></strong>
                  <span>{{ stat.minutesPlayed ?? '-' }} 分钟 · 进球 {{ stat.goals ?? 0 }} · 助攻 {{ stat.assists ?? 0 }}</span>
                </div>
              </article>

              <article class="info-card" tabindex="0">
                <p class="eyebrow">证据链</p>
                <h3>证据链</h3>
                <div v-for="item in selected.evidence" :key="item.id" class="stack-item">
                  <strong>{{ item.sourceName }} <small>{{ sourceTypeLabel(item.sourceType) }}</small></strong>
                  <div class="chip-row">
                    <span class="mini-chip" :class="`mini-chip--${evidenceQualityTone(item.qualityLevel)}`">
                      {{ enumLabel('evidenceQualityLevel', item.qualityLevel) }}
                    </span>
                    <span class="mini-chip" :class="`mini-chip--${freshnessTone(item.freshnessStatus)}`">
                      {{ enumLabel('evidenceFreshnessStatus', item.freshnessStatus) }}
                    </span>
                    <span class="mini-chip mini-chip--accent">{{ item.supportsConclusion || '背景证据' }}</span>
                  </div>
                  <span>{{ readablePublicText(item.summary, '暂无摘要') }}</span>
                  <small>可信度 {{ scoreText(item.reliabilityScore) }} · {{ formatDateTime(item.evidenceTime) }}</small>
                  <em>证据动作：{{ publicEvidenceAction(item.suggestedAction) }}</em>
                </div>
                <p v-if="!selected.evidence.length" class="empty-copy">暂无公开证据；官方比分、阵容、场地和伤停来源缺口较多。</p>
              </article>

              <article class="info-card" tabindex="0">
                <p class="eyebrow">冲突</p>
                <h3>数据冲突</h3>
                <div v-for="conflict in selected.conflicts" :key="conflict.id" class="stack-item">
                  <strong>{{ enumLabel('conflictType', conflict.conflictType) }} <small>{{ fieldNameLabel(conflict.fieldName) }}</small></strong>
                  <span>状态：{{ enumLabel('resolutionStatus', conflict.resolutionStatus) }}</span>
                  <small>来源复验：对照 {{ fieldNameLabel(conflict.fieldName) }} 的正式来源；冲突处理后入库/修正。</small>
                </div>
                <p v-if="!selected.conflicts.length" class="empty-copy">暂无公开冲突状态。</p>
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
.evidence-page__content { display: grid; gap: 18px; min-width: 0; }
.evidence-hero, .stat-card, .match-catalog-structure, .side-panel, .detail-panel, .info-card, .summary-grid div, .alert-panel, .evidence-quality-panel, .result-contrast-panel, .score-overview-panel {
  background: var(--wc-glass); border: 1px solid var(--wc-border); border-radius: var(--wc-radius-lg); color: var(--wc-text);
}
.evidence-hero { align-items: center; display: grid; gap: 18px; grid-template-columns: minmax(0, 1fr) auto; padding: clamp(20px, 4vw, 38px); }
.evidence-hero h1 { font-family: var(--wc-font-display); font-size: clamp(34px, 6vw, 68px); line-height: 1; margin: 0 0 12px; }
.evidence-hero p:not(.eyebrow), .empty-copy, .stack-item span, .summary-grid span, small, .quality-insight p, .quality-checklist li, .empty-filter-state p, .score-overview-copy p { color: var(--wc-text-muted); }
.eyebrow { color: var(--wc-warning); font-family: var(--wc-font-mono); font-size: 12px; font-weight: 800; letter-spacing: .08em; margin: 0 0 8px; text-transform: uppercase; }
.action-button { background: var(--wc-accent); border: 0; border-radius: 999px; color: var(--wc-on-accent); cursor: pointer; font-weight: 800; min-height: 44px; padding: 0 16px; }
.stat-grid, .summary-grid { display: grid; gap: 14px; grid-template-columns: repeat(4, minmax(0, 1fr)); }
.stat-card, .match-catalog-structure, .side-panel, .detail-panel, .info-card, .alert-panel, .evidence-quality-panel, .result-contrast-panel, .score-overview-panel { display: grid; gap: 12px; min-width: 0; padding: 18px; }
.stat-card strong { color: var(--wc-primary); font-family: var(--wc-font-mono); font-size: 36px; }
.match-catalog-structure {
  max-height: min(78dvh, 680px);
  overflow: auto;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}
.match-catalog-rings {
  display: grid;
  gap: 13px;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  max-height: 520px;
  min-width: 0;
  overflow: auto;
  padding-right: 2px;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}
.score-overview-panel {
  align-items: stretch;
  background:
    radial-gradient(circle at 0% 10%, rgba(34, 197, 94, .12), transparent 28%),
    radial-gradient(circle at 100% 0%, rgba(245, 158, 11, .11), transparent 30%),
    var(--wc-glass);
  grid-template-columns: minmax(0, .94fr) minmax(0, 1.06fr);
  max-height: min(82dvh, 720px);
  overflow: auto;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}
.score-overview-copy,
.score-overview-bars {
  align-content: start;
  background: rgba(15, 23, 42, .52);
  border: 1px solid rgba(147, 197, 253, .16);
  border-radius: var(--wc-radius-md);
  display: grid;
  gap: 12px;
  min-width: 0;
  padding: 16px;
}
.score-overview-bars {
  max-height: 520px;
  overflow: auto;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}
.score-overview-copy h2 {
  font-family: var(--wc-font-display);
  font-size: clamp(28px, 4vw, 44px);
  line-height: 1;
  margin: 0;
}
.score-overview-copy strong {
  color: var(--wc-primary);
  line-height: 1.55;
}
.quick-filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.quick-filter-row button {
  background: rgba(147, 197, 253, .1);
  border: 1px solid rgba(147, 197, 253, .24);
  border-radius: 999px;
  color: var(--wc-text);
  cursor: pointer;
  font-weight: 800;
  min-height: 44px;
  padding: 0 13px;
}
.quick-filter-row button:focus-visible {
  box-shadow: var(--wc-focus-ring);
  outline: none;
}
.score-coverage {
  background: rgba(2, 6, 23, .4);
  border: 1px solid rgba(148, 163, 184, .14);
  border-radius: var(--wc-radius-sm);
  display: grid;
  gap: 5px;
  padding: 12px;
}
.score-coverage span {
  color: var(--wc-text-muted);
  font-size: 12px;
  font-weight: 800;
}
.score-coverage strong {
  color: var(--wc-primary);
  font-family: var(--wc-font-mono);
  font-size: 34px;
}
.evidence-grid { align-items: start; display: grid; gap: 16px; grid-template-columns: minmax(0, 340px) minmax(0, 1fr); min-width: 0; }
.side-panel {
  max-height: min(84dvh, 820px);
  overflow: auto;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}
.panel-heading { align-items: center; display: flex; gap: 12px; justify-content: space-between; }
.panel-heading h2, .info-card h3 { margin: 0; }
.match-filter-panel {
  background: rgba(15, 23, 42, .46);
  border: 1px solid rgba(147, 197, 253, .16);
  border-radius: var(--wc-radius-md);
  display: grid;
  gap: 10px;
  min-width: 0;
  padding: 12px;
}
.filter-row {
  display: grid;
  gap: 8px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}
.filter-field {
  display: grid;
  gap: 6px;
  min-width: 0;
}
.filter-field span {
  color: var(--wc-text-muted);
  font-size: 12px;
  font-weight: 800;
}
.filter-field input,
.filter-field select {
  background: rgba(2, 6, 23, .64);
  border: 1px solid rgba(148, 163, 184, .22);
  border-radius: 14px;
  color: var(--wc-text);
  font: inherit;
  min-height: 44px;
  min-width: 0;
  padding: 0 12px;
  width: 100%;
}
.filter-field input::placeholder {
  color: var(--wc-text-subtle);
}
.filter-field input:focus,
.filter-field select:focus,
.ghost-button:focus-visible,
.match-pager button:focus-visible,
.match-catalog-structure:focus-visible,
.match-catalog-rings:focus-visible,
.score-overview-panel:focus-visible,
.score-overview-bars:focus-visible,
.side-panel:focus-visible,
.match-list-scroll:focus-visible,
.detail-panel:focus-visible,
.result-contrast-panel:focus-visible,
.match-visual-grid:focus-visible,
.event-timeline:focus-visible,
.evidence-quality-panel:focus-visible,
.evidence-quality-layout:focus-visible,
.card-grid:focus-visible,
.quality-rings:focus-visible,
.quality-bars:focus-visible,
.quality-supports:focus-visible,
.quality-checklist:focus-visible,
.card-grid > .info-card:focus-visible {
  box-shadow: var(--wc-focus-ring);
  outline: none;
}
.filter-summary {
  align-items: center;
  display: flex;
  gap: 8px;
  justify-content: space-between;
  min-width: 0;
}
.filter-summary span {
  color: var(--wc-primary);
  font-family: var(--wc-font-mono);
  font-size: 12px;
  font-weight: 900;
}
.ghost-button {
  background: rgba(147, 197, 253, .1);
  border: 1px solid rgba(147, 197, 253, .24);
  border-radius: 999px;
  color: var(--wc-text);
  cursor: pointer;
  font-weight: 800;
  min-height: 36px;
  padding: 0 12px;
}
.match-list-scroll {
  display: grid;
  gap: 10px;
  max-height: 720px;
  min-width: 0;
  overflow: auto;
  padding-right: 2px;
}
.list-card, .stack-item { background: rgba(15, 23, 42, .5); border: 1px solid rgba(147, 197, 253, .18); border-radius: var(--wc-radius-md); color: var(--wc-text); display: grid; gap: 6px; min-width: 0; padding: 14px; text-align: left; }
.list-card { cursor: pointer; min-height: 44px; transition: border-color 180ms ease, transform 180ms ease; }
.list-card--active { border-color: rgba(217, 119, 6, .56); }
.empty-filter-state {
  background: rgba(15, 23, 42, .46);
  border: 1px dashed rgba(147, 197, 253, .24);
  border-radius: var(--wc-radius-md);
  display: grid;
  gap: 8px;
  padding: 14px;
}
.match-pager {
  align-items: center;
  display: grid;
  gap: 8px;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
}
.match-pager button {
  background: rgba(147, 197, 253, .1);
  border: 1px solid rgba(147, 197, 253, .22);
  border-radius: 999px;
  color: var(--wc-text);
  cursor: pointer;
  font-weight: 800;
  min-height: 44px;
  padding: 0 12px;
}
.match-pager button:disabled {
  cursor: not-allowed;
  opacity: .45;
}
.match-pager span {
  color: var(--wc-text-muted);
  font-family: var(--wc-font-mono);
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}
.status-pill { background: rgba(147, 197, 253, .12); border-radius: 999px; color: var(--wc-primary); font-family: var(--wc-font-mono); font-size: 12px; font-weight: 800; padding: 6px 9px; }
.summary-grid { grid-template-columns: repeat(4, minmax(0, 1fr)); }
.summary-grid div { display: grid; gap: 5px; min-width: 0; padding: 14px; }
.detail-panel {
  max-height: min(88dvh, 860px);
  overflow: auto;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}
.match-visual-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: minmax(0, 1.25fr) minmax(0, 1fr);
  max-height: min(82dvh, 720px);
  min-width: 0;
  overflow: auto;
  padding-right: 2px;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}
.info-card--wide { grid-row: span 2; }
.event-timeline { display: grid; gap: 9px; list-style: none; margin: 0; max-height: 520px; overflow: auto; padding: 0; scrollbar-color: rgba(147, 197, 253, .38) transparent; }
.event-timeline li { align-items: center; background: rgba(15, 23, 42, .48); border: 1px solid rgba(148, 163, 184, .14); border-radius: var(--wc-radius-sm); display: grid; gap: 8px; grid-template-columns: 54px minmax(0, 1fr) minmax(0, 1.2fr); min-width: 0; padding: 11px; }
.event-timeline span { color: var(--wc-warning); font-family: var(--wc-font-mono); font-weight: 900; }
.team-bars { background: rgba(15, 23, 42, .44); border: 1px solid rgba(148, 163, 184, .14); border-radius: var(--wc-radius-sm); display: grid; gap: 10px; min-width: 0; padding: 12px; }
.team-bars strong { font-size: 16px; }
.result-contrast-panel {
  align-items: stretch;
  background:
    radial-gradient(circle at 10% 0%, rgba(34, 197, 94, .13), transparent 34%),
    radial-gradient(circle at 100% 0%, rgba(96, 165, 250, .12), transparent 30%),
    var(--wc-glass);
  grid-template-columns: minmax(0, .92fr) minmax(0, 1.24fr) minmax(0, .84fr);
  max-height: min(82dvh, 720px);
  overflow: auto;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}
.result-verdict,
.duel-chart,
.evidence-meter-card {
  align-content: start;
  background: rgba(15, 23, 42, .54);
  border: 1px solid rgba(147, 197, 253, .16);
  border-radius: var(--wc-radius-md);
  display: grid;
  gap: 12px;
  min-width: 0;
  padding: 16px;
}
.duel-chart {
  grid-column: 2 / -1;
}
.evidence-meter-card {
  grid-column: 1 / -1;
}
.result-verdict h3 {
  color: var(--wc-primary);
  font-family: var(--wc-font-display);
  font-size: clamp(30px, 4vw, 46px);
  line-height: 1;
  margin: 0;
}
.result-verdict p:not(.eyebrow),
.duel-row small {
  color: var(--wc-text-muted);
  line-height: 1.62;
}
.result-verdict--success {
  border-color: rgba(34, 197, 94, .36);
}
.result-verdict--success h3 {
  color: var(--wc-success);
}
.result-verdict--warning {
  border-color: rgba(245, 158, 11, .38);
}
.result-verdict--warning h3 {
  color: var(--wc-warning);
}
.result-verdict--info {
  border-color: rgba(96, 165, 250, .32);
}
.result-verdict__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.result-verdict__chips span {
  background: rgba(2, 6, 23, .48);
  border: 1px solid rgba(148, 163, 184, .18);
  border-radius: 999px;
  color: var(--wc-text);
  font-family: var(--wc-font-mono);
  font-size: 12px;
  font-weight: 800;
  padding: 7px 9px;
}
.duel-chart header {
  align-items: center;
  display: grid;
  gap: 10px;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
}
.duel-chart header span {
  color: var(--wc-text);
  font-weight: 900;
  min-width: 0;
}
.duel-chart header span:last-child {
  text-align: right;
}
.duel-chart header strong {
  background: rgba(147, 197, 253, .12);
  border: 1px solid rgba(147, 197, 253, .18);
  border-radius: 999px;
  color: var(--wc-primary);
  font-family: var(--wc-font-mono);
  font-size: 12px;
  padding: 6px 9px;
}
.duel-row {
  background: rgba(2, 6, 23, .34);
  border: 1px solid rgba(148, 163, 184, .12);
  border-radius: var(--wc-radius-sm);
  display: grid;
  gap: 8px;
  min-width: 0;
  padding: 11px;
}
.duel-row__top {
  align-items: baseline;
  display: flex;
  gap: 10px;
  justify-content: space-between;
}
.duel-row__top span {
  color: var(--wc-text-muted);
  font-size: 12px;
  font-weight: 800;
}
.duel-row__top strong {
  color: var(--wc-text);
  font-family: var(--wc-font-mono);
}
.duel-row__top b {
  color: var(--wc-text-subtle);
  font-size: 11px;
  margin: 0 4px;
}
.duel-row__bars {
  display: grid;
  gap: 6px;
}
.duel-row__bar {
  border-radius: 999px;
  display: block;
  height: 10px;
}
.duel-row__bar--home {
  background: linear-gradient(90deg, var(--wc-success), #bbf7d0);
  justify-self: start;
}
.duel-row__bar--away {
  background: linear-gradient(90deg, #93c5fd, var(--wc-primary));
  justify-self: end;
}
.duel-row--home {
  border-color: rgba(34, 197, 94, .32);
}
.duel-row--away {
  border-color: rgba(96, 165, 250, .34);
}
.duel-row--balanced {
  border-color: rgba(245, 158, 11, .26);
}
.match-evidence-ring-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(178px, 1fr));
  min-width: 0;
}
.detail-panel, .evidence-quality-panel { align-content: start; }
.evidence-quality-panel {
  max-height: min(84dvh, 760px);
  overflow: auto;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}
.evidence-quality-layout {
  align-items: start;
  display: grid;
  gap: 12px;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 1fr);
  max-height: min(78dvh, 700px);
  min-width: 0;
  overflow: auto;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}
.quality-insight, .quality-bars, .quality-supports, .quality-checklist, .quality-rings {
  align-content: start; background: rgba(15, 23, 42, .44); border: 1px solid rgba(148, 163, 184, .14); border-radius: var(--wc-radius-md); display: grid; gap: 10px; min-width: 0; padding: 14px;
}
.quality-rings {
  grid-column: 1 / -1;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  max-height: 420px;
  overflow: auto;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}
.quality-bars,
.quality-supports,
.quality-checklist {
  max-height: 360px;
  overflow: auto;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}
.quality-insight strong, .quality-supports strong, .quality-checklist strong { color: var(--wc-text); }
.quality-numbers { display: grid; gap: 8px; grid-template-columns: repeat(3, minmax(0, 1fr)); }
.quality-numbers span { background: rgba(2, 6, 23, .42); border-radius: var(--wc-radius-sm); color: var(--wc-text-muted); display: grid; gap: 4px; padding: 10px; }
.quality-numbers b { color: var(--wc-primary); font-family: var(--wc-font-mono); font-size: 22px; }
.support-row { align-items: center; background: rgba(2, 6, 23, .36); border: 1px solid rgba(148, 163, 184, .12); border-radius: var(--wc-radius-sm); display: grid; gap: 8px; grid-template-columns: minmax(0, 1fr) auto; min-width: 0; padding: 10px; }
.support-row span { color: var(--wc-text); font-weight: 800; }
.support-row b { color: var(--wc-warning); font-family: var(--wc-font-mono); }
.support-row small { grid-column: 1 / -1; }
.quality-checklist ul { display: grid; gap: 8px; margin: 0; padding-left: 18px; }
.chip-row { display: flex; flex-wrap: wrap; gap: 6px; }
.stack-item .mini-chip { border: 1px solid rgba(148, 163, 184, .24); border-radius: 999px; color: var(--wc-text); font-size: 12px; font-weight: 800; padding: 4px 8px; }
.stack-item .mini-chip--success { background: rgba(34, 197, 94, .13); border-color: rgba(34, 197, 94, .36); color: #bbf7d0; }
.stack-item .mini-chip--warning { background: rgba(245, 158, 11, .13); border-color: rgba(245, 158, 11, .36); color: #fde68a; }
.stack-item .mini-chip--danger { background: rgba(239, 68, 68, .13); border-color: rgba(239, 68, 68, .36); color: #fecaca; }
.stack-item .mini-chip--info { background: rgba(96, 165, 250, .12); border-color: rgba(96, 165, 250, .32); color: #bfdbfe; }
.stack-item .mini-chip--accent { background: rgba(217, 119, 6, .15); border-color: rgba(217, 119, 6, .38); color: #fed7aa; }
.stack-item em { color: var(--wc-warning); font-style: normal; font-size: 12px; }
.card-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  max-height: min(84dvh, 820px);
  min-width: 0;
  overflow: auto;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}
.card-grid > .info-card {
  max-height: 560px;
  overflow: auto;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}
@media (max-width: 1024px) {
  .evidence-hero, .evidence-grid, .summary-grid, .match-visual-grid, .evidence-quality-layout, .result-contrast-panel, .score-overview-panel { grid-template-columns: 1fr; }
  .stat-grid, .card-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .info-card--wide { grid-row: auto; }
  .duel-chart, .evidence-meter-card { grid-column: auto; }
}
@media (max-width: 640px) {
  .evidence-hero, .evidence-grid, .match-visual-grid, .evidence-quality-layout, .card-grid, .result-contrast-panel { grid-template-columns: 1fr; }
  .stat-grid, .match-catalog-rings, .score-overview-bars, .summary-grid, .quality-numbers, .quality-rings, .match-evidence-ring-grid, .filter-row, .match-pager {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .score-overview-panel {
    grid-template-columns: 1fr;
  }
  .evidence-hero, .stat-card, .match-catalog-structure, .side-panel, .detail-panel, .info-card, .summary-grid div, .alert-panel, .evidence-quality-panel, .result-contrast-panel, .score-overview-panel {
    border-radius: var(--wc-radius-md);
    gap: 10px;
    padding: 12px;
  }
  .evidence-hero h1 {
    font-size: clamp(28px, 12vw, 42px);
    margin-bottom: 8px;
  }
  .evidence-hero p:not(.eyebrow) {
    display: none;
    line-height: 1.55;
  }
  .stat-grid {
    gap: 10px;
  }
  .stat-card strong {
    font-size: 28px;
  }
  .match-catalog-rings,
  .quality-rings,
  .match-evidence-ring-grid {
    gap: 10px;
  }
  .match-catalog-rings :deep(.coverage-donut),
  .quality-rings :deep(.coverage-donut),
  .match-evidence-ring-grid :deep(.coverage-donut),
  .score-coverage :deep(.coverage-donut) {
    align-content: start;
    background: rgba(15, 23, 42, .42);
    border: 1px solid rgba(147, 197, 253, .14);
    border-radius: var(--wc-radius-md);
    gap: 6px;
    grid-template-columns: 1fr !important;
    justify-items: center;
    min-height: 96px;
    padding: 9px;
    text-align: center;
  }
  .match-catalog-rings :deep(.coverage-donut__ring),
  .quality-rings :deep(.coverage-donut__ring),
  .match-evidence-ring-grid :deep(.coverage-donut__ring),
  .score-coverage :deep(.coverage-donut__ring) {
    width: 66px;
  }
  .match-catalog-rings :deep(.coverage-donut__ring span),
  .quality-rings :deep(.coverage-donut__ring span),
  .match-evidence-ring-grid :deep(.coverage-donut__ring span),
  .score-coverage :deep(.coverage-donut__ring span) {
    font-size: 20px;
  }
  .match-catalog-rings :deep(.coverage-donut__copy),
  .quality-rings :deep(.coverage-donut__copy),
  .match-evidence-ring-grid :deep(.coverage-donut__copy),
  .score-coverage :deep(.coverage-donut__copy) {
    gap: 3px;
  }
  .match-catalog-rings :deep(.coverage-donut__copy strong),
  .quality-rings :deep(.coverage-donut__copy strong),
  .match-evidence-ring-grid :deep(.coverage-donut__copy strong),
  .score-coverage :deep(.coverage-donut__copy strong) {
    font-size: 12px;
    line-height: 1.2;
  }
  .match-catalog-rings :deep(.coverage-donut__copy small),
  .quality-rings :deep(.coverage-donut__copy small),
  .match-evidence-ring-grid :deep(.coverage-donut__copy small),
  .score-coverage :deep(.coverage-donut__copy small) {
    display: none;
    font-size: 11px;
    line-height: 1.25;
  }
  .score-overview-copy,
  .score-overview-bars,
  .result-verdict,
  .duel-chart,
  .evidence-meter-card,
  .quality-insight,
  .quality-bars,
  .quality-supports,
  .quality-checklist,
  .quality-rings {
    border-radius: var(--wc-radius-md);
    gap: 10px;
    padding: 10px;
  }
  .score-overview-copy h2,
  .result-verdict h3 {
    font-size: 22px;
  }
  .score-overview-copy p:not(.eyebrow) {
    display: none;
  }
  .score-overview-copy {
    grid-template-columns: 1fr;
  }
  .quick-filter-row {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .quick-filter-row button,
  .action-button,
  .ghost-button,
  .match-pager button {
    min-height: 44px;
  }
  .score-overview-bars :deep(.metric-bar small),
  .quality-bars :deep(.metric-bar small),
  .match-visual-grid :deep(.metric-bar small),
  .evidence-meter-card :deep(.metric-bar small) {
    display: none;
  }
  .score-coverage {
    grid-column: 1 / -1;
    padding: 0;
  }
  .panel-heading,
  .filter-summary {
    align-items: stretch;
    flex-direction: column;
  }
  .side-panel {
    max-height: none;
    overflow: visible;
    scrollbar-gutter: auto;
  }
  .match-catalog-structure {
    max-height: none;
    overflow: visible;
    scrollbar-gutter: auto;
  }
  .match-catalog-rings {
    max-height: min(46dvh, 360px);
    overflow: auto;
    scrollbar-color: rgba(147, 197, 253, .38) transparent;
  }
  .score-overview-panel {
    max-height: none;
    overflow: visible;
    scrollbar-gutter: auto;
  }
  .score-overview-bars {
    max-height: min(42dvh, 340px);
    overflow: auto;
    scrollbar-color: rgba(147, 197, 253, .38) transparent;
  }
  .match-list-scroll {
    max-height: min(48dvh, 420px);
    overflow: auto;
    padding-right: 0;
    scrollbar-gutter: auto;
    scrollbar-width: none;
  }
  .match-list-scroll::-webkit-scrollbar {
    display: none;
  }
  .list-card,
  .stack-item {
    border-radius: 14px;
    gap: 5px;
    padding: 10px;
  }
  .list-card {
    align-content: start;
    min-height: 252px;
  }
  .list-card :deep(.scoreboard-card__main),
  .detail-scoreboard-card:deep(.scoreboard-card__main) {
    grid-template-columns: minmax(0, 1fr) 82px minmax(0, 1fr) !important;
  }
  .list-card :deep(.scoreboard-card__team),
  .detail-scoreboard-card:deep(.scoreboard-card__team) {
    border: 0;
    padding: 0;
  }
  .list-card :deep(.scoreboard-card__team .flag-team),
  .detail-scoreboard-card:deep(.scoreboard-card__team .flag-team) {
    align-items: center;
    flex-direction: column;
    gap: 5px;
  }
  .list-card :deep(.scoreboard-card__team--away .flag-team),
  .detail-scoreboard-card:deep(.scoreboard-card__team--away .flag-team) {
    align-items: center;
    flex-direction: column;
  }
  .list-card :deep(.scoreboard-card__team--away),
  .detail-scoreboard-card:deep(.scoreboard-card__team--away) {
    justify-items: center;
    text-align: center;
  }
  .list-card :deep(.scoreboard-card__team .flag-team__copy),
  .detail-scoreboard-card:deep(.scoreboard-card__team .flag-team__copy) {
    justify-items: center;
    text-align: center;
    width: 100%;
  }
  .list-card :deep(.scoreboard-card__team .flag-team__copy strong),
  .detail-scoreboard-card:deep(.scoreboard-card__team .flag-team__copy strong) {
    display: -webkit-box;
    font-size: 12px;
    overflow: hidden;
    overflow-wrap: anywhere;
    text-overflow: ellipsis;
    white-space: normal;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
  }
  .list-card :deep(.scoreboard-card__score),
  .detail-scoreboard-card:deep(.scoreboard-card__score) {
    min-width: 0;
    padding: 7px 5px;
    width: 82px;
  }
  .list-card :deep(.scoreboard-card__score strong),
  .detail-scoreboard-card:deep(.scoreboard-card__score strong) {
    font-size: 18px;
  }
  .detail-scoreboard-card:deep(.scoreboard-card__signals) {
    grid-template-columns: 1fr auto auto;
  }
  .detail-scoreboard-card:deep(.scoreboard-card__signals .metric-bar small) {
    display: none;
  }
  .summary-grid {
    gap: 10px;
  }
  .summary-grid div {
    min-height: 92px;
  }
  .result-contrast-panel {
    max-height: none;
    overflow: visible;
    scrollbar-gutter: auto;
  }
  .detail-panel {
    max-height: none;
    overflow: visible;
    scrollbar-gutter: auto;
  }
  .match-visual-grid {
    max-height: none;
    overflow: visible;
    scrollbar-gutter: auto;
  }
  .event-timeline {
    max-height: min(34dvh, 260px);
    overflow: auto;
    scrollbar-color: rgba(147, 197, 253, .38) transparent;
  }
  .event-timeline li,
  .support-row,
  .duel-chart header {
    grid-template-columns: 1fr;
  }
  .duel-chart header span:last-child {
    text-align: left;
  }
  .duel-row {
    gap: 6px;
    padding: 9px;
  }
  .duel-row small {
    display: none;
  }
  .result-verdict p:not(.eyebrow),
  .match-visual-grid .info-card h3 + p:not(.empty-copy) {
    display: none;
  }
  .evidence-meter-card > strong {
    font-size: 42px;
  }
  .evidence-quality-panel {
    max-height: none;
    overflow: visible;
    scrollbar-gutter: auto;
  }
  .evidence-quality-layout {
    gap: 10px;
    max-height: none;
    overflow: visible;
    scrollbar-gutter: auto;
  }
  .quality-rings {
    max-height: min(42dvh, 360px);
    overflow: auto;
    scrollbar-color: rgba(147, 197, 253, .38) transparent;
  }
  .quality-checklist ul {
    display: none;
  }
  .quality-checklist {
    display: none;
  }
  .quality-supports {
    max-height: none;
    overflow: visible;
    scrollbar-gutter: auto;
  }
  .card-grid {
    max-height: none;
    overflow: visible;
    scrollbar-gutter: auto;
  }
  .card-grid > .info-card {
    max-height: none;
    overflow: visible;
    scrollbar-gutter: auto;
  }
}
@media (prefers-reduced-motion: reduce) { .list-card { transition: none; } }
</style>
