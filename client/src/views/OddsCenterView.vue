<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import {
  getPublicMatchOdds,
  listPublicBookmakers,
  listPublicOddsMarkets,
  listPublicOddsOverview,
  type PublicOddsMarketDetail,
  type PublicOddsMarketDictionaryItem,
  type PublicOddsMarketSummary,
  type PublicOddsMatchDetail,
} from '@/api/odds';
import { formatMarketLine } from '@/utils/odds-format';

const loading = ref(false);
const detailLoading = ref(false);
const error = ref('');
const detailError = ref('');
const overview = ref<PublicOddsMarketSummary[]>([]);
const bookmakers = ref<string[]>([]);
const markets = ref<PublicOddsMarketDictionaryItem[]>([]);
const selectedBookmaker = ref('');
const selectedMarketCode = ref('');
const selectedMatch = ref<PublicOddsMatchDetail | null>(null);
const selectedMarketId = ref<number | null>(null);

const filteredOverview = computed(() => overview.value.filter((item) => {
  const bookmakerOk = !selectedBookmaker.value || item.bookmaker === selectedBookmaker.value;
  const marketOk = !selectedMarketCode.value || item.marketCode === selectedMarketCode.value;
  return bookmakerOk && marketOk;
}));

const stats = computed(() => ({
  matches: new Set(overview.value.map((item) => item.matchId).filter((id) => id != null)).size,
  bookmakers: bookmakers.value.length,
  markets: markets.value.length,
  selections: overview.value.reduce((sum, item) => sum + item.selectionCount, 0),
}));

const currentMarket = computed<PublicOddsMarketDetail | null>(() => {
  if (!selectedMatch.value) {
    return null;
  }
  if (selectedMarketId.value == null) {
    return selectedMatch.value.markets[0] ?? null;
  }
  return selectedMatch.value.markets.find((market) => market.id === selectedMarketId.value)
    ?? selectedMatch.value.markets[0]
    ?? null;
});

function formatDateTime(value?: string): string {
  return value ? value.replace('T', ' ').slice(0, 16) : '待同步';
}

function oddsText(value?: number): string {
  return value == null ? '-' : Number(value).toFixed(4).replace(/0+$/, '').replace(/\.$/, '');
}

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const [overviewResponse, bookmakerResponse, marketResponse] = await Promise.all([
      listPublicOddsOverview(),
      listPublicBookmakers(),
      listPublicOddsMarkets(),
    ]);
    overview.value = overviewResponse.data;
    bookmakers.value = bookmakerResponse.data;
    markets.value = marketResponse.data;
    const first = filteredOverview.value[0] ?? overview.value[0];
    if (first) {
      await openMarket(first);
    } else {
      selectedMatch.value = null;
      selectedMarketId.value = null;
    }
  } catch (cause) {
    overview.value = [];
    bookmakers.value = [];
    markets.value = [];
    selectedMatch.value = null;
    selectedMarketId.value = null;
    error.value = cause instanceof Error ? cause.message : '无法读取公开赔率中心数据。';
  } finally {
    loading.value = false;
  }
}

async function openMarket(row: PublicOddsMarketSummary) {
  const matchId = row.matchId;
  if (matchId == null) {
    detailError.value = '该赔率快照暂未绑定比赛，无法查看比赛维度详情。';
    return;
  }
  detailLoading.value = true;
  detailError.value = '';
  try {
    const response = await getPublicMatchOdds(matchId);
    selectedMatch.value = response.data;
    selectedMarketId.value = row.id;
  } catch (cause) {
    selectedMatch.value = null;
    detailError.value = cause instanceof Error ? cause.message : '无法读取公开赔率详情。';
  } finally {
    detailLoading.value = false;
  }
}

function selectMarket(market: PublicOddsMarketDetail) {
  selectedMarketId.value = market.id;
}

onMounted(load);
</script>

<template>
  <section class="page-shell evidence-page odds-page" aria-labelledby="odds-center-title">
    <section class="page-content odds-page__content">
      <header class="evidence-hero">
        <div>
          <p class="eyebrow">Evidence · Odds</p>
          <h1 id="odds-center-title">赔率中心</h1>
          <p>公开展示已入库的公司、玩法、盘口与选项赔率；只保留分析所需的汇总字段，不展示采集原文或后台审核信息。</p>
        </div>
        <button class="action-button" type="button" :disabled="loading" @click="load">
          {{ loading ? '刷新中' : '刷新公开数据' }}
        </button>
      </header>

      <section class="stat-grid" aria-label="赔率中心统计">
        <article class="stat-card"><span>比赛</span><strong>{{ stats.matches }}</strong><small>已关联公开赔率</small></article>
        <article class="stat-card"><span>公司</span><strong>{{ stats.bookmakers }}</strong><small>公开来源数量</small></article>
        <article class="stat-card"><span>玩法</span><strong>{{ stats.markets }}</strong><small>盘口字典</small></article>
        <article class="stat-card"><span>选项</span><strong>{{ stats.selections }}</strong><small>可读赔率点</small></article>
      </section>

      <div v-if="error" class="alert-panel" role="alert">{{ error }}</div>

      <section class="filter-panel" aria-label="赔率筛选">
        <label>
          公司
          <select v-model="selectedBookmaker">
            <option value="">全部公司</option>
            <option v-for="bookmaker in bookmakers" :key="bookmaker" :value="bookmaker">{{ bookmaker }}</option>
          </select>
        </label>
        <label>
          玩法
          <select v-model="selectedMarketCode">
            <option value="">全部玩法</option>
            <option v-for="market in markets" :key="market.marketCode" :value="market.marketCode">
              {{ market.marketCode }} {{ market.marketName || '' }}
            </option>
          </select>
        </label>
      </section>

      <section class="evidence-grid">
        <aside class="side-panel" aria-label="盘口快照">
          <div class="panel-heading">
            <div><p class="eyebrow">Markets</p><h2>盘口快照</h2></div>
            <span class="count-pill">{{ filteredOverview.length }}</span>
          </div>
          <p v-if="loading && !overview.length" class="empty-copy">正在加载公开盘口...</p>
          <p v-else-if="!filteredOverview.length" class="empty-copy">暂无符合筛选条件的盘口。</p>
          <button
            v-for="item in filteredOverview"
            v-else
            :key="item.id"
            class="list-card"
            :class="{ 'list-card--active': item.id === selectedMarketId }"
            type="button"
            @click="openMarket(item)"
          >
            <span>{{ item.bookmaker }} · {{ item.snapshotType }}</span>
            <strong>{{ item.matchName || '比赛待同步' }}</strong>
            <small>{{ item.marketCode }} {{ item.marketName || '' }} · {{ formatMarketLine(item.lineValue, item.handicapLine) }}</small>
            <small>{{ formatDateTime(item.capturedAt) }} · {{ item.selectionCount }} 项</small>
          </button>
        </aside>

        <article class="detail-panel">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">Match Odds</p>
              <h2>{{ selectedMatch?.matchName || '赔率详情' }}</h2>
            </div>
            <span v-if="selectedMatch" class="status-pill">JC {{ selectedMatch.jcCode || '待定' }}</span>
          </div>

          <div v-if="detailError" class="alert-panel" role="alert">{{ detailError }}</div>
          <p v-else-if="detailLoading && !selectedMatch" class="empty-copy">正在加载赔率详情...</p>
          <p v-else-if="!selectedMatch" class="empty-copy">请选择左侧盘口。</p>

          <template v-else>
            <section class="market-card-grid" aria-label="比赛盘口">
              <button
                v-for="market in selectedMatch.markets"
                :key="market.id"
                class="market-card"
                :class="{ 'market-card--active': market.id === currentMarket?.id }"
                type="button"
                @click="selectMarket(market)"
              >
                <span>{{ market.bookmaker }} · {{ market.snapshotType }}</span>
                <strong>{{ market.marketCode }} {{ market.marketName || '' }}</strong>
                <small>{{ formatMarketLine(market.lineValue, market.handicapLine) }} · {{ formatDateTime(market.capturedAt) }}</small>
              </button>
            </section>

            <section class="info-card">
              <p class="eyebrow">Selections</p>
              <h3>当前玩法选项赔率</h3>
              <div v-if="currentMarket?.selections.length" class="selection-grid">
                <article v-for="selection in currentMarket.selections" :key="selection.id" class="selection-card">
                  <span>{{ selection.selectionCode }}</span>
                  <strong>{{ selection.selectionName }}</strong>
                  <b>{{ oddsText(selection.oddsValue) }}</b>
                  <small>{{ selection.selectionStatus }} · 隐含概率 {{ oddsText(selection.impliedProbability) }}</small>
                </article>
              </div>
              <p v-else class="empty-copy">当前玩法暂无选项赔率。</p>
            </section>
          </template>
        </article>
      </section>
    </section>
  </section>
</template>

<style scoped>
.evidence-page { max-width: 100%; overflow-x: hidden; }
.odds-page__content { display: grid; gap: 18px; min-width: 0; }
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
.evidence-hero p:not(.eyebrow), .empty-copy, .list-card span, .list-card small, .market-card span, .market-card small, .selection-card small {
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
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
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
.evidence-grid {
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
.panel-heading h2, .info-card h3 { margin: 0; }
.list-card, .market-card, .selection-card {
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
.list-card, .market-card {
  cursor: pointer;
  transition: border-color 180ms ease, transform 180ms ease;
}
.list-card--active, .market-card--active { border-color: rgba(217, 119, 6, .62); }
.count-pill, .status-pill {
  background: rgba(147, 197, 253, .12);
  border-radius: 999px;
  color: var(--wc-primary);
  font-family: var(--wc-font-mono);
  font-size: 12px;
  font-weight: 800;
  padding: 6px 9px;
}
.market-card-grid, .selection-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  min-width: 0;
}
.selection-card b {
  color: var(--wc-accent);
  font-family: var(--wc-font-mono);
  font-size: 26px;
}
@media (max-width: 1024px) {
  .evidence-hero, .evidence-grid { grid-template-columns: 1fr; }
  .stat-grid, .market-card-grid, .selection-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 640px) {
  .evidence-hero, .stat-grid, .filter-panel, .evidence-grid, .market-card-grid, .selection-grid { grid-template-columns: 1fr; }
  .panel-heading { align-items: stretch; flex-direction: column; }
  .action-button { width: 100%; }
}
@media (prefers-reduced-motion: reduce) {
  .list-card, .market-card { transition: none; }
}
</style>
