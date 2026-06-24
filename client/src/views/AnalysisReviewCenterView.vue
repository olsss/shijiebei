<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import {
  listPublicDecisionReports,
  listPublicDecisionReviews,
  type PublicDecisionLesson,
  type PublicDecisionReport,
  type PublicDecisionReview,
} from '@/api/analysisReview';
import { enumLabel, readablePublicText } from '@/utils/display-labels';

const loading = ref(false);
const error = ref('');
const reports = ref<PublicDecisionReport[]>([]);
const reviews = ref<PublicDecisionReview[]>([]);
const selectedReportId = ref<number | null>(null);

const selectedReport = computed<PublicDecisionReport | null>(() => {
  if (reports.value.length === 0) {
    return null;
  }
  if (selectedReportId.value == null) {
    return reports.value[0];
  }
  return reports.value.find((report) => report.id === selectedReportId.value) ?? reports.value[0];
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

const stats = computed(() => ({
  reports: reports.value.length,
  reviews: reviews.value.length,
  matches: new Set([
    ...reports.value.map((report) => report.matchId ?? report.matchName),
    ...reviews.value.map((review) => review.matchId ?? review.matchName),
  ].filter(Boolean)).size,
  lessons: reviews.value.reduce((sum, review) => sum + review.lessons.length, 0),
}));

function formatDate(value?: string): string {
  return value ? value.replace('T', ' ').slice(0, 10) : '日期待同步';
}

const publicSensitivePattern = /\b(?:ticketNo|ticket|stakeSuggestion|stake|betPlan|rawPayload|profitLoss|profit|loss|budgetAmount|returnAmount|ROI|CLV)\b|票号|投入|返还|盈亏|预算|下注|金额建议|原始\s*JSON/gi;

function publicText(value?: string, fallback = '暂无公开摘要。'): string {
  const text = value?.trim();
  if (!text) {
    return fallback;
  }
  return readablePublicText(text.replace(publicSensitivePattern, '已脱敏指标'), fallback);
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

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const [reportsResponse, reviewsResponse] = await Promise.all([
      listPublicDecisionReports(),
      listPublicDecisionReviews(),
    ]);
    reports.value = reportsResponse.data;
    reviews.value = reviewsResponse.data;
    selectedReportId.value = reports.value[0]?.id ?? null;
  } catch (cause) {
    reports.value = [];
    reviews.value = [];
    selectedReportId.value = null;
    error.value = cause instanceof Error ? cause.message : '无法读取公开决策复盘数据。';
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <section class="page-shell decisions-page" aria-labelledby="decisions-title">
    <section class="page-content decisions-page__content">
      <header class="decisions-hero">
        <div>
          <p class="eyebrow">决策 · 复盘</p>
          <h1 id="decisions-title">决策复盘中心</h1>
          <p>公开展示分析结论、风险摘要、赛后复盘和规则沉淀；涉及后台执行、票据、资金和收益的明细保留在管理员区域。</p>
        </div>
        <button class="action-button" type="button" :disabled="loading" @click="load">
          {{ loading ? '刷新中' : '刷新公开数据' }}
        </button>
      </header>

      <section class="stat-grid" aria-label="决策复盘统计">
        <article class="stat-card"><span>报告</span><strong>{{ stats.reports }}</strong><small>公开分析摘要</small></article>
        <article class="stat-card"><span>复盘</span><strong>{{ stats.reviews }}</strong><small>赛后总结</small></article>
        <article class="stat-card"><span>比赛</span><strong>{{ stats.matches }}</strong><small>覆盖场次</small></article>
        <article class="stat-card"><span>规则</span><strong>{{ stats.lessons }}</strong><small>经验沉淀</small></article>
      </section>

      <div v-if="error" class="alert-panel" role="alert">{{ error }}</div>

      <section class="decisions-grid">
        <aside class="side-panel" aria-label="公开分析报告">
          <div class="panel-heading">
            <div><p class="eyebrow">报告</p><h2>分析报告</h2></div>
            <span class="count-pill">{{ reports.length }}</span>
          </div>
          <p v-if="loading && !reports.length" class="empty-copy">正在加载公开报告...</p>
          <p v-else-if="!reports.length" class="empty-copy">暂无公开分析报告。</p>
          <button
            v-for="report in reports"
            v-else
            :key="report.id"
            class="list-card"
            :class="{ 'list-card--active': report.id === selectedReport?.id }"
            type="button"
            @click="selectReport(report)"
          >
            <span>{{ report.jcCode ? `竞彩 ${report.jcCode}` : '竞彩待定' }} · {{ formatDate(report.matchday) }}</span>
            <strong>{{ report.matchName || '比赛待同步' }}</strong>
            <small>{{ enumLabel('conclusionType', report.conclusionType, '结论待同步') }} · 置信度 {{ report.confidence || '-' }}</small>
            <small>{{ publicText(report.riskSummary, '暂无风险摘要') }}</small>
          </button>
        </aside>

        <article class="detail-panel">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">决策详情</p>
              <h2>{{ selectedReport?.matchName || '公开决策详情' }}</h2>
            </div>
            <span v-if="selectedReport" class="status-pill">{{ enumLabel('conclusionType', selectedReport.conclusionType, '结论待定') }}</span>
          </div>

          <p v-if="!selectedReport && !loading" class="empty-copy">请选择左侧公开报告。</p>
          <template v-else-if="selectedReport">
            <section class="summary-grid" aria-label="报告摘要">
              <div><span>比赛编号</span><strong>{{ selectedReport.jcCode || '-' }}</strong></div>
              <div><span>比赛日期</span><strong>{{ formatDate(selectedReport.matchday) }}</strong></div>
              <div><span>结论类型</span><strong>{{ enumLabel('conclusionType', selectedReport.conclusionType, '-') }}</strong></div>
              <div><span>置信度</span><strong>{{ selectedReport.confidence || '-' }}</strong></div>
            </section>

            <section class="card-grid">
              <article class="info-card">
                <p class="eyebrow">风险</p>
                <h3>风险摘要</h3>
                <p>{{ publicText(selectedReport.riskSummary) }}</p>
              </article>
              <article class="info-card">
                <p class="eyebrow">复盘</p>
                <h3>复盘摘要</h3>
                <p>{{ publicText(selectedReport.reviewSummary) }}</p>
              </article>
              <article class="info-card info-card--wide">
                <p class="eyebrow">规则摘要</p>
                <h3>规则沉淀摘要</h3>
                <p>{{ publicText(selectedReport.lessonSummary) }}</p>
              </article>
            </section>

            <section class="info-card">
              <div class="panel-heading">
                <div><p class="eyebrow">赛后复盘</p><h3>相关赛后复盘</h3></div>
                <span class="count-pill">{{ relatedReviews.length }}</span>
              </div>
              <div v-if="relatedReviews.length" class="review-grid">
                <article v-for="review in relatedReviews" :key="review.id" class="review-card">
                  <span>{{ review.reviewKey }} · {{ formatDate(review.matchday) }}</span>
                  <strong>{{ review.title }}</strong>
                  <p>{{ publicText(review.overallSummary, '暂无总评。') }}</p>
                  <div class="review-layers">
                    <small>{{ publicText(review.mathSummary, '数学层待同步') }}</small>
                    <small>{{ publicText(review.footballSummary, '足球层待同步') }}</small>
                    <small>{{ publicText(review.handicapSummary, '盘口层待同步') }}</small>
                    <small>{{ publicText(review.tournamentTemperamentSummary, '大赛气质层待同步') }}</small>
                    <small>{{ publicText(review.oddsValueSummary, '价值层待同步') }}</small>
                  </div>
                </article>
              </div>
              <p v-else class="empty-copy">暂无相关赛后复盘。</p>
            </section>

            <section class="info-card">
              <div class="panel-heading">
                <div><p class="eyebrow">规则</p><h3>规则沉淀</h3></div>
                <span class="count-pill">{{ lessons.length }}</span>
              </div>
              <div v-if="lessons.length" class="lesson-grid">
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
.stat-card,
.side-panel,
.detail-panel,
.info-card,
.summary-grid div,
.alert-panel {
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
.stat-grid,
.summary-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}
.stat-card,
.side-panel,
.detail-panel,
.info-card,
.summary-grid div,
.alert-panel {
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
  min-width: 0;
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
  transition: border-color 180ms ease, transform 180ms ease;
}
.list-card--active {
  border-color: rgba(217, 119, 6, .62);
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
  min-width: 0;
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
  .stat-grid,
  .decisions-grid,
  .summary-grid,
  .card-grid,
  .review-grid,
  .lesson-grid,
  .review-layers {
    grid-template-columns: 1fr;
  }
  .panel-heading {
    align-items: stretch;
    flex-direction: column;
  }
  .action-button {
    width: 100%;
  }
}
@media (prefers-reduced-motion: reduce) {
  .list-card {
    transition: none;
  }
}
</style>
