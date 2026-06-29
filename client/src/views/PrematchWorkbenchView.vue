<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import {
  getPublicPrematchWorkbenchMatch,
  listPublicPrematchWorkbenchMatches,
  type PublicIntegrityCheck,
  type PublicPrematchWorkbenchDetail,
  type PublicVisualMetric,
  type PublicWorkbenchMatchSummary,
  type PublicWorkbenchSentimentRisk,
} from '@/api/prematchWorkbench';
import FlagTeamName from '@/components/football/FlagTeamName.vue';
import CoverageDonut from '@/components/football/CoverageDonut.vue';
import MetricBar from '@/components/football/MetricBar.vue';
import ScoreboardCard from '@/components/football/ScoreboardCard.vue';
import { useAuthStore } from '@/stores/auth';
import {
  enumLabel,
  fieldNameLabel,
  lineupRoleLabel,
  marketLabel,
  matchStatusLabel,
  positionLabel,
  readablePublicText,
  sourceTypeLabel,
} from '@/utils/display-labels';
import { isPlaceholderTeamName, teamNameFromMatchName } from '@/utils/football-visuals';

const authStore = useAuthStore();
const loading = ref(false);
const detailLoading = ref(false);
const error = ref('');
const detailError = ref('');
const matches = ref<PublicWorkbenchMatchSummary[]>([]);
const selected = ref<PublicPrematchWorkbenchDetail | null>(null);
const selectedMatchId = ref<number | null>(null);
const matchSearchQuery = ref('');
const readinessFilter = ref('ALL');
const issueFilter = ref('ALL');
const matchPage = ref(1);
const MATCH_PAGE_SIZE = 10;


const stats = computed(() => ({
  matches: matches.value.length,
  avgScore: matches.value.length
    ? Math.round(matches.value.reduce((sum, match) => sum + match.integrityScore, 0) / matches.value.length)
    : 0,
  missing: matches.value.reduce((sum, match) => sum + match.missingCount, 0),
  conflicts: matches.value.reduce((sum, match) => sum + match.conflictCount, 0),
}));

const decisionSteps = computed(() => {
  const summary = selected.value?.summary;
  return [
    {
      label: '资料准备度',
      value: summary ? `${summary.integrityScore}%` : '-',
      meta: summary ? `缺失 ${summary.missingCount} · 过期 ${summary.staleCount} · 冲突 ${summary.conflictCount}` : '等待选择比赛',
    },
    {
      label: '阵容与状态',
      value: summary ? `${summary.teamProfileCount}/${summary.playerProfileCount}` : '-',
      meta: '球队画像 / 球员画像',
    },
    {
      label: '赔率市场',
      value: summary?.oddsMarketCount ?? 0,
      meta: '公开赔率快照',
    },
    {
      label: '舆情外因',
      value: summary?.sentimentFactorCount ?? 0,
      meta: '风险与外部变量',
    },
    {
      label: '分析结论',
      value: summary?.analysisReportCount ?? 0,
      meta: '已批准报告摘要',
    },
  ];
});

const currentReadinessBars = computed(() => {
  const summary = selected.value?.summary;
  if (!summary) {
    return [];
  }
  return [
    { label: '资料准备度', value: summary.integrityScore, max: 100, tone: scoreBarTone(summary.integrityScore), caption: '资料准备度分，低分代表资料缺口' },
    { label: '球队画像', value: summary.teamProfileCount, max: 2, tone: summary.teamProfileCount >= 2 ? 'success' : 'warning', caption: '至少主客队各一份' },
    { label: '球员画像', value: summary.playerProfileCount, max: 22, tone: summary.playerProfileCount >= 11 ? 'success' : 'warning', caption: '首发/核心球员覆盖' },
    { label: '赔率市场', value: summary.oddsMarketCount, max: Math.max(1, summary.oddsMarketCount, 6), tone: 'info', caption: '胜平负、让球、大小球等' },
    { label: '舆情外因', value: summary.sentimentFactorCount, max: Math.max(1, summary.sentimentFactorCount, 8), tone: 'accent', caption: '伤停、天气、裁判、舆论等' },
  ];
});
const currentReadinessRings = computed(() => currentReadinessBars.value.map((bar) => ({
  label: bar.label,
  value: bar.value,
  max: bar.max,
  unit: bar.label === '资料准备度' ? '%' : '',
  tone: bar.tone,
  caption: bar.caption,
})));

const prematchPanel = computed(() => {
  const summary = selected.value?.summary;
  if (!summary) {
    return null;
  }
  const readiness = readinessLevel(summary.integrityScore);
    const riskPressure = Math.min(
    100,
    summary.conflictCount * 24 + summary.missingCount * 10 + summary.staleCount * 8,
  );
  return {
    rings: [
      {
        label: '资料准备度',
        value: summary.integrityScore,
        tone: scoreBarTone(summary.integrityScore),
        caption: `${readiness.label} · 缺失 ${summary.missingCount} / 过期 ${summary.staleCount} / 冲突 ${summary.conflictCount}`,
      },
      {
        label: '风险压力',
        value: riskPressure,
        tone: riskPressure >= 65 ? 'danger' : riskPressure >= 30 ? 'warning' : 'success',
        caption: `冲突 ${summary.conflictCount} · 缺失 ${summary.missingCount} · 过期 ${summary.staleCount}`,
      },
    ],
    bars: [
      {
        label: '资料准备度',
        value: summary.integrityScore,
        max: 100,
        tone: scoreBarTone(summary.integrityScore),
        caption: `${readiness.label}：${readiness.explanation}`,
      },
      {
        label: '风险压力',
        value: riskPressure,
        max: 100,
        tone: riskPressure >= 65 ? 'danger' : riskPressure >= 30 ? 'warning' : 'success',
        caption: riskPriorityText(summary),
      },
      {
        label: '证据覆盖',
        value: selected.value?.evidence.length ?? 0,
        max: Math.max(1, selected.value?.evidence.length ?? 0, 8),
        tone: selected.value?.evidence.length ? 'info' : 'warning',
        caption: '公开证据覆盖比分、阵容和外部风险。',
      },
    ],
  };
});

const filteredMatches = computed(() => {
  const query = normalizeSearchText(matchSearchQuery.value);
  return matches.value.filter((match) => {
    const matchesSearch = !query || matchSearchHaystack(match).includes(query);
    const matchesReadiness =
      readinessFilter.value === 'ALL' ||
      (readinessFilter.value === 'READY' && match.integrityScore >= 85) ||
      (readinessFilter.value === 'PARTIAL' && match.integrityScore >= 65 && match.integrityScore < 85) ||
      (readinessFilter.value === 'WEAK' && match.integrityScore < 65);
    const matchesIssue =
      issueFilter.value === 'ALL' ||
      (issueFilter.value === 'HAS_ISSUE' && match.missingCount + match.staleCount + match.conflictCount > 0) ||
      (issueFilter.value === 'CONFLICT' && match.conflictCount > 0) ||
      (issueFilter.value === 'MISSING' && match.missingCount > 0) ||
      (issueFilter.value === 'STALE' && match.staleCount > 0) ||
      (issueFilter.value === 'NO_ODDS' && match.oddsMarketCount === 0) ||
      (issueFilter.value === 'NO_SENTIMENT' && match.sentimentFactorCount === 0) ||
      (issueFilter.value === 'NO_REPORT' && match.analysisReportCount === 0);
    return matchesSearch && matchesReadiness && matchesIssue;
  });
});

const matchPageCount = computed(() => Math.max(1, Math.ceil(filteredMatches.value.length / MATCH_PAGE_SIZE)));

const pagedMatches = computed(() => {
  const start = (matchPage.value - 1) * MATCH_PAGE_SIZE;
  return filteredMatches.value.slice(start, start + MATCH_PAGE_SIZE);
});

const matchFilterActive = computed(
  () => !!matchSearchQuery.value.trim() || readinessFilter.value !== 'ALL' || issueFilter.value !== 'ALL',
);

const publicSensitivePattern =
  /\b(?:ticketNo|ticket|stakeSuggestion|stake|betPlan|rawPayload|profitLoss|profit|loss|budgetAmount|returnAmount|ROI|CLV|closingOdds|closing[_\s-]?odds)\b\s*(?::|=|：|为|是)?\s*(?:[+\-]?\d+(?:\.\d+)?%?|[^\s,;，。；、/]+)?|(?:票号|投入|返还|盈亏|资金|收益|预算|下注|金额建议|原始\s*JSON)\s*(?::|=|：|为|是)?\s*(?:[+\-]?\d+(?:\.\d+)?%?|[^\s,;，。；、/]+)?/gi;
const publicProfessionalPattern = /置信度|推荐玩法|数学层|盘口层|赔率价值层|价值层|投注建议|下注建议/gi;

function publicText(value?: string, fallback = '暂无公开摘要。'): string {
  const text = value?.trim();
  if (!text) {
    return fallback;
  }
  const publicSafeText = text
    .replace(publicSensitivePattern, '已脱敏指标')
    .replace(publicProfessionalPattern, replacePublicProfessionalTerm);
  return readablePublicText(publicSafeText, fallback);
}

function replacePublicProfessionalTerm(match: string): string {
  if (match === '置信度') {
    return '把握程度';
  }
  if (match === '推荐玩法') {
    return '市场维度';
  }
  if (match === '数学层') {
    return '模型校验';
  }
  if (match === '盘口层') {
    return '市场变化';
  }
  if (match === '赔率价值层' || match === '价值层') {
    return '市场价值';
  }
  return '公开数据边界';
}

function publicDecisionLabel(value?: string | null): string {
  const label = enumLabel('conclusionType', value || '', '判断待定');
  if (label === '不建议介入') {
    return '仅观察';
  }
  return label;
}

function confidenceLabel(value?: string | null): string {
  const normalized = (value || '').trim().toUpperCase();
  if (!normalized) {
    return '待同步';
  }
  if (normalized === 'HIGH') {
    return '较高';
  }
  if (normalized === 'MEDIUM') {
    return '中等';
  }
  if (normalized === 'LOW') {
    return '偏低';
  }
  return publicText(value || '', '待同步');
}

function matchTitle(match?: PublicWorkbenchMatchSummary | null): string {
  if (!match) {
    return '赛前分析作战室';
  }
  return `${workbenchTeamDisplayName(match, 'HOME')} vs ${workbenchTeamDisplayName(match, 'AWAY')}`;
}

function workbenchTeamDisplayName(match: PublicWorkbenchMatchSummary, side: 'HOME' | 'AWAY'): string {
  const visualName = side === 'HOME' ? match.homeTeam?.teamName : match.awayTeam?.teamName;
  const legacyName = side === 'HOME' ? match.homeTeamName : match.awayTeamName;
  const candidate = !isPlaceholderTeamName(visualName) ? visualName : legacyName;
  if (!isPlaceholderTeamName(candidate)) {
    return candidate?.trim() || teamNameFromMatchName(match.matchName, side);
  }
  return teamNameFromMatchName(match.matchName, side);
}

function formatDateTime(value?: string): string {
  if (!value) {
    return '待同步';
  }
  return value.replace('T', ' ').slice(0, 16);
}

function numberText(value?: number): string {
  if (value == null) {
    return '-';
  }
  return Number(value).toFixed(4).replace(/0+$/, '').replace(/\.$/, '');
}

function percentText(value?: number): string {
  if (value == null) {
    return '-';
  }
  return `${(Number(value) * 100).toFixed(1).replace(/\.0$/, '')}%`;
}

function metricNumber(value?: number | null): number | null {
  if (value == null) {
    return null;
  }
  const number = Number(value);
  return Number.isFinite(number) ? number : null;
}

function metricUnit(metric: PublicVisualMetric): string {
  if (metric.unit === 'percent' || metric.unit === 'percentage') {
    return '%';
  }
  return metric.unit || '';
}

function metricTone(metric?: PublicVisualMetric): string {
  return (metric?.tone || 'info').toLowerCase();
}

function metricMax(metric: PublicVisualMetric): number {
  const value = Math.abs(metricNumber(metric.value) ?? 0);
  if (metricUnit(metric) === '%' || metric.key === 'integrity') {
    return 100;
  }
  if (metric.key.includes('probability')) {
    return 100;
  }
  if (['goals_for', 'goals_against', 'first_goal_minute'].includes(metric.key)) {
    return metric.key === 'first_goal_minute' ? 120 : Math.max(5, value);
  }
  if (['xg', 'xga', 'npxg', 'set_piece_xg'].includes(metric.key)) {
    return Math.max(3, value);
  }
  if (metric.key === 'ppda') {
    return Math.max(25, value);
  }
  return Math.max(10, value);
}

function scoreTone(score: number): string {
  if (score >= 90) {
    return 'tone-success';
  }
  if (score >= 70) {
    return 'tone-warning';
  }
  return 'tone-danger';
}

function scoreBarTone(score?: number): 'success' | 'warning' | 'danger' {
  if ((score ?? 0) >= 85) {
    return 'success';
  }
  if ((score ?? 0) >= 65) {
    return 'warning';
  }
  return 'danger';
}

function matchMeta(match?: PublicWorkbenchMatchSummary | null): string {
  if (!match) {
    return '世界杯 · 待定阶段';
  }
  return `${match.competition || '世界杯'} · ${match.stage || '待定阶段'} · ${formatDateTime(
    match.kickoffTime || match.matchday,
  )}`;
}

function statusTone(status?: string): string {
  switch (status) {
    case 'PASS':
    case 'RESOLVED':
    case 'FIT':
    case 'OPEN':
      return 'tone-success';
    case 'STALE':
    case 'PENDING':
    case 'MEDIUM':
      return 'tone-warning';
    case 'MISSING':
    case 'CONFLICT':
    case 'HIGH':
      return 'tone-danger';
    default:
      return 'tone-info';
  }
}

function riskTone(risk?: PublicWorkbenchSentimentRisk): string {
  return statusTone(risk?.riskLevel);
}

function normalizeSearchText(value?: string | number | null): string {
  return String(value ?? '')
    .trim()
    .toLowerCase();
}

function matchSearchHaystack(match: PublicWorkbenchMatchSummary): string {
  return normalizeSearchText(
    [
      match.matchName,
      match.matchKey,
      match.homeTeamName,
      match.awayTeamName,
      match.homeTeam?.teamName,
      match.awayTeam?.teamName,
      match.homeTeam?.fifaCode,
      match.awayTeam?.fifaCode,
      match.homeTeam?.countryRegion,
      match.awayTeam?.countryRegion,
      match.jcCode,
      match.competition,
      match.stage,
      match.venue,
      matchStatusLabel(match.status),
      match.resultStatus,
      match.scoreboard?.scoreDisplay,
      match.scoreboard?.resultText,
    ].join(' '),
  );
}

function readinessLevel(score: number): { label: string; explanation: string } {
  if (score >= 85) {
    return { label: '准备充分', explanation: '临场伤停与市场价格仍需复核' };
  }
  if (score >= 65) {
    return { label: '基础可读', explanation: '关键证据缺口' };
  }
  return { label: '偏薄弱', explanation: '证据薄弱，判断待定' };
}

function scoreContextText(match: PublicWorkbenchMatchSummary): string {
  const scoreDisplay = match.scoreboard?.scoreDisplay?.trim();
  const resultText = match.scoreboard?.resultText?.trim();
  if (scoreDisplay && !['待同步', '比分待校验'].includes(scoreDisplay)) {
    return `${scoreDisplay}${resultText ? ` · ${resultText}` : ''}`;
  }
  if (resultText && resultText !== '赛果待同步') {
    return resultText;
  }
  return `${matchStatusLabel(match.status)} · ${formatDateTime(match.kickoffTime || match.matchday)}`;
}

function riskPriorityText(match: PublicWorkbenchMatchSummary): string {
  if (match.conflictCount > 0) {
    return `${match.conflictCount} 个冲突待核；阵容、市场和舆情需同步复核。`;
  }
  if (match.missingCount > 0 || match.staleCount > 0) {
    return `缺失 ${match.missingCount}、过期 ${match.staleCount}：伤停、市场价格快照、天气/裁判缺口。`;
  }
  if (match.sentimentFactorCount > 0) {
    return `已有 ${match.sentimentFactorCount} 条外部因素，临场变化待同步。`;
  }
  return '暂无明显公开风险，阵容和市场价格快照为赛前复验项。';
}

function goToMatchPage(page: number) {
  matchPage.value = Math.min(Math.max(1, page), matchPageCount.value);
}

function resetMatchFilters() {
  matchSearchQuery.value = '';
  readinessFilter.value = 'ALL';
  issueFilter.value = 'ALL';
  matchPage.value = 1;
}

watch([matchSearchQuery, readinessFilter, issueFilter], () => {
  matchPage.value = 1;
});

watch(matchPageCount, (pageCount) => {
  if (matchPage.value > pageCount) {
    matchPage.value = pageCount;
  }
});

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const response = await listPublicPrematchWorkbenchMatches();
    matches.value = response.data;
    const nextMatch = selectedMatchId.value
      ? matches.value.find((match) => match.matchId === selectedMatchId.value) ?? matches.value[0]
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
    error.value = cause instanceof Error ? cause.message : '无法读取公开赛前作战室数据。';
  } finally {
    loading.value = false;
  }
}

async function openMatch(match: PublicWorkbenchMatchSummary) {
  selectedMatchId.value = match.matchId;
  detailLoading.value = true;
  detailError.value = '';
  try {
    const response = await getPublicPrematchWorkbenchMatch(match.matchId);
    selected.value = response.data;
  } catch (cause) {
    selected.value = null;
    detailError.value = cause instanceof Error ? cause.message : '无法读取公开作战室详情。';
  } finally {
    detailLoading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <section class="page-shell prematch-page" aria-labelledby="prematch-title">
    <section class="page-content prematch-page__content">
      <header class="prematch-hero">
        <div>
          <p class="eyebrow">赛前决策流</p>
          <h1 id="prematch-title">赛前分析作战室</h1>
          <p>
            以公开赛前数据聚合资料准备度、阵容、赔率、舆情、证据与分析摘要。访客可只读查看；管理类动作只在管理员登录后显示。
          </p>
        </div>
        <div class="hero-actions">
          <RouterLink class="action-link action-link--ghost" to="/">返回总览</RouterLink>
          <RouterLink v-if="authStore.canWrite" class="action-link action-link--admin" to="/admin/import-review">
            管理员审核入口
          </RouterLink>
        </div>
      </header>

      <section class="stat-grid" aria-label="赛前作战室统计">
        <article class="stat-card">
          <span>公开比赛</span>
          <strong>{{ stats.matches }}</strong>
          <small>待分析队列</small>
        </article>
        <article class="stat-card">
          <span>平均准备度</span>
          <strong>{{ stats.avgScore }}%</strong>
          <small>资料准备度均值</small>
        </article>
        <article class="stat-card">
          <span>缺失项</span>
          <strong>{{ stats.missing }}</strong>
          <small>需补证据</small>
        </article>
        <article class="stat-card">
          <span>冲突项</span>
          <strong>{{ stats.conflicts }}</strong>
          <small>来源待校验</small>
        </article>
      </section>
<div v-if="error" class="alert-panel" role="alert">
        <strong>赛前作战室暂不可用</strong>
        <span>{{ error }}</span>
        <button class="action-link action-link--button" type="button" @click="load">重试</button>
      </div>

      <section
        v-if="selected && prematchPanel"
        class="prematch-console"
        data-test="workbench-decision-console"
        aria-label="赛前数据面板"
      >
        <div class="prematch-console__score">
          <p class="eyebrow">赛前数据</p>
          <h2>赛前数据面板</h2>
          <ScoreboardCard
            compact
            :home-team="selected.summary.homeTeam"
            :away-team="selected.summary.awayTeam"
            :scoreboard="selected.summary.scoreboard"
            :match-name="selected.summary.matchName"
            :meta="matchMeta(selected.summary)"
            :status="matchStatusLabel(selected.summary.status)"
            :integrity-score="selected.summary.integrityScore"
            :risk-count="selected.summary.conflictCount"
            :evidence-count="selected.evidence.length"
          />
        </div>
        <div class="prematch-console__rings" tabindex="0" aria-label="赛前资料图">
          <CoverageDonut
            v-for="ring in prematchPanel.rings"
            :key="ring.label"
            :label="ring.label"
            :value="ring.value"
            :tone="ring.tone"
            size="compact"
            :caption="ring.caption"
          />
        </div>
        <div class="prematch-console__bars">
          <MetricBar
            v-for="bar in prematchPanel.bars"
            :key="bar.label"
            :label="bar.label"
            :value="bar.value"
            :max="bar.max"
            :unit="bar.label === '资料准备度' ? '%' : ''"
            :tone="bar.tone"
            :caption="bar.caption"
          />
        </div>
      </section>

      <section class="workbench-grid">
        <aside class="match-rail" tabindex="0" aria-label="比赛准备队列">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">队列</p>
              <h2>比赛准备清单</h2>
            </div>
            <button class="refresh-button" type="button" :disabled="loading" @click="load">
              {{ loading ? '刷新中' : '刷新' }}
            </button>
          </div>

          <div class="match-directory" aria-label="比赛目录筛选">
            <label class="filter-field">
              <span>搜索比赛</span>
              <input
                v-model.trim="matchSearchQuery"
                data-test="workbench-match-search"
                type="search"
                placeholder="球队、FIFA、竞彩、场地"
              />
            </label>
            <label class="filter-field">
                <span>准备度</span>
                <select v-model="readinessFilter" data-test="workbench-readiness-filter">
                  <option value="ALL">全部准备度</option>
                <option value="READY">准备充分（≥85%）</option>
                  <option value="PARTIAL">基础可读（65-84%）</option>
                  <option value="WEAK">偏薄弱（&lt;65%）</option>
                </select>
            </label>
            <label class="filter-field">
              <span>问题类型</span>
              <select v-model="issueFilter" data-test="workbench-issue-filter">
                <option value="ALL">全部问题</option>
                <option value="HAS_ISSUE">有缺失/过期/冲突</option>
                <option value="CONFLICT">有冲突</option>
                <option value="MISSING">有缺失</option>
                <option value="STALE">有过期</option>
                <option value="NO_ODDS">缺赔率</option>
                <option value="NO_SENTIMENT">缺外因</option>
                <option value="NO_REPORT">缺分析摘要</option>
              </select>
            </label>
            <button
              class="clear-filter-button"
              type="button"
              data-test="workbench-clear-filters"
              :disabled="!matchFilterActive"
              @click="resetMatchFilters"
            >
              清除筛选
            </button>
          </div>

          <p class="filter-summary" data-test="workbench-filter-summary">
            已筛出 {{ filteredMatches.length }} / {{ matches.length }} 场赛前比赛
          </p>

          <div v-if="loading && !matches.length" class="loading-card" aria-live="polite">正在加载公开赛前队列...</div>
          <p v-else-if="!matches.length" class="empty-copy">暂无公开赛前比赛。</p>
          <div v-else-if="filteredMatches.length" class="match-list-scroll" data-test="workbench-match-list" tabindex="0">
            <button
              v-for="match in pagedMatches"
              :key="match.matchId"
              class="match-card"
              :class="{ 'match-card--active': match.matchId === selectedMatchId }"
              type="button"
              data-test="workbench-match-card"
              @click="openMatch(match)"
            >
              <ScoreboardCard
                compact
                :home-team="match.homeTeam"
                :away-team="match.awayTeam"
                :scoreboard="match.scoreboard"
                :match-name="match.matchName"
                :meta="matchMeta(match)"
                :status="matchStatusLabel(match.status)"
              />
              <small>竞彩 {{ match.jcCode || '待定' }}</small>
              <em :class="scoreTone(match.integrityScore)">准备度 {{ match.integrityScore }}%</em>
              <small>缺失 {{ match.missingCount }} · 过期 {{ match.staleCount }} · 冲突 {{ match.conflictCount }}</small>
            </button>
          </div>
          <div v-else class="empty-filter-state" data-test="workbench-empty-filter">
            <strong>没有符合条件的比赛</strong>
            <p>换一个球队名、FIFA code、竞彩编号，或清除筛选后查看全部赛前队列。</p>
            <button class="clear-filter-button" type="button" @click="resetMatchFilters">清除筛选</button>
          </div>

          <nav v-if="filteredMatches.length > MATCH_PAGE_SIZE" class="directory-pager" aria-label="比赛目录分页">
            <button type="button" :disabled="matchPage <= 1" @click="goToMatchPage(matchPage - 1)">上一页</button>
            <span>第 {{ matchPage }} / {{ matchPageCount }} 页</span>
            <button type="button" :disabled="matchPage >= matchPageCount" @click="goToMatchPage(matchPage + 1)">下一页</button>
          </nav>
        </aside>

        <article class="decision-board" tabindex="0">
          <div class="panel-heading decision-board__heading">
            <div>
              <p class="eyebrow">当前比赛</p>
              <h2>{{ matchTitle(selected?.summary) }}</h2>
            </div>
            <span v-if="selected" class="status-pill" :class="scoreTone(selected.summary.integrityScore)">
              准备度 {{ selected.summary.integrityScore }}%
            </span>
          </div>

          <div v-if="detailError" class="alert-panel" role="alert">
            <strong>详情加载失败</strong>
            <span>{{ detailError }}</span>
          </div>
          <div v-else-if="detailLoading && !selected" class="loading-card" aria-live="polite">正在加载比赛详情...</div>
          <p v-else-if="!selected" class="empty-copy">当前未选中比赛。</p>

          <template v-else>
            <ScoreboardCard
              :home-team="selected.summary.homeTeam"
              :away-team="selected.summary.awayTeam"
              :scoreboard="selected.summary.scoreboard"
              :match-name="selected.summary.matchName"
              :meta="matchMeta(selected.summary)"
              :status="matchStatusLabel(selected.summary.status)"
              :integrity-score="selected.summary.integrityScore"
              :risk-count="selected.summary.conflictCount"
              :evidence-count="selected.evidence.length"
            />

            <section
              v-if="selected.visualSummary"
              class="visual-summary-card"
              aria-label="赛前可视化摘要"
            >
              <div class="visual-summary-card__copy">
                <p class="eyebrow">一眼结论</p>
                <h3>{{ selected.visualSummary.statusText || '赛前状态待同步' }}</h3>
                <p>{{ selected.visualSummary.readinessText || '资料准备度仍有校验缺口。' }}</p>
                <small>{{ selected.visualSummary.riskText || '暂无额外风险摘要。' }}</small>
                <small>{{ selected.visualSummary.nextCheckText || '资料缺口：球队、球员、赔率、舆情和证据。' }}</small>
              </div>
              <div class="visual-summary-card__metrics" tabindex="0">
                <MetricBar
                  v-for="metric in selected.visualSummary.metrics"
                  :key="metric.key"
                  :label="metric.label"
                  :value="metricNumber(metric.value)"
                  :max="metricMax(metric)"
                  :unit="metricUnit(metric)"
                  :tone="metricTone(metric)"
                  :caption="metric.explanation"
                />
              </div>
            </section>

            <section
              v-if="selected.teamComparison?.length"
              class="team-comparison-board"
              aria-label="球队关键指标对比"
            >
              <div class="section-title">
                <p class="eyebrow">对比</p>
                <h3>球队关键指标对比</h3>
              </div>
              <div class="team-comparison-grid" tabindex="0">
                <article
                  v-for="side in selected.teamComparison"
                  :key="side.team.teamId ?? side.team.teamName ?? 'team-comparison'"
                  class="team-comparison-card"
                >
                  <FlagTeamName :team="side.team" />
                  <MetricBar
                    v-for="metric in side.metrics"
                    :key="`${side.team.teamId ?? side.team.teamName ?? 'team'}-${metric.key}`"
                    :label="metric.label"
                    :value="metricNumber(metric.value)"
                    :max="metricMax(metric)"
                    :unit="metricUnit(metric)"
                    :tone="metricTone(metric)"
                    :caption="metric.explanation"
                  />
                </article>
              </div>
            </section>

            <section class="match-summary-card" aria-label="比赛摘要">
              <div>
                <span>赛事</span>
                <strong>{{ selected.summary.competition || '世界杯' }}</strong>
              </div>
              <div>
                <span>阶段</span>
                <strong>{{ selected.summary.stage || '待定阶段' }}</strong>
              </div>
              <div>
                <span>开球</span>
                <strong>{{ formatDateTime(selected.summary.kickoffTime) }}</strong>
              </div>
              <div>
                <span>状态</span>
                <strong>{{ matchStatusLabel(selected.summary.status) }}</strong>
              </div>
            </section>

            <section class="decision-flow" data-test="decision-flow" aria-label="赛前决策流">
              <article v-for="step in decisionSteps" :key="step.label" class="flow-step">
                <span>{{ step.label }}</span>
                <strong>{{ step.value }}</strong>
                <small>{{ step.meta }}</small>
              </article>
            </section>

            <section class="readiness-board" data-test="workbench-readiness-board" aria-label="赛前准备度图表">
              <div class="section-title">
                <p class="eyebrow">图表</p>
                <h3>准备度图形</h3>
              </div>
              <div class="readiness-ring-grid" tabindex="0" aria-label="赛前准备度环形图">
                <CoverageDonut
                  v-for="ring in currentReadinessRings"
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
              <div class="readiness-grid" tabindex="0">
                <MetricBar
                  v-for="bar in currentReadinessBars"
                  :key="bar.label"
                  :label="bar.label"
                  :value="bar.value"
                  :max="bar.max"
                  :unit="bar.label === '资料准备度' ? '%' : ''"
                  :tone="bar.tone"
                  :caption="bar.caption"
                />
              </div>
            </section>

            <section class="section-block" aria-labelledby="integrity-title">
              <div class="section-title">
                <p class="eyebrow">资料校验</p>
                <h3 id="integrity-title">资料准备度检查</h3>
              </div>
              <div class="card-grid card-grid--checks" tabindex="0">
                <article v-for="check in selected.integrityChecks" :key="check.code" class="info-card">
                  <div class="info-card__title">
                    <strong>{{ check.label }}</strong>
                    <span class="status-pill" :class="statusTone(check.status)">{{ enumLabel('integrityStatus', check.status) }}</span>
                  </div>
                  <p>{{ check.message }}</p>
                  <small>证据 {{ check.evidenceCount }} · {{ enumLabel('severity', check.severity) }} · {{ formatDateTime(check.lastUpdatedAt) }}</small>
                </article>
              </div>
            </section>

            <section class="evidence-grid" tabindex="0" aria-label="赛前公开证据卡片">
              <article class="info-card" data-test="team-card">
                <div class="section-title">
                  <p class="eyebrow">球队</p>
                  <h3>球队画像</h3>
                </div>
                <div v-for="team in selected.teams" :key="team.teamId" class="stack-item">
                  <strong>{{ team.teamName }} <small>{{ team.fifaCode || '' }}</small></strong>
                  <p>{{ team.styleTags || team.attackProfile || '暂无风格标签' }}</p>
                  <span v-for="fact in team.facts" :key="fact.factId" class="fact-chip">{{ fact.title }}</span>
                </div>
              </article>

              <article class="info-card" data-test="player-card">
                <div class="section-title">
                  <p class="eyebrow">球员</p>
                  <h3>球员状态</h3>
                </div>
                <div v-for="player in selected.players" :key="player.playerId" class="stack-item">
                  <strong>{{ player.playerName }} <small>{{ player.teamName || '' }} · {{ positionLabel(player.position) }}</small></strong>
                  <p>{{ player.injuryStatus || enumLabel('playerStatus', player.status, '状态待同步') }} · {{ player.cardStatus || '纪律待同步' }}</p>
                  <span v-for="fact in player.facts" :key="fact.factId" class="fact-chip">{{ fact.title }}</span>
                </div>
              </article>

              <article class="info-card">
                <div class="section-title">
                  <p class="eyebrow">阵容</p>
                  <h3>阵容线索</h3>
                </div>
                <div v-for="lineup in selected.lineups" :key="lineup.id" class="stack-item">
                  <strong>{{ lineup.playerName || '球员待定' }} <small>{{ lineup.teamName || '' }}</small></strong>
                  <p>{{ positionLabel(lineup.position) }} · {{ lineupRoleLabel(lineup.role) }} · {{ lineup.starter ? '首发' : '替补' }}</p>
                </div>
              </article>

              <article class="info-card" data-test="odds-card">
                <div class="section-title">
                  <p class="eyebrow">赔率</p>
                  <h3>赔率与市场</h3>
                </div>
                <div v-for="market in selected.oddsMarkets" :key="market.marketId" class="stack-item">
                  <strong>{{ market.bookmaker }} <small>{{ marketLabel(market.marketCode, market.marketName) }} · {{ enumLabel('oddsSnapshot', market.snapshotType, '快照') }}</small></strong>
                  <p>{{ marketLabel(market.marketCode, market.marketName) }} · {{ market.lineValue || '无市场线' }} · {{ formatDateTime(market.capturedAt) }}</p>
                  <span v-for="selection in market.selections" :key="selection.selectionId" class="fact-chip">
                    {{ selection.selectionName }} {{ numberText(selection.oddsValue) }} / {{ percentText(selection.impliedProbability) }}
                  </span>
                  <MetricBar
                    v-for="selection in market.selections"
                    :key="`prob-${selection.selectionId}`"
                    :label="`${selection.selectionName} 隐含概率`"
                    :value="selection.impliedProbability != null ? selection.impliedProbability * 100 : null"
                    :max="100"
                    unit="%"
                    tone="info"
                    caption="由赔率换算的市场倾向值"
                  />
                </div>
              </article>

              <article class="info-card" data-test="sentiment-card">
                <div class="section-title">
                  <p class="eyebrow">舆情</p>
                  <h3>舆情与外部因素</h3>
                </div>
                <div v-for="factor in selected.sentimentFactors" :key="factor.factorId" class="stack-item">
                  <strong>{{ factor.title }} <small>{{ enumLabel('factorCategory', factor.factorCategory) }} · {{ enumLabel('impactDirection', factor.impactDirection, '影响待定') }}</small></strong>
                  <p>{{ readablePublicText(factor.summary, '暂无摘要') }}</p>
                  <span v-for="risk in factor.risks" :key="risk.riskId" class="fact-chip" :class="riskTone(risk)">
                    {{ risk.title }} {{ enumLabel('riskLevel', risk.riskLevel, '') }}
                  </span>
                </div>
              </article>

              <article class="info-card">
                <div class="section-title">
                  <p class="eyebrow">证据</p>
                  <h3>多源证据</h3>
                </div>
                <div v-for="item in selected.evidence" :key="item.evidenceId" class="stack-item">
                  <strong>{{ item.sourceName }} <small>{{ sourceTypeLabel(item.sourceType) }}</small></strong>
                  <p>{{ readablePublicText(item.summary, '暂无摘要') }}</p>
                  <small>可信度 {{ numberText(item.reliabilityScore) }} · {{ formatDateTime(item.evidenceTime) }}</small>
                </div>
              </article>

              <article class="info-card">
                <div class="section-title">
                  <p class="eyebrow">冲突</p>
                  <h3>数据冲突</h3>
                </div>
                <div v-for="conflict in selected.conflicts" :key="conflict.conflictId" class="stack-item">
                  <strong>{{ enumLabel('conflictType', conflict.conflictType) }} <small>{{ fieldNameLabel(conflict.fieldName) }}</small></strong>
                  <p>状态：{{ enumLabel('resolutionStatus', conflict.resolutionStatus) }}</p>
                </div>
                <p v-if="!selected.conflicts.length" class="empty-copy">暂无公开冲突项。</p>
              </article>

              <article class="info-card" data-test="analysis-card">
                <div class="section-title">
                  <p class="eyebrow">报告</p>
                  <h3>分析摘要</h3>
                </div>
                <div v-for="report in selected.analysisReports" :key="report.reportId" class="stack-item">
                  <strong>赛前判断：{{ publicDecisionLabel(report.conclusionType) }} <small>把握程度：{{ confidenceLabel(report.confidence) }}</small></strong>
                  <p>{{ publicText(report.riskSummary, '暂无风险摘要') }}</p>
                  <small>{{ publicText(report.recommendedMarkets, '市场维度待定') }} &middot; {{ publicText(report.dimensions, '维度待定') }}</small>
                </div>
              </article>
            </section>

            <aside class="public-boundary" aria-label="公开数据边界">
              <strong>公开边界</strong>
              <span>本页只展示已批准的赛前摘要、证据、赔率与风险信号；个人执行明细不在公开作战室展示。</span>
            </aside>
          </template>
        </article>
      </section>
    </section>
  </section>
</template>

<style scoped>
.prematch-page {
  max-width: 100%;
  overflow-x: hidden;
}

.prematch-page__content {
  display: grid;
  gap: 18px;
  min-width: 0;
}

.prematch-hero,
.stat-card,
.prematch-console,
.match-rail,
.decision-board,
.info-card,
.match-summary-card,
.readiness-board,
.visual-summary-card,
.team-comparison-board,
.team-comparison-card,
.alert-panel,
.loading-card,
.empty-filter-state,
.public-boundary {
  background: var(--wc-glass);
  border: 1px solid var(--wc-border);
  border-radius: var(--wc-radius-lg);
  color: var(--wc-text);
}

.prematch-hero {
  align-items: stretch;
  display: grid;
  gap: 18px;
  grid-template-columns: minmax(0, 1fr) auto;
  padding: clamp(20px, 4vw, 38px);
}

.prematch-hero h1 {
  font-family: var(--wc-font-display);
  font-size: clamp(34px, 6vw, 70px);
  line-height: 0.98;
  margin: 0 0 14px;
}

.prematch-hero p:not(.eyebrow) {
  color: var(--wc-text-muted);
  font-size: clamp(16px, 2vw, 19px);
  line-height: 1.7;
  margin: 0;
  max-width: 860px;
}

.eyebrow {
  color: var(--wc-warning);
  font-family: var(--wc-font-mono);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  margin: 0 0 8px;
  text-transform: uppercase;
}

.hero-actions,
.panel-heading,
.decision-board__heading,
.info-card__title {
  align-items: center;
  display: flex;
  gap: 12px;
  justify-content: space-between;
}

.hero-actions {
  align-content: start;
  flex-wrap: wrap;
  justify-content: end;
}

.action-link,
.refresh-button,
.clear-filter-button,
.directory-pager button {
  align-items: center;
  border-radius: 999px;
  cursor: pointer;
  display: inline-flex;
  font-weight: 800;
  justify-content: center;
  min-height: 44px;
  padding: 0 16px;
  text-decoration: none;
  transition: border-color 180ms ease, opacity 180ms ease, transform 180ms ease;
}

.action-link--ghost,
.refresh-button,
.clear-filter-button,
.directory-pager button {
  background: rgba(147, 197, 253, 0.1);
  border: 1px solid rgba(147, 197, 253, 0.28);
  color: var(--wc-text);
}

.action-link--admin,
.action-link--button {
  background: var(--wc-accent);
  border: 1px solid transparent;
  color: var(--wc-on-accent);
}

.refresh-button:disabled,
.clear-filter-button:disabled,
.directory-pager button:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

.action-link:focus-visible,
.refresh-button:focus-visible,
.clear-filter-button:focus-visible,
.directory-pager button:focus-visible,
.match-card:focus-visible,
.match-list-scroll:focus-visible,
.filter-field input:focus-visible,
.filter-field select:focus-visible,
.prematch-console__rings:focus-visible,
.match-rail:focus-visible,
.decision-board:focus-visible,
.visual-summary-card__metrics:focus-visible,
.team-comparison-grid:focus-visible,
.readiness-ring-grid:focus-visible,
.readiness-grid:focus-visible,
.card-grid--checks:focus-visible,
.evidence-grid:focus-visible {
  box-shadow: var(--wc-focus-ring);
  outline: none;
}

.action-link:hover,
.refresh-button:hover:not(:disabled),
.clear-filter-button:hover:not(:disabled),
.directory-pager button:hover:not(:disabled),
.match-card:hover {
  border-color: rgba(147, 197, 253, 0.5);
  transform: translateY(-2px);
}

.stat-grid,
.decision-flow,
.match-summary-card {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}










.prematch-console {
  display: grid;
  gap: 18px;
  grid-template-columns: minmax(0, 0.9fr) minmax(0, 0.72fr) minmax(0, 1fr);
  min-width: 0;
  padding: 20px;
}

.prematch-console h2 {
  font-family: var(--wc-font-display);
  font-size: clamp(28px, 4vw, 42px);
  line-height: 1.05;
  margin: 0 0 12px;
}

.prematch-console__score,
.prematch-console__rings,
.prematch-console__bars {
  display: grid;
  gap: 14px;
  min-width: 0;
}

.prematch-console__rings {
  align-content: start;
  gap: 10px;
}

.prematch-console__rings :deep(.coverage-donut) {
  background: rgba(2, 6, 23, 0.34);
  border: 1px solid rgba(147, 197, 253, 0.14);
  border-radius: var(--wc-radius-md);
  grid-template-columns: 76px minmax(0, 1fr);
  min-height: 104px;
  padding: 12px;
}

.prematch-console__rings :deep(.coverage-donut__copy) {
  gap: 5px;
}

.prematch-console__rings :deep(.coverage-donut__copy strong) {
  font-size: 15px;
}

.prematch-console__rings :deep(.coverage-donut__copy small) {
  font-size: 12px;
  line-height: 1.45;
}








.stat-card {
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 18px;
}

.stat-card span,
.stat-card small,
.match-card span,
.match-card small,
.flow-step small,
.match-summary-card span,
.stack-item small,
.public-boundary span,
.empty-copy {
  color: var(--wc-text-muted);
}

.stat-card strong {
  color: var(--wc-primary);
  font-family: var(--wc-font-mono);
  font-size: clamp(30px, 5vw, 44px);
  line-height: 1;
}

.workbench-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: minmax(0, 360px) minmax(0, 1fr);
  min-width: 0;
}

.match-rail,
.decision-board,
.alert-panel,
.loading-card,
.public-boundary {
  display: grid;
  gap: 14px;
  min-width: 0;
  padding: 18px;
}

.match-rail {
  align-content: start;
  max-height: min(84dvh, 820px);
  overflow: auto;
  scrollbar-color: rgba(147, 197, 253, 0.38) transparent;
}

.match-directory {
  display: grid;
  gap: 10px;
}

.filter-field {
  color: var(--wc-text-muted);
  display: grid;
  font-size: 13px;
  font-weight: 800;
  gap: 6px;
}

.filter-field input,
.filter-field select {
  background: rgba(15, 23, 42, 0.62);
  border: 1px solid rgba(147, 197, 253, 0.22);
  border-radius: 14px;
  color: var(--wc-text);
  min-height: 44px;
  padding: 0 13px;
  width: 100%;
}

.filter-field input::placeholder {
  color: rgba(226, 232, 240, 0.48);
}

.filter-summary {
  color: var(--wc-text-muted);
  font-family: var(--wc-font-mono);
  font-size: 12px;
  margin: 0;
}

.match-list-scroll {
  display: grid;
  gap: 12px;
  max-height: 760px;
  min-width: 0;
  overflow: auto;
  padding-right: 4px;
}

.panel-heading h2,
.section-title h3 {
  margin: 0;
}

.match-card {
  background: rgba(15, 23, 42, 0.58);
  border: 1px solid rgba(147, 197, 253, 0.18);
  border-radius: var(--wc-radius-md);
  color: var(--wc-text);
  display: grid;
  gap: 7px;
  min-height: 132px;
  min-width: 0;
  padding: 15px;
  text-align: left;
}

.empty-filter-state {
  display: grid;
  gap: 10px;
  padding: 16px;
}

.empty-filter-state p {
  color: var(--wc-text-muted);
  line-height: 1.6;
  margin: 0;
}

.directory-pager {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: space-between;
}

.directory-pager span {
  color: var(--wc-text-muted);
  font-family: var(--wc-font-mono);
  font-size: 12px;
}

.match-card--active {
  border-color: rgba(217, 119, 6, 0.56);
  box-shadow: inset 0 0 0 1px rgba(217, 119, 6, 0.22);
}

.match-card strong,
.info-card strong {
  font-size: 18px;
}

.match-card em,
.status-pill,
.fact-chip {
  border-radius: 999px;
  display: inline-flex;
  font-family: var(--wc-font-mono);
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
  justify-self: start;
  padding: 6px 9px;
}

.decision-board {
  align-content: start;
  max-height: min(88dvh, 860px);
  overflow: auto;
  scrollbar-color: rgba(147, 197, 253, 0.38) transparent;
}

.decision-board__heading h2 {
  font-size: clamp(24px, 4vw, 36px);
  line-height: 1.1;
  margin: 0;
}

.match-summary-card {
  padding: 16px;
}

.visual-summary-card {
  display: grid;
  gap: 18px;
  grid-template-columns: minmax(0, 0.9fr) minmax(0, 1.4fr);
  min-width: 0;
  padding: 18px;
}

.visual-summary-card__copy {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.visual-summary-card__copy h3,
.team-comparison-board h3 {
  color: var(--wc-text);
  font-family: var(--wc-font-display);
  font-size: 24px;
  line-height: 1.1;
  margin: 0;
}

.visual-summary-card__copy p,
.visual-summary-card__copy small,
.board-note {
  color: var(--wc-text-muted);
  line-height: 1.65;
  margin: 0;
}

.visual-summary-card__metrics {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  min-width: 0;
}

.team-comparison-board {
  display: grid;
  gap: 16px;
  min-width: 0;
  padding: 18px;
}

.team-comparison-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  min-width: 0;
}

.team-comparison-card {
  display: grid;
  gap: 12px;
  min-width: 0;
  padding: 16px;
}

.match-summary-card div,
.flow-step {
  background: rgba(15, 23, 42, 0.48);
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: var(--wc-radius-sm);
  display: grid;
  gap: 5px;
  min-width: 0;
  padding: 14px;
}

.match-summary-card strong,
.flow-step strong {
  font-family: var(--wc-font-mono);
}

.decision-flow {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.readiness-board {
  display: grid;
  gap: 13px;
  min-width: 0;
  padding: 18px;
}

.readiness-ring-grid {
  display: grid;
  gap: 13px;
  grid-template-columns: repeat(auto-fit, minmax(178px, 1fr));
  min-width: 0;
}

.readiness-grid {
  display: grid;
  gap: 13px;
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.flow-step span {
  color: var(--wc-text);
  font-weight: 800;
}

.flow-step strong {
  color: var(--wc-primary);
  font-size: 24px;
}

.section-block {
  display: grid;
  gap: 12px;
}

.section-title {
  display: grid;
  gap: 2px;
}

.card-grid,
.evidence-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  min-width: 0;
}

.card-grid--checks {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.evidence-grid {
  max-height: min(84dvh, 820px);
  overflow: auto;
  padding-right: 4px;
  scrollbar-color: rgba(147, 197, 253, 0.38) transparent;
}

.info-card {
  display: grid;
  gap: 12px;
  min-width: 0;
  padding: 18px;
}

.stack-item {
  background: rgba(15, 23, 42, 0.42);
  border: 1px solid rgba(148, 163, 184, 0.14);
  border-radius: var(--wc-radius-sm);
  display: grid;
  gap: 7px;
  min-width: 0;
  padding: 13px;
}

.stack-item p,
.info-card p {
  color: var(--wc-text-muted);
  line-height: 1.6;
  margin: 0;
}

.stack-item small {
  display: block;
  font-size: 12px;
  margin-top: 3px;
}

.fact-chip {
  background: rgba(147, 197, 253, 0.11);
  border: 1px solid rgba(147, 197, 253, 0.2);
  color: var(--wc-text);
  margin: 0 6px 6px 0;
}

.tone-success {
  background: rgba(134, 239, 172, 0.14);
  color: var(--wc-success);
}

.tone-warning {
  background: rgba(253, 230, 138, 0.14);
  color: var(--wc-warning);
}

.tone-danger {
  background: rgba(252, 165, 165, 0.16);
  color: var(--wc-danger);
}

.tone-info {
  background: rgba(147, 197, 253, 0.12);
  color: var(--wc-primary);
}

.alert-panel {
  border-color: rgba(252, 165, 165, 0.32);
}

.loading-card {
  color: var(--wc-text-muted);
}

.public-boundary {
  border-color: rgba(217, 119, 6, 0.32);
}

@media (max-width: 1100px) {
  .prematch-hero,
  .prematch-console,
  .workbench-grid,
  .decision-flow,
  .readiness-ring-grid,
  .readiness-grid,
  .visual-summary-card,
  .visual-summary-card__metrics,
  .team-comparison-grid {
    grid-template-columns: 1fr;
  }

  .stat-grid,
  .match-summary-card {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .prematch-page__content {
    gap: 14px;
  }

  .prematch-hero,
  .prematch-console,
  .workbench-grid,
  .visual-summary-card,
  .team-comparison-grid,
  .evidence-grid {
    grid-template-columns: 1fr;
  }

  .stat-grid,
  .decision-flow,
  .readiness-ring-grid,
  .readiness-grid,
  .match-summary-card,
  .card-grid--checks,
  .prematch-console__rings,
  .visual-summary-card__metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .prematch-console__rings {
    grid-template-columns: 1fr;
  }

  .prematch-hero,
  .stat-card,
  .prematch-console,
  .match-rail,
  .decision-board,
  .info-card,
  .match-summary-card,
  .readiness-board,
  .visual-summary-card,
  .team-comparison-board,
  .team-comparison-card,
  .public-boundary {
    border-radius: var(--wc-radius-md);
    gap: 10px;
    padding: 12px;
  }

  .prematch-hero h1 {
    font-size: clamp(28px, 12vw, 42px);
    margin-bottom: 8px;
  }

  .prematch-hero p:not(.eyebrow),
  .visual-summary-card__copy p,
  .visual-summary-card__copy small,
  .board-note,
  .stack-item p,
  .info-card p,
  .public-boundary span,
  .empty-filter-state p,
  .prematch-console__bars :deep(.metric-bar small),
  .readiness-grid :deep(.metric-bar small),
  .visual-summary-card__metrics :deep(.metric-bar small),
  .team-comparison-card :deep(.metric-bar small),
  .info-card :deep(.metric-bar small) {
    display: none;
  }

  .stat-grid,
  .decision-flow,
  .match-summary-card,
  .readiness-ring-grid,
  .readiness-grid,
  .card-grid--checks,
  .prematch-console__rings,
  .visual-summary-card__metrics,
  .team-comparison-grid,
  .evidence-grid,
  .match-list-scroll {
    gap: 10px;
  }

  .prematch-console__rings :deep(.coverage-donut),
  .readiness-ring-grid :deep(.coverage-donut) {
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

  .prematch-console__rings :deep(.coverage-donut__ring),
  .readiness-ring-grid :deep(.coverage-donut__ring) {
    width: 66px;
  }

  .prematch-console__rings :deep(.coverage-donut__ring span),
  .readiness-ring-grid :deep(.coverage-donut__ring span) {
    font-size: 20px;
  }

  .prematch-console__rings :deep(.coverage-donut__copy),
  .readiness-ring-grid :deep(.coverage-donut__copy) {
    gap: 3px;
  }

  .prematch-console__rings :deep(.coverage-donut__copy strong),
  .readiness-ring-grid :deep(.coverage-donut__copy strong) {
    font-size: 12px;
    line-height: 1.2;
  }

  .prematch-console__rings :deep(.coverage-donut__copy small),
  .readiness-ring-grid :deep(.coverage-donut__copy small) {
    font-size: 11px;
    line-height: 1.25;
  }

  .stat-card,
  .match-summary-card div,
  .flow-step {
    min-height: 96px;
  }

  .stat-card strong,
  .flow-step strong {
    font-size: 26px;
  }

  .match-rail {
    max-height: none;
    overflow: visible;
    scrollbar-gutter: auto;
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

  .decision-board {
    max-height: none;
    overflow: visible;
    scrollbar-gutter: auto;
  }

  .evidence-grid,
  .card-grid--checks {
    max-height: none;
    overflow: visible;
    padding-right: 0;
    scrollbar-gutter: auto;
  }

  .readiness-ring-grid,
  .readiness-grid,
  .visual-summary-card__metrics,
  .team-comparison-grid {
    max-height: none;
    overflow: visible;
    padding-right: 0;
    scrollbar-gutter: auto;
  }

  .team-comparison-card {
    padding: 10px;
  }

  .team-comparison-card :deep(.flag-team) {
    align-items: center;
    flex-direction: row;
    gap: 7px;
  }

  .match-card,
  .stack-item {
    border-radius: 14px;
    gap: 5px;
    padding: 10px;
  }

  .match-card {
    align-content: start;
    min-height: 252px;
  }

  .match-card :deep(.scoreboard-card),
  .prematch-console :deep(.scoreboard-card),
  .decision-board > :deep(.scoreboard-card) {
    gap: 8px;
    padding: 10px;
  }

  .match-card :deep(.scoreboard-card__main),
  .prematch-console :deep(.scoreboard-card__main),
  .decision-board > :deep(.scoreboard-card__main) {
    grid-template-columns: minmax(0, 1fr) 84px minmax(0, 1fr) !important;
  }

  .match-card :deep(.scoreboard-card__team),
  .prematch-console :deep(.scoreboard-card__team),
  .decision-board > :deep(.scoreboard-card__team) {
    border: 0;
    padding: 0;
  }

  .match-card :deep(.scoreboard-card__team .flag-team),
  .prematch-console :deep(.scoreboard-card__team .flag-team),
  .decision-board > :deep(.scoreboard-card__team .flag-team) {
    align-items: center;
    flex-direction: column;
    gap: 5px;
  }

  .match-card :deep(.scoreboard-card__team--away .flag-team),
  .prematch-console :deep(.scoreboard-card__team--away .flag-team),
  .decision-board > :deep(.scoreboard-card__team--away .flag-team) {
    align-items: center;
    flex-direction: column;
  }

  .match-card :deep(.scoreboard-card__team--away),
  .prematch-console :deep(.scoreboard-card__team--away),
  .decision-board > :deep(.scoreboard-card__team--away) {
    justify-items: center;
    text-align: center;
  }

  .match-card :deep(.scoreboard-card__team .flag-team__copy),
  .prematch-console :deep(.scoreboard-card__team .flag-team__copy),
  .decision-board > :deep(.scoreboard-card__team .flag-team__copy) {
    justify-items: center;
    text-align: center;
    width: 100%;
  }

  .match-card :deep(.scoreboard-card__team .flag-team__copy strong),
  .prematch-console :deep(.scoreboard-card__team .flag-team__copy strong),
  .decision-board > :deep(.scoreboard-card__team .flag-team__copy strong) {
    display: -webkit-box;
    font-size: 12px;
    overflow: hidden;
    overflow-wrap: anywhere;
    text-overflow: ellipsis;
    white-space: normal;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
  }

  .match-card :deep(.scoreboard-card__score),
  .prematch-console :deep(.scoreboard-card__score),
  .decision-board > :deep(.scoreboard-card__score) {
    min-width: 0;
    padding: 7px 5px;
    width: 84px;
  }

  .match-card :deep(.scoreboard-card__score strong),
  .prematch-console :deep(.scoreboard-card__score strong),
  .decision-board > :deep(.scoreboard-card__score strong) {
    font-size: 18px;
  }

  .decision-board > :deep(.scoreboard-card__signals) {
    grid-template-columns: 1fr auto auto;
  }

  .decision-board > :deep(.scoreboard-card__signals .metric-bar small) {
    display: none;
  }

  .hero-actions,
  .panel-heading,
  .decision-board__heading,
  .info-card__title {
    align-items: stretch;
    flex-direction: column;
  }

  .action-link,
  .refresh-button,
  .clear-filter-button {
    width: 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .action-link,
  .refresh-button,
  .clear-filter-button,
  .directory-pager button,
  .match-card {
    transition: none;
  }
}
</style>
