<template>
  <section class="page-shell evidence-hub" aria-labelledby="evidence-hub-title">
    <section class="page-content evidence-hub__content">
      <header class="evidence-hero">
        <div class="evidence-hero__copy">
          <p class="eyebrow">证据中心入口</p>
          <h1 id="evidence-hub-title">证据中心总览</h1>
          <div class="evidence-hero__actions" aria-label="证据中心快捷入口">
            <RouterLink class="primary-link" to="/evidence/matches">查看比赛比分</RouterLink>
            <RouterLink class="secondary-link" to="/evidence/sentiment">查看外部风险</RouterLink>
          </div>
        </div>

        <article class="evidence-hero__panel" aria-label="公开证据覆盖总分" data-test="evidence-hub-score" tabindex="0">
          <span>公开数据可读性</span>
          <CoverageDonut
            label="综合覆盖"
            :value="overviewScore"
            unit="%"
            :tone="overviewTone"
            :caption="overviewCaption"
          />
        </article>
      </header>

      <div v-if="loading && !hasAnyData" class="loading-panel" aria-live="polite">
        <span class="loading-panel__bar"></span>
        <span class="loading-panel__bar loading-panel__bar--short"></span>
        <span>正在汇总公开证据入口...</span>
      </div>

      <div v-else-if="loadError" class="error-panel" role="alert">
        <strong>证据中心总览暂不可用</strong>
        <span>{{ loadError }}</span>
        <button class="secondary-link evidence-hub__retry" type="button" @click="loadEvidenceHub">重试</button>
      </div>

      <template v-else>
        <div v-if="loadWarning" class="warning-panel" role="status">
          <strong>部分数据暂未同步</strong>
          <span>{{ loadWarning }}</span>
        </div>

        <section v-if="headlineMatch" class="headline-match" aria-label="焦点比赛数据" tabindex="0">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">焦点比赛</p>
              <h2>比赛状态：{{ headlineMatch.matchName }}</h2>
            </div>
            <RouterLink class="panel-link" :to="`/evidence/matches?matchId=${headlineMatch.id}`">打开比赛中心</RouterLink>
          </div>
          <div class="headline-match__grid">
            <ScoreboardCard
              :home-team="headlineMatch.homeTeam"
              :away-team="headlineMatch.awayTeam"
              :scoreboard="headlineMatch.scoreboard"
              :match-name="headlineMatch.matchName"
              :meta="matchMeta(headlineMatch)"
              :status="statusLabel(headlineMatch.status || headlineMatch.resultStatus)"
              :evidence-count="headlineMatch.evidenceCount"
              :risk-count="headlineRiskCount"
            />
            <div class="headline-match__status">
              <strong>{{ headlineVerdict }}</strong>
              <p>{{ headlineCopy }}</p>
              <ul class="headline-match__facts" aria-label="焦点比赛摘要">
                <li>来源证据：{{ headlineMatch.evidenceCount ?? 0 }} 项</li>
                <li>外部风险：{{ headlineRiskCount }} 条</li>
                <li>比赛状态：{{ headlineStatusSummary }}</li>
              </ul>
            </div>
          </div>
        </section>

        <section class="stat-grid" aria-label="证据中心关键统计" tabindex="0">
          <article v-for="stat in topStats" :key="stat.label" class="stat-card" :class="`stat-card--${stat.tone}`">
            <span>{{ stat.label }}</span>
            <strong>{{ stat.value }}</strong>
            <small>{{ stat.caption }}</small>
          </article>
        </section>

        <section class="evidence-catalog-structure" data-test="evidence-catalog-rings" tabindex="0" aria-label="证据目录结构">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">目录结构</p>
              <h2>证据目录结构</h2>
            </div>
            <span class="command-status">{{ evidenceCatalogTotal }} 项材料</span>
          </div>
          <div class="evidence-catalog-rings" tabindex="0">
            <CoverageDonut
              v-for="ring in evidenceCatalogRings"
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

        <section class="evidence-command-board" tabindex="0" aria-label="PC 端证据态势" data-test="evidence-command-board">
          <article class="command-priority-card">
            <div class="panel-heading">
              <div>
                <p class="eyebrow">PC 证据态势</p>
                <h2>主要数据缺口</h2>
              </div>
              <span class="command-status">风险排序</span>
            </div>
            <div class="triage-action-list">
              <article
                v-for="(action, index) in evidenceTriageActions"
                :key="action.id"
                class="triage-action-card"
                :class="`triage-action-card--${action.tone}`"
              >
                <span class="triage-action-card__rank">{{ String(index + 1).padStart(2, '0') }}</span>
                <div>
                  <small>{{ action.kicker }} · {{ action.value }}</small>
                  <strong>{{ action.title }}</strong>
                  <p>{{ action.body }}</p>
                </div>
                <RouterLink class="triage-action-card__link" :to="action.to">{{ action.cta }}</RouterLink>
              </article>
            </div>
          </article>

          <article class="command-readiness-card">
            <div class="panel-heading">
              <div>
                <p class="eyebrow">五类证据覆盖</p>
                <h2>证据维度覆盖</h2>
              </div>
            </div>
            <div class="readiness-ring-grid" aria-label="五类证据覆盖环形图" tabindex="0">
              <CoverageDonut
                v-for="ring in evidenceReadinessRings"
                :key="ring.key"
                :label="ring.label"
                :value="ring.value"
                unit="%"
                :tone="ring.tone"
                size="compact"
                :caption="ring.caption"
              />
            </div>
            <div class="readiness-row-list">
              <article v-for="row in evidenceReadinessRows" :key="row.key" class="readiness-row">
                <div class="readiness-row__copy">
                  <strong>{{ row.label }}</strong>
                  <span>{{ row.question }}</span>
                </div>
                <MetricBar
                  :label="row.label"
                  :value="row.value"
                  :max="row.max"
                  :unit="row.unit"
                  :tone="row.tone"
                  :caption="row.caption"
                />
                <small>{{ row.next }}</small>
              </article>
            </div>
          </article>
        </section>

        <section class="quality-board" tabindex="0" aria-label="证据质量速览" data-test="evidence-quality-board">
          <article class="quality-score-card">
            <p class="eyebrow">证据质量速览</p>
            <h2>证据覆盖评分</h2>
            <strong>{{ overviewScore }}%</strong>
            <span>{{ overviewCaption }}</span>
          </article>
          <div class="quality-bars" aria-label="证据覆盖条形图" tabindex="0">
            <MetricBar
              v-for="bar in evidenceQualityBars"
              :key="bar.label"
              :label="bar.label"
              :value="bar.value"
              :max="bar.max"
              :unit="bar.unit"
              :tone="bar.tone"
              :caption="bar.caption"
            />
          </div>
          <article class="quality-action-card">
            <p class="eyebrow">公开缺口</p>
            <h3>公开数据缺口</h3>
            <div class="quality-gap-rings" tabindex="0" aria-label="公开缺口结构环形图">
              <CoverageDonut
                v-for="ring in evidenceQualityGapRings"
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
            <ol>
              <li v-for="action in evidenceQualityActions" :key="action">{{ action }}</li>
            </ol>
          </article>
        </section>

        <section class="data-literacy-board" tabindex="0" aria-label="数据维度覆盖矩阵" data-test="evidence-data-literacy-board">
          <article class="data-literacy-intro">
            <p class="eyebrow">数据覆盖矩阵</p>
            <h2>六类证据覆盖情况</h2>
          </article>
          <div class="data-literacy-bars" aria-label="数据维度覆盖条" tabindex="0">
            <article v-for="item in evidenceDataRows" :key="item.label" class="data-matrix-card">
              <div class="data-matrix-card__copy">
                <span>{{ item.plainLabel }}</span>
                <strong>{{ item.label }}</strong>
              </div>
              <MetricBar
                :label="item.metricLabel"
                :value="item.value"
                :max="item.max"
                :unit="item.unit"
                :tone="item.tone"
                :caption="item.caption"
              />
            </article>
          </div>
        </section>

        <section class="supplement-priority-board" tabindex="0" aria-label="AI 分析补采优先级" data-test="evidence-supplement-priority">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">补采优先级</p>
              <h2>AI 分析补采优先级</h2>
            </div>
            <span class="command-status">{{ supplementPendingCount }} 类待补</span>
          </div>
          <div class="supplement-priority-grid">
            <article
              v-for="item in supplementPriorityCards"
              :key="item.key"
              class="supplement-priority-card"
              :class="`supplement-priority-card--${item.tone}`"
            >
              <span class="supplement-priority-card__rank">{{ item.rank }}</span>
              <div class="supplement-priority-card__copy">
                <small>{{ item.kicker }}</small>
                <strong>{{ item.title }}</strong>
                <p>{{ item.reason }}</p>
              </div>
              <MetricBar
                :label="item.metricLabel"
                :value="item.value"
                :max="item.max"
                :unit="item.unit"
                :tone="item.tone"
                :caption="item.caption"
              />
              <RouterLink class="supplement-priority-card__link" :to="item.to">{{ item.cta }}</RouterLink>
            </article>
          </div>
        </section>

        <section class="route-grid" tabindex="0" aria-label="证据中心分区入口" data-test="evidence-route-grid">
          <RouterLink v-for="card in routeCards" :key="card.to" class="route-card" :to="card.to" :class="`route-card--${card.tone}`">
            <div class="route-card__top">
              <span>{{ card.kicker }}</span>
              <strong>{{ card.count }}</strong>
            </div>
            <h2>{{ card.title }}</h2>
            <ul>
              <li v-for="item in card.highlights" :key="item">{{ item }}</li>
            </ul>
            <span class="route-card__cta">{{ card.cta }}</span>
          </RouterLink>
        </section>

        <section class="coverage-grid" tabindex="0" aria-label="证据缺口统计">
          <article class="coverage-panel">
            <div class="panel-heading">
              <div>
                <p class="eyebrow">缺口雷达</p>
                <h2>证据缺口统计</h2>
              </div>
            </div>
            <div class="gap-list" data-test="evidence-gap-list" tabindex="0">
              <article v-for="gap in evidenceGaps" :key="gap.label" class="gap-card" :class="{ 'gap-card--active': gap.value > 0 }">
                <span>{{ gap.label }}</span>
                <strong>{{ gap.value }}</strong>
                <p>{{ gap.caption }}</p>
              </article>
            </div>
          </article>

          <article class="coverage-panel coverage-panel--data-map" aria-label="足球 AI 分析数据维度" tabindex="0">
            <div class="panel-heading">
              <div>
                <p class="eyebrow">数据维度</p>
                <h2>AI 足球分析数据维度</h2>
              </div>
            </div>
            <div class="ai-data-ring-grid" tabindex="0" aria-label="AI 足球分析数据维度覆盖环形图">
              <CoverageDonut
                v-for="ring in aiDataRings"
                :key="ring.label"
                :label="ring.label"
                :value="ring.value"
                unit="%"
                :tone="ring.tone"
                size="compact"
                :caption="ring.caption"
              />
            </div>
            <div class="data-map">
              <span v-for="item in aiDataMap" :key="item">{{ item }}</span>
            </div>
          </article>
        </section>
      </template>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import CoverageDonut from '@/components/football/CoverageDonut.vue';
import MetricBar from '@/components/football/MetricBar.vue';
import ScoreboardCard from '@/components/football/ScoreboardCard.vue';
import { listPublicMatches, type PublicMatchSummary } from '@/api/matches';
import { listPublicOddsOverview, type PublicOddsMarketSummary } from '@/api/odds';
import { listPublicSentimentOverview, type PublicSentimentFactorSummary } from '@/api/sentiment';
import {
  listPublicPlayerProfiles,
  listPublicTeamProfiles,
  type PublicPlayerProfileSummary,
  type PublicTeamProfileSummary,
} from '@/api/profiles';
import type { ApiResponse } from '@/api/system';
import { scoreboardFallback, scoreTone, statusLabel } from '@/utils/football-visuals';

const REQUIRED_EXTERNAL_CATEGORIES = [
  '天气、场馆与转播',
  '裁判与判罚',
  '伤停与训练',
  '赛程旅行休息',
  '近期赛果/状态走势',
  '战意轮换',
  '更衣室/发布会',
  '媒体球迷',
  '赔率与市场价格',
  '历史交锋战术对位',
  '进球方式与时间',
  '关键球员进球参与',
  '基础技术统计/球队风格画像',
  '犯规/越位样本',
  '门将/防守承压',
  '阵容结构/年龄经验',
  '首发位置结构',
];

const PRIORITY_EXTERNAL_CATEGORIES = [
  '裁判与判罚',
  '伤停与训练',
  '赛程旅行休息',
  '近期赛果/状态走势',
  '战意轮换',
  '赔率与市场价格',
  '历史交锋战术对位',
];

const aiDataMap = [
  'xG/xGA/PPDA',
  '伤停与训练',
  '裁判倾向',
  '市场价格快照',
  '公众热度/市场倾向',
  '球队新闻/发布会',
  '赛程旅行休息',
  '天气、场馆与转播',
  '历史交锋/战术对位',
  '进球方式与时间',
  '关键球员进球参与',
  '基础技术统计/球队风格画像',
  '犯规/越位/点球尝试样本',
  '门将/防守承压',
  '阵容结构/年龄经验',
  '首发位置结构',
];

const matches = ref<PublicMatchSummary[]>([]);
const odds = ref<PublicOddsMarketSummary[]>([]);
const sentiments = ref<PublicSentimentFactorSummary[]>([]);
const teams = ref<PublicTeamProfileSummary[]>([]);
const players = ref<PublicPlayerProfileSummary[]>([]);
const loading = ref(false);
const loadError = ref('');
const loadWarning = ref('');

const hasAnyData = computed(() =>
  matches.value.length + odds.value.length + sentiments.value.length + teams.value.length + players.value.length > 0,
);

const headlineMatch = computed(() => matches.value[0] ?? null);
const scoreReadyCount = computed(() => matches.value.filter((match) => hasScore(match)).length);
const finishedCount = computed(() => matches.value.filter((match) => isFinished(match)).length);
const missingFinishedScoreCount = computed(() => matches.value.filter((match) => isFinished(match) && !hasScore(match)).length);
const conflictCount = computed(() => matches.value.reduce((total, match) => total + (match.conflictCount ?? 0), 0));
const evidenceCount = computed(() => matches.value.reduce((total, match) => total + (match.evidenceCount ?? 0), 0));
const matchesWithEvidenceCount = computed(() => matches.value.filter((match) => (match.evidenceCount ?? 0) > 0).length);
const bookmakerCount = computed(() => new Set(odds.value.map((item) => item.bookmaker).filter(Boolean)).size);
const liveOddsCount = computed(() => odds.value.filter((item) => isLiveSnapshot(item.snapshotType)).length);
const highRiskCount = computed(() => sentiments.value.filter((item) => isHighRisk(item.highestRiskLevel)).length);
const staleFactorCount = computed(() => sentiments.value.filter((item) => item.stale).length);
const coveredExternalLabels = computed(() => new Set(sentiments.value
  .map((item) => categoryLabel([item.factorCategory, item.factorType].filter(Boolean).join(' ')))
  .filter(Boolean)));
const coveredExternalCount = computed(() => coveredExternalLabels.value.size);
const priorityExternalCoveredCount = computed(() => PRIORITY_EXTERNAL_CATEGORIES.filter((category) => coveredExternalLabels.value.has(category)).length);
const teamWithProfileCount = computed(() => teams.value.filter((team) => (team.factCount ?? 0) > 0 || Boolean(team.attackProfile || team.defenseProfile)).length);
const teamTacticalProfileCount = computed(() => teams.value.filter((team) => Boolean(team.attackProfile || team.defenseProfile)).length);
const teamTechnicalMetricCount = computed(() => teams.value.filter((team) => (team.technicalMetricCount ?? 0) > 0).length);
const teamAdvancedMetricCount = computed(() => teams.value.filter((team) => (team.advancedMetricCount ?? 0) > 0).length);
const teamCountryContextCount = computed(() => teams.value.filter((team) => Boolean(team.countryIso2 && team.confederation && team.groupName)).length);
const playerWithStatusCount = computed(() => players.value.filter((player) => Boolean(player.status || player.injuryStatus || player.cardStatus || player.lockerRoomStatus)).length);
const playerAvailabilityDetailCount = computed(() => players.value.filter((player) => Boolean(player.injuryStatus || player.cardStatus || player.lockerRoomStatus)).length);
const playerPerformanceMetricCount = computed(() => players.value.filter((player) => (player.performanceMetricCount ?? 0) > 0).length);
const playerAdvancedMetricCount = computed(() => players.value.filter((player) => (player.advancedMetricCount ?? 0) > 0).length);
const kickoffReadyCount = computed(() => matches.value.filter((match) => Boolean(match.kickoffTime)).length);
const headlineRiskCount = computed(() => sentiments.value.filter((item) => item.matchId === headlineMatch.value?.id).length);
const oddsCoveredMatchCount = computed(() => {
  const matchIds = new Set<number>();
  odds.value.forEach((item) => {
    if (typeof item.matchId === 'number') {
      matchIds.add(item.matchId);
    }
  });
  return matchIds.size || Math.min(odds.value.length, matches.value.length);
});
const conflictFreeMatchCount = computed(() => Math.max(0, matches.value.length - matches.value.filter((match) => (match.conflictCount ?? 0) > 0).length));

const overviewScore = computed(() => {
  const parts = [
    percentage(scoreReadyCount.value, Math.max(matches.value.length, 1)),
    percentage(oddsCoveredMatchCount.value, Math.max(matches.value.length, 1)),
    percentage(Math.min(coveredExternalCount.value, REQUIRED_EXTERNAL_CATEGORIES.length), REQUIRED_EXTERNAL_CATEGORIES.length),
    percentage(teamWithProfileCount.value, Math.max(teams.value.length, 1)),
    percentage(playerWithStatusCount.value, Math.max(players.value.length, 1)),
  ];
  return Math.round(parts.reduce((sum, item) => sum + item, 0) / parts.length);
});

const overviewTone = computed(() => scoreTone(overviewScore.value));
const overviewCaption = computed(() => {
  if (overviewScore.value >= 80) {
    return '公开证据资料较齐。';
  }
  if (overviewScore.value >= 55) {
    return '基础可读，关键缺口仍存在。';
  }
  return '数据偏薄弱，缺口较多。';
});
const overviewSummary = computed(() =>
  `已接入 ${matches.value.length} 场比赛、${evidenceCount.value} 项来源证据、${odds.value.length} 个市场快照、${sentiments.value.length} 条外部因素、${teams.value.length} 支球队和 ${players.value.length} 名球员。`,
);

const headlineVerdict = computed(() => {
  if (!headlineMatch.value) {
    return '暂无焦点比赛';
  }
  const scoreboard = scoreboardFallback(headlineMatch.value as unknown as Record<string, unknown>);
  return `${scoreboard.scoreDisplay || '比分待同步'} · ${scoreboard.resultText || '赛果待同步'}`;
});
const headlineCopy = computed(() => {
  if (!headlineMatch.value) {
    return '正式库同步比赛数据后，总览显示第一场可读比赛。';
  }
  if (hasScore(headlineMatch.value)) {
    return '这场已有比分记录，胜负结果与证据质量、冲突数据已关联。';
  }
  return '这场暂无可靠比分，开球时间、赛程来源和阵容/伤停为当前关联资料。';
});
const headlineStatusSummary = computed(() => {
  if (!headlineMatch.value) {
    return '待同步';
  }
  if (hasScore(headlineMatch.value)) {
    return '比分已入库';
  }
  return statusLabel(headlineMatch.value.status || headlineMatch.value.resultStatus);
});

const topStats = computed(() => [
  { label: '公开比赛', value: matches.value.length, caption: `${scoreReadyCount.value} 场已有比分记录`, tone: 'score' },
  { label: '来源证据', value: evidenceCount.value, caption: `${matchesWithEvidenceCount.value} 场比赛已有来源材料`, tone: 'evidence' },
  { label: '市场快照', value: odds.value.length, caption: `${bookmakerCount.value} 个来源，${liveOddsCount.value} 个实时市场`, tone: 'market' },
  { label: '外部因素', value: sentiments.value.length, caption: `${highRiskCount.value} 条高风险，${staleFactorCount.value} 条过期`, tone: 'risk' },
  { label: '球队/球员', value: teams.value.length + players.value.length, caption: `${teams.value.length} 支球队，${players.value.length} 名球员`, tone: 'profile' },
]);

const profileMaterialCount = computed(() => teams.value.length + players.value.length);
const evidenceCatalogTotal = computed(() => matches.value.length + evidenceCount.value + odds.value.length + sentiments.value.length + profileMaterialCount.value);
const evidenceCatalogMax = computed(() => Math.max(1, matches.value.length, evidenceCount.value, odds.value.length, sentiments.value.length, profileMaterialCount.value, evidenceCatalogTotal.value));
const evidenceCatalogRings = computed(() => [
  {
    label: '公开比赛',
    value: matches.value.length,
    max: evidenceCatalogMax.value,
    unit: '场',
    tone: matches.value.length ? 'success' : 'info',
    caption: `${matches.value.length} 场公开比赛`,
  },
  {
    label: '来源证据',
    value: evidenceCount.value,
    max: evidenceCatalogMax.value,
    unit: '项',
    tone: evidenceCount.value ? 'success' : 'warning',
    caption: `${evidenceCount.value} 项正式来源证据`,
  },
  {
    label: '市场快照',
    value: odds.value.length,
    max: evidenceCatalogMax.value,
    unit: '个',
    tone: odds.value.length ? 'success' : 'warning',
    caption: `${odds.value.length} 个公开市场快照`,
  },
  {
    label: '外部因素',
    value: sentiments.value.length,
    max: evidenceCatalogMax.value,
    unit: '条',
    tone: highRiskCount.value ? 'warning' : sentiments.value.length ? 'success' : 'info',
    caption: `${sentiments.value.length} 条外部因素记录`,
  },
  {
    label: '画像资料',
    value: profileMaterialCount.value,
    max: evidenceCatalogMax.value,
    unit: '项',
    tone: profileMaterialCount.value ? 'accent' : 'info',
    caption: `${teams.value.length} 支球队 + ${players.value.length} 名球员`,
  },
  {
    label: '材料合计',
    value: evidenceCatalogTotal.value,
    max: Math.max(1, evidenceCatalogTotal.value),
    unit: '项',
    tone: evidenceCatalogTotal.value ? 'accent' : 'info',
    caption: `${evidenceCatalogTotal.value} 项公开证据目录材料`,
  },
]);

const evidenceTriageActions = computed(() => {
  const externalGap = Math.max(0, REQUIRED_EXTERNAL_CATEGORIES.length - coveredExternalCount.value);
  const actions = [
    {
      id: 'score',
      rank: missingFinishedScoreCount.value ? 100 : 20,
      tone: missingFinishedScoreCount.value ? 'danger' : 'success',
      kicker: '比分记录',
      title: missingFinishedScoreCount.value ? '完赛缺比分' : '比分记录可读',
      value: missingFinishedScoreCount.value ? `${missingFinishedScoreCount.value} 场` : `${scoreReadyCount.value}/${matches.value.length} 场`,
      body: missingFinishedScoreCount.value
        ? '完赛比分缺口会降低证据、市场和复盘关联度。'
        : '已有比分的比赛已关联胜负强对比和证据数据。',
      to: { path: '/evidence/matches', query: { filter: missingFinishedScoreCount.value ? 'MISSING_FINISHED' : 'HAS_SCORE' } },
      cta: '打开比赛中心',
    },
    {
      id: 'conflict',
      rank: conflictCount.value ? 90 : 18,
      tone: conflictCount.value ? 'warning' : 'success',
      kicker: '证据冲突',
      title: conflictCount.value ? '来源冲突' : '冲突压力较低',
      value: `${conflictCount.value} 项`,
      body: conflictCount.value
        ? '公开冲突记录关联官方比分、赛程、阵容和第二来源。'
        : '暂无公开冲突记录；来源时效和外部风险为当前展示项。',
      to: { path: '/evidence/matches', query: { filter: 'CONFLICT' } },
      cta: '冲突比赛',
    },
    {
      id: 'external',
      rank: externalGap ? 80 : 16,
      tone: externalGap ? 'warning' : 'success',
      kicker: '外部因素',
      title: externalGap ? '场外变量缺口' : '外因覆盖较齐',
      value: `${coveredExternalCount.value}/${REQUIRED_EXTERNAL_CATEGORIES.length} 类`,
      body: externalGap
        ? '伤停训练、裁判尺度、赛程旅行、近期赛果、关键球员贡献、出线/轮换和赔率与市场价格存在缺口。'
        : `${REQUIRED_EXTERNAL_CATEGORIES.length} 类外部因素和基础技术维度可读，风险等级和过期情况已展示。`,
      to: '/evidence/sentiment',
      cta: '外部因素详情',
    },
    {
      id: 'player',
      rank: playersMissingFactsCount.value ? 70 : 14,
      tone: playersMissingFactsCount.value ? 'warning' : 'success',
      kicker: '球员状态',
      title: playersMissingFactsCount.value ? '核心球员事实缺口' : '球员状态基础可读',
      value: playersMissingFactsCount.value ? `${playersMissingFactsCount.value} 人` : `${playerWithStatusCount.value} 人`,
      body: playersMissingFactsCount.value
        ? '核心球员伤停、训练负荷、停赛和预计出场资料缺口。'
        : '球员可用性线索已同步。',
      to: '/evidence/players',
      cta: '查看球员画像',
    },
  ];
  return actions.sort((left, right) => right.rank - left.rank).slice(0, 3);
});

const evidenceReadinessRows = computed(() => [
  {
    key: 'score',
    label: '比分/赛果',
    question: '胜负与比分记录',
    value: scoreReadyCount.value,
    max: Math.max(matches.value.length, 1),
    unit: '场',
    tone: scoreReadyCount.value >= matches.value.length && matches.value.length ? 'success' : scoreReadyCount.value ? 'warning' : 'danger',
    caption: `${scoreReadyCount.value} / ${matches.value.length} 场已有比分记录。`,
    next: missingFinishedScoreCount.value ? '完赛缺比分待官方来源。' : '胜负强对比已关联。',
  },
  {
    key: 'market',
    label: '市场快照',
    question: '市场价格记录',
    value: oddsCoveredMatchCount.value,
    max: Math.max(matches.value.length, 1),
    unit: '场',
    tone: oddsCoveredMatchCount.value >= matches.value.length && matches.value.length ? 'success' : oddsCoveredMatchCount.value ? 'warning' : 'danger',
    caption: `${oddsCoveredMatchCount.value} / ${matches.value.length} 场有市场快照。`,
    next: '市场快照展示公开市场倾向。',
  },
  {
    key: 'external',
    label: '外部风险',
    question: '外因覆盖量',
    value: coveredExternalCount.value,
    max: REQUIRED_EXTERNAL_CATEGORIES.length,
    unit: '类',
    tone: coveredExternalCount.value >= 7 ? 'success' : coveredExternalCount.value >= 3 ? 'warning' : 'danger',
    caption: `${coveredExternalCount.value} / ${REQUIRED_EXTERNAL_CATEGORIES.length} 类外因已覆盖。`,
    next: '伤停、裁判、旅行休息、公众热度存在缺口。',
  },
  {
    key: 'team',
    label: '球队资料',
    question: '国家与攻防画像覆盖',
    value: teamWithProfileCount.value,
    max: Math.max(teams.value.length, 1),
    unit: '队',
    tone: teamWithProfileCount.value >= teams.value.length && teams.value.length ? 'success' : teamWithProfileCount.value ? 'warning' : 'danger',
    caption: `${teamWithProfileCount.value} / ${teams.value.length} 支球队有画像线索。`,
    next: 'xG/xGA/PPDA、近期状态和发布会为画像缺口。',
  },
  {
    key: 'player',
    label: '球员可用性',
    question: '关键球员可用性',
    value: playerWithStatusCount.value,
    max: Math.max(players.value.length, 1),
    unit: '人',
    tone: playerWithStatusCount.value >= players.value.length && players.value.length ? 'success' : playerWithStatusCount.value ? 'warning' : 'danger',
    caption: `${playerWithStatusCount.value} / ${players.value.length} 名球员有状态线索。`,
    next: '核心球员伤停、停赛、训练和预计首发存在缺口。',
  },
]);

const evidenceReadinessRings = computed(() => evidenceReadinessRows.value.map((row) => ({
  key: row.key,
  label: row.label,
  value: Math.round(percentage(row.value, row.max)),
  tone: row.tone,
  caption: `${row.value} / ${row.max}${row.unit} · ${row.question}`,
})));

const evidenceQualityBars = computed(() => [
  {
    label: '比分记录',
    value: scoreReadyCount.value,
    max: Math.max(matches.value.length, 1),
    unit: '场',
    tone: scoreReadyCount.value >= matches.value.length && matches.value.length ? 'success' : scoreReadyCount.value ? 'warning' : 'danger',
    caption: `${scoreReadyCount.value} / ${matches.value.length} 场已有比分；完赛无比分显示为缺口。`,
  },
  {
    label: '来源证据',
    value: evidenceCount.value,
    max: Math.max(evidenceCount.value, matches.value.length * 4, 1),
    unit: '项',
    tone: matchesWithEvidenceCount.value >= matches.value.length && matches.value.length ? 'success' : matchesWithEvidenceCount.value ? 'warning' : 'danger',
    caption: `${evidenceCount.value} 项来源证据覆盖 ${matchesWithEvidenceCount.value} / ${matches.value.length} 场比赛。`,
  },
  {
    label: '市场目录',
    value: oddsCoveredMatchCount.value,
    max: Math.max(matches.value.length, 1),
    unit: '场',
    tone: oddsCoveredMatchCount.value >= matches.value.length && matches.value.length ? 'success' : oddsCoveredMatchCount.value ? 'warning' : 'danger',
    caption: `${odds.value.length} 个市场快照覆盖 ${oddsCoveredMatchCount.value} 场；展示隐含概率和来源状态。`,
  },
  {
    label: '外因覆盖',
    value: coveredExternalCount.value,
    max: REQUIRED_EXTERNAL_CATEGORIES.length,
    unit: '类',
    tone: coveredExternalCount.value >= 7 ? 'success' : coveredExternalCount.value >= 3 ? 'warning' : 'danger',
    caption: `已覆盖 ${coveredExternalCount.value} / ${REQUIRED_EXTERNAL_CATEGORIES.length} 类外部因素。`,
  },
  {
    label: '球队画像',
    value: teamWithProfileCount.value,
    max: Math.max(teams.value.length, 1),
    unit: '队',
    tone: teamWithProfileCount.value >= teams.value.length && teams.value.length ? 'success' : teamWithProfileCount.value ? 'warning' : 'danger',
    caption: `${teamWithProfileCount.value} / ${teams.value.length} 支球队有画像事实或攻防线索。`,
  },
  {
    label: '球员状态',
    value: playerWithStatusCount.value,
    max: Math.max(players.value.length, 1),
    unit: '人',
    tone: playerWithStatusCount.value >= players.value.length && players.value.length ? 'success' : playerWithStatusCount.value ? 'warning' : 'danger',
    caption: `${playerWithStatusCount.value} / ${players.value.length} 名球员有可用性、伤停或牌面线索。`,
  },
]);

const evidenceQualityGapRings = computed(() => {
  const finishedTotal = finishedCount.value || matches.value.length;
  const externalGap = Math.max(0, REQUIRED_EXTERNAL_CATEGORIES.length - coveredExternalCount.value);
  const profileGap = teams.value.filter((team) => (team.factCount ?? 0) <= 0).length + playersMissingFactsCount.value;
  const profileTotal = teams.value.length + players.value.length;
  const marketGap = Math.max(0, matches.value.length - oddsCoveredMatchCount.value);
  return [
    {
      label: '比分缺口',
      value: missingFinishedScoreCount.value,
      max: Math.max(finishedTotal, 1),
      unit: '场',
      tone: missingFinishedScoreCount.value ? 'danger' : 'success',
      caption: `${missingFinishedScoreCount.value} / ${finishedTotal} 场完赛比分缺口`,
    },
    {
      label: '证据冲突',
      value: conflictCount.value,
      max: Math.max(evidenceCount.value, conflictCount.value, 1),
      unit: '项',
      tone: conflictCount.value ? 'warning' : 'success',
      caption: `${conflictCount.value} / ${evidenceCount.value} 项公开证据冲突`,
    },
    {
      label: '外因缺口',
      value: externalGap,
      max: REQUIRED_EXTERNAL_CATEGORIES.length,
      unit: '类',
      tone: externalGap >= 5 ? 'danger' : externalGap ? 'warning' : 'success',
      caption: `${externalGap} / ${REQUIRED_EXTERNAL_CATEGORIES.length} 类外部因素缺口`,
    },
    {
      label: '画像事实缺口',
      value: profileGap,
      max: Math.max(profileTotal, 1),
      unit: '项',
      tone: profileGap >= Math.ceil(Math.max(profileTotal, 1) / 2) ? 'danger' : profileGap ? 'warning' : 'success',
      caption: `${profileGap} / ${profileTotal} 项球队/球员事实缺口`,
    },
    {
      label: '市场缺口',
      value: marketGap,
      max: Math.max(matches.value.length, 1),
      unit: '场',
      tone: marketGap ? 'warning' : 'success',
      caption: `${marketGap} / ${matches.value.length} 场市场快照缺口`,
    },
  ];
});

const evidenceQualityActions = computed(() => [
  missingFinishedScoreCount.value
    ? `${missingFinishedScoreCount.value} 场完赛但缺比分的比赛，比分不清会降低结论可用性。`
    : '完赛比分记录已基本覆盖。',
  conflictCount.value
    ? `${conflictCount.value} 项证据冲突，关联官方比分、赛程和阵容来源。`
    : '暂无公开冲突记录。',
  Math.max(0, REQUIRED_EXTERNAL_CATEGORIES.length - coveredExternalCount.value)
    ? `外因缺口：还缺 ${Math.max(0, REQUIRED_EXTERNAL_CATEGORIES.length - coveredExternalCount.value)} 类，包括伤停、裁判、赛程旅行、近期赛果、关键球员贡献、犯规/越位样本、阵容结构、首发位置结构、出线/轮换和赔率与市场价格。`
    : '外因资料覆盖较齐，风险过期和来源可靠性已展示。',
  playersMissingFactsCount.value
    ? `球队/球员画像缺口：仍有 ${playersMissingFactsCount.value} 名球员缺画像事实。`
    : '球队/球员状态字段覆盖较齐。',
]);

const evidenceDataRows = computed(() => [
  {
    label: '比分/赛果',
    plainLabel: '比分与胜负',
    metricLabel: '比分记录',
    value: scoreReadyCount.value,
    max: Math.max(matches.value.length, 1),
    unit: '场',
    tone: scoreReadyCount.value >= matches.value.length && matches.value.length ? 'success' : scoreReadyCount.value ? 'warning' : 'danger',
    caption: `${scoreReadyCount.value} / ${matches.value.length} 场已有明确比分或赛果。`,
  },
  {
    label: '球队基础画像',
    plainLabel: '球队攻防',
    metricLabel: '球队画像',
    value: teamWithProfileCount.value,
    max: Math.max(teams.value.length, 1),
    unit: '队',
    tone: teamWithProfileCount.value >= teams.value.length && teams.value.length ? 'success' : teamWithProfileCount.value ? 'warning' : 'danger',
    caption: `${teamWithProfileCount.value} / ${teams.value.length} 支球队已有画像或攻防线索；xG/PPDA 另按高阶指标补采。`,
  },
  {
    label: '球员可用性',
    plainLabel: '关键球员状态',
    metricLabel: '球员状态',
    value: playerWithStatusCount.value,
    max: Math.max(players.value.length, 1),
    unit: '人',
    tone: playerWithStatusCount.value >= players.value.length && players.value.length ? 'success' : playerWithStatusCount.value ? 'warning' : 'danger',
    caption: `${playerWithStatusCount.value} / ${players.value.length} 名球员已有可用性线索。`,
  },
  {
    label: '外部风险',
    plainLabel: '外因变量',
    metricLabel: '外因覆盖',
    value: coveredExternalCount.value,
    max: REQUIRED_EXTERNAL_CATEGORIES.length,
    unit: '类',
    tone: coveredExternalCount.value >= 7 ? 'success' : coveredExternalCount.value >= 3 ? 'warning' : 'danger',
    caption: `已覆盖 ${coveredExternalCount.value} / ${REQUIRED_EXTERNAL_CATEGORIES.length} 类外部因素。`,
  },
  {
    label: '市场倾向',
    plainLabel: '市场价格',
    metricLabel: '市场覆盖',
    value: oddsCoveredMatchCount.value,
    max: Math.max(matches.value.length, 1),
    unit: '场',
    tone: oddsCoveredMatchCount.value >= matches.value.length && matches.value.length ? 'success' : oddsCoveredMatchCount.value ? 'warning' : 'danger',
    caption: `${oddsCoveredMatchCount.value} / ${matches.value.length} 场比赛已有市场快照。`,
  },
  {
    label: '来源可信度',
    plainLabel: '来源一致性',
    metricLabel: '无冲突比赛',
    value: conflictFreeMatchCount.value,
    max: Math.max(matches.value.length, 1),
    unit: '场',
    tone: conflictCount.value > 0 ? 'warning' : matches.value.length ? 'success' : 'danger',
    caption: `${conflictFreeMatchCount.value} / ${matches.value.length} 场当前暂无公开冲突。`,
  },
]);

const supplementBaseCards = computed(() => [
  {
    key: 'kickoff',
    kicker: '赛程基础',
    title: '开球时间与赛程时效',
    metricLabel: '开球时间',
    value: kickoffReadyCount.value,
    max: Math.max(matches.value.length, 1),
    unit: '场',
    reason: '赛程密集、休息天数、临场名单复核都依赖准确开球时间。',
    caption: `${kickoffReadyCount.value} / ${matches.value.length} 场已有开球时间。`,
    to: '/evidence/matches',
    cta: '核比赛资料',
  },
  {
    key: 'team-metrics',
    kicker: '球队底层表现',
    title: 'xG/xGA/PPDA 与攻防画像',
    metricLabel: '专业高阶',
    value: teamAdvancedMetricCount.value,
    max: Math.max(teams.value.length, 1),
    unit: '队',
    reason: `Opta/StatsBomb 口径都强调射门质量、压迫和推进数据；当前 ${teamTechnicalMetricCount.value} 支球队已有基础技术统计，但专业 xG/PPDA 仍需补采。`,
    caption: `${teamAdvancedMetricCount.value} / ${teams.value.length} 支球队已有 xG/xGA/PPDA 等专业高阶指标。`,
    to: '/evidence/teams',
    cta: '补球队资料',
  },
  {
    key: 'player-availability',
    kicker: '球员影响',
    title: '伤停、牌面、训练与预计首发',
    metricLabel: '可用性细节',
    value: playerAvailabilityDetailCount.value,
    max: Math.max(players.value.length, 1),
    unit: '人',
    reason: `阵容由实际出场 11 人决定；当前 ${playerPerformanceMetricCount.value} 名球员有比赛级表现统计，但伤停训练、xG/xA 与预计首发仍需补采。`,
    caption: `${playerAvailabilityDetailCount.value} / ${players.value.length} 名球员有伤停/牌面/更衣室细节；${playerAdvancedMetricCount.value} 名有 xG/xA/训练等高级字段。`,
    to: '/evidence/players',
    cta: '补球员状态',
  },
  {
    key: 'external-priority',
    kicker: '场外变量',
    title: '裁判、伤停、旅行、战意和战术对位',
    metricLabel: '高优先外因',
    value: priorityExternalCoveredCount.value,
    max: PRIORITY_EXTERNAL_CATEGORIES.length,
    unit: '类',
    reason: '媒体预览和 ML 综述都把人类因素、赛程背景与临场变量列为基础统计之外的增量信息。',
    caption: `${priorityExternalCoveredCount.value} / ${PRIORITY_EXTERNAL_CATEGORIES.length} 类高优先外因已有线索。`,
    to: '/evidence/sentiment',
    cta: '补外部因素',
  },
  {
    key: 'market-timeline',
    kicker: '市场信号',
    title: '开盘、当前与价格变化时间线',
    metricLabel: '市场覆盖',
    value: oddsCoveredMatchCount.value,
    max: Math.max(matches.value.length, 1),
    unit: '场',
    reason: '赔率隐含概率有噪声，需多时间点和多市场交叉，不能只看单个快照。',
    caption: `${oddsCoveredMatchCount.value} / ${matches.value.length} 场已有市场快照；价格变化仍需多时点补采。`,
    to: '/evidence/odds',
    cta: '核市场快照',
  },
  {
    key: 'country-context',
    kicker: '世界杯语境',
    title: '国旗、洲别、小组与来源追踪',
    metricLabel: '国家上下文',
    value: teamCountryContextCount.value,
    max: Math.max(teams.value.length, 1),
    unit: '队',
    reason: '国家队元数据是公开页国旗、小组筛选和普通用户识别球队的基础。',
    caption: `${teamCountryContextCount.value} / ${teams.value.length} 支球队已有 ISO、洲别和小组。`,
    to: '/evidence/teams',
    cta: '核国家上下文',
  },
]);

const supplementPriorityCards = computed(() => supplementBaseCards.value
  .map((item) => {
    const pct = percentage(item.value, item.max);
    return {
      ...item,
      rankScore: 100 - pct,
      tone: pct >= 80 ? 'success' : pct >= 35 ? 'warning' : 'danger',
    };
  })
  .sort((left, right) => right.rankScore - left.rankScore)
  .map((item, index) => ({
    ...item,
    rank: String(index + 1).padStart(2, '0'),
  })));

const supplementPendingCount = computed(() => supplementPriorityCards.value.filter((item) => item.tone !== 'success').length);

const aiDataRings = computed(() => [
  {
    label: '专业高阶指标',
    value: Math.round(percentage(teamAdvancedMetricCount.value, Math.max(teams.value.length, 1))),
    tone: teamAdvancedMetricCount.value >= teams.value.length && teams.value.length ? 'success' : teamAdvancedMetricCount.value ? 'warning' : 'danger',
    caption: `${teamAdvancedMetricCount.value} / ${teams.value.length} 支球队有 xG/xGA/PPDA 等专业高阶指标`,
  },
  {
    label: '伤停训练/球员状态',
    value: Math.round(percentage(playerWithStatusCount.value, Math.max(players.value.length, 1))),
    tone: playerWithStatusCount.value >= players.value.length && players.value.length ? 'success' : playerWithStatusCount.value ? 'warning' : 'danger',
    caption: `${playerWithStatusCount.value} / ${players.value.length} 名球员有状态线索`,
  },
  {
    label: '市场价格/公众热度',
    value: Math.round(percentage(oddsCoveredMatchCount.value, Math.max(matches.value.length, 1))),
    tone: oddsCoveredMatchCount.value >= matches.value.length && matches.value.length ? 'success' : oddsCoveredMatchCount.value ? 'warning' : 'danger',
    caption: `${oddsCoveredMatchCount.value} / ${matches.value.length} 场比赛有市场快照`,
  },
  {
    label: '天气裁判/赛程旅行',
    value: Math.round(percentage(coveredExternalCount.value, REQUIRED_EXTERNAL_CATEGORIES.length)),
    tone: coveredExternalCount.value >= 7 ? 'success' : coveredExternalCount.value >= 3 ? 'warning' : 'danger',
    caption: `${coveredExternalCount.value} / ${REQUIRED_EXTERNAL_CATEGORIES.length} 类外部因素覆盖`,
  },
  {
    label: '来源一致性',
    value: Math.round(percentage(conflictFreeMatchCount.value, Math.max(matches.value.length, 1))),
    tone: conflictCount.value > 0 ? 'warning' : matches.value.length ? 'success' : 'danger',
    caption: `${conflictFreeMatchCount.value} / ${matches.value.length} 场当前暂无公开冲突`,
  },
]);

const routeCards = computed(() => [
  {
    kicker: '比分与胜负',
    title: '比赛中心',
    to: '/evidence/matches',
    count: `${matches.value.length} 场`,
    lead: '展示对阵、比分、胜负和证据冲突。',
    highlights: [`已有比分 ${scoreReadyCount.value} 场`, `完赛 ${finishedCount.value} 场`, `未解冲突 ${conflictCount.value} 项`],
    cta: '进入比赛中心',
    tone: 'score',
  },
  {
    kicker: '市场信号',
    title: '赔率中心',
    to: '/evidence/odds',
    count: `${odds.value.length} 个`,
    lead: '展示赔率快照、不同玩法和隐含概率，只解释市场偏向。',
    highlights: [`来源 ${bookmakerCount.value} 家`, `实时市场 ${liveOddsCount.value} 个`, '隐含概率已归档'],
    cta: '查看赔率证据',
    tone: 'market',
  },
  {
    kicker: '外部风险',
    title: '舆情与外部因素',
    to: '/evidence/sentiment',
    count: `${sentiments.value.length} 条`,
    lead: '展示伤停、天气、裁判、赛程旅行、公众热度和媒体球迷情绪。',
    highlights: [`覆盖 ${coveredExternalCount.value} / ${REQUIRED_EXTERNAL_CATEGORIES.length} 类`, `高风险 ${highRiskCount.value} 条`, `过期 ${staleFactorCount.value} 条`],
    cta: '外部因素详情',
    tone: 'risk',
  },
  {
    kicker: '球队画像',
    title: '球队画像',
    to: '/evidence/teams',
    count: `${teams.value.length} 支`,
    lead: '展示国家国旗、小组、攻防画像、基础技术统计和高阶指标缺口。',
    highlights: [`基础技术 ${teamTechnicalMetricCount.value} 支`, `专业高阶 ${teamAdvancedMetricCount.value} 支`, `国家元数据 ${teamsWithFlagCount.value} 支`],
    cta: '查看球队画像',
    tone: 'profile',
  },
  {
    kicker: '球员可用性',
    title: '球员画像',
    to: '/evidence/players',
    count: `${players.value.length} 名`,
    lead: '展示位置、号码、伤停、牌面、训练和预计出场状态。',
    highlights: [`有状态字段 ${playerWithStatusCount.value} 名`, `缺事实 ${playersMissingFactsCount.value} 名`, '核心球员伤停缺口'],
    cta: '查看球员画像',
    tone: 'player',
  },
]);

const teamsWithFlagCount = computed(() => teams.value.filter((team) => Boolean(team.fifaCode || team.countryIso2 || team.flagAssetKey)).length);
const playersMissingFactsCount = computed(() => players.value.filter((player) => (player.factCount ?? 0) <= 0).length);

const evidenceGaps = computed(() => [
  {
    label: '完赛缺比分',
    value: missingFinishedScoreCount.value,
    caption: '完赛但无可靠比分时，官方比分与事件缺口存在。',
  },
  {
    label: '证据冲突',
    value: conflictCount.value,
    caption: '冲突项关联正式来源复验状态。',
  },
  {
    label: '外因缺口',
    value: Math.max(0, REQUIRED_EXTERNAL_CATEGORIES.length - coveredExternalCount.value),
    caption: '伤停、裁判、赛程旅行、市场热度等缺口影响判断。',
  },
  {
    label: '画像事实不足',
    value: teams.value.filter((team) => (team.factCount ?? 0) <= 0).length + playersMissingFactsCount.value,
    caption: '球队/球员事实或状态字段缺口会降低强弱解读可用性。',
  },
]);

async function loadEvidenceHub() {
  loading.value = true;
  loadError.value = '';
  loadWarning.value = '';

  const results = await Promise.allSettled([
    listPublicMatches(),
    listPublicOddsOverview(),
    listPublicSentimentOverview(),
    listPublicTeamProfiles(),
    listPublicPlayerProfiles(),
  ]);

  matches.value = readSettled(results[0], []);
  odds.value = readSettled(results[1], []);
  sentiments.value = readSettled(results[2], []);
  teams.value = readSettled(results[3], []);
  players.value = readSettled(results[4], []);

  const failedCount = results.filter((result) => result.status === 'rejected').length;
  if (failedCount === results.length) {
    loadError.value = '公开 API 全部请求失败，稍后重试。';
  } else if (failedCount > 0) {
    loadWarning.value = `${failedCount} 个公开数据源暂不可用，当前总览只展示已同步部分。`;
  }
  loading.value = false;
}

function readSettled<T>(result: PromiseSettledResult<ApiResponse<T>>, fallback: T): T {
  if (result.status === 'fulfilled' && result.value.success) {
    return result.value.data ?? fallback;
  }
  return fallback;
}

function hasScore(match: PublicMatchSummary): boolean {
  const scoreboard = scoreboardFallback(match as unknown as Record<string, unknown>);
  return scoreboard.homeScore != null && scoreboard.awayScore != null;
}

function isFinished(match: PublicMatchSummary): boolean {
  const statusText = `${match.status || ''} ${match.resultStatus || ''}`.toUpperCase();
  return hasScore(match) || /FINISHED|COMPLETED|FINAL|RESULT|END|已完赛/.test(statusText);
}

function isLiveSnapshot(value?: string | null): boolean {
  return (value || '').toUpperCase().includes('LIVE');
}

function isHighRisk(value?: string | null): boolean {
  const normalized = (value || '').toUpperCase();
  return normalized.includes('HIGH') || normalized.includes('CRITICAL') || normalized.includes('高');
}

function categoryLabel(value?: string | null): string {
  const normalized = (value || '').toUpperCase();
  if (!normalized) {
    return '';
  }
  if (/WEATHER|VENUE|PITCH|FIELD|STADIUM|ATTENDANCE|CROWD|BROADCAST|场地|天气|球场|上座|观众|转播/.test(normalized)) return '天气、场馆与转播';
  if (/INFRACTION_PROFILE|FOUL_OFFSIDE|OFFSIDE_PROFILE|PENALTY_KICK_SAMPLE|FOULS?COMMITTED|OFFSIDES|PENALTYKICKSHOTS|犯规\/越位|犯规|越位|点球尝试/.test(normalized)) return '犯规/越位样本';
  if (/SCORING|SET_PIECE|GOAL_TIMING|ATTACK_PATTERN|GOAL_TIME|SPECIAL_SCORING|HEADER|FREE_KICK|PENALTY_SCORED|PENALTY_MISSED|OWN_GOAL|CORNER_GOAL|CROSS_GOAL|THROUGH_BALL|FAST_BREAK|PLAYER_GOAL_INVOLVEMENT|进球|定位球|任意球|头球|乌龙|点球命中|点球罚失/.test(normalized)) return '进球方式与时间';
  if (/KEY_PLAYER|PLAYER_IMPACT|ATTACK_CONTRIBUTION|GOAL_CONTRIBUTION|TOP_SCORER_CONTRIBUTION|ASSIST_CONTRIBUTION|CONTRIBUTION_DISTRIBUTION|关键球员|球员影响|进攻贡献|进球参与|助攻贡献|贡献分布/.test(normalized)) return '关键球员进球参与';
  if (/GOALKEEP|SAVE_PRESSURE|SAVE_PROFILE|SHOT_STOPPING|DEFENSIVE_PRESSURE|门将|扑救|防守承压/.test(normalized)) return '门将/防守承压';
  if (/SQUAD|ROSTER|AGE_EXPERIENCE|TEAM_EXPERIENCE|CLUB_DISTRIBUTION|HEIGHT_PROFILE|POSITION_PROFILE|官方名单|阵容结构|年龄经验|名单结构|俱乐部分布|位置分布/.test(normalized)) return '阵容结构/年龄经验';
  if (/LINEUP_STRUCTURE|CONFIRMED_STARTING|STARTING_XI|FORMATION_PROFILE|CONFIRMED_LINEUP|首发位置|已确认首发|确认阵容|位置结构画像/.test(normalized)) return '首发位置结构';
  if (/TECHNICAL|BOXSCORE|SHOT_PROFILE|POSSESSION|PASSING|CROSSING|CORNER_PROFILE|FOUL_PROFILE|DEFENSIVE_ACTION|DEFENSIVE_ACTIVITY|基础技术|射门|控球|传球|传中|角球|犯规|防守动作/.test(normalized)) return '基础技术统计/球队风格画像';
  if (/REFEREE|CARD|DISCIPLINE|PENALTY|裁判|纪律|牌面/.test(normalized)) return '裁判与判罚';
  if (/INJUR|TRAIN|SUSPENSION|FITNESS|伤停|训练|体能|恢复/.test(normalized)) return '伤停与训练';
  if (/TRAVEL|REST|SCHEDULE|FATIGUE|WORKLOAD|PLAYING_TIME|MINUTES|旅行|赛程|休息|出场负荷|出场时间/.test(normalized)) return '赛程旅行休息';
  if (/FORM|TOURNAMENT_FORM|RECENT_RESULTS|RESULT_TREND|FORM_TREND|UNBEATEN|WINLESS|CLEAN_SHEET|近期赛果|赛果走势|本届赛果|连续不败|连续未胜|零封/.test(normalized)) return '近期赛果/状态走势';
  if (/GROUP_QUALIFICATION|QUALIFICATION|KNOCKOUT|ADVANCEMENT|ELIMINATION|TOURNAMENT_FORMAT|ROUND_OF_32|MOTIV|ROTATION|STARTING_CONTINUITY|LOCKER|更衣室|战意|轮换|首发连续|出线|淘汰赛|晋级|出局|赛制|32强/.test(normalized)) return '战意轮换';
  if (/PRESS|COACH|NEWS|发布会|新闻/.test(normalized)) return '更衣室/发布会';
  if (/MEDIA|FAN|PUBLIC|舆论|球迷|媒体/.test(normalized)) return '媒体球迷';
  if (/ODDS|MARKET|PRICE|IMPLIED_PROBABILITY|H2H_1X2|SPREAD|TOTAL_GOALS|赔率|市场价格|隐含概率|胜平负|让球|总进球|市场/.test(normalized)) return '赔率与市场价格';
  if (/H2H|TACTIC|MATCHUP|历史|战术/.test(normalized)) return '历史交锋战术对位';
  return value || '';
}

function percentage(value: number, total: number): number {
  if (total <= 0) {
    return 0;
  }
  return Math.max(0, Math.min(100, (value / total) * 100));
}

function matchMeta(match: PublicMatchSummary): string {
  return [match.matchday, match.jcCode ? `竞彩 ${match.jcCode}` : '', match.venue].filter(Boolean).join(' · ') || '赛程待同步';
}

onMounted(loadEvidenceHub);
</script>

<style scoped>
.evidence-hub__content {
  display: grid;
  gap: 18px;
  min-width: 0;
}

.evidence-hero,
.headline-match,
.evidence-catalog-structure,
.evidence-command-board,
.command-priority-card,
.command-readiness-card,
.triage-action-card,
.readiness-row,
.quality-board,
.quality-score-card,
.quality-action-card,
.data-literacy-board,
.data-matrix-card,
.supplement-priority-board,
.supplement-priority-card,
.coverage-panel,
.route-card,
.stat-card,
.loading-panel,
.error-panel,
.warning-panel {
  background: var(--wc-glass);
  border: 1px solid var(--wc-border);
  border-radius: var(--wc-radius-lg);
  color: var(--wc-text);
  min-width: 0;
}

.evidence-hero {
  display: grid;
  gap: 18px;
  grid-template-columns: minmax(0, 1.55fr) minmax(0, 0.75fr);
  padding: clamp(18px, 4vw, 34px);
}

.evidence-hero__copy {
  display: grid;
  gap: 14px;
  min-width: 0;
}

.eyebrow {
  color: var(--wc-warning);
  font-family: var(--wc-font-mono);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  margin: 0;
  text-transform: uppercase;
}

.evidence-hero h1,
.coverage-panel h2,
.headline-match h2,
.route-card h2 {
  font-family: var(--wc-font-display);
  line-height: 1.05;
  margin: 0;
}

.evidence-hero h1 {
  font-size: clamp(34px, 6vw, 64px);
}

.evidence-hero p,
.headline-match__status p,
.coverage-panel__copy,
.route-card p,
.gap-card p {
  color: var(--wc-text-muted);
  line-height: 1.65;
  margin: 0;
}

.evidence-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.primary-link,
.secondary-link,
.panel-link,
.route-card__cta {
  align-items: center;
  border-radius: 999px;
  display: inline-flex;
  font-weight: 800;
  justify-content: center;
  min-height: 44px;
  padding: 10px 16px;
  text-decoration: none;
  transition: border-color 180ms ease, color 180ms ease, transform 180ms ease, background 180ms ease;
}

.primary-link {
  background: var(--wc-accent);
  color: var(--wc-on-accent);
}

.secondary-link,
.panel-link {
  background: rgba(147, 197, 253, 0.1);
  border: 1px solid rgba(147, 197, 253, 0.24);
  color: var(--wc-primary);
}

.primary-link:hover,
.secondary-link:hover,
.panel-link:hover,
.route-card:hover {
  transform: translateY(-2px);
}

.primary-link:focus-visible,
.secondary-link:focus-visible,
.panel-link:focus-visible,
	.route-card:focus-visible,
	.evidence-hub__retry:focus-visible,
	.evidence-hero__panel:focus-visible,
	.headline-match:focus-visible,
	.evidence-catalog-structure:focus-visible,
	.evidence-catalog-rings:focus-visible,
	.evidence-command-board:focus-visible,
	.readiness-ring-grid:focus-visible,
	.readiness-row-list:focus-visible,
	.quality-board:focus-visible,
	.quality-bars:focus-visible,
	.quality-gap-rings:focus-visible,
	.data-literacy-board:focus-visible,
	.data-literacy-bars:focus-visible,
	.supplement-priority-board:focus-visible,
	.route-grid:focus-visible,
	.coverage-grid:focus-visible,
	.coverage-panel--data-map:focus-visible,
	.gap-list:focus-visible,
	.ai-data-ring-grid:focus-visible {
	  box-shadow: var(--wc-focus-ring);
	  outline: none;
	}

.evidence-hero__panel {
  background:
    radial-gradient(circle at top right, rgba(34, 211, 238, 0.16), transparent 42%),
    rgba(2, 6, 23, 0.42);
  border: 1px solid rgba(147, 197, 253, 0.2);
  border-radius: var(--wc-radius-lg);
  display: grid;
  gap: 12px;
  padding: 18px;
}

.evidence-hero__panel > span,
.route-card__top span,
.stat-card span,
.gap-card span {
  color: var(--wc-text-muted);
  font-family: var(--wc-font-mono);
  font-size: 12px;
  font-weight: 800;
}

.evidence-hero__panel > strong {
  color: var(--wc-warning);
  font-family: var(--wc-font-display);
  font-size: clamp(40px, 7vw, 72px);
  line-height: 0.92;
}

.loading-panel,
.error-panel,
.warning-panel {
  display: grid;
  gap: 10px;
  padding: 18px;
}

.loading-panel__bar {
  background: linear-gradient(90deg, rgba(147, 197, 253, 0.12), rgba(147, 197, 253, 0.34), rgba(147, 197, 253, 0.12));
  border-radius: 999px;
  height: 12px;
  max-width: 460px;
}

.loading-panel__bar--short {
  max-width: 280px;
}

.error-panel {
  border-color: rgba(252, 165, 165, 0.42);
}

.warning-panel {
  border-color: rgba(253, 230, 138, 0.38);
}

.panel-heading {
  align-items: center;
  display: flex;
  gap: 16px;
  justify-content: space-between;
  min-width: 0;
}

.panel-heading h2 {
  font-size: clamp(22px, 3vw, 34px);
}

.headline-match,
.quality-board,
.data-literacy-board,
.coverage-panel {
  display: grid;
  gap: 16px;
  padding: 18px;
}

.coverage-panel {
  max-height: 260px;
  overflow: auto;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}

.quality-board {
  background:
    radial-gradient(circle at 10% 0%, rgba(34, 197, 94, 0.12), transparent 30%),
    var(--wc-glass);
  grid-template-columns: minmax(0, 0.85fr) minmax(0, 1.25fr) minmax(0, 0.9fr);
}

.quality-score-card,
.quality-action-card {
  background: rgba(2, 6, 23, 0.34);
  border-color: rgba(147, 197, 253, 0.18);
  display: grid;
  gap: 10px;
  min-width: 0;
  padding: 15px;
}

.quality-score-card h2,
.quality-action-card h3 {
  font-family: var(--wc-font-display);
  font-size: clamp(22px, 3vw, 32px);
  line-height: 1.08;
  margin: 0;
}

.quality-score-card strong {
  color: var(--wc-primary);
  font-family: var(--wc-font-display);
  font-size: clamp(46px, 7vw, 76px);
  line-height: 0.92;
}

.quality-score-card > span {
  color: var(--wc-warning);
  font-weight: 900;
}

.quality-score-card p,
.quality-action-card li {
  color: var(--wc-text-muted);
  line-height: 1.62;
  margin: 0;
}

.quality-bars {
  display: grid;
  gap: 12px;
  max-height: 260px;
  min-width: 0;
  overflow: auto;
  padding-right: 4px;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}

.quality-gap-rings {
  display: grid;
  gap: 13px;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  max-height: 240px;
  min-width: 0;
  overflow: auto;
  padding-right: 4px;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}

.quality-action-card ol {
  display: grid;
  gap: 9px;
  margin: 0;
  padding-left: 20px;
}

.data-literacy-board {
  background:
    radial-gradient(circle at top left, rgba(59, 130, 246, 0.14), transparent 34%),
    var(--wc-glass);
  max-height: min(38dvh, 320px);
  overflow: auto;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}

.data-literacy-intro {
  display: grid;
  gap: 8px;
  max-width: 940px;
}

.data-literacy-intro h2 {
  font-family: var(--wc-font-display);
  font-size: clamp(24px, 3.5vw, 38px);
  line-height: 1.08;
  margin: 0;
}


.data-literacy-bars {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  max-height: 240px;
  min-width: 0;
  overflow: auto;
  padding-right: 4px;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}

.data-matrix-card {
  background:
    linear-gradient(135deg, rgba(59, 130, 246, 0.12), transparent 44%),
    rgba(2, 6, 23, 0.34);
  display: grid;
  gap: 12px;
  min-width: 0;
  padding: 15px;
}

.data-matrix-card__copy {
  display: grid;
  gap: 7px;
}

.data-matrix-card__copy span {
  color: var(--wc-warning);
  font-family: var(--wc-font-mono);
  font-size: 12px;
  font-weight: 800;
}

.data-matrix-card__copy strong {
  color: var(--wc-text);
  font-size: 18px;
}

.supplement-priority-board {
  background:
    radial-gradient(circle at top right, rgba(248, 113, 113, 0.11), transparent 32%),
    var(--wc-glass);
  display: grid;
  gap: 16px;
  max-height: min(42dvh, 340px);
  overflow: auto;
  padding: 18px;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}

.supplement-priority-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  min-width: 0;
}

.supplement-priority-card {
  align-content: start;
  background:
    linear-gradient(135deg, rgba(248, 113, 113, 0.12), transparent 40%),
    rgba(2, 6, 23, 0.34);
  display: grid;
  gap: 12px;
  min-width: 0;
  padding: 15px;
}

.supplement-priority-card--success {
  border-color: rgba(134, 239, 172, 0.32);
}

.supplement-priority-card--warning {
  border-color: rgba(253, 230, 138, 0.36);
}

.supplement-priority-card--danger {
  border-color: rgba(252, 165, 165, 0.38);
}

.supplement-priority-card__rank {
  color: var(--wc-warning);
  font-family: var(--wc-font-display);
  font-size: 26px;
  line-height: 1;
}

.supplement-priority-card__copy {
  display: grid;
  gap: 7px;
}

.supplement-priority-card__copy small {
  color: var(--wc-primary);
  font-family: var(--wc-font-mono);
  font-size: 12px;
  font-weight: 800;
}

.supplement-priority-card__copy strong {
  color: var(--wc-text);
  font-size: 18px;
  line-height: 1.25;
}

.supplement-priority-card__copy p {
  color: var(--wc-text-muted);
  line-height: 1.58;
  margin: 0;
}

.supplement-priority-card__link {
  align-items: center;
  background: rgba(147, 197, 253, 0.1);
  border: 1px solid rgba(147, 197, 253, 0.22);
  border-radius: 999px;
  color: var(--wc-primary);
  display: inline-flex;
  font-weight: 900;
  justify-content: center;
  min-height: 44px;
  padding: 9px 14px;
  text-decoration: none;
}


.headline-match__grid {
  display: grid;
  gap: 16px;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 0.85fr);
}

.headline-match__status {
  background: rgba(15, 23, 42, 0.56);
  border: 1px solid rgba(147, 197, 253, 0.18);
  border-radius: var(--wc-radius-md);
  display: grid;
  gap: 12px;
  padding: 16px;
}

.headline-match__status strong {
  color: var(--wc-warning);
  font-family: var(--wc-font-display);
  font-size: clamp(26px, 4vw, 42px);
  line-height: 1;
}

.headline-match__status ul,
.route-card ul {
  color: var(--wc-text-muted);
  display: grid;
  gap: 8px;
  margin: 0;
  padding-left: 18px;
}

.headline-match__facts {
  gap: 8px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  list-style: none;
  padding-left: 0 !important;
}

.headline-match__facts li {
  background: rgba(15, 23, 42, 0.64);
  border: 1px solid rgba(147, 197, 253, 0.14);
  border-radius: 14px;
  color: var(--wc-text);
  font-family: var(--wc-font-mono);
  font-size: 12px;
  font-weight: 800;
  line-height: 1.35;
  min-width: 0;
  padding: 10px;
}

.stat-grid,
.route-grid,
.coverage-grid,
.gap-list {
  display: grid;
  gap: 14px;
  min-width: 0;
}

.stat-grid {
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
}

.stat-card {
  display: grid;
  gap: 8px;
  padding: 16px;
}

.stat-card strong {
  color: var(--wc-text);
  font-family: var(--wc-font-display);
  font-size: clamp(30px, 5vw, 48px);
  line-height: 0.95;
}

.stat-card small {
  color: var(--wc-text-muted);
  line-height: 1.5;
}

.stat-card--score,
.route-card--score { border-color: rgba(253, 230, 138, 0.34); }
.stat-card--evidence { border-color: rgba(147, 197, 253, 0.36); }
.stat-card--market,
.route-card--market { border-color: rgba(34, 211, 238, 0.32); }
.stat-card--risk,
.route-card--risk { border-color: rgba(252, 165, 165, 0.34); }
.stat-card--profile,
.route-card--profile,
.route-card--player { border-color: rgba(134, 239, 172, 0.3); }

.evidence-catalog-structure {
  display: grid;
  gap: 14px;
  max-height: min(44dvh, 340px);
  overflow: auto;
  padding: 18px;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}

.evidence-catalog-rings {
  display: grid;
  gap: 13px;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  max-height: 280px;
  min-width: 0;
  overflow: auto;
  padding-right: 4px;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}

.evidence-command-board {
  align-items: start;
  display: grid;
  gap: 16px;
  grid-template-columns: minmax(0, 0.95fr) minmax(0, 1.05fr);
  min-width: 0;
}

.command-priority-card,
.command-readiness-card {
  align-content: start;
  display: grid;
  gap: 14px;
  max-height: 360px;
  overflow: auto;
  padding: 18px;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}

.command-priority-card {
  background:
    radial-gradient(circle at top left, rgba(245, 158, 11, 0.13), transparent 34%),
    var(--wc-glass);
}

.command-readiness-card {
  background:
    radial-gradient(circle at top right, rgba(59, 130, 246, 0.12), transparent 34%),
    var(--wc-glass);
}

.command-priority-card h2,
.command-readiness-card h2 {
  font-family: var(--wc-font-display);
  font-size: clamp(22px, 3vw, 32px);
  line-height: 1.08;
  margin: 0;
}

.command-status {
  background: rgba(217, 119, 6, 0.16);
  border: 1px solid rgba(217, 119, 6, 0.32);
  border-radius: 999px;
  color: var(--wc-warning);
  font-family: var(--wc-font-mono);
  font-size: 12px;
  font-weight: 900;
  padding: 8px 10px;
}

.triage-action-list,
.readiness-row-list {
  display: grid;
  gap: 12px;
  max-height: 240px;
  min-width: 0;
  overflow: auto;
  padding-right: 4px;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}

.readiness-ring-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(178px, 1fr));
  max-height: 360px;
  min-width: 0;
  overflow: auto;
  padding-right: 4px;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}

.triage-action-card {
  align-items: start;
  background: rgba(2, 6, 23, 0.38);
  display: grid;
  gap: 12px;
  grid-template-columns: auto minmax(0, 1fr) auto;
  padding: 14px;
}

.triage-action-card--danger { border-color: rgba(252, 165, 165, 0.38); }
.triage-action-card--warning { border-color: rgba(253, 230, 138, 0.38); }
.triage-action-card--success { border-color: rgba(134, 239, 172, 0.3); }

.triage-action-card__rank {
  align-items: center;
  background: rgba(147, 197, 253, 0.12);
  border: 1px solid rgba(147, 197, 253, 0.2);
  border-radius: 14px;
  color: var(--wc-primary);
  display: inline-flex;
  font-family: var(--wc-font-mono);
  font-size: 12px;
  font-weight: 900;
  justify-content: center;
  min-height: 44px;
  min-width: 44px;
}

.triage-action-card small,
.readiness-row small,
.readiness-row__copy span {
  color: var(--wc-text-muted);
  line-height: 1.5;
}

.triage-action-card strong,
.readiness-row__copy strong {
  color: var(--wc-text);
  display: block;
  font-size: 16px;
  margin-top: 4px;
}

.triage-action-card p {
  color: var(--wc-text-muted);
  line-height: 1.58;
  margin: 8px 0 0;
}

.triage-action-card__link {
  align-items: center;
  align-self: center;
  background: rgba(217, 119, 6, 0.16);
  border: 1px solid rgba(217, 119, 6, 0.34);
  border-radius: 999px;
  color: var(--wc-warning);
  display: inline-flex;
  font-weight: 900;
  justify-content: center;
  min-height: 44px;
  padding: 0 12px;
  text-decoration: none;
  white-space: nowrap;
}

.triage-action-card__link:focus-visible {
  box-shadow: var(--wc-focus-ring);
  outline: none;
}

.readiness-row {
  align-items: center;
  background: rgba(2, 6, 23, 0.34);
  display: grid;
  gap: 12px;
  grid-template-columns: minmax(0, 0.65fr) minmax(0, 1fr) minmax(0, 0.8fr);
  padding: 13px;
}

.readiness-row__copy {
  display: grid;
  gap: 4px;
}


.route-grid {
  grid-template-columns: repeat(5, minmax(0, 1fr));
  max-height: min(40dvh, 320px);
  overflow: auto;
  padding-right: 4px;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}

.route-card {
  display: grid;
  gap: 12px;
  min-height: 220px;
  padding: 18px;
  text-decoration: none;
  transition: border-color 180ms ease, transform 180ms ease, background 180ms ease;
}

.route-card:hover {
  background: rgba(15, 23, 42, 0.96);
  border-color: rgba(147, 197, 253, 0.52);
}

.route-card__top {
  align-items: center;
  display: flex;
  gap: 10px;
  justify-content: space-between;
}

.route-card__top strong {
  color: var(--wc-warning);
  font-family: var(--wc-font-mono);
}

.route-card h2 {
  color: var(--wc-text);
  font-size: 25px;
}

.route-card__cta {
  align-self: end;
  background: rgba(217, 119, 6, 0.18);
  border: 1px solid rgba(217, 119, 6, 0.36);
  color: var(--wc-warning);
  margin-top: auto;
}

.coverage-grid {
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 0.9fr);
}

.gap-list {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  max-height: 300px;
  overflow: auto;
  padding-right: 4px;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}

.gap-card {
  background: rgba(2, 6, 23, 0.34);
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: var(--wc-radius-md);
  display: grid;
  gap: 8px;
  min-width: 0;
  padding: 15px;
}

.gap-card--active {
  border-color: rgba(253, 230, 138, 0.38);
}

.gap-card strong {
  color: var(--wc-warning);
  font-family: var(--wc-font-display);
  font-size: 36px;
  line-height: 1;
}

.data-map {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  max-height: 140px;
  overflow: auto;
  padding-right: 3px;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}

.ai-data-ring-grid {
  display: grid;
  gap: 13px;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  max-height: 300px;
  min-width: 0;
  overflow: auto;
  padding-right: 4px;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}

.data-map span {
  background: rgba(147, 197, 253, 0.1);
  border: 1px solid rgba(147, 197, 253, 0.22);
  border-radius: 999px;
  color: var(--wc-text);
  font-size: 13px;
  font-weight: 800;
  min-height: 34px;
  padding: 7px 11px;
}

@media (max-width: 1280px) {
  .route-grid,
  .data-literacy-bars {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .evidence-command-board {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .evidence-hero,
  .headline-match__grid,
  .quality-board,
  .coverage-grid,
  .data-literacy-bars,
  .evidence-catalog-rings,
  .ai-data-ring-grid,
  .quality-gap-rings,
  .readiness-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .panel-heading,
  .evidence-hero__actions {
    align-items: stretch;
    flex-direction: column;
  }

  .stat-grid,
  .evidence-catalog-rings,
  .readiness-ring-grid,
  .quality-gap-rings,
  .data-literacy-bars,
  .ai-data-ring-grid,
  .gap-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .headline-match__grid,
  .route-grid,
  .readiness-row {
    grid-template-columns: 1fr;
  }

  .evidence-hero,
  .headline-match,
  .evidence-catalog-structure,
  .quality-board,
  .data-literacy-board,
  .coverage-panel,
  .stat-card,
  .command-priority-card,
  .command-readiness-card,
  .quality-score-card,
  .quality-action-card,
  .data-matrix-card,
  .route-card,
  .gap-card {
    border-radius: var(--wc-radius-md);
    gap: 10px;
    padding: 12px;
  }

  .evidence-hero h1 {
    font-size: clamp(28px, 12vw, 42px);
  }

  .evidence-hero p:not(.eyebrow),
  .coverage-panel__copy,
  .headline-match__status p,
  .quality-score-card p,
  .quality-action-card ol,
  .route-card p,
  .gap-card p,
  .readiness-row small,
  .readiness-row__copy span,
  .data-matrix-card__copy span {
    display: none;
  }

  .stat-grid,
  .evidence-catalog-rings,
  .readiness-ring-grid,
  .quality-gap-rings,
  .data-literacy-bars,
  .route-grid,
  .coverage-grid,
  .ai-data-ring-grid,
  .gap-list {
    gap: 10px;
  }

  .evidence-catalog-rings :deep(.coverage-donut),
  .readiness-ring-grid :deep(.coverage-donut),
  .quality-gap-rings :deep(.coverage-donut),
  .ai-data-ring-grid :deep(.coverage-donut),
  .evidence-hero__panel :deep(.coverage-donut) {
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

  .evidence-catalog-rings :deep(.coverage-donut__ring),
  .readiness-ring-grid :deep(.coverage-donut__ring),
  .quality-gap-rings :deep(.coverage-donut__ring),
  .ai-data-ring-grid :deep(.coverage-donut__ring),
  .evidence-hero__panel :deep(.coverage-donut__ring) {
    width: 66px;
  }

  .evidence-catalog-rings :deep(.coverage-donut__ring span),
  .readiness-ring-grid :deep(.coverage-donut__ring span),
  .quality-gap-rings :deep(.coverage-donut__ring span),
  .ai-data-ring-grid :deep(.coverage-donut__ring span),
  .evidence-hero__panel :deep(.coverage-donut__ring span) {
    font-size: 20px;
  }

  .evidence-catalog-rings :deep(.coverage-donut__copy),
  .readiness-ring-grid :deep(.coverage-donut__copy),
  .quality-gap-rings :deep(.coverage-donut__copy),
  .ai-data-ring-grid :deep(.coverage-donut__copy),
  .evidence-hero__panel :deep(.coverage-donut__copy) {
    gap: 3px;
  }

  .evidence-catalog-rings :deep(.coverage-donut__copy strong),
  .readiness-ring-grid :deep(.coverage-donut__copy strong),
  .quality-gap-rings :deep(.coverage-donut__copy strong),
  .ai-data-ring-grid :deep(.coverage-donut__copy strong),
  .evidence-hero__panel :deep(.coverage-donut__copy strong) {
    font-size: 12px;
    line-height: 1.2;
  }

  .evidence-catalog-rings :deep(.coverage-donut__copy small),
  .readiness-ring-grid :deep(.coverage-donut__copy small),
  .quality-gap-rings :deep(.coverage-donut__copy small),
  .ai-data-ring-grid :deep(.coverage-donut__copy small),
  .evidence-hero__panel :deep(.coverage-donut__copy small) {
    display: none;
    font-size: 11px;
    line-height: 1.25;
  }

  .headline-match :deep(.scoreboard-card) {
    gap: 8px;
    padding: 10px;
  }

  .headline-match :deep(.scoreboard-card__main) {
    grid-template-columns: minmax(0, 1fr) 84px minmax(0, 1fr) !important;
  }

  .headline-match :deep(.scoreboard-card__team) {
    border: 0;
    padding: 0;
  }

  .headline-match :deep(.scoreboard-card__team .flag-team) {
    align-items: center;
    flex-direction: column;
    gap: 5px;
  }

  .headline-match :deep(.scoreboard-card__team--away .flag-team) {
    align-items: center;
    flex-direction: column;
  }

  .headline-match :deep(.scoreboard-card__team--away) {
    justify-items: center;
    text-align: center;
  }

  .headline-match :deep(.scoreboard-card__team .flag-team__copy) {
    justify-items: center;
    text-align: center;
    width: 100%;
  }

  .headline-match :deep(.scoreboard-card__team .flag-team__copy strong) {
    font-size: 12px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .headline-match :deep(.scoreboard-card__score) {
    min-width: 0;
    padding: 7px 5px;
    width: 84px;
  }

  .headline-match :deep(.scoreboard-card__score strong) {
    font-size: 18px;
  }

  .headline-match :deep(.scoreboard-card__signals) {
    grid-template-columns: 1fr auto auto;
  }

  .headline-match :deep(.scoreboard-card__signals .metric-bar small),
  .quality-bars :deep(.metric-bar small),
  .data-literacy-bars :deep(.metric-bar small),
  .readiness-row :deep(.metric-bar small) {
    display: none;
  }

  .headline-match__status {
    gap: 8px;
    padding: 10px;
  }

  .headline-match__status strong {
    font-size: 24px;
  }

  .headline-match__facts {
    grid-template-columns: 1fr;
  }

  .stat-grid {
    max-height: min(34dvh, 250px);
    overflow: auto;
    padding-right: 4px;
    scrollbar-color: rgba(147, 197, 253, .38) transparent;
  }

  .stat-card {
    min-height: 82px;
  }

  .stat-card strong,
  .gap-card strong {
    font-size: 28px;
  }

  .evidence-command-board,
  .quality-board,
  .coverage-grid {
    max-height: min(38dvh, 300px);
    overflow: auto;
    padding-right: 4px;
    scrollbar-color: rgba(147, 197, 253, .38) transparent;
  }

  .data-literacy-board {
    max-height: min(38dvh, 300px);
    overflow: auto;
    padding-right: 4px;
    scrollbar-color: rgba(147, 197, 253, .38) transparent;
  }

  .route-grid {
    max-height: min(38dvh, 300px);
    overflow: auto;
    padding-right: 4px;
    scrollbar-color: rgba(147, 197, 253, .38) transparent;
  }

  .readiness-row-list,
  .data-literacy-bars {
    max-height: min(42dvh, 320px);
    overflow: auto;
    padding-right: 4px;
    scrollbar-color: rgba(147, 197, 253, .38) transparent;
  }

  .evidence-catalog-rings,
  .quality-gap-rings,
  .ai-data-ring-grid {
    max-height: min(32dvh, 240px);
    overflow: auto;
    padding-right: 4px;
    scrollbar-color: rgba(147, 197, 253, .38) transparent;
  }

  .data-map {
    gap: 6px;
    max-height: min(26dvh, 180px);
    overflow: auto;
    padding-right: 3px;
    scrollbar-color: rgba(147, 197, 253, .38) transparent;
  }

  .data-map span {
    font-size: 11px;
    min-height: 28px;
    padding: 5px 8px;
  }

  .supplement-priority-card {
    gap: 8px;
    padding: 10px;
  }

  .supplement-priority-card__copy p,
  .supplement-priority-card :deep(.metric-bar small) {
    display: none;
  }

  .triage-action-list,
  .readiness-row-list,
  .quality-bars {
    gap: 10px;
  }

  .triage-action-card {
    grid-template-columns: 1fr;
    gap: 8px;
    padding: 10px;
  }

  .triage-action-card p {
    display: none;
  }

  .triage-action-card__link {
    width: 100%;
  }

  .route-card {
    min-height: auto;
  }

  .route-card ul {
    gap: 5px;
  }

  .route-card__cta {
    min-height: 44px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .primary-link,
  .secondary-link,
  .panel-link,
  .route-card {
    transition: none;
  }

  .primary-link:hover,
  .secondary-link:hover,
  .panel-link:hover,
  .route-card:hover {
    transform: none;
  }
}
</style>
