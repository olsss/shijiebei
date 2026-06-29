<template>
  <section class="page-shell overview-dashboard" aria-labelledby="overview-title">
    <section class="page-content overview-dashboard__content">
      <header class="overview-hero">
        <div class="overview-hero__copy">
          <p class="eyebrow">公开态势总览</p>
          <h1 id="overview-title">赛事指挥总览</h1>
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
        <section v-if="headlineMatch" class="match-intel-panel" tabindex="0" aria-label="焦点赛事情报" data-test="match-intel-panel">
          <div class="panel-heading match-intel-panel__heading">
            <div>
              <p class="eyebrow">焦点比赛</p>
              <h2>赛事情报：{{ headlineMatch.matchName }}</h2>
            </div>
            <RouterLink class="panel-link" :to="`/evidence/matches?matchId=${headlineMatch.matchId}`">打开比赛详情</RouterLink>
          </div>

          <div class="match-intel-panel__grid">
            <article class="match-intel-panel__score">
              <ScoreboardCard
                :home-team="headlineMatch.homeTeam"
                :away-team="headlineMatch.awayTeam"
                :scoreboard="headlineMatch.scoreboard"
                :match-name="headlineMatch.matchName"
                :meta="matchMeta(headlineMatch)"
                :status="statusLabel(headlineMatch.status || headlineMatch.resultStatus)"
                :integrity-score="headlineMatch.integrityScore"
                :risk-count="headlineMatch.riskCount"
              />
              <p class="match-intel-panel__copy">{{ headlineMatchSummary }}</p>
            </article>

            <div class="match-intel-panel__metrics" aria-label="比赛关键指标">
              <article
                v-for="insight in headlineMetrics"
                :key="insight.label"
                class="match-metric-card"
                :class="`match-metric-card--${insight.tone}`"
              >
                <span>{{ insight.label }}</span>
                <strong>{{ insight.value }}</strong>
                <p>{{ insight.caption }}</p>
              </article>
            </div>

            <article class="match-intel-panel__charts" aria-label="首页数据态势">
              <header>
                <span>数据态势</span>
                <strong>{{ headlineVerdict }}</strong>
              </header>
              <div class="match-intel-panel__rings" tabindex="0" aria-label="焦点比赛资料图">
                <CoverageDonut
                  label="资料准备度"
                  :value="headlineMatch.integrityScore"
                  :tone="readinessTone(headlineMatch.integrityScore)"
                  size="compact"
                  :caption="integrityCopy(headlineMatch.integrityScore)"
                />
                <CoverageDonut
                  label="风险压力"
                  :value="headlineRiskPressureScore"
                  unit="%"
                  :tone="riskToneForCount(headlineMatch.riskCount)"
                  size="compact"
                  :caption="headlineRiskPressureCaption"
                />
              </div>
              <MetricBar
                label="资料准备度"
                :value="headlineMatch.integrityScore"
                unit="%"
                :tone="readinessTone(headlineMatch.integrityScore)"
                :caption="integrityCopy(headlineMatch.integrityScore)"
              />
              <MetricBar
                label="风险压力"
                :value="headlineMatch.riskCount"
                :max="riskPressureMax"
                :tone="riskToneForCount(headlineMatch.riskCount)"
                caption="风险记录覆盖伤停、市场价格快照、天气、来源冲突"
              />
              <ul class="match-intel-panel__checklist" aria-label="比赛资料项">
                <li v-for="item in headlineFacts" :key="item">{{ item }}</li>
              </ul>
            </article>
          </div>
        </section>

        <section class="kpi-grid" aria-label="公开关键指标">
          <article v-for="kpi in publicKpis" :key="kpi.label" class="kpi-card" :data-test="kpi.testId">
            <span>{{ kpi.label }}</span>
            <strong>{{ kpi.value }}</strong>
            <small>{{ kpi.caption }}</small>
          </article>
        </section>

        <section v-if="focusMatches.length" class="focus-board" tabindex="0" aria-label="焦点比赛">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">焦点赛程</p>
              <h2>比分、状态与资料准备度</h2>
            </div>
            <RouterLink class="panel-link" to="/evidence/matches">进入比赛中心</RouterLink>
          </div>
          <div class="focus-grid" tabindex="0">
            <ScoreboardCard
              v-for="match in focusMatches"
              :key="match.matchId"
              :home-team="match.homeTeam"
              :away-team="match.awayTeam"
              :scoreboard="match.scoreboard"
              :match-name="match.matchName"
              :meta="matchMeta(match)"
              :status="statusLabel(match.status || match.resultStatus)"
              :integrity-score="match.integrityScore"
              :risk-count="match.riskCount"
            />
          </div>
        </section>

        <section class="overview-grid" tabindex="0" aria-label="公开态势面板">
          <article class="command-panel command-panel--wide">
            <div class="panel-heading">
              <div>
                <p class="eyebrow">近期赛程</p>
                <h2>近期比赛</h2>
              </div>
              <RouterLink class="panel-link" to="/evidence/matches">查看赛程</RouterLink>
            </div>

            <div v-if="upcomingMatches.length" class="match-list" tabindex="0">
              <RouterLink
                v-for="match in upcomingMatches"
                :key="match.matchId"
                class="match-card"
                data-test="upcoming-match-card"
                :to="`/evidence/matches?matchId=${match.matchId}`"
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
                <div class="match-card__badges" aria-label="比赛资料准备度与风险">
                  <span>竞彩 {{ match.jcCode || '待定' }}</span>
                  <span>准备度 {{ match.integrityScore }}%</span>
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
            <div class="panel-bars" aria-label="风险条形图">
              <div class="overview-ring-grid" tabindex="0" aria-label="风险分布环形图">
                <CoverageDonut
                  v-for="ring in riskRingRows"
                  :key="ring.label"
                  :label="ring.label"
                  :value="ring.value"
                  :max="ring.max"
                  unit="项"
                  :tone="ring.tone"
                  size="compact"
                  :caption="ring.caption"
                />
              </div>
              <MetricBar
                label="高风险占比"
                :value="publicOverview?.riskCounters.highRiskCount ?? 0"
                :max="riskTotal"
                tone="danger"
                caption="高风险记录占公开风险的比例"
              />
              <MetricBar
                label="中风险占比"
                :value="publicOverview?.riskCounters.mediumRiskCount ?? 0"
                :max="riskTotal"
                tone="warning"
                caption="中风险记录占公开风险的比例"
              />
            </div>
          </article>

          <article class="command-panel">
            <div class="panel-heading">
              <div>
                <p class="eyebrow">证据</p>
                <h2>资料准备度</h2>
              </div>
              <RouterLink class="panel-link" to="/workbench">进入作战室</RouterLink>
            </div>
            <dl class="metric-list metric-list--integrity">
              <div>
                <dt>资料齐全</dt>
                <dd>{{ publicOverview?.integrityCounters.completeCount ?? 0 }}</dd>
              </div>
              <div>
                <dt>基础可读</dt>
                <dd>{{ publicOverview?.integrityCounters.partialCount ?? 0 }}</dd>
              </div>
              <div>
                <dt>待补资料</dt>
                <dd>{{ publicOverview?.integrityCounters.blockedCount ?? 0 }}</dd>
              </div>
            </dl>
            <div class="panel-bars" aria-label="资料准备度条形图">
              <div class="overview-ring-grid" tabindex="0" aria-label="资料准备度分布环形图">
                <CoverageDonut
                  v-for="ring in integrityRingRows"
                  :key="ring.label"
                  :label="ring.label"
                  :value="ring.value"
                  :max="ring.max"
                  unit="场"
                  :tone="ring.tone"
                  size="compact"
                  :caption="ring.caption"
                />
              </div>
              <MetricBar
                label="资料齐全比赛"
                :value="publicOverview?.integrityCounters.completeCount ?? 0"
                :max="integrityTotal"
                tone="success"
                caption="公开资料齐全场次占比"
              />
            </div>
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
                <dt>实时市场</dt>
                <dd>{{ publicOverview?.oddsFreshness.liveMarketCount ?? 0 }}</dd>
              </div>
              <div>
                <dt>过期实时</dt>
                <dd>{{ publicOverview?.oddsFreshness.staleLiveMarketCount ?? 0 }}</dd>
              </div>
            </dl>
            <div class="odds-ring-grid" tabindex="0" aria-label="赔率快照结构环形图">
              <CoverageDonut
                v-for="ring in oddsMarketRingRows"
                :key="ring.label"
                :label="ring.label"
                :value="ring.value"
                :max="ring.max"
                unit="个"
                :tone="ring.tone"
                :caption="ring.caption"
                size="compact"
              />
            </div>
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
            <div class="decision-ring-grid" tabindex="0" aria-label="决策复盘结构环形图">
              <CoverageDonut
                v-for="ring in decisionRingRows"
                :key="ring.label"
                :label="ring.label"
                :value="ring.value"
                :max="ring.max"
                :unit="ring.unit"
                :tone="ring.tone"
                :caption="ring.caption"
                size="compact"
              />
            </div>
          </article>
        </section>

        <section class="entry-grid" tabindex="0" aria-label="阶段入口">
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
import CoverageDonut from '@/components/football/CoverageDonut.vue';
import MetricBar from '@/components/football/MetricBar.vue';
import ScoreboardCard from '@/components/football/ScoreboardCard.vue';
import {
  scoreTone,
  scoreboardFallback,
  statusLabel,
  isPlaceholderTeamName,
  teamNameFromMatchName,
  type Scoreboard,
} from '@/utils/football-visuals';

const publicOverview = ref<PublicOverviewResponse | null>(null);
const overviewLoading = ref(false);
const overviewError = ref('');

const publicModules = [
  { mark: '01', title: '比赛中心', description: '查看赛程、比分与比赛档案', to: '/evidence/matches' },
  { mark: '02', title: '赔率中心', description: '查看赔率快照与隐含概率', to: '/evidence/odds' },
  { mark: '03', title: '舆情与外部因素中心', description: '查看新闻、天气与外部变量', to: '/evidence/sentiment' },
  { mark: '04', title: '决策复盘中心', description: '查看分析报告与赛后复盘摘要', to: '/decisions' },
  { mark: '05', title: '赛前分析作战室', description: '按九维框架整理赛前证据、结论与风险', to: '/workbench' },
  { mark: '06', title: '球队画像中心', description: '查看球队档案、战术与近期状态', to: '/evidence/teams' },
  { mark: '07', title: '球员画像中心', description: '查看球员档案、状态与伤停影响', to: '/evidence/players' },
  { mark: '08', title: '更多入口', description: '查看移动端更多功能入口', to: '/more' },
];

const generatedAtLabel = computed(() => formatDisplayTime(publicOverview.value?.generatedAt));
const upcomingMatches = computed<PublicOverviewMatch[]>(() => publicOverview.value?.upcomingMatches ?? []);
const focusMatches = computed<PublicOverviewMatch[]>(() => upcomingMatches.value.slice(0, 3));
const headlineMatch = computed<PublicOverviewMatch | null>(() => focusMatches.value[0] ?? null);
const headlineScoreboard = computed<Scoreboard | null>(() =>
  headlineMatch.value ? scoreboardFallback(headlineMatch.value as unknown as Record<string, unknown>) : null,
);
const nonLiveMarketCount = computed(() => {
  const freshness = publicOverview.value?.oddsFreshness;
  if (!freshness) {
    return 0;
  }
  return Math.max(0, freshness.marketCount - freshness.liveMarketCount - freshness.staleLiveMarketCount);
});
const oddsMarketTotal = computed(() => Math.max(1, publicOverview.value?.oddsFreshness.marketCount ?? 0));
const oddsMarketRingRows = computed(() => {
  const freshness = publicOverview.value?.oddsFreshness;
  const marketCount = freshness?.marketCount ?? 0;
  const liveMarketCount = freshness?.liveMarketCount ?? 0;
  const staleLiveMarketCount = freshness?.staleLiveMarketCount ?? 0;
  const nonLiveCount = nonLiveMarketCount.value;
  const max = oddsMarketTotal.value;
  return [
    {
      label: '市场总数',
      value: marketCount,
      max,
      tone: marketCount ? 'info' : 'warning',
      caption: `${marketCount} 个公开市场快照`,
    },
    {
      label: '实时市场',
      value: liveMarketCount,
      max,
      tone: liveMarketCount ? 'success' : 'info',
      caption: `${liveMarketCount} 个实时市场字段`,
    },
    {
      label: '非实时快照',
      value: nonLiveCount,
      max,
      tone: nonLiveCount ? 'warning' : 'info',
      caption: `${nonLiveCount} 个赛前/赛后归档`,
    },
    {
      label: '过期实时',
      value: staleLiveMarketCount,
      max,
      tone: staleLiveMarketCount ? 'danger' : 'success',
      caption: `${staleLiveMarketCount} 个实时快照时效待更新`,
    },
  ];
});
const decisionMaterialTotal = computed(() => {
  const summary = publicOverview.value?.decisionSummary;
  if (!summary) {
    return 1;
  }
  return Math.max(1, summary.reportCount + summary.reviewCount);
});
const decisionRingRows = computed(() => {
  const summary = publicOverview.value?.decisionSummary;
  const reportCount = summary?.reportCount ?? 0;
  const reviewCount = summary?.reviewCount ?? 0;
  const materialTotal = reportCount + reviewCount;
  const hasLatestRecord = summary?.latestDecisionAt ? 1 : 0;
  const max = decisionMaterialTotal.value;
  return [
    {
      label: '分析报告',
      value: reportCount,
      max,
      unit: '篇',
      tone: reportCount ? 'info' : 'warning',
      caption: `${reportCount} 篇公开分析报告`,
    },
    {
      label: '复盘条目',
      value: reviewCount,
      max,
      unit: '条',
      tone: reviewCount ? 'success' : 'warning',
      caption: `${reviewCount} 条赛后复盘记录`,
    },
    {
      label: '材料合计',
      value: materialTotal,
      max,
      unit: '条',
      tone: materialTotal ? 'success' : 'warning',
      caption: `${materialTotal} 条公开复盘材料`,
    },
    {
      label: '最新记录',
      value: hasLatestRecord,
      max: 1,
      unit: '项',
      tone: hasLatestRecord ? 'success' : 'warning',
      caption: `最新时间 ${formatDisplayTime(summary?.latestDecisionAt)}`,
    },
  ];
});
const riskTotal = computed(() => {
  const counters = publicOverview.value?.riskCounters;
  if (!counters) {
    return 1;
  }
  return Math.max(
    1,
    counters.highRiskCount +
      counters.mediumRiskCount +
      counters.staleFactorCount +
      counters.unresolvedConflictCount,
  );
});
const riskRingRows = computed(() => {
  const counters = publicOverview.value?.riskCounters;
  const max = riskTotal.value;
  return [
    {
      label: '高风险',
      value: counters?.highRiskCount ?? 0,
      max,
      tone: 'danger',
      caption: '高影响外部风险',
    },
    {
      label: '中风险',
      value: counters?.mediumRiskCount ?? 0,
      max,
      tone: 'warning',
      caption: '中等影响风险',
    },
    {
      label: '过期因素',
      value: counters?.staleFactorCount ?? 0,
      max,
      tone: 'accent',
      caption: '时效待更新线索',
    },
    {
      label: '未解冲突',
      value: counters?.unresolvedConflictCount ?? 0,
      max,
      tone: 'info',
      caption: '来源冲突状态',
    },
  ];
});
const integrityTotal = computed(() => {
  const counters = publicOverview.value?.integrityCounters;
  if (!counters) {
    return 1;
  }
  return Math.max(1, counters.completeCount + counters.partialCount + counters.blockedCount);
});
const integrityRingRows = computed(() => {
  const counters = publicOverview.value?.integrityCounters;
  const max = integrityTotal.value;
  return [
    {
      label: '资料齐全',
      value: counters?.completeCount ?? 0,
      max,
      tone: 'success',
      caption: '资料齐全场次',
    },
    {
      label: '基础可读',
      value: counters?.partialCount ?? 0,
      max,
      tone: 'warning',
      caption: '基础资料可读场次',
    },
    {
      label: '待补资料',
      value: counters?.blockedCount ?? 0,
      max,
      tone: 'danger',
      caption: '资料缺口场次',
    },
  ];
});
const riskPressureMax = computed(() => Math.max(3, riskTotal.value));
const headlineRiskPressureScore = computed(() => {
  const value = headlineMatch.value?.riskCount ?? 0;
  return Math.min(100, Math.round((value / riskPressureMax.value) * 100));
});
const headlineRiskPressureCaption = computed(() => {
  const value = headlineMatch.value?.riskCount ?? 0;
  return value
    ? `风险 ${value} 项；伤停、天气、裁判、市场变化与来源冲突记录。`
    : '暂无公开风险记录；阵容、天气和市场变化继续同步。';
});

const headlineMatchSummary = computed(() => {
  const match = headlineMatch.value;
  const scoreboard = headlineScoreboard.value;
  if (!match || !scoreboard) {
    return '暂无焦点比赛，等待公开赛程同步。';
  }
  return scoreInsight(match, scoreboard);
});

const headlineMetrics = computed(() => {
  const match = headlineMatch.value;
  const scoreboard = headlineScoreboard.value;
  if (!match || !scoreboard) {
    return [];
  }
  return [
    {
      label: '比分/赛果',
      value: scoreboard.scoreDisplay || '待同步',
      caption: scoreboard.resultText || scoreInsight(match, scoreboard),
      tone: scoreResultTone(scoreboard),
    },
    {
      label: '资料准备度',
      value: `${match.integrityScore ?? 0}%`,
      caption: integrityCopy(match.integrityScore),
      tone: readinessTone(match.integrityScore),
    },
    {
      label: '风险记录',
      value: `${match.riskCount ?? 0}项`,
      caption: riskCopy(match.riskCount),
      tone: riskToneForCount(match.riskCount),
    },
  ];
});

const headlineFacts = computed(() => {
  const match = headlineMatch.value;
  const scoreboard = headlineScoreboard.value;
  if (!match || !scoreboard) {
    return ['等待公开比赛同步后显示比分、资料准备度和风险记录。'];
  }
  const items = [
    `比分状态：${scoreboard.scoreDisplay || '待同步'} · ${
      scoreboard.resultText || statusLabel(match.status || match.resultStatus)
    }。`,
    `资料状态：${integrityCopy(match.integrityScore)}。`,
  ];
  if ((match.riskCount ?? 0) > 0) {
    items.push(`风险记录：${match.riskCount} 个，覆盖伤停、天气、裁判、市场价格快照与来源冲突。`);
  } else {
    items.push('暂无公开风险记录；临场阵容、天气和市场价格快照仍需持续同步。');
  }
  return items;
});

const headlineVerdict = computed(() => {
  const match = headlineMatch.value;
  if (!match) {
    return '等待赛程同步';
  }
  const integrityScore = match.integrityScore ?? 0;
  const riskCount = match.riskCount ?? 0;
  if (integrityScore >= 85 && riskCount === 0) {
    return '资料较齐，比分已关联';
  }
  if (integrityScore >= 65 && riskCount > 0) {
    return '基础可读，存在风险';
  }
  if (integrityScore < 65) {
    return '资料偏薄，数据缺口较多';
  }
  return '比分与证据同步中';
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
    caption: '审核状态待定',
    testId: 'public-kpi-conflicts',
  },
  {
    label: '赔率市场',
    value: publicOverview.value?.oddsFreshness.marketCount ?? 0,
    caption: '已入库市场快照',
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

function matchMeta(match: PublicOverviewMatch): string {
  return `${match.competition || '世界杯'} · ${match.stage || '待定阶段'} · ${formatDisplayTime(
    match.kickoffTime || match.matchday,
  )}`;
}

function teamDisplayName(match: PublicOverviewMatch, side: 'HOME' | 'AWAY'): string {
  const team = side === 'HOME' ? match.homeTeam : match.awayTeam;
  return !isPlaceholderTeamName(team?.teamName)
    ? team?.teamName?.trim() || teamNameFromMatchName(match.matchName, side)
    : teamNameFromMatchName(match.matchName, side);
}

function scoreInsight(match: PublicOverviewMatch, scoreboard: Scoreboard): string {
  if (scoreboard.winnerSide === 'HOME') {
    return `${teamDisplayName(match, 'HOME')} 胜出或领先；比分、胜负、资料准备度与风险记录同步展示。`;
  }
  if (scoreboard.winnerSide === 'AWAY') {
    return `${teamDisplayName(match, 'AWAY')} 胜出或领先；比分、胜负、资料准备度与风险记录同步展示。`;
  }
  if (scoreboard.winnerSide === 'DRAW') {
    return '当前或最终为平局；两队攻防指标、伤停和临场阵容资料同步展示。';
  }
  if ((scoreboard.scoreDisplay || '').includes('待开球')) {
    return `待开球：${formatDisplayTime(match.kickoffTime || match.matchday)} 开始；阵容伤停、天气场地和资料准备度待同步。`;
  }
  if ((scoreboard.scoreDisplay || '').includes('待核')) {
    return '已完赛但正式赛果来源暂缺。';
  }
  return '比分暂未同步；开球时间、资料准备度和风险记录已展示。';
}

function integrityCopy(score?: number | null): string {
  const value = score ?? 0;
  if (value >= 85) {
    return '资料较齐，比分、阵容和证据链关联完整';
  }
  if (value >= 65) {
    return '基础资料可读，伤停、裁判、天气或市场信号存在缺口';
  }
  return '资料偏薄弱，球队、球员、赔率、舆情和证据来源缺口较多';
}

function riskCopy(count?: number | null): string {
  const value = count ?? 0;
  if (value >= 3) {
    return `有 ${value} 个风险，含冲突或高影响外部因素`;
  }
  if (value > 0) {
    return `有 ${value} 个风险，包含伤停、市场价格快照、天气或来源冲突`;
  }
  return '暂无公开风险记录，阵容、天气和市场价格快照暂未产生风险';
}

function readinessTone(score?: number | null) {
  return scoreTone(score);
}

function riskToneForCount(count?: number | null): 'success' | 'warning' | 'danger' {
  const value = count ?? 0;
  if (value >= 3) {
    return 'danger';
  }
  if (value > 0) {
    return 'warning';
  }
  return 'success';
}

function scoreResultTone(scoreboard: Scoreboard): 'success' | 'warning' | 'info' {
  if (scoreboard.winnerSide === 'HOME' || scoreboard.winnerSide === 'AWAY') {
    return 'success';
  }
  if (scoreboard.winnerSide === 'DRAW') {
    return 'warning';
  }
  return 'info';
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
.focus-board,
.match-intel-panel,
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
  grid-template-columns: minmax(0, 1fr) minmax(0, 320px);
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
.entry-card:focus-visible,
.match-intel-panel:focus-visible,
.match-intel-panel__rings:focus-visible,
.focus-board:focus-visible,
.focus-grid:focus-visible,
.overview-grid:focus-visible,
.match-list:focus-visible,
.overview-ring-grid:focus-visible,
.odds-ring-grid:focus-visible,
.decision-ring-grid:focus-visible,
.entry-grid:focus-visible {
  box-shadow: var(--wc-focus-ring);
  outline: none;
}

.kpi-grid,
.focus-grid,
.module-grid,
.entry-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.match-intel-panel {
  display: grid;
  gap: 16px;
  min-width: 0;
  padding: 18px;
}

.match-intel-panel__heading h2 {
  max-width: 860px;
}

.match-intel-panel__grid {
  display: grid;
  gap: 14px;
  grid-template-columns: minmax(0, 1.08fr) minmax(0, 0.82fr) minmax(0, 0.96fr);
}

.match-intel-panel__score,
.match-intel-panel__metrics,
.match-intel-panel__charts {
  min-width: 0;
}

.match-intel-panel__score,
.match-intel-panel__metrics,
.match-intel-panel__charts {
  display: grid;
  gap: 12px;
}

.match-intel-panel__copy,
.match-intel-panel__charts,
.match-metric-card,
.match-intel-panel__checklist li {
  background: rgba(15, 23, 42, 0.58);
  border: 1px solid rgba(147, 197, 253, 0.16);
  border-radius: var(--wc-radius-md);
}

.match-intel-panel__copy {
  color: var(--wc-text-muted);
  line-height: 1.7;
  margin: 0;
  padding: 14px;
}

.match-metric-card {
  display: grid;
  gap: 7px;
  min-height: 112px;
  padding: 14px;
}

.match-metric-card span,
.match-intel-panel__charts header span {
  color: var(--wc-text-muted);
  font-size: 12px;
  font-weight: 800;
}

.match-metric-card strong {
  color: var(--wc-primary);
  font-family: var(--wc-font-display);
  font-size: clamp(30px, 4vw, 44px);
  line-height: 1;
}

.match-metric-card p {
  color: var(--wc-text-muted);
  font-size: 13px;
  line-height: 1.55;
  margin: 0;
}

.match-metric-card--success {
  border-color: rgba(134, 239, 172, 0.32);
}

.match-metric-card--success strong {
  color: var(--wc-success);
}

.match-metric-card--warning {
  border-color: rgba(253, 230, 138, 0.34);
}

.match-metric-card--warning strong {
  color: var(--wc-warning);
}

.match-metric-card--danger {
  border-color: rgba(248, 113, 113, 0.34);
}

.match-metric-card--danger strong {
  color: var(--wc-danger);
}

.match-intel-panel__charts {
  align-content: start;
  background:
    radial-gradient(circle at 10% 0%, rgba(134, 239, 172, 0.08), transparent 36%),
    rgba(15, 23, 42, 0.58);
  padding: 16px;
}

.match-intel-panel__charts header {
  align-items: start;
  border-bottom: 1px solid rgba(148, 163, 184, 0.14);
  display: flex;
  gap: 10px;
  justify-content: space-between;
  padding-bottom: 12px;
}

.match-intel-panel__charts header strong {
  color: var(--wc-text);
  line-height: 1.35;
  max-width: 220px;
  text-align: right;
}

.match-intel-panel__rings {
  display: grid;
  gap: 10px;
  grid-template-columns: 1fr;
  min-width: 0;
}

.match-intel-panel__rings :deep(.coverage-donut) {
  background: rgba(2, 6, 23, 0.34);
  border: 1px solid rgba(147, 197, 253, 0.14);
  border-radius: var(--wc-radius-md);
  grid-template-columns: 76px minmax(0, 1fr);
  min-height: 104px;
  padding: 12px;
}

.match-intel-panel__rings :deep(.coverage-donut__copy) {
  gap: 5px;
}

.match-intel-panel__rings :deep(.coverage-donut__copy strong) {
  font-size: 15px;
}

.match-intel-panel__rings :deep(.coverage-donut__copy small) {
  font-size: 12px;
  line-height: 1.45;
}

.overview-ring-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(178px, 1fr));
  min-width: 0;
}

.odds-ring-grid,
.decision-ring-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(176px, 1fr));
  min-width: 0;
}

.odds-ring-grid :deep(.coverage-donut),
.decision-ring-grid :deep(.coverage-donut) {
  background: rgba(15, 23, 42, 0.4);
  border: 1px solid rgba(147, 197, 253, 0.16);
  border-radius: var(--wc-radius-md);
  padding: 12px;
}

.match-intel-panel__checklist {
  display: grid;
  gap: 8px;
  list-style: none;
  margin: 0;
  padding: 0;
}

.match-intel-panel__checklist li {
  color: var(--wc-text-muted);
  line-height: 1.6;
  padding: 10px 12px;
}

.focus-board {
  display: grid;
  gap: 16px;
  min-width: 0;
  padding: 18px;
}

.focus-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.module-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
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
  max-height: min(62dvh, 560px);
  overflow: auto;
  padding-right: 4px;
  scrollbar-color: rgba(147, 197, 253, 0.38) transparent;
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

.panel-bars {
  border-top: 1px solid rgba(148, 163, 184, 0.14);
  display: grid;
  gap: 12px;
  padding-top: 12px;
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
  .match-intel-panel__grid,
  .overview-grid {
    grid-template-columns: 1fr;
  }

  .command-panel--wide {
    grid-row: auto;
  }

  .kpi-grid,
  .focus-grid,
  .module-grid,
  .entry-grid,
  .match-intel-panel__rings,
  .overview-ring-grid,
  .odds-ring-grid,
  .decision-ring-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .overview-dashboard__content {
    gap: 14px;
  }

  .overview-hero,
  .focus-grid,
  .module-grid,
  .overview-grid,
  .match-intel-panel__grid {
    grid-template-columns: 1fr;
  }

  .kpi-grid,
  .entry-grid,
  .metric-list,
  .metric-list--integrity,
  .match-intel-panel__rings,
  .overview-ring-grid,
  .odds-ring-grid,
  .decision-ring-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .match-intel-panel__rings {
    grid-template-columns: 1fr;
  }

  .overview-hero,
  .command-panel,
  .focus-board,
  .match-intel-panel,
  .kpi-card,
  .entry-card {
    border-radius: var(--wc-radius-md);
    gap: 10px;
    padding: 12px;
  }

  .overview-hero h1 {
    font-size: clamp(28px, 12vw, 42px);
    margin-bottom: 8px;
  }

  .overview-hero p:not(.eyebrow),
  .match-intel-panel__copy,
  .match-metric-card p,
  .match-intel-panel__checklist,
  .panel-note,
  .panel-bars :deep(.metric-bar small),
  .match-intel-panel__charts > :deep(.metric-bar small) {
    display: none;
  }

  .kpi-grid,
  .focus-grid,
  .entry-grid,
  .metric-list,
  .metric-list--integrity,
  .match-intel-panel__rings,
  .overview-ring-grid,
  .odds-ring-grid,
  .decision-ring-grid,
  .match-intel-panel__metrics,
  .match-list {
    gap: 10px;
  }

  .match-intel-panel__rings :deep(.coverage-donut),
  .overview-ring-grid :deep(.coverage-donut),
  .odds-ring-grid :deep(.coverage-donut),
  .decision-ring-grid :deep(.coverage-donut) {
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

  .match-intel-panel__rings :deep(.coverage-donut__ring),
  .overview-ring-grid :deep(.coverage-donut__ring),
  .odds-ring-grid :deep(.coverage-donut__ring),
  .decision-ring-grid :deep(.coverage-donut__ring) {
    width: 66px;
  }

  .match-intel-panel__rings :deep(.coverage-donut__ring span),
  .overview-ring-grid :deep(.coverage-donut__ring span),
  .odds-ring-grid :deep(.coverage-donut__ring span),
  .decision-ring-grid :deep(.coverage-donut__ring span) {
    font-size: 20px;
  }

  .match-intel-panel__rings :deep(.coverage-donut__copy),
  .overview-ring-grid :deep(.coverage-donut__copy),
  .odds-ring-grid :deep(.coverage-donut__copy),
  .decision-ring-grid :deep(.coverage-donut__copy) {
    gap: 3px;
  }

  .match-intel-panel__rings :deep(.coverage-donut__copy strong),
  .overview-ring-grid :deep(.coverage-donut__copy strong),
  .odds-ring-grid :deep(.coverage-donut__copy strong),
  .decision-ring-grid :deep(.coverage-donut__copy strong) {
    font-size: 12px;
    line-height: 1.2;
  }

  .match-intel-panel__rings :deep(.coverage-donut__copy small),
  .overview-ring-grid :deep(.coverage-donut__copy small),
  .odds-ring-grid :deep(.coverage-donut__copy small),
  .decision-ring-grid :deep(.coverage-donut__copy small) {
    font-size: 11px;
    line-height: 1.25;
  }

  .match-list {
    max-height: min(42dvh, 320px);
    overflow: auto;
    padding-right: 0;
    scrollbar-gutter: auto;
    scrollbar-width: none;
  }

  .match-list::-webkit-scrollbar {
    display: none;
  }

  .entry-grid,
  .overview-ring-grid,
  .odds-ring-grid,
  .decision-ring-grid {
    max-height: none;
    overflow: visible;
    padding-right: 0;
    scrollbar-gutter: auto;
  }

  .kpi-card,
  .entry-card,
  .metric-list div,
  .match-metric-card {
    min-height: 96px;
    padding: 10px;
  }

  .kpi-card strong,
  .metric-list dd,
  .match-metric-card strong {
    font-size: 26px;
  }

  .match-card {
    border-radius: 14px;
    gap: 5px;
    padding: 10px;
  }

  .match-card :deep(.scoreboard-card),
  .focus-grid :deep(.scoreboard-card),
  .match-intel-panel__score :deep(.scoreboard-card) {
    gap: 8px;
    padding: 10px;
  }

  .match-card :deep(.scoreboard-card__main),
  .focus-grid :deep(.scoreboard-card__main),
  .match-intel-panel__score :deep(.scoreboard-card__main) {
    grid-template-columns: minmax(0, 1fr) 84px minmax(0, 1fr) !important;
  }

  .match-card :deep(.scoreboard-card__team),
  .focus-grid :deep(.scoreboard-card__team),
  .match-intel-panel__score :deep(.scoreboard-card__team) {
    border: 0;
    padding: 0;
  }

  .match-card :deep(.scoreboard-card__team .flag-team),
  .focus-grid :deep(.scoreboard-card__team .flag-team),
  .match-intel-panel__score :deep(.scoreboard-card__team .flag-team) {
    align-items: center;
    flex-direction: column;
    gap: 5px;
  }

  .match-card :deep(.scoreboard-card__team--away .flag-team),
  .focus-grid :deep(.scoreboard-card__team--away .flag-team),
  .match-intel-panel__score :deep(.scoreboard-card__team--away .flag-team) {
    align-items: center;
    flex-direction: column;
  }

  .match-card :deep(.scoreboard-card__team--away),
  .focus-grid :deep(.scoreboard-card__team--away),
  .match-intel-panel__score :deep(.scoreboard-card__team--away) {
    justify-items: center;
    text-align: center;
  }

  .match-card :deep(.scoreboard-card__team .flag-team__copy),
  .focus-grid :deep(.scoreboard-card__team .flag-team__copy),
  .match-intel-panel__score :deep(.scoreboard-card__team .flag-team__copy) {
    justify-items: center;
    text-align: center;
    width: 100%;
  }

  .match-card :deep(.scoreboard-card__team .flag-team__copy strong),
  .focus-grid :deep(.scoreboard-card__team .flag-team__copy strong),
  .match-intel-panel__score :deep(.scoreboard-card__team .flag-team__copy strong) {
    font-size: 12px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .match-card :deep(.scoreboard-card__score),
  .focus-grid :deep(.scoreboard-card__score),
  .match-intel-panel__score :deep(.scoreboard-card__score) {
    min-width: 0;
    padding: 7px 5px;
    width: 84px;
  }

  .match-card :deep(.scoreboard-card__score strong),
  .focus-grid :deep(.scoreboard-card__score strong),
  .match-intel-panel__score :deep(.scoreboard-card__score strong) {
    font-size: 18px;
  }

  .focus-grid :deep(.scoreboard-card__signals),
  .match-intel-panel__score :deep(.scoreboard-card__signals) {
    grid-template-columns: 1fr auto;
  }

  .focus-grid :deep(.scoreboard-card__signals .metric-bar small),
  .match-intel-panel__score :deep(.scoreboard-card__signals .metric-bar small) {
    display: none;
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
