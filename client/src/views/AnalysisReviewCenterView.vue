<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import CoverageDonut from '@/components/football/CoverageDonut.vue';
import MetricBar from '@/components/football/MetricBar.vue';
import ScoreboardCard from '@/components/football/ScoreboardCard.vue';
import {
  listPublicDecisionReports,
  listPublicDecisionReviews,
  type PublicDecisionLesson,
  type PublicDecisionReport,
  type PublicDecisionReview,
} from '@/api/analysisReview';
import { listPublicMatches, type PublicMatchSummary } from '@/api/matches';
import { enumLabel, readablePublicText } from '@/utils/display-labels';
import { scoreboardFallback, scoreTone, statusLabel } from '@/utils/football-visuals';

const loading = ref(false);
const error = ref('');
const reports = ref<PublicDecisionReport[]>([]);
const reviews = ref<PublicDecisionReview[]>([]);
const matches = ref<PublicMatchSummary[]>([]);
const selectedReportId = ref<number | null>(null);
const reportSearchQuery = ref('');
const selectedConclusionType = ref('');
const reportPage = ref(1);
const REPORT_PAGE_SIZE = 10;


const selectedReport = computed<PublicDecisionReport | null>(() => {
  if (reports.value.length === 0) {
    return null;
  }
  if (selectedReportId.value == null) {
    return reports.value[0];
  }
  return reports.value.find((report) => report.id === selectedReportId.value) ?? reports.value[0];
});

const selectedMatch = computed(() => {
  const selected = selectedReport.value;
  if (!selected) {
    return null;
  }
  return matches.value.find((match) => (
    (selected.matchId != null && match.id === selected.matchId)
    || (selected.matchName && match.matchName === selected.matchName)
  )) ?? null;
});

const relatedReviews = computed(() => {
  if (!selectedReport.value) {
    return reviews.value;
  }
  const selected = selectedReport.value;
  return reviews.value.filter((review) => (
    review.analysisReportId === selected.id
    || (selected.matchId != null && review.matchId === selected.matchId)
  ));
});

const lessons = computed<PublicDecisionLesson[]>(() => relatedReviews.value.flatMap((review) => review.lessons));

const emptyReviewMatch = computed<PublicMatchSummary | null>(() => (
  matches.value.find((match) => hasScore(match)) ?? matches.value[0] ?? null
));

const emptyReviewReadiness = computed(() => {
  const match = emptyReviewMatch.value;
  const scoreAnchor = match ? (hasScore(match) ? 100 : 45) : 0;
  const evidenceCover = Math.min(100, (match?.evidenceCount ?? 0) * 25);
  const conflictClarity = match ? Math.max(0, 100 - (match.conflictCount ?? 0) * 25) : 0;
  const reportState = reports.value.length ? 100 : 0;
  return Math.round(scoreAnchor * 0.35 + evidenceCover * 0.25 + conflictClarity * 0.15 + reportState * 0.25);
});

const emptyReviewBars = computed(() => {
  const match = emptyReviewMatch.value;
  const scoreAnchor = match ? (hasScore(match) ? 100 : 45) : 0;
  const evidenceCover = Math.min(100, (match?.evidenceCount ?? 0) * 25);
  const conflictClarity = match ? Math.max(0, 100 - (match.conflictCount ?? 0) * 25) : 0;
  const reportState = reports.value.length ? 100 : 0;
  return [
    { label: '赛果记录', value: scoreAnchor, tone: scoreAnchor >= 80 ? 'success' : scoreAnchor > 0 ? 'warning' : 'danger', caption: match && hasScore(match) ? '已有公开比分。' : '比分暂缺。' },
    { label: '证据基础', value: evidenceCover, tone: evidenceCover >= 60 ? 'success' : evidenceCover > 0 ? 'warning' : 'danger', caption: `当前候选比赛有 ${match?.evidenceCount ?? 0} 条公开证据。` },
    { label: '冲突清晰度', value: conflictClarity, tone: conflictClarity >= 80 ? 'success' : conflictClarity >= 50 ? 'warning' : 'danger', caption: match?.conflictCount ? `${match.conflictCount} 个公开冲突。` : '暂无公开冲突记录。' },
    { label: '复盘入库', value: reportState, tone: reportState ? 'success' : 'warning', caption: reportState ? '已有公开复盘报告。' : '复盘报告待管理员审核入库。' },
  ];
});

const emptyReviewRings = computed(() => [
  {
    label: '复盘资料度',
    value: emptyReviewReadiness.value,
    tone: scoreTone(emptyReviewReadiness.value),
    caption: '赛果、证据、冲突、复盘入库状态',
  },
  ...emptyReviewBars.value.map((bar) => ({
    label: bar.label,
    value: bar.value,
    tone: bar.tone,
    caption: bar.caption,
  })),
]);

const conclusionOptions = computed(() => (
  Array.from(new Set(reports.value.map((report) => report.conclusionType).filter(Boolean) as string[]))
    .map((value) => ({ value, label: decisionConclusionLabel(value) }))
));

const filteredReports = computed(() => {
  const query = reportSearchQuery.value.trim().toLowerCase();
  return reports.value.filter((report) => {
    if (selectedConclusionType.value && report.conclusionType !== selectedConclusionType.value) {
      return false;
    }
    if (!query) {
      return true;
    }
    const haystack = [
      report.matchName,
      report.matchday,
      report.jcCode,
      report.conclusionType,
      enumLabel('conclusionType', report.conclusionType, ''),
      report.confidence,
      report.riskSummary,
      report.reviewSummary,
      report.lessonSummary,
    ].filter(Boolean).join(' ').toLowerCase();
    return haystack.includes(query);
  });
});

const reportPageCount = computed(() => Math.max(1, Math.ceil(filteredReports.value.length / REPORT_PAGE_SIZE)));

const pagedReports = computed(() => {
  const start = (reportPage.value - 1) * REPORT_PAGE_SIZE;
  return filteredReports.value.slice(start, start + REPORT_PAGE_SIZE);
});

const reportFilterActive = computed(() => Boolean(reportSearchQuery.value.trim() || selectedConclusionType.value));

const stats = computed(() => ({
  reports: reports.value.length,
  reviews: reviews.value.length,
  matches: new Set([
    ...reports.value.map((report) => report.matchId ?? report.matchName),
    ...reviews.value.map((review) => review.matchId ?? review.matchName),
  ].filter(Boolean)).size,
  lessons: reviews.value.reduce((sum, review) => sum + review.lessons.length, 0),
}));

const decisionMaterialTotal = computed(() => stats.value.reports + stats.value.reviews + stats.value.lessons);

const decisionOverviewRings = computed(() => {
  const materialTotal = decisionMaterialTotal.value;
  const materialMax = Math.max(1, materialTotal, stats.value.matches);
  return [
    {
      label: '公开报告',
      value: stats.value.reports,
      max: materialMax,
      unit: '份',
      tone: stats.value.reports ? 'success' : 'info',
      caption: `${stats.value.reports} 份公开分析摘要`,
    },
    {
      label: '赛后复盘',
      value: stats.value.reviews,
      max: materialMax,
      unit: '篇',
      tone: stats.value.reviews ? 'success' : 'info',
      caption: `${stats.value.reviews} 篇公开赛后复盘`,
    },
    {
      label: '覆盖比赛',
      value: stats.value.matches,
      max: Math.max(1, stats.value.matches, stats.value.reports + stats.value.reviews),
      unit: '场',
      tone: stats.value.matches ? 'success' : 'info',
      caption: `${stats.value.matches} 场公开比赛`,
    },
    {
      label: '规则沉淀',
      value: stats.value.lessons,
      max: materialMax,
      unit: '条',
      tone: stats.value.lessons ? 'success' : 'info',
      caption: `${stats.value.lessons} 条公开规则`,
    },
    {
      label: '材料合计',
      value: materialTotal,
      max: Math.max(1, materialTotal),
      unit: '项',
      tone: materialTotal ? 'accent' : 'info',
      caption: `${materialTotal} 项报告、复盘和规则材料`,
    },
  ];
});

const reviewReadiness = computed(() => {
  const selected = selectedReport.value;
  if (!selected) {
    return 0;
  }
  let score = 0;
  if (selectedMatch.value && hasScore(selectedMatch.value)) {
    score += 20;
  }
  if (selected.riskSummary) {
    score += 20;
  }
  if (selected.reviewSummary) {
    score += 20;
  }
  if (selected.lessonSummary) {
    score += 15;
  }
  if (relatedReviews.value.length > 0) {
    score += 15;
  }
  if (lessons.value.length > 0) {
    score += 10;
  }
  return Math.min(100, score);
});

const readinessTone = computed(() => scoreTone(reviewReadiness.value));
const readabilityVerdict = computed(() => {
  if (reviewReadiness.value >= 80) {
    return '复盘上下文资料较齐';
  }
  if (reviewReadiness.value >= 55) {
    return '复盘基础资料可读';
  }
  return '复盘信息偏薄弱';
});

const selectedScoreAnchor = computed(() => {
  const match = selectedMatch.value;
  if (!match) {
    return 0;
  }
  return hasScore(match) ? 100 : 45;
});

const selectedSummaryCount = computed(() => {
  const selected = selectedReport.value;
  if (!selected) {
    return 0;
  }
  return [selected.riskSummary, selected.reviewSummary, selected.lessonSummary].filter(Boolean).length;
});

const selectedSummaryCoverage = computed(() => Math.round((selectedSummaryCount.value / 3) * 100));
const selectedRuleCoverage = computed(() => Math.min(100, relatedReviews.value.length * 45 + lessons.value.length * 25));
const selectedScoreboard = computed(() => (
  selectedMatch.value ? scoreboardFallback(selectedMatch.value as unknown as Record<string, unknown>) : null
));

const reviewReadinessRings = computed(() => [
  {
    label: '复盘可读性',
    value: reviewReadiness.value,
    tone: readinessTone.value,
    caption: '比分、风险、复盘、规则覆盖',
  },
  {
    label: '赛果记录',
    value: selectedScoreAnchor.value,
    tone: scoreTone(selectedScoreAnchor.value),
    caption: selectedScoreboard.value?.scoreDisplay || '比分上下文待匹配',
  },
  {
    label: '摘要覆盖',
    value: selectedSummaryCoverage.value,
    tone: scoreTone(selectedSummaryCoverage.value),
    caption: `${selectedSummaryCount.value} / 3 类摘要`,
  },
  {
    label: '规则记录',
    value: selectedRuleCoverage.value,
    tone: scoreTone(selectedRuleCoverage.value),
    caption: `${relatedReviews.value.length} 篇复盘 · ${lessons.value.length} 条规则`,
  },
]);

const relatedReviewRings = computed(() => {
  const reviewsTotal = relatedReviews.value.length;
  const reviewMax = Math.max(1, reviewsTotal, 3);
  const lessonTotal = lessons.value.length;
  const lessonMax = Math.max(1, lessonTotal, 5);
  const totalSummaryCount = relatedReviews.value.filter((review) => Boolean(review.overallSummary)).length;
  const modelSummaryCount = relatedReviews.value.filter((review) => Boolean(review.mathSummary)).length;
  const marketSummaryCount = relatedReviews.value.filter((review) => Boolean(review.handicapSummary || review.oddsValueSummary)).length;
  return [
    {
      label: '复盘记录',
      value: reviewsTotal,
      max: reviewMax,
      unit: '篇',
      tone: reviewsTotal ? 'success' : 'warning',
      caption: `${reviewsTotal} 篇相关赛后复盘`,
    },
    {
      label: '规则条目',
      value: lessonTotal,
      max: lessonMax,
      unit: '条',
      tone: lessonTotal ? 'success' : 'info',
      caption: `${lessonTotal} 条公开规则沉淀`,
    },
    {
      label: '总评覆盖',
      value: totalSummaryCount,
      max: Math.max(1, reviewsTotal),
      unit: '篇',
      tone: totalSummaryCount === reviewsTotal && reviewsTotal ? 'success' : totalSummaryCount ? 'warning' : 'info',
      caption: `${totalSummaryCount} / ${reviewsTotal} 篇总评摘要`,
    },
    {
      label: '模型校验',
      value: modelSummaryCount,
      max: Math.max(1, reviewsTotal),
      unit: '篇',
      tone: modelSummaryCount === reviewsTotal && reviewsTotal ? 'success' : modelSummaryCount ? 'warning' : 'info',
      caption: `${modelSummaryCount} / ${reviewsTotal} 篇模型摘要`,
    },
    {
      label: '市场摘要',
      value: marketSummaryCount,
      max: Math.max(1, reviewsTotal),
      unit: '篇',
      tone: marketSummaryCount === reviewsTotal && reviewsTotal ? 'success' : marketSummaryCount ? 'warning' : 'info',
      caption: `${marketSummaryCount} / ${reviewsTotal} 篇市场变化摘要`,
    },
  ];
});

const lessonSeverityRings = computed(() => {
  const lessonTotal = lessons.value.length;
  const max = Math.max(1, lessonTotal);
  const highCount = lessons.value.filter((lesson) => ['CRITICAL', 'HIGH'].includes((lesson.severity || '').toUpperCase())).length;
  const mediumCount = lessons.value.filter((lesson) => (lesson.severity || '').toUpperCase() === 'MEDIUM').length;
  const lowCount = lessons.value.filter((lesson) => ['LOW', 'INFO'].includes((lesson.severity || '').toUpperCase())).length;
  const typeCount = new Set(lessons.value.map((lesson) => lesson.lessonType).filter(Boolean)).size;
  return [
    {
      label: '规则总数',
      value: lessonTotal,
      max: Math.max(1, lessonTotal, 5),
      unit: '条',
      tone: lessonTotal ? 'success' : 'info',
      caption: `${lessonTotal} 条公开规则`,
    },
    {
      label: '高等级规则',
      value: highCount,
      max,
      unit: '条',
      tone: highCount ? 'danger' : 'success',
      caption: `${highCount} / ${lessonTotal} 条规则`,
    },
    {
      label: '中等级规则',
      value: mediumCount,
      max,
      unit: '条',
      tone: mediumCount ? 'warning' : 'success',
      caption: `${mediumCount} / ${lessonTotal} 条规则`,
    },
    {
      label: '低等级规则',
      value: lowCount,
      max,
      unit: '条',
      tone: lowCount ? 'success' : 'info',
      caption: `${lowCount} / ${lessonTotal} 条规则`,
    },
    {
      label: '类型覆盖',
      value: typeCount,
      max: Math.max(1, typeCount, 5),
      unit: '类',
      tone: typeCount >= 3 ? 'success' : typeCount ? 'warning' : 'info',
      caption: `${typeCount} 类公开规则类型`,
    },
  ];
});


function formatDate(value?: string): string {
  return value ? value.replace('T', ' ').slice(0, 10) : '日期待同步';
}

const publicSensitivePattern = /\b(?:ticketNo|ticket|stakeSuggestion|stake|betPlan|rawPayload|profitLoss|profit|loss|budgetAmount|returnAmount|ROI|CLV)\b|票号|票据|投入|返还|盈亏|预算|资金|收益|下注|金额建议|原始\s*JSON/gi;
const publicProfessionalPattern = /置信度|数学层|盘口层|赔率价值层|价值层/gi;

function confidenceLabel(value?: string | null): string {
  switch ((value || '').toUpperCase()) {
    case 'HIGH':
      return '较高';
    case 'MEDIUM':
      return '中等';
    case 'LOW':
      return '偏低';
    default:
      return value || '待同步';
  }
}

function decisionConclusionLabel(value?: string | null): string {
  const label = enumLabel('conclusionType', value, '判断待同步');
  if (label === '不建议介入') {
    return '仅观察';
  }
  return label;
}

function publicText(value?: string, fallback = '暂无公开摘要。'): string {
  const text = value?.trim();
  if (!text) {
    return fallback;
  }
  return readablePublicText(
    text
      .replace(publicSensitivePattern, '已脱敏指标')
      .replace(publicProfessionalPattern, (match) => {
        if (match === '置信度') {
          return '把握程度';
        }
        if (match === '数学层') {
          return '模型校验';
        }
        if (match === '盘口层') {
          return '市场变化';
        }
        return '市场价值';
      }),
    fallback,
  );
}

function severityClass(severity?: string): string {
  switch (severity) {
    case 'HIGH':
    case 'CRITICAL':
      return 'severity-pill--danger';
    case 'MEDIUM':
      return 'severity-pill--warning';
    case 'LOW':
      return 'severity-pill--success';
    default:
      return 'severity-pill--info';
  }
}

function selectReport(report: PublicDecisionReport) {
  selectedReportId.value = report.id;
}

function resetReportFilters() {
  reportSearchQuery.value = '';
  selectedConclusionType.value = '';
  reportPage.value = 1;
}

function goReportPage(direction: 1 | -1) {
  reportPage.value = Math.min(reportPageCount.value, Math.max(1, reportPage.value + direction));
}

function hasScore(match: PublicMatchSummary): boolean {
  const scoreboard = scoreboardFallback(match as unknown as Record<string, unknown>);
  return scoreboard.homeScore != null && scoreboard.awayScore != null;
}

function matchMeta(match?: PublicMatchSummary | null): string {
  if (!match) {
    return selectedReport.value
      ? [selectedReport.value.matchday, selectedReport.value.jcCode ? `竞彩 ${selectedReport.value.jcCode}` : ''].filter(Boolean).join(' · ') || '赛程待同步'
      : '赛程待同步';
  }
  return [match.matchday, match.jcCode ? `竞彩 ${match.jcCode}` : '', match.venue].filter(Boolean).join(' · ') || '赛程待同步';
}

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const [reportsResponse, reviewsResponse, matchesResponse] = await Promise.all([
      listPublicDecisionReports(),
      listPublicDecisionReviews(),
      listPublicMatches(),
    ]);
    reports.value = reportsResponse.data;
    reviews.value = reviewsResponse.data;
    matches.value = matchesResponse.data;
    selectedReportId.value = reports.value[0]?.id ?? null;
  } catch (cause) {
    reports.value = [];
    reviews.value = [];
    matches.value = [];
    selectedReportId.value = null;
    error.value = cause instanceof Error ? cause.message : '无法读取公开决策复盘数据。';
  } finally {
    loading.value = false;
  }
}

watch([reportSearchQuery, selectedConclusionType], () => {
  reportPage.value = 1;
});

watch(filteredReports, (currentReports) => {
  if (reportPage.value > reportPageCount.value) {
    reportPage.value = reportPageCount.value;
  }
  if (!currentReports.length) {
    return;
  }
  if (selectedReportId.value != null && currentReports.some((report) => report.id === selectedReportId.value)) {
    return;
  }
  selectedReportId.value = currentReports[0].id;
});

onMounted(load);
</script>

<template>
  <section class="page-shell decisions-page" aria-labelledby="decisions-title">
    <section class="page-content decisions-page__content">
      <header class="decisions-hero">
        <div>
          <p class="eyebrow">决策 · 复盘</p>
          <h1 id="decisions-title">决策复盘中心</h1>
        </div>
        <button class="action-button" type="button" :disabled="loading" @click="load">
          {{ loading ? '刷新中' : '刷新公开数据' }}
        </button>
      </header>

      <section v-if="selectedReport" class="decision-console" aria-label="复盘状态面板" data-test="decision-console" tabindex="0">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">重点</p>
            <h2>比赛结果与复盘结论</h2>
          </div>
          <span class="status-pill">{{ readabilityVerdict }}</span>
        </div>

        <div class="decision-console__grid">
          <ScoreboardCard
            v-if="selectedMatch"
            class="decision-scoreboard"
            :home-team="selectedMatch.homeTeam"
            :away-team="selectedMatch.awayTeam"
            :scoreboard="selectedMatch.scoreboard"
            :match-name="selectedMatch.matchName"
            :meta="matchMeta(selectedMatch)"
            :status="statusLabel(selectedMatch.status || selectedMatch.resultStatus)"
            :evidence-count="selectedMatch.evidenceCount"
          />
          <article v-else class="match-fallback-card" aria-label="比赛比分上下文待同步">
            <span>{{ selectedReport.jcCode ? `竞彩 ${selectedReport.jcCode}` : '竞彩待定' }} · {{ formatDate(selectedReport.matchday) }}</span>
            <strong>{{ selectedReport.matchName || '比赛待同步' }}</strong>
            <p>比赛中心暂未匹配比分与队伍展示数据。</p>
          </article>

          <article class="readiness-panel">
            <header>
              <span>复盘可读性</span>
              <strong>{{ reviewReadiness }}%</strong>
            </header>
            <div class="decision-ring-grid" aria-label="复盘资料环形图" tabindex="0">
              <CoverageDonut
                v-for="ring in reviewReadinessRings"
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
              label="复盘上下文"
              :value="reviewReadiness"
              unit="%"
              :tone="readinessTone"
              caption="比分资料、风险摘要、赛后复盘和规则记录覆盖"
            />
          </article>
        </div>
      </section>
      <section v-else-if="!loading" class="decision-console decision-console--empty" aria-label="暂无复盘状态" data-test="decision-empty-state" tabindex="0">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">暂无公开复盘</p>
            <h2>公开复盘数据待入库</h2>
          </div>
          <span class="status-pill">候选比赛与证据状态</span>
        </div>
        <div class="decision-empty-console-grid" tabindex="0">
          <ScoreboardCard
            v-if="emptyReviewMatch"
            class="decision-scoreboard"
            :home-team="emptyReviewMatch.homeTeam"
            :away-team="emptyReviewMatch.awayTeam"
            :scoreboard="emptyReviewMatch.scoreboard"
            :match-name="emptyReviewMatch.matchName"
            :meta="matchMeta(emptyReviewMatch)"
            :status="statusLabel(emptyReviewMatch.status || emptyReviewMatch.resultStatus)"
            :integrity-score="emptyReviewReadiness"
            :evidence-count="emptyReviewMatch.evidenceCount"
          />
          <article v-else class="match-fallback-card" aria-label="暂无可复盘比赛">
            <span>比赛待同步</span>
            <strong>暂无可复盘比赛</strong>
            <p>当前暂无可展示的公开比分和队伍信息。</p>
          </article>

          <article class="readiness-panel empty-readiness-panel" data-test="decision-empty-readiness">
            <header>
              <span>复盘资料度</span>
              <strong>{{ emptyReviewReadiness }}%</strong>
            </header>
            <div class="decision-ring-grid" aria-label="复盘资料状态环形图" tabindex="0">
              <CoverageDonut
                v-for="ring in emptyReviewRings"
                :key="ring.label"
                :label="ring.label"
                :value="ring.value"
                unit="%"
                :tone="ring.tone"
                size="compact"
                :caption="ring.caption"
              />
            </div>
            <div class="empty-readiness-bars" aria-label="复盘资料分项" tabindex="0">
              <MetricBar
                v-for="bar in emptyReviewBars"
                :key="bar.label"
                :label="bar.label"
                :value="bar.value"
                unit="%"
                :tone="bar.tone"
                :caption="bar.caption"
              />
            </div>
          </article>

          <div class="decision-empty-actions" aria-label="复盘相关入口">
            <RouterLink class="empty-action-link" to="/evidence/matches">打开比赛中心</RouterLink>
            <RouterLink class="empty-action-link" to="/evidence">打开证据中心</RouterLink>
          </div>
        </div>
      </section>
      <section class="stat-grid" aria-label="决策复盘统计">
        <article class="stat-card"><span>报告</span><strong>{{ stats.reports }}</strong><small>公开分析摘要</small></article>
        <article class="stat-card"><span>复盘</span><strong>{{ stats.reviews }}</strong><small>赛后总结</small></article>
        <article class="stat-card"><span>比赛</span><strong>{{ stats.matches }}</strong><small>覆盖场次</small></article>
        <article class="stat-card"><span>规则</span><strong>{{ stats.lessons }}</strong><small>经验沉淀</small></article>
      </section>

      <section class="decision-overview-panel" data-test="decision-overview-rings" aria-label="决策复盘材料结构" tabindex="0">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">材料结构</p>
            <h2>公开复盘材料结构</h2>
          </div>
          <span class="count-pill">{{ decisionMaterialTotal }} 项</span>
        </div>
        <div class="decision-overview-rings" aria-label="公开复盘材料结构环形图" tabindex="0">
          <CoverageDonut
            v-for="ring in decisionOverviewRings"
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

      <div v-if="error" class="alert-panel" role="alert">{{ error }}</div>

      <section class="decisions-grid" tabindex="0">
        <aside class="side-panel" aria-label="公开分析报告" tabindex="0">
          <div class="panel-heading">
            <div><p class="eyebrow">报告</p><h2>分析报告</h2></div>
            <span class="count-pill">{{ filteredReports.length }} / {{ reports.length }}</span>
          </div>

          <div class="report-filters" aria-label="公开报告筛选">
            <label class="filter-field">
              <span>搜索报告</span>
              <input
                v-model="reportSearchQuery"
                type="search"
                aria-label="搜索比赛、竞彩编号、判断或风险摘要"
                placeholder="搜球队、竞彩编号、风险..."
              />
            </label>
            <label class="filter-field">
              <span>赛前判断</span>
              <select v-model="selectedConclusionType" aria-label="按赛前判断筛选报告">
                <option value="">全部判断</option>
                <option v-for="option in conclusionOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </label>
            <div class="filter-summary">
              <span>已筛出 {{ filteredReports.length }} / {{ reports.length }} 份报告</span>
              <button v-if="reportFilterActive" class="ghost-button" type="button" @click="resetReportFilters">清除筛选</button>
            </div>
          </div>

          <p v-if="loading && !reports.length" class="empty-copy">正在加载公开报告...</p>
          <p v-else-if="!reports.length" class="empty-copy">暂无公开分析报告。</p>
          <div v-else-if="pagedReports.length" class="report-list-scroll" tabindex="0" aria-label="筛选后的公开分析报告">
            <button
              v-for="report in pagedReports"
              :key="report.id"
              class="list-card"
              :class="{ 'list-card--active': report.id === selectedReport?.id }"
              type="button"
              @click="selectReport(report)"
            >
              <span>{{ report.jcCode ? `竞彩 ${report.jcCode}` : '竞彩待定' }} · {{ formatDate(report.matchday) }}</span>
              <strong>{{ report.matchName || '比赛待同步' }}</strong>
              <small>{{ decisionConclusionLabel(report.conclusionType) }} · 把握程度 {{ confidenceLabel(report.confidence) }}</small>
              <small>{{ publicText(report.riskSummary, '暂无风险摘要') }}</small>
            </button>
          </div>
          <div v-else class="empty-filter-state">
            <strong>没有找到匹配报告</strong>
            <span>当前筛选条件无匹配报告。</span>
            <button class="ghost-button" type="button" @click="resetReportFilters">清除筛选</button>
          </div>

          <div v-if="filteredReports.length > REPORT_PAGE_SIZE" class="report-pager" aria-label="报告分页">
            <button type="button" :disabled="reportPage <= 1" @click="goReportPage(-1)">上一页</button>
            <span>第 {{ reportPage }} / {{ reportPageCount }} 页</span>
            <button type="button" :disabled="reportPage >= reportPageCount" @click="goReportPage(1)">下一页</button>
          </div>
        </aside>

        <article class="detail-panel" tabindex="0">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">复盘详情</p>
              <h2>{{ selectedReport?.matchName || '公开复盘详情' }}</h2>
            </div>
            <span v-if="selectedReport" class="status-pill">{{ decisionConclusionLabel(selectedReport.conclusionType) }}</span>
          </div>

          <p v-if="!selectedReport && !loading" class="empty-copy">当前未选中公开报告。</p>
          <template v-else-if="selectedReport">
            <section class="summary-grid" aria-label="报告摘要">
              <div><span>比赛编号</span><strong>{{ selectedReport.jcCode || '-' }}</strong></div>
              <div><span>比赛日期</span><strong>{{ formatDate(selectedReport.matchday) }}</strong></div>
              <div><span>比分状态</span><strong>{{ selectedMatch ? scoreboardFallback(selectedMatch as unknown as Record<string, unknown>).scoreDisplay : '待匹配' }}</strong></div>
              <div><span>赛前判断</span><strong>{{ decisionConclusionLabel(selectedReport.conclusionType) }}</strong></div>
              <div><span>把握程度</span><strong>{{ confidenceLabel(selectedReport.confidence) }}</strong></div>
            </section>

            <section class="card-grid" tabindex="0">
              <article class="info-card" tabindex="0">
                <p class="eyebrow">风险</p>
                <h3>风险摘要</h3>
                <p>{{ publicText(selectedReport.riskSummary) }}</p>
              </article>
              <article class="info-card" tabindex="0">
                <p class="eyebrow">复盘</p>
                <h3>复盘摘要</h3>
                <p>{{ publicText(selectedReport.reviewSummary) }}</p>
              </article>
              <article class="info-card info-card--wide" tabindex="0">
                <p class="eyebrow">规则摘要</p>
                <h3>规则沉淀摘要</h3>
                <p>{{ publicText(selectedReport.lessonSummary) }}</p>
              </article>
            </section>

            <section class="info-card" tabindex="0">
              <div class="panel-heading">
                <div><p class="eyebrow">赛后复盘</p><h3>相关赛后复盘</h3></div>
                <span class="count-pill">{{ relatedReviews.length }}</span>
              </div>
              <div class="review-structure-rings" data-test="review-structure-rings" aria-label="相关赛后复盘资料环形图" tabindex="0">
                <CoverageDonut
                  v-for="ring in relatedReviewRings"
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
              <div v-if="relatedReviews.length" class="review-grid" tabindex="0">
                <article v-for="review in relatedReviews" :key="review.id" class="review-card">
                  <span>{{ review.reviewKey }} · {{ formatDate(review.matchday) }}</span>
                  <strong>{{ review.title }}</strong>
                  <p>{{ publicText(review.overallSummary, '暂无总评。') }}</p>
                  <div class="review-layers">
                    <small>{{ publicText(review.mathSummary, '模型校验待同步') }}</small>
                    <small>{{ publicText(review.footballSummary, '足球层待同步') }}</small>
                    <small>{{ publicText(review.handicapSummary, '市场变化待同步') }}</small>
                    <small>{{ publicText(review.tournamentTemperamentSummary, '大赛气质层待同步') }}</small>
                    <small>{{ publicText(review.oddsValueSummary, '市场价值待同步') }}</small>
                  </div>
                </article>
              </div>
              <p v-else class="empty-copy">暂无相关赛后复盘。</p>
            </section>

            <section class="info-card" tabindex="0">
              <div class="panel-heading">
                <div><p class="eyebrow">规则</p><h3>规则沉淀</h3></div>
                <span class="count-pill">{{ lessons.length }}</span>
              </div>
              <div class="lesson-structure-rings" data-test="lesson-structure-rings" aria-label="规则沉淀严重度环形图" tabindex="0">
                <CoverageDonut
                  v-for="ring in lessonSeverityRings"
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
              <div v-if="lessons.length" class="lesson-grid" tabindex="0">
                <article v-for="lesson in lessons" :key="lesson.id" class="lesson-card">
                  <span class="severity-pill" :class="severityClass(lesson.severity)">{{ enumLabel('severity', lesson.severity) }}</span>
                  <strong>{{ enumLabel('lessonType', lesson.lessonType) }}</strong>
                  <p>{{ publicText(lesson.lessonText, '暂无规则描述。') }}</p>
                </article>
              </div>
              <p v-else class="empty-copy">暂无规则沉淀。</p>
            </section>
          </template>
        </article>
      </section>
    </section>
  </section>
</template>

<style scoped>
.decisions-page {
  max-width: 100%;
  overflow-x: hidden;
}
.decisions-page__content {
  display: grid;
  gap: 18px;
  min-width: 0;
}
.decisions-hero,
.decision-console,
.decision-overview-panel,
.stat-card,
.side-panel,
.detail-panel,
.info-card,
.match-fallback-card,
.readiness-panel,
.summary-grid div,
.alert-panel,
.empty-filter-state {
  background: var(--wc-glass);
  border: 1px solid var(--wc-border);
  border-radius: var(--wc-radius-lg);
  color: var(--wc-text);
}
.decisions-hero {
  align-items: center;
  display: grid;
  gap: 18px;
  grid-template-columns: minmax(0, 1fr) auto;
  padding: clamp(20px, 4vw, 38px);
}
.decisions-hero h1 {
  font-family: var(--wc-font-display);
  font-size: clamp(34px, 6vw, 68px);
  line-height: 1;
  margin: 0 0 12px;
}
.decisions-hero p:not(.eyebrow),
.empty-copy,
.list-card span,
.list-card small,
.summary-grid span,
.info-card p,
.review-card span,
.review-card p,
.review-layers small,
.lesson-card p {
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

.action-button:focus-visible,
.list-card:focus-visible,
.filter-field input:focus-visible,
.filter-field select:focus-visible,
	.ghost-button:focus-visible,
	.report-pager button:focus-visible,
	.report-list-scroll:focus-visible,
	.decision-console:focus-visible,
	.decision-empty-console-grid:focus-visible,
	.decision-ring-grid:focus-visible,
	.empty-readiness-bars:focus-visible,
	.decision-overview-panel:focus-visible,
	.decision-overview-rings:focus-visible,
	.decisions-grid:focus-visible,
	.side-panel:focus-visible,
	.detail-panel:focus-visible,
	.card-grid:focus-visible,
	.review-structure-rings:focus-visible,
	.review-grid:focus-visible,
	.lesson-structure-rings:focus-visible,
	.lesson-grid:focus-visible,
	.info-card:focus-visible {
	  box-shadow: var(--wc-focus-ring);
	  outline: none;
	}

	.decision-console {
	  display: grid;
	  gap: 16px;
	  padding: 18px;
	}

	.decision-overview-panel {
	  display: grid;
	  gap: 14px;
	  padding: 18px;
	}

	.decision-overview-rings {
	  display: grid;
	  gap: 12px;
	  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
	  max-height: 360px;
	  min-width: 0;
	  overflow: auto;
	  padding-right: 4px;
	  scrollbar-color: rgba(147, 197, 253, .38) transparent;
	}

.decision-console__grid {
  display: grid;
  gap: 16px;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, .95fr);
  min-width: 0;
}

.match-fallback-card,
.readiness-panel {
  display: grid;
  gap: 12px;
  min-width: 0;
  padding: 18px;
}
.info-card {
  max-height: 620px;
  overflow: auto;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}

.match-fallback-card span,
.readiness-panel header span,
.filter-field span,
.filter-summary,
.empty-filter-state span,
.report-pager span {
  color: var(--wc-text-muted);
}

.match-fallback-card > span,
.readiness-panel header span,
.filter-field span {
  font-family: var(--wc-font-mono);
  font-size: 12px;
  font-weight: 800;
}

.match-fallback-card strong {
  color: var(--wc-warning);
  font-family: var(--wc-font-display);
  font-size: clamp(28px, 4vw, 44px);
  line-height: 1;
}

.match-fallback-card p {
  line-height: 1.6;
  margin: 0;
}

.readiness-panel {
  background:
    radial-gradient(circle at top right, rgba(34, 211, 238, .14), transparent 42%),
    rgba(15, 23, 42, .68);
}

.readiness-panel header {
  align-items: end;
  display: flex;
  gap: 12px;
  justify-content: space-between;
}

.readiness-panel header strong {
  color: var(--wc-warning);
  font-family: var(--wc-font-display);
  font-size: clamp(34px, 6vw, 58px);
  line-height: .92;
}

	.decision-ring-grid {
	  display: grid;
	  gap: 12px;
	  grid-template-columns: repeat(auto-fit, minmax(176px, 1fr));
	  min-width: 0;
	}




	.decision-empty-console-grid {
	  align-items: start;
	  display: grid;
	  gap: 16px;
	  grid-template-columns: minmax(0, .9fr) minmax(0, 1.35fr) minmax(0, .55fr);
	  min-width: 0;
	}

.decision-scoreboard {
  align-self: start;
  height: auto;
}

.empty-readiness-panel .decision-ring-grid {
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
}

	.empty-readiness-bars {
	  display: grid;
	  gap: 12px;
	  grid-template-columns: repeat(2, minmax(0, 1fr));
	  max-height: 260px;
	  min-width: 0;
	  overflow: auto;
	  padding-right: 4px;
	  scrollbar-color: rgba(147, 197, 253, .38) transparent;
	}













.decision-empty-actions {
  align-content: start;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.empty-action-link {
  align-items: center;
  background: rgba(217, 119, 6, .18);
  border: 1px solid rgba(217, 119, 6, .36);
  border-radius: 999px;
  color: var(--wc-warning);
  display: inline-flex;
  font-weight: 800;
  justify-content: center;
  justify-self: start;
  min-height: 44px;
  padding: 0 14px;
  text-decoration: none;
}

.empty-action-link:focus-visible {
  box-shadow: var(--wc-focus-ring);
  outline: none;
}

.stat-grid,
.summary-grid {
  display: grid;
  gap: 14px;
}

.stat-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.summary-grid {
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
}

.stat-card,
.side-panel,
.detail-panel,
.info-card,
.summary-grid div,
.alert-panel,
.empty-filter-state {
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
.decisions-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: minmax(0, 350px) minmax(0, 1fr);
  max-height: min(74dvh, 640px);
  min-width: 0;
  overflow: auto;
  padding-right: 4px;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}
.panel-heading {
  align-items: center;
  display: flex;
  gap: 12px;
  justify-content: space-between;
}
.panel-heading h2,
.panel-heading h3,
.info-card h3 {
  margin: 0;
}

.report-filters {
  display: grid;
  gap: 10px;
}

.filter-field {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.filter-field input,
.filter-field select {
  background: rgba(2, 6, 23, .56);
  border: 1px solid rgba(147, 197, 253, .18);
  border-radius: 14px;
  color: var(--wc-text);
  min-height: 44px;
  padding: 0 12px;
  width: 100%;
}

.filter-summary {
  align-items: center;
  display: flex;
  font-size: 13px;
  gap: 8px;
  justify-content: space-between;
}

.ghost-button,
.report-pager button {
  background: rgba(147, 197, 253, .1);
  border: 1px solid rgba(147, 197, 253, .24);
  border-radius: 999px;
  color: var(--wc-primary);
  cursor: pointer;
  font-weight: 800;
  min-height: 44px;
  padding: 0 12px;
}

.report-pager button:disabled {
  cursor: not-allowed;
  opacity: .48;
}

.report-list-scroll {
  display: grid;
  gap: 10px;
  max-height: 720px;
  min-width: 0;
  overflow: auto;
  padding-right: 4px;
}

.side-panel,
.detail-panel {
  max-height: min(72dvh, 620px);
  overflow: auto;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}

.list-card,
.review-card,
.lesson-card {
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
  min-height: 92px;
  transition: border-color 180ms ease, transform 180ms ease;
}

.list-card:hover {
  border-color: rgba(147, 197, 253, .44);
  transform: translateY(-1px);
}

.list-card--active {
  border-color: rgba(217, 119, 6, .62);
}

.empty-filter-state {
  background: rgba(15, 23, 42, .58);
  justify-items: start;
}

.report-pager {
  align-items: center;
  display: flex;
  gap: 10px;
  justify-content: space-between;
}

.count-pill,
.status-pill,
.severity-pill {
  border-radius: 999px;
  font-family: var(--wc-font-mono);
  font-size: 12px;
  font-weight: 800;
  padding: 6px 9px;
}
.count-pill,
.status-pill {
  background: rgba(147, 197, 253, .12);
  color: var(--wc-primary);
}
.card-grid,
.review-grid,
.lesson-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  max-height: 560px;
  min-width: 0;
  overflow: auto;
  padding-right: 4px;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}
.info-card--wide {
  grid-column: 1 / -1;
}
.review-layers {
  display: grid;
  gap: 6px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  min-width: 0;
}
.review-structure-rings {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  max-height: 360px;
  min-width: 0;
  overflow: auto;
  padding-right: 4px;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}
.lesson-structure-rings {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  max-height: 360px;
  min-width: 0;
  overflow: auto;
  padding-right: 4px;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}
.severity-pill--danger {
  background: rgba(239, 68, 68, .16);
  color: #fecaca;
}
.severity-pill--warning {
  background: rgba(245, 158, 11, .18);
  color: #fde68a;
}
.severity-pill--success {
  background: rgba(34, 197, 94, .16);
  color: #bbf7d0;
}
.severity-pill--info {
  background: rgba(147, 197, 253, .12);
  color: var(--wc-primary);
}
@media (max-width: 1024px) {
  .decisions-hero,
  .decision-console__grid,
  .decision-empty-console-grid,
    .decisions-grid,
  .summary-grid {
    grid-template-columns: 1fr;
  }
  .stat-grid,
  .card-grid,
  .review-grid,
  .lesson-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 640px) {
  .decisions-hero,
  .decision-console__grid,
  .decision-empty-console-grid,
  .decisions-grid,
  .summary-grid,
  .card-grid,
  .review-grid,
  .lesson-grid,
  .review-layers {
    grid-template-columns: 1fr;
  }
  .stat-grid,
  .decision-ring-grid,
  .decision-overview-rings,
  .empty-readiness-bars,
  .review-structure-rings,
  .lesson-structure-rings {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .panel-heading {
    align-items: stretch;
    flex-direction: column;
  }
  .decisions-hero,
  .decision-console,
  .decision-overview-panel,
  .stat-card,
  .side-panel,
  .detail-panel,
  .info-card,
  .match-fallback-card,
  .readiness-panel,
  .summary-grid div,
  .empty-filter-state {
    border-radius: var(--wc-radius-md);
    gap: 10px;
    padding: 12px;
  }
  .decisions-hero h1 {
    font-size: clamp(28px, 12vw, 42px);
    margin-bottom: 0;
  }
  .decisions-hero p:not(.eyebrow),
  .match-fallback-card p,
  .readiness-panel :deep(.metric-bar small),
  .empty-readiness-bars :deep(.metric-bar small),
  .review-card p,
  .review-layers,
  .lesson-card p {
    display: none;
  }
  .review-structure-rings,
  .lesson-structure-rings {
    max-height: min(36dvh, 280px);
    overflow: auto;
    padding-right: 4px;
    scrollbar-color: rgba(147, 197, 253, .38) transparent;
  }

  .decision-ring-grid,
  .decision-overview-rings {
    padding-right: 0;
  }

  .empty-readiness-bars {
    max-height: none;
    overflow: visible;
    padding-right: 0;
    scrollbar-gutter: auto;
  }
  .stat-grid {
    max-height: none;
    overflow: visible;
    scrollbar-gutter: auto;
  }
  .decisions-grid {
    max-height: none;
    overflow: visible;
    padding-right: 0;
    scrollbar-gutter: auto;
  }
  .side-panel,
  .detail-panel {
    max-height: none;
    overflow: visible;
    scrollbar-gutter: auto;
  }
  .card-grid,
  .review-grid,
  .lesson-grid {
    max-height: none;
    overflow: visible;
    padding-right: 0;
    scrollbar-gutter: auto;
  }
  .decision-ring-grid :deep(.coverage-donut),
  .decision-overview-rings :deep(.coverage-donut),
  .review-structure-rings :deep(.coverage-donut),
  .lesson-structure-rings :deep(.coverage-donut) {
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
  .decision-ring-grid :deep(.coverage-donut__ring),
  .decision-overview-rings :deep(.coverage-donut__ring),
  .review-structure-rings :deep(.coverage-donut__ring),
  .lesson-structure-rings :deep(.coverage-donut__ring) {
    width: 66px;
  }
  .decision-ring-grid :deep(.coverage-donut__ring span),
  .decision-overview-rings :deep(.coverage-donut__ring span),
  .review-structure-rings :deep(.coverage-donut__ring span),
  .lesson-structure-rings :deep(.coverage-donut__ring span) {
    font-size: 20px;
  }
  .decision-ring-grid :deep(.coverage-donut__copy small),
  .decision-overview-rings :deep(.coverage-donut__copy small),
  .review-structure-rings :deep(.coverage-donut__copy small),
  .lesson-structure-rings :deep(.coverage-donut__copy small) {
    display: none;
  }
  .decision-scoreboard:deep(.scoreboard-card__main) {
    grid-template-columns: minmax(0, 1fr) 84px minmax(0, 1fr) !important;
  }
  .decision-scoreboard:deep(.scoreboard-card__team) {
    border: 0;
    padding: 0;
  }
  .decision-scoreboard:deep(.scoreboard-card__team .flag-team) {
    align-items: center;
    flex-direction: column;
    gap: 5px;
  }
  .decision-scoreboard:deep(.scoreboard-card__team--away) {
    justify-items: center;
    text-align: center;
  }
  .decision-scoreboard:deep(.scoreboard-card__team .flag-team__copy) {
    justify-items: center;
    text-align: center;
    width: 100%;
  }
  .decision-scoreboard:deep(.scoreboard-card__team .flag-team__copy strong) {
    font-size: 12px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .decision-scoreboard:deep(.scoreboard-card__score) {
    min-width: 0;
    padding: 7px 5px;
    width: 84px;
  }
  .decision-scoreboard:deep(.scoreboard-card__score strong) {
    font-size: 18px;
  }
  .decision-scoreboard:deep(.scoreboard-card__signals) {
    grid-template-columns: 1fr auto auto;
  }
  .decision-scoreboard:deep(.scoreboard-card__signals .metric-bar small) {
    display: none;
  }
  .action-button {
    width: 100%;
  }
  .filter-summary,
  .report-pager,
  .readiness-panel header {
    align-items: stretch;
    flex-direction: column;
  }
}
@media (prefers-reduced-motion: reduce) {
  .list-card {
    transition: none;
  }

  .list-card:hover {
    transform: none;
  }
}
</style>
