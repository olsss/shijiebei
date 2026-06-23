<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import {
  getPublicMatchSentiment,
  listPublicSentimentCategories,
  listPublicSentimentOverview,
  listPublicSentimentRiskTypes,
  type PublicSentimentFactorDetail,
  type PublicSentimentFactorSummary,
  type PublicSentimentMatchDetail,
  type PublicSentimentRisk,
} from '@/api/sentiment';

const loading = ref(false);
const detailLoading = ref(false);
const error = ref('');
const detailError = ref('');
const overview = ref<PublicSentimentFactorSummary[]>([]);
const categories = ref<string[]>([]);
const riskTypes = ref<string[]>([]);
const selectedCategory = ref('');
const selectedRiskLevel = ref('');
const staleOnly = ref(false);
const selectedMatch = ref<PublicSentimentMatchDetail | null>(null);
const selectedFactorId = ref<number | null>(null);

const riskLevels = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'UNKNOWN'];

const filteredOverview = computed(() => overview.value.filter((item) => {
  const categoryOk = !selectedCategory.value || item.factorCategory === selectedCategory.value;
  const riskOk = !selectedRiskLevel.value || item.highestRiskLevel === selectedRiskLevel.value;
  const staleOk = !staleOnly.value || item.stale;
  return categoryOk && riskOk && staleOk;
}));

const stats = computed(() => ({
  matches: new Set(overview.value.map((item) => item.matchId).filter((id) => id != null)).size,
  factors: overview.value.length,
  risks: Math.max(
    overview.value.reduce((sum, item) => sum + item.riskCount, 0),
    selectedMatch.value?.risks.length ?? 0,
  ),
  stale: overview.value.filter((item) => item.stale).length,
}));

const currentFactor = computed<PublicSentimentFactorDetail | null>(() => {
  if (!selectedMatch.value) {
    return null;
  }
  if (selectedFactorId.value == null) {
    return selectedMatch.value.factors[0] ?? null;
  }
  return selectedMatch.value.factors.find((factor) => factor.id === selectedFactorId.value)
    ?? selectedMatch.value.factors[0]
    ?? null;
});

const currentFactorRisks = computed<PublicSentimentRisk[]>(() => {
  if (!currentFactor.value || !selectedMatch.value) {
    return [];
  }
  return selectedMatch.value.risks.filter((risk) => risk.factorId === currentFactor.value?.id);
});

const matchLevelRisks = computed<PublicSentimentRisk[]>(() => {
  if (!selectedMatch.value) {
    return [];
  }
  return selectedMatch.value.risks.filter((risk) => risk.factorId == null);
});

function formatDateTime(value?: string): string {
  return value ? value.replace('T', ' ').slice(0, 16) : '待同步';
}

function scoreText(value?: number): string {
  return value == null ? '-' : Number(value).toFixed(1).replace(/\.0$/, '');
}

function riskClass(level?: string): string {
  switch (level) {
    case 'CRITICAL':
    case 'HIGH':
      return 'risk-pill--danger';
    case 'MEDIUM':
      return 'risk-pill--warning';
    case 'LOW':
      return 'risk-pill--success';
    default:
      return 'risk-pill--info';
  }
}

function factorRiskCount(factor: PublicSentimentFactorDetail): number {
  if (!selectedMatch.value) {
    return 0;
  }
  return selectedMatch.value.risks.filter((risk) => risk.factorId === factor.id).length;
}

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const [overviewResponse, categoryResponse, riskTypeResponse] = await Promise.all([
      listPublicSentimentOverview(),
      listPublicSentimentCategories(),
      listPublicSentimentRiskTypes(),
    ]);
    overview.value = overviewResponse.data;
    categories.value = categoryResponse.data;
    riskTypes.value = riskTypeResponse.data;
    const first = filteredOverview.value[0] ?? overview.value[0];
    if (first) {
      await openFactor(first);
    } else {
      selectedMatch.value = null;
      selectedFactorId.value = null;
    }
  } catch (cause) {
    overview.value = [];
    categories.value = [];
    riskTypes.value = [];
    selectedMatch.value = null;
    selectedFactorId.value = null;
    error.value = cause instanceof Error ? cause.message : '无法读取公开舆情与外部因素数据。';
  } finally {
    loading.value = false;
  }
}

async function openFactor(row: PublicSentimentFactorSummary) {
  const matchId = row.matchId;
  if (matchId == null) {
    detailError.value = '该因素暂未绑定比赛，无法查看比赛维度详情。';
    return;
  }
  detailLoading.value = true;
  detailError.value = '';
  try {
    const response = await getPublicMatchSentiment(matchId);
    selectedMatch.value = response.data;
    selectedFactorId.value = row.id;
  } catch (cause) {
    selectedMatch.value = null;
    detailError.value = cause instanceof Error ? cause.message : '无法读取公开舆情详情。';
  } finally {
    detailLoading.value = false;
  }
}

function selectFactor(factor: PublicSentimentFactorDetail) {
  selectedFactorId.value = factor.id;
}

onMounted(load);
</script>

<template>
  <section class="page-shell evidence-page sentiment-page" aria-labelledby="sentiment-center-title">
    <section class="page-content sentiment-page__content">
      <header class="evidence-hero">
        <div>
          <p class="eyebrow">Evidence · Sentiment</p>
          <h1 id="sentiment-center-title">舆情与外部因素中心</h1>
          <p>聚合天气、公众热度、伤停与环境因素，只展示已入库的事实摘要、来源可信度和风险评分，不展示采集底稿。</p>
        </div>
        <button class="action-button" type="button" :disabled="loading" @click="load">
          {{ loading ? '刷新中' : '刷新公开数据' }}
        </button>
      </header>

      <section class="stat-grid" aria-label="舆情统计">
        <article class="stat-card"><span>比赛</span><strong>{{ stats.matches }}</strong><small>公开因素覆盖</small></article>
        <article class="stat-card"><span>因素</span><strong>{{ stats.factors }}</strong><small>A1 事实记录</small></article>
        <article class="stat-card"><span>风险</span><strong>{{ stats.risks }}</strong><small>A2 评分项</small></article>
        <article class="stat-card"><span>过期</span><strong>{{ stats.stale }}</strong><small>需关注时效</small></article>
      </section>

      <div v-if="error" class="alert-panel" role="alert">{{ error }}</div>

      <section class="filter-panel" aria-label="舆情筛选">
        <label>
          因素分类
          <select v-model="selectedCategory">
            <option value="">全部分类</option>
            <option v-for="category in categories" :key="category" :value="category">{{ category }}</option>
          </select>
        </label>
        <label>
          风险等级
          <select v-model="selectedRiskLevel">
            <option value="">全部等级</option>
            <option v-for="level in riskLevels" :key="level" :value="level">{{ level }}</option>
          </select>
        </label>
        <label class="check-row">
          <input v-model="staleOnly" type="checkbox">
          只看过期提醒
        </label>
        <div class="risk-type-row" aria-label="风险类型">
          <span v-for="riskType in riskTypes" :key="riskType" class="type-chip">{{ riskType }}</span>
          <span v-if="riskTypes.length === 0" class="muted-text">暂无风险类型</span>
        </div>
      </section>

      <section class="evidence-grid">
        <aside class="side-panel" aria-label="外部因素列表">
          <div class="panel-heading">
            <div><p class="eyebrow">Factors</p><h2>因素记录</h2></div>
            <span class="count-pill">{{ filteredOverview.length }}</span>
          </div>
          <p v-if="loading && !overview.length" class="empty-copy">正在加载公开舆情...</p>
          <p v-else-if="!filteredOverview.length" class="empty-copy">暂无符合筛选条件的因素。</p>
          <button
            v-for="item in filteredOverview"
            v-else
            :key="item.id"
            class="list-card"
            :class="{ 'list-card--active': item.id === selectedFactorId }"
            type="button"
            @click="openFactor(item)"
          >
            <span>{{ item.matchName || '比赛待同步' }} · {{ item.factorCategory }}</span>
            <strong>{{ item.title }}</strong>
            <small>{{ item.summary || '暂无摘要' }}</small>
            <small>{{ item.sourceName || '来源待同步' }} · {{ item.stale ? '已过期' : '有效' }}</small>
            <span class="risk-pill" :class="riskClass(item.highestRiskLevel)">{{ item.highestRiskLevel || 'UNKNOWN' }}</span>
          </button>
        </aside>

        <article class="detail-panel">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">Match Detail</p>
              <h2>{{ selectedMatch?.matchName || '舆情详情' }}</h2>
            </div>
            <span v-if="selectedMatch" class="status-pill">JC {{ selectedMatch.jcCode || '待定' }}</span>
          </div>

          <div v-if="detailError" class="alert-panel" role="alert">{{ detailError }}</div>
          <p v-else-if="detailLoading && !selectedMatch" class="empty-copy">正在加载舆情详情...</p>
          <p v-else-if="!selectedMatch" class="empty-copy">请选择左侧因素。</p>

          <template v-else>
            <section class="factor-card-grid" aria-label="比赛因素">
              <button
                v-for="factor in selectedMatch.factors"
                :key="factor.id"
                class="factor-card"
                :class="{ 'factor-card--active': factor.id === currentFactor?.id }"
                type="button"
                @click="selectFactor(factor)"
              >
                <span>{{ factor.factorCategory }} · {{ factor.factorType || '类型待定' }}</span>
                <strong>{{ factor.title }}</strong>
                <small>{{ factor.sourceName || factor.sourceRef || '来源待同步' }} · 风险 {{ factorRiskCount(factor) }}</small>
              </button>
            </section>

            <section class="detail-card-grid">
              <article class="info-card">
                <p class="eyebrow">Current Factor</p>
                <h3>当前因素摘要</h3>
                <template v-if="currentFactor">
                  <div class="summary-grid">
                    <div><span>影响方向</span><strong>{{ currentFactor.impactDirection || '-' }}</strong></div>
                    <div><span>证据等级</span><strong>{{ currentFactor.evidenceLevel || '-' }}</strong></div>
                    <div><span>置信分</span><strong>{{ scoreText(currentFactor.confidenceScore) }}</strong></div>
                    <div><span>可信度</span><strong>{{ scoreText(currentFactor.reliabilityScore) }}</strong></div>
                  </div>
                  <p>{{ currentFactor.summary || '暂无摘要' }}</p>
                  <small>{{ formatDateTime(currentFactor.observedAt) }} · 过期时间 {{ formatDateTime(currentFactor.expiresAt) }}</small>
                </template>
                <p v-else class="empty-copy">暂无当前因素。</p>
              </article>

              <article class="info-card">
                <p class="eyebrow">Factor Risks</p>
                <h3>关联风险评分</h3>
                <div v-if="currentFactorRisks.length" class="risk-grid">
                  <article v-for="risk in currentFactorRisks" :key="risk.id" class="risk-card">
                    <span class="risk-pill" :class="riskClass(risk.riskLevel)">{{ risk.riskLevel }}</span>
                    <strong>{{ risk.title }}</strong>
                    <small>{{ risk.riskType }} · 分数 {{ scoreText(risk.riskScore) }}</small>
                    <p>{{ risk.rationale || risk.suggestedAction || '保持观察' }}</p>
                  </article>
                </div>
                <p v-else class="empty-copy">当前因素暂无风险评分。</p>
              </article>
            </section>

            <section class="info-card">
              <p class="eyebrow">Match Risks</p>
              <h3>比赛级风险评分</h3>
              <div v-if="matchLevelRisks.length" class="risk-grid">
                <article v-for="risk in matchLevelRisks" :key="risk.id" class="risk-card">
                  <span class="risk-pill" :class="riskClass(risk.riskLevel)">{{ risk.riskLevel }}</span>
                  <strong>{{ risk.title }}</strong>
                  <small>{{ risk.riskType }} · 分数 {{ scoreText(risk.riskScore) }}</small>
                  <p>{{ risk.rationale || risk.suggestedAction || '保持观察' }}</p>
                </article>
              </div>
              <p v-else class="empty-copy">暂无比赛级风险评分。</p>
            </section>
          </template>
        </article>
      </section>
    </section>
  </section>
</template>

<style scoped>
.evidence-page { max-width: 100%; overflow-x: hidden; }
.sentiment-page__content { display: grid; gap: 18px; min-width: 0; }
.evidence-hero, .stat-card, .filter-panel, .side-panel, .detail-panel, .info-card, .alert-panel {
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
.evidence-hero p:not(.eyebrow), .empty-copy, .list-card span, .list-card small, .factor-card span, .factor-card small, .summary-grid span, .risk-card small, .risk-card p, .muted-text {
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
.stat-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}
.stat-card, .side-panel, .detail-panel, .info-card, .alert-panel {
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
.filter-panel {
  align-items: end;
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  min-width: 0;
  padding: 16px;
}
.filter-panel label {
  color: var(--wc-text-muted);
  display: grid;
  font-size: 13px;
  font-weight: 800;
  gap: 8px;
}
.filter-panel select {
  background: rgba(15, 23, 42, .66);
  border: 1px solid rgba(147, 197, 253, .22);
  border-radius: 14px;
  color: var(--wc-text);
  min-height: 44px;
  min-width: 0;
  padding: 0 12px;
}
.check-row {
  align-items: center;
  display: flex !important;
  min-height: 44px;
}
.check-row input { min-height: 20px; min-width: 20px; }
.risk-type-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}
.type-chip, .risk-pill, .count-pill, .status-pill {
  border-radius: 999px;
  font-family: var(--wc-font-mono);
  font-size: 12px;
  font-weight: 800;
  padding: 6px 9px;
}
.type-chip, .count-pill, .status-pill {
  background: rgba(147, 197, 253, .12);
  color: var(--wc-primary);
}
.evidence-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: minmax(0, 360px) minmax(0, 1fr);
  min-width: 0;
}
.panel-heading {
  align-items: center;
  display: flex;
  gap: 12px;
  justify-content: space-between;
}
.panel-heading h2, .info-card h3 { margin: 0; }
.list-card, .factor-card, .risk-card, .summary-grid div {
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
.list-card, .factor-card {
  cursor: pointer;
  transition: border-color 180ms ease, transform 180ms ease;
}
.list-card--active, .factor-card--active { border-color: rgba(217, 119, 6, .62); }
.factor-card-grid, .detail-card-grid, .risk-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  min-width: 0;
}
.summary-grid {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  min-width: 0;
}
.risk-pill--danger { background: rgba(239, 68, 68, .16); color: #fecaca; }
.risk-pill--warning { background: rgba(245, 158, 11, .18); color: #fde68a; }
.risk-pill--success { background: rgba(34, 197, 94, .16); color: #bbf7d0; }
.risk-pill--info { background: rgba(147, 197, 253, .12); color: var(--wc-primary); }
@media (max-width: 1024px) {
  .evidence-hero, .evidence-grid { grid-template-columns: 1fr; }
  .stat-grid, .filter-panel, .factor-card-grid, .detail-card-grid, .risk-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 640px) {
  .evidence-hero, .stat-grid, .filter-panel, .evidence-grid, .factor-card-grid, .detail-card-grid, .risk-grid, .summary-grid { grid-template-columns: 1fr; }
  .panel-heading { align-items: stretch; flex-direction: column; }
  .action-button { width: 100%; }
}
@media (prefers-reduced-motion: reduce) {
  .list-card, .factor-card { transition: none; }
}
</style>
