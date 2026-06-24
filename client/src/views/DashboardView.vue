<template>
  <section class="page-shell overview-dashboard" aria-labelledby="overview-title">
    <section class="page-content overview-dashboard__content">
      <header class="overview-hero">
        <div class="overview-hero__copy">
          <p class="eyebrow">公开态势总览</p>
          <h1 id="overview-title">赛事指挥总览</h1>
          <p>
            汇总已批准的公开赛程、风险、赔率快照与决策复盘信号。访客可只读查看，数据审核与入库继续留在管理员后台。
          </p>
        </div>
        <div class="overview-hero__status" aria-label="公开概览更新时间">
          <span>最近生成</span>
          <strong>{{ generatedAtLabel }}</strong>
          <button class="refresh-button" type="button" :disabled="overviewLoading" @click="loadPublicOverview">
            {{ overviewLoading ? '刷新中' : '刷新概览' }}
          </button>
        </div>
      </header>

      <div v-if="overviewLoading && !publicOverview" class="loading-panel" aria-live="polite">
        <span class="loading-panel__bar"></span>
        <span class="loading-panel__bar loading-panel__bar--short"></span>
        <span>正在加载公开概览...</span>
      </div>

      <div v-else-if="overviewError" class="error-panel" role="alert">
        <strong>公开概览暂不可用</strong>
        <span>{{ overviewError }}</span>
        <button class="refresh-button refresh-button--light" type="button" @click="loadPublicOverview">重试</button>
      </div>

      <template v-else>
        <section class="kpi-grid" aria-label="公开关键指标">
          <article v-for="kpi in publicKpis" :key="kpi.label" class="kpi-card" :data-test="kpi.testId">
            <span>{{ kpi.label }}</span>
            <strong>{{ kpi.value }}</strong>
            <small>{{ kpi.caption }}</small>
          </article>
        </section>

        <section class="overview-grid" aria-label="公开态势面板">
          <article class="command-panel command-panel--wide">
            <div class="panel-heading">
              <div>
                <p class="eyebrow">近期赛程</p>
                <h2>近期比赛</h2>
              </div>
              <RouterLink class="panel-link" to="/evidence/matches">查看赛程</RouterLink>
            </div>

            <div v-if="upcomingMatches.length" class="match-list">
              <RouterLink
                v-for="match in upcomingMatches"
                :key="match.matchId"
                class="match-card"
                data-test="upcoming-match-card"
                :to="`/evidence/matches?matchId=${match.matchId}`"
              >
                <span class="match-card__meta">{{ match.competition || '世界杯' }} · {{ match.stage || '待定阶段' }}</span>
                <strong>{{ match.matchName }}</strong>
                <span>{{ formatDisplayTime(match.kickoffTime || match.matchday) }}</span>
                <div class="match-card__badges" aria-label="比赛完整度与风险">
                  <span>竞彩 {{ match.jcCode || '待定' }}</span>
                  <span>完整度 {{ match.integrityScore }}%</span>
                  <span>风险 {{ match.riskCount }}</span>
                </div>
              </RouterLink>
            </div>
            <p v-else class="empty-copy">暂无近期比赛，等待公开数据同步。</p>
          </article>

          <article class="command-panel">
            <div class="panel-heading">
              <div>
                <p class="eyebrow">风险</p>
                <h2>风险雷达</h2>
              </div>
              <RouterLink class="panel-link" to="/evidence/sentiment">查看舆情</RouterLink>
            </div>
            <dl class="metric-list">
              <div data-test="risk-counter-high">
                <dt>高风险</dt>
                <dd>{{ publicOverview?.riskCounters.highRiskCount ?? 0 }}</dd>
              </div>
              <div>
                <dt>中风险</dt>
                <dd>{{ publicOverview?.riskCounters.mediumRiskCount ?? 0 }}</dd>
              </div>
              <div>
                <dt>过期因素</dt>
                <dd>{{ publicOverview?.riskCounters.staleFactorCount ?? 0 }}</dd>
              </div>
              <div>
                <dt>未解冲突</dt>
                <dd>{{ publicOverview?.riskCounters.unresolvedConflictCount ?? 0 }}</dd>
              </div>
            </dl>
          </article>

          <article class="command-panel">
            <div class="panel-heading">
              <div>
                <p class="eyebrow">证据</p>
                <h2>证据完整度</h2>
              </div>
              <RouterLink class="panel-link" to="/workbench">进入作战室</RouterLink>
            </div>
            <dl class="metric-list metric-list--integrity">
              <div>
                <dt>完整</dt>
                <dd>{{ publicOverview?.integrityCounters.completeCount ?? 0 }}</dd>
              </div>
              <div>
                <dt>部分</dt>
                <dd>{{ publicOverview?.integrityCounters.partialCount ?? 0 }}</dd>
              </div>
              <div>
                <dt>阻塞</dt>
                <dd>{{ publicOverview?.integrityCounters.blockedCount ?? 0 }}</dd>
              </div>
            </dl>
          </article>

          <article class="command-panel">
            <div class="panel-heading">
              <div>
                <p class="eyebrow">赔率</p>
                <h2>赔率快照</h2>
              </div>
              <RouterLink class="panel-link" to="/evidence/odds">查看赔率</RouterLink>
            </div>
            <dl class="metric-list">
              <div>
                <dt>市场总数</dt>
                <dd>{{ publicOverview?.oddsFreshness.marketCount ?? 0 }}</dd>
              </div>
              <div data-test="odds-freshness-non-live">
                <dt>非实时快照</dt>
                <dd>{{ nonLiveMarketCount }}</dd>
              </div>
              <div data-test="odds-freshness-live">
                <dt>实时盘口</dt>
                <dd>{{ publicOverview?.oddsFreshness.liveMarketCount ?? 0 }}</dd>
              </div>
              <div>
                <dt>过期实时</dt>
                <dd>{{ publicOverview?.oddsFreshness.staleLiveMarketCount ?? 0 }}</dd>
              </div>
            </dl>
            <p class="panel-note">实时为 0 不代表缺少赔率；当前已入库盘口主要是赛前或赛后归档快照。</p>
          </article>

          <article class="command-panel">
            <div class="panel-heading">
              <div>
                <p class="eyebrow">决策</p>
                <h2>决策复盘</h2>
              </div>
              <RouterLink class="panel-link" to="/decisions">查看复盘</RouterLink>
            </div>
            <dl class="metric-list">
              <div data-test="decision-summary-report">
                <dt>分析报告</dt>
                <dd>{{ publicOverview?.decisionSummary.reportCount ?? 0 }}</dd>
              </div>
              <div>
                <dt>复盘条目</dt>
                <dd>{{ publicOverview?.decisionSummary.reviewCount ?? 0 }}</dd>
              </div>
              <div>
                <dt>最新决策</dt>
                <dd class="metric-list__time">{{ formatDisplayTime(publicOverview?.decisionSummary.latestDecisionAt) }}</dd>
              </div>
            </dl>
          </article>
        </section>

        <section class="entry-grid" aria-label="阶段入口">
          <RouterLink v-for="item in publicModules" :key="item.title" class="entry-card" :to="item.to">
            <span>{{ item.mark }}</span>
            <strong>{{ item.title }}</strong>
            <small>{{ item.description }}</small>
          </RouterLink>
        </section>
      </template>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { fetchPublicOverview, type PublicOverviewMatch, type PublicOverviewResponse } from '@/api/publicOverview';

const publicOverview = ref<PublicOverviewResponse | null>(null);
const overviewLoading = ref(false);
const overviewError = ref('');

const publicModules = [
  { mark: '01', title: '比赛中心', description: '查看赛程、比分与比赛档案', to: '/evidence/matches' },
  { mark: '02', title: '赔率中心', description: '查看赔率快照与盘口变化', to: '/evidence/odds' },
  { mark: '03', title: '舆情与外部因素中心', description: '查看新闻、天气与外部变量', to: '/evidence/sentiment' },
  { mark: '04', title: '分析下注复盘中心', description: '查看分析报告与赛后复盘摘要', to: '/decisions' },
  { mark: '05', title: '赛前分析作战室', description: '按九维框架整理赛前证据、结论与风险', to: '/workbench' },
  { mark: '06', title: '球队画像中心', description: '查看球队档案、战术与近期状态', to: '/evidence/teams' },
  { mark: '07', title: '球员画像中心', description: '查看球员档案、状态与伤停影响', to: '/evidence/players' },
  { mark: '08', title: '更多入口', description: '查看移动端更多功能入口', to: '/more' },
];

const generatedAtLabel = computed(() => formatDisplayTime(publicOverview.value?.generatedAt));
const upcomingMatches = computed<PublicOverviewMatch[]>(() => publicOverview.value?.upcomingMatches ?? []);
const nonLiveMarketCount = computed(() => {
  const freshness = publicOverview.value?.oddsFreshness;
  if (!freshness) {
    return 0;
  }
  return Math.max(0, freshness.marketCount - freshness.liveMarketCount - freshness.staleLiveMarketCount);
});

const publicKpis = computed(() => [
  {
    label: '近期比赛',
    value: upcomingMatches.value.length,
    caption: '公开赛程窗口',
    testId: 'public-kpi-upcoming',
  },
  {
    label: '未解冲突',
    value: publicOverview.value?.riskCounters.unresolvedConflictCount ?? 0,
    caption: '需审核确认',
    testId: 'public-kpi-conflicts',
  },
  {
    label: '赔率市场',
    value: publicOverview.value?.oddsFreshness.marketCount ?? 0,
    caption: '已入库盘口',
    testId: 'public-kpi-odds-market',
  },
  {
    label: '分析报告',
    value: publicOverview.value?.decisionSummary.reportCount ?? 0,
    caption: '公开复盘素材',
    testId: 'public-kpi-reports',
  },
]);

function formatDisplayTime(value?: string) {
  if (!value) {
    return '待同步';
  }
  return value.replace('T', ' ').slice(0, 16);
}

async function loadPublicOverview() {
  overviewError.value = '';
  overviewLoading.value = true;
  try {
    const response = await fetchPublicOverview();
    publicOverview.value = response.data;
  } catch (cause) {
    publicOverview.value = null;
    overviewError.value = cause instanceof Error ? cause.message : '无法读取公开概览。';
  } finally {
    overviewLoading.value = false;
  }
}

onMounted(loadPublicOverview);
</script>

<style scoped>
.overview-dashboard {
  max-width: 100%;
  overflow-x: hidden;
}

.overview-dashboard__content {
  display: grid;
  gap: 20px;
  min-width: 0;
}

.overview-hero,
.command-panel,
.kpi-card,
.entry-card,
.loading-panel,
.error-panel {
  background: var(--wc-glass);
  border: 1px solid var(--wc-border);
  border-radius: var(--wc-radius-lg);
  color: var(--wc-text);
}

.overview-hero {
  align-items: stretch;
  display: grid;
  gap: 18px;
  grid-template-columns: minmax(0, 1fr) minmax(240px, 320px);
  padding: clamp(20px, 4vw, 42px);
}

.overview-hero__copy {
  max-width: 820px;
  min-width: 0;
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

.overview-hero h1 {
  font-family: var(--wc-font-display);
  font-size: clamp(36px, 7vw, 78px);
  line-height: 0.96;
  margin: 0 0 14px;
  overflow-wrap: anywhere;
}

.overview-hero p:not(.eyebrow) {
  color: var(--wc-text-muted);
  font-size: clamp(16px, 2vw, 19px);
  line-height: 1.72;
  margin: 0;
  overflow-wrap: anywhere;
}

.overview-hero__status {
  background: rgba(15, 23, 42, 0.72);
  border: 1px solid rgba(147, 197, 253, 0.22);
  border-radius: var(--wc-radius-md);
  display: grid;
  gap: 10px;
  justify-items: start;
  min-width: 0;
  padding: 18px;
}

.overview-hero__status span,
.kpi-card span,
.metric-list dt,
.entry-card small,
.match-card__meta,
.empty-copy,
.panel-note {
  color: var(--wc-text-muted);
}

.overview-hero__status strong {
  font-family: var(--wc-font-mono);
  font-size: 18px;
}

.refresh-button,
.panel-link {
  align-items: center;
  border-radius: 999px;
  cursor: pointer;
  display: inline-flex;
  font-weight: 800;
  justify-content: center;
  min-height: 44px;
  text-decoration: none;
  transition: border-color 180ms ease, color 180ms ease, opacity 180ms ease, transform 180ms ease;
}

.refresh-button {
  background: var(--wc-accent);
  border: 1px solid transparent;
  color: var(--wc-on-accent);
  padding: 0 16px;
}

.refresh-button:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

.refresh-button--light,
.panel-link {
  background: rgba(147, 197, 253, 0.1);
  border: 1px solid rgba(147, 197, 253, 0.28);
  color: var(--wc-text);
  padding: 0 14px;
}

.refresh-button:focus-visible,
.panel-link:focus-visible,
.match-card:focus-visible,
.entry-card:focus-visible {
  box-shadow: var(--wc-focus-ring);
  outline: none;
}

.kpi-grid,
.entry-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.kpi-card {
  display: grid;
  gap: 6px;
  min-height: 132px;
  min-width: 0;
  padding: 18px;
}

.kpi-card strong {
  color: var(--wc-primary);
  font-family: var(--wc-font-mono);
  font-size: clamp(30px, 5vw, 46px);
  line-height: 1;
}

.kpi-card small {
  color: var(--wc-text-subtle);
}

.overview-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.command-panel {
  display: grid;
  gap: 16px;
  min-width: 0;
  padding: 18px;
}

.command-panel--wide {
  grid-row: span 2;
}

.panel-heading {
  align-items: start;
  display: flex;
  gap: 12px;
  justify-content: space-between;
}

.panel-heading h2 {
  font-size: clamp(22px, 3vw, 30px);
  line-height: 1.1;
  margin: 0;
}

.match-list {
  display: grid;
  gap: 12px;
}

.match-card {
  background: rgba(15, 23, 42, 0.62);
  border: 1px solid rgba(147, 197, 253, 0.18);
  border-radius: var(--wc-radius-md);
  color: var(--wc-text);
  display: grid;
  gap: 8px;
  min-height: 124px;
  min-width: 0;
  padding: 16px;
  text-decoration: none;
  transition: border-color 180ms ease, transform 180ms ease;
}

.match-card:hover,
.entry-card:hover,
.panel-link:hover,
.refresh-button:hover:not(:disabled) {
  border-color: rgba(147, 197, 253, 0.5);
  transform: translateY(-2px);
}

.match-card strong {
  font-size: 20px;
}

.match-card__badges {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.match-card__badges span {
  background: rgba(147, 197, 253, 0.11);
  border: 1px solid rgba(147, 197, 253, 0.2);
  border-radius: 999px;
  color: var(--wc-text);
  font-family: var(--wc-font-mono);
  font-size: 12px;
  padding: 6px 9px;
}

.metric-list {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin: 0;
}

.metric-list--integrity {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.metric-list div {
  background: rgba(15, 23, 42, 0.52);
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: var(--wc-radius-sm);
  min-width: 0;
  padding: 13px;
}

.metric-list dt {
  font-size: 13px;
}

.metric-list dd {
  color: var(--wc-primary);
  font-family: var(--wc-font-mono);
  font-size: 26px;
  font-weight: 800;
  margin: 2px 0 0;
}

.metric-list__time {
  font-size: 16px !important;
  line-height: 1.3;
}

.panel-note {
  border-top: 1px solid rgba(148, 163, 184, 0.14);
  font-size: 13px;
  line-height: 1.6;
  margin: 0;
  padding-top: 12px;
}

.entry-card {
  display: grid;
  gap: 8px;
  min-height: 140px;
  min-width: 0;
  padding: 18px;
  text-decoration: none;
  transition: border-color 180ms ease, transform 180ms ease;
}

.entry-card span {
  color: var(--wc-warning);
  font-family: var(--wc-font-mono);
  font-size: 12px;
  font-weight: 800;
}

.entry-card strong {
  color: var(--wc-text);
  font-size: 19px;
}

.loading-panel,
.error-panel {
  display: grid;
  gap: 12px;
  padding: 18px;
}

.loading-panel__bar {
  animation: pulse 1.2s ease-in-out infinite;
  background: rgba(147, 197, 253, 0.18);
  border-radius: 999px;
  display: block;
  height: 14px;
  width: 100%;
}

.loading-panel__bar--short {
  width: 62%;
}

.error-panel strong {
  color: var(--wc-danger);
}

@keyframes pulse {
  0%,
  100% {
    opacity: 0.48;
  }
  50% {
    opacity: 1;
  }
}

@media (max-width: 1024px) {
  .overview-hero,
  .overview-grid {
    grid-template-columns: 1fr;
  }

  .command-panel--wide {
    grid-row: auto;
  }

  .kpi-grid,
  .entry-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .overview-dashboard__content {
    gap: 14px;
  }

  .overview-hero,
  .kpi-grid,
  .overview-grid,
  .entry-grid,
  .metric-list,
  .metric-list--integrity {
    grid-template-columns: 1fr;
  }

  .overview-hero,
  .command-panel,
  .kpi-card,
  .entry-card {
    border-radius: var(--wc-radius-md);
    padding: 16px;
  }

  .panel-heading {
    align-items: stretch;
    flex-direction: column;
  }

  .panel-link,
  .refresh-button {
    width: 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .loading-panel__bar {
    animation: none;
  }
}
</style>
