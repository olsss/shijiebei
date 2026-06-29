<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import {
  getPublicMatchOdds,
  listPublicBookmakers,
  listPublicOddsMarkets,
  listPublicOddsOverview,
  type PublicOddsMarketDetail,
  type PublicOddsMarketDictionaryItem,
  type PublicOddsMarketSummary,
  type PublicOddsMatchDetail,
  type PublicOddsSelection,
} from '@/api/odds';
import CoverageDonut from '@/components/football/CoverageDonut.vue';
import MetricBar from '@/components/football/MetricBar.vue';
import ScoreboardCard from '@/components/football/ScoreboardCard.vue';
import { enumLabel, marketLabel } from '@/utils/display-labels';
import { formatMarketLine } from '@/utils/odds-format';

const loading = ref(false);
const detailLoading = ref(false);
const error = ref('');
const detailError = ref('');
const overview = ref<PublicOddsMarketSummary[]>([]);
const bookmakers = ref<string[]>([]);
const markets = ref<PublicOddsMarketDictionaryItem[]>([]);
const oddsSearchQuery = ref('');
const selectedBookmaker = ref('');
const selectedMarketCode = ref('');
const selectedSnapshotType = ref('');
const oddsPage = ref(1);
const selectedMatch = ref<PublicOddsMatchDetail | null>(null);
const selectedMarketId = ref<number | null>(null);
const ODDS_PAGE_SIZE = 12;

const filteredOverview = computed(() => overview.value.filter((item) => {
  const bookmakerOk = !selectedBookmaker.value || item.bookmaker === selectedBookmaker.value;
  const marketOk = !selectedMarketCode.value || item.marketCode === selectedMarketCode.value;
  const snapshotOk = !selectedSnapshotType.value || item.snapshotType === selectedSnapshotType.value;
  const query = oddsSearchQuery.value.trim().toLowerCase();
  const marketText = marketLabel(item.marketCode, item.marketName);
  const searchText = [
    item.matchName,
    item.jcCode,
    item.homeTeam?.teamName,
    item.homeTeam?.fifaCode,
    item.awayTeam?.teamName,
    item.awayTeam?.fifaCode,
    item.bookmaker,
    item.marketCode,
    item.marketName,
    marketText,
    snapshotTypeText(item.snapshotType),
    formatMarketLine(item.lineValue, item.handicapLine),
    item.lineValue,
    item.capturedAt,
    item.scoreboard?.scoreDisplay,
    item.scoreboard?.resultText,
  ].filter(Boolean).join(' ').toLowerCase();
  const searchOk = !query || searchText.includes(query);
  return bookmakerOk && marketOk && snapshotOk && searchOk;
}));

const snapshotTypeOptions = computed(() => {
  const values = new Set(overview.value.map((item) => item.snapshotType).filter(Boolean));
  return [...values].sort((left, right) => snapshotTypeText(left).localeCompare(snapshotTypeText(right), 'zh-Hans-CN'));
});

const oddsPageCount = computed(() => Math.max(1, Math.ceil(filteredOverview.value.length / ODDS_PAGE_SIZE)));
const pagedOverview = computed(() => {
  const start = (oddsPage.value - 1) * ODDS_PAGE_SIZE;
  return filteredOverview.value.slice(start, start + ODDS_PAGE_SIZE);
});
const oddsFilterActive = computed(() => Boolean(
  oddsSearchQuery.value.trim()
  || selectedBookmaker.value
  || selectedMarketCode.value
  || selectedSnapshotType.value,
));

const stats = computed(() => ({
  matches: new Set(overview.value.map((item) => item.matchId).filter((id) => id != null)).size,
  bookmakers: bookmakers.value.length,
  markets: markets.value.length,
  selections: overview.value.reduce((sum, item) => sum + item.selectionCount, 0),
}));

const oddsOverviewRings = computed(() => {
  const snapshotCount = overview.value.length;
  const maxSnapshot = Math.max(1, snapshotCount);
  const maxMatches = Math.max(1, stats.value.matches);
  const maxBookmakers = Math.max(1, stats.value.bookmakers);
  const maxMarkets = Math.max(1, stats.value.markets);
  const maxSelections = Math.max(1, stats.value.selections);

  return [
    {
      label: '市场快照',
      value: snapshotCount,
      max: maxSnapshot,
      unit: '项',
      tone: snapshotCount ? 'success' : 'info',
      caption: `${snapshotCount} 项公开市场快照`,
    },
    {
      label: '覆盖比赛',
      value: stats.value.matches,
      max: maxMatches,
      unit: '场',
      tone: stats.value.matches ? 'accent' : 'info',
      caption: `${stats.value.matches} 场比赛有关联市场`,
    },
    {
      label: '来源公司',
      value: stats.value.bookmakers,
      max: maxBookmakers,
      unit: '家',
      tone: stats.value.bookmakers ? 'info' : 'warning',
      caption: `${stats.value.bookmakers} 家公开来源`,
    },
    {
      label: '玩法字典',
      value: stats.value.markets,
      max: maxMarkets,
      unit: '类',
      tone: stats.value.markets ? 'accent' : 'warning',
      caption: `${stats.value.markets} 类公开玩法`,
    },
    {
      label: '选项总数',
      value: stats.value.selections,
      max: maxSelections,
      unit: '项',
      tone: stats.value.selections ? 'success' : 'info',
      caption: `${stats.value.selections} 个可读赔率点`,
    },
  ];
});

const marketTimelineGroups = computed(() => {
  const groups = new Map<string, Set<string>>();
  overview.value.forEach((item) => {
    const key = [item.matchId, item.bookmaker, item.marketCode].filter((part) => part != null && part !== '').join('|');
    if (!key) {
      return;
    }
    if (!groups.has(key)) {
      groups.set(key, new Set<string>());
    }
    groups.get(key)?.add(item.capturedAt || item.snapshotType || String(item.id));
  });
  return [...groups.values()];
});

const timelineReadyGroupCount = computed(() => marketTimelineGroups.value.filter((group) => group.size >= 2).length);
const timelineGroupCount = computed(() => marketTimelineGroups.value.length);
const timelineReadyPct = computed(() => Math.round(percentage(timelineReadyGroupCount.value, Math.max(timelineGroupCount.value, 1))));

const filterSummary = computed(() => {
  if (!overview.value.length) {
    return '暂无市场快照';
  }
  return `已筛出 ${filteredOverview.value.length} / ${overview.value.length} 个市场快照`;
});

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

const currentSelections = computed(() => currentMarket.value?.selections ?? []);

const rankedSelections = computed(() => currentSelections.value
  .filter((selection) => selection.impliedProbability != null)
  .sort((left, right) => Number(right.impliedProbability) - Number(left.impliedProbability)));

const marketStructureRings = computed(() => {
  const match = selectedMatch.value;
  const matchMarkets = match?.markets ?? [];
  const marketCount = matchMarkets.length;
  const dictionaryTotal = Math.max(1, markets.value.length || marketCount);
  const countMax = Math.max(1, marketCount);
  const liveCount = matchMarkets.filter((market) => (market.snapshotType || '').toUpperCase() === 'LIVE').length;
  const preMatchCount = matchMarkets.filter((market) => (market.snapshotType || '').toUpperCase() === 'PRE_MATCH').length;
  const selectionTotal = matchMarkets.reduce((sum, market) => sum + (market.selections?.length || market.selectionCount || 0), 0);
  const probabilityTotal = matchMarkets.reduce(
    (sum, market) => sum + (market.selections?.filter((selection) => selection.impliedProbability != null).length || 0),
    0,
  );
  const probabilityScore = selectionTotal ? Math.round((probabilityTotal / selectionTotal) * 100) : 0;
  return [
    {
      label: '玩法覆盖',
      value: marketCount,
      max: dictionaryTotal,
      unit: '项',
      tone: marketCount ? 'accent' : 'info',
      caption: `${marketCount} / ${dictionaryTotal} 类公开市场`,
    },
    {
      label: '即时快照',
      value: liveCount,
      max: countMax,
      unit: '项',
      tone: liveCount ? 'success' : 'info',
      caption: `${liveCount} / ${marketCount} 个市场快照`,
    },
    {
      label: '赛前快照',
      value: preMatchCount,
      max: countMax,
      unit: '项',
      tone: preMatchCount ? 'warning' : 'info',
      caption: `${preMatchCount} / ${marketCount} 个市场快照`,
    },
    {
      label: '选项密度',
      value: selectionTotal,
      max: Math.max(1, marketCount * 3),
      unit: '项',
      tone: selectionTotal >= marketCount * 3 ? 'success' : selectionTotal ? 'warning' : 'info',
      caption: `${selectionTotal} 个公开选项`,
    },
    {
      label: '全场概率覆盖',
      value: probabilityScore,
      max: 100,
      unit: '%',
      tone: probabilityScore >= 80 ? 'success' : probabilityScore >= 40 ? 'warning' : 'info',
      caption: `${probabilityTotal} / ${selectionTotal} 项已有隐含概率`,
    },
  ];
});

const marketLeaderProbability = computed(() => {
  const top = rankedSelections.value[0];
  return top?.impliedProbability == null ? null : Number(top.impliedProbability) * 100;
});

const probabilityCoverageScore = computed(() => {
  const total = currentSelections.value.length;
  if (!total) {
    return 0;
  }
  const covered = currentSelections.value.filter((selection) => selection.impliedProbability != null).length;
  return Math.round((covered / total) * 100);
});

const probabilityCoverageCaption = computed(() => {
  const total = currentSelections.value.length;
  const covered = currentSelections.value.filter((selection) => selection.impliedProbability != null).length;
  return total ? `${covered} / ${total} 项已有隐含概率` : '当前玩法暂无可读选项';
});

const marketLeaderCaption = computed(() => {
  const top = rankedSelections.value[0];
  if (!top) {
    return '当前玩法缺少隐含概率';
  }
  return `${top.selectionName} · ${oddsFormatLabel(top.oddsValue)} ${oddsValueText(top.oddsValue)}`;
});

const marketLeaderTone = computed<'success' | 'warning' | 'info'>(() => {
  const probability = marketLeaderProbability.value;
  if (probability == null) {
    return 'info';
  }
  if (probability >= 50) {
    return 'success';
  }
  if (probability >= 35) {
    return 'warning';
  }
  return 'info';
});

const marketInsight = computed(() => {
  const top = rankedSelections.value[0];
  if (!top) {
    return {
      title: '市场倾向待同步',
      body: '当前玩法缺少隐含概率；赔率换算、价格时间线、公众热度和市场热度比例为空。',
    };
  }
  return {
    title: `市场更偏向：${top.selectionName}`,
    body: `隐含概率最高为 ${percentText(top.impliedProbability)}；同屏保留比分、资料状态、伤停、天气、裁判和市场价格快照。`,
  };
});

function formatDateTime(value?: string): string {
  return value ? value.replace('T', ' ').slice(0, 16) : '待同步';
}

function oddsText(value?: number): string {
  return value == null ? '-' : Number(value).toFixed(4).replace(/0+$/, '').replace(/\.$/, '');
}

function oddsValueText(value?: number): string {
  if (value == null) {
    return '-';
  }
  const numericValue = Number(value);
  if (Math.abs(numericValue) >= 20) {
    return numericValue > 0 ? `+${Math.round(numericValue)}` : `${Math.round(numericValue)}`;
  }
  return oddsText(numericValue);
}

function oddsFormatLabel(value?: number): string {
  if (value == null) {
    return '赔率';
  }
  return Math.abs(Number(value)) >= 20 ? '美式赔率' : '十进制赔率';
}

function percentText(value?: number): string {
  if (value == null) {
    return '-';
  }
  return `${(Number(value) * 100).toFixed(1).replace(/\.0$/, '')}%`;
}

function probabilityValue(selection: PublicOddsSelection): number | null {
  if (selection.impliedProbability == null) {
    return null;
  }
  return Number(selection.impliedProbability) * 100;
}

function selectionTone(selection: PublicOddsSelection): 'success' | 'warning' | 'info' {
  const probability = probabilityValue(selection);
  if (probability == null) {
    return 'info';
  }
  if (probability >= 50) {
    return 'success';
  }
  if (probability >= 35) {
    return 'warning';
  }
  return 'info';
}

function percentage(value: number, total: number): number {
  if (total <= 0) {
    return 0;
  }
  return Math.max(0, Math.min(100, (value / total) * 100));
}

function snapshotTypeText(value?: string): string {
  return enumLabel('oddsSnapshot', value, '快照');
}

function resetOddsFilters() {
  oddsSearchQuery.value = '';
  selectedBookmaker.value = '';
  selectedMarketCode.value = '';
  selectedSnapshotType.value = '';
  oddsPage.value = 1;
}

function goOddsPage(direction: 'PREV' | 'NEXT') {
  if (direction === 'PREV') {
    oddsPage.value = Math.max(1, oddsPage.value - 1);
    return;
  }
  oddsPage.value = Math.min(oddsPageCount.value, oddsPage.value + 1);
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
    detailError.value = '该市场快照暂未绑定比赛，无法查看比赛维度详情。';
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

watch([oddsSearchQuery, selectedBookmaker, selectedMarketCode, selectedSnapshotType], async () => {
  oddsPage.value = 1;
  if (loading.value) {
    return;
  }
  if (selectedMarketId.value != null && filteredOverview.value.some((item) => item.id === selectedMarketId.value)) {
    return;
  }
  const first = filteredOverview.value[0];
  if (first) {
    await openMarket(first);
  } else {
    selectedMatch.value = null;
    selectedMarketId.value = null;
  }
});

watch(oddsPageCount, (next) => {
  if (oddsPage.value > next) {
    oddsPage.value = next;
  }
});

onMounted(load);
</script>

<template>
  <section class="page-shell evidence-page odds-page" aria-labelledby="odds-center-title">
    <section class="page-content odds-page__content">
      <header class="evidence-hero">
        <div>
          <p class="eyebrow">证据 · 赔率</p>
          <h1 id="odds-center-title">赔率中心</h1>
          <p>公开展示已入库的来源公司、玩法、市场线与选项赔率；实时记录、赛前快照、赛后归档会分开标识。</p>
        </div>
        <button class="action-button" type="button" :disabled="loading" @click="load">
          {{ loading ? '刷新中' : '刷新公开数据' }}
        </button>
      </header>

      <section class="stat-grid" aria-label="赔率中心统计">
        <article class="stat-card"><span>比赛</span><strong>{{ stats.matches }}</strong><small>已关联公开赔率</small></article>
        <article class="stat-card"><span>公司</span><strong>{{ stats.bookmakers }}</strong><small>公开来源数量</small></article>
        <article class="stat-card"><span>玩法</span><strong>{{ stats.markets }}</strong><small>市场字典</small></article>
        <article class="stat-card"><span>选项</span><strong>{{ stats.selections }}</strong><small>可读赔率点</small></article>
      </section>

      <section class="odds-overview-card" data-test="odds-overview-rings" aria-label="公开市场结构">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">市场结构</p>
            <h2>公开市场结构</h2>
          </div>
          <span class="count-pill">{{ overview.length }} 个市场快照</span>
        </div>
        <div class="odds-overview-rings" tabindex="0">
          <CoverageDonut
            v-for="ring in oddsOverviewRings"
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

      <section class="market-timeline-audit" data-test="market-timeline-audit" aria-label="市场时间线核查" tabindex="0">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">时间线核查</p>
            <h2>单点快照 vs 价格变化时间线</h2>
          </div>
          <span class="count-pill">{{ timelineReadyGroupCount }} / {{ timelineGroupCount }} 组具备多时点</span>
        </div>
        <div class="timeline-audit-grid">
          <CoverageDonut
            label="多时点覆盖"
            :value="timelineReadyPct"
            unit="%"
            :tone="timelineReadyPct >= 80 ? 'success' : timelineReadyPct >= 35 ? 'warning' : 'danger'"
            size="compact"
            :caption="`${timelineReadyGroupCount} / ${timelineGroupCount} 组市场拥有两个以上采集时间`"
          />
          <article>
            <strong>当前正式库已有赔率快照和单点市场倾向。</strong>
            <p>单点快照只能说明某个时间的隐含概率；真实价格变化仍需开盘、当前和至少一个中间时点，不能把 `UNKNOWN` 变化方向展示成盘口变动。</p>
          </article>
        </div>
      </section>

      <div v-if="error" class="alert-panel" role="alert">{{ error }}</div>

      <section class="filter-panel" aria-label="赔率筛选">
        <label>
          搜索市场线索
          <input
            v-model="oddsSearchQuery"
            type="search"
            aria-label="搜索比赛、球队、玩法、市场线或竞彩编号"
            placeholder="如 巴西 / 总进球 / 001 / 2.5"
          />
        </label>
        <label>
          公司
          <select v-model="selectedBookmaker" aria-label="按公司筛选市场快照">
            <option value="">全部公司</option>
            <option v-for="bookmaker in bookmakers" :key="bookmaker" :value="bookmaker">{{ bookmaker }}</option>
          </select>
        </label>
        <label>
          玩法
          <select v-model="selectedMarketCode" aria-label="按玩法筛选市场快照">
            <option value="">全部玩法</option>
            <option v-for="market in markets" :key="market.marketCode" :value="market.marketCode">
              {{ marketLabel(market.marketCode, market.marketName) }}
            </option>
          </select>
        </label>
        <label>
          快照
          <select v-model="selectedSnapshotType" aria-label="按快照类型筛选市场快照">
            <option value="">全部快照</option>
            <option v-for="snapshot in snapshotTypeOptions" :key="snapshot" :value="snapshot">
              {{ snapshotTypeText(snapshot) }}
            </option>
          </select>
        </label>
        <div class="filter-summary">
          <span>{{ filterSummary }}</span>
          <button v-if="oddsFilterActive" class="ghost-button" type="button" @click="resetOddsFilters">清除筛选</button>
        </div>
      </section>

      <section class="evidence-grid">
        <aside class="side-panel" tabindex="0" aria-label="市场快照">
          <div class="panel-heading">
            <div><p class="eyebrow">市场</p><h2>市场快照</h2></div>
            <span class="count-pill">第 {{ oddsPage }} / {{ oddsPageCount }} 页</span>
          </div>
          <p v-if="loading && !overview.length" class="empty-copy">正在加载公开市场快照...</p>
          <p v-else-if="!overview.length" class="empty-copy">暂无公开市场快照。</p>
          <div v-else-if="filteredOverview.length" class="odds-list-scroll" tabindex="0" aria-label="市场快照筛选结果">
            <button
              v-for="item in pagedOverview"
              :key="item.id"
              class="list-card"
              :class="{ 'list-card--active': item.id === selectedMarketId }"
              type="button"
              @click="openMarket(item)"
            >
              <ScoreboardCard
                compact
                :home-team="item.homeTeam"
                :away-team="item.awayTeam"
                :scoreboard="item.scoreboard"
                :match-name="item.matchName"
                :meta="formatDateTime(item.matchday)"
              />
              <span>{{ item.bookmaker }} · {{ snapshotTypeText(item.snapshotType) }}</span>
              <small>{{ marketLabel(item.marketCode, item.marketName) }} · {{ formatMarketLine(item.lineValue, item.handicapLine) }}</small>
              <small>{{ formatDateTime(item.capturedAt) }} · {{ item.selectionCount }} 项</small>
            </button>
          </div>
          <div v-else class="empty-filter-state">
            <strong>没有找到匹配市场快照</strong>
            <p>换一个球队名、玩法、竞彩编号或市场线，或清除筛选后查看。</p>
            <button class="ghost-button" type="button" @click="resetOddsFilters">清除筛选</button>
          </div>
          <nav v-if="filteredOverview.length > ODDS_PAGE_SIZE" class="odds-pager" aria-label="市场快照列表分页">
            <button type="button" :disabled="oddsPage <= 1" @click="goOddsPage('PREV')">上一页</button>
            <span>第 {{ oddsPage }} / {{ oddsPageCount }} 页</span>
            <button type="button" :disabled="oddsPage >= oddsPageCount" @click="goOddsPage('NEXT')">下一页</button>
          </nav>
        </aside>

        <article class="detail-panel" tabindex="0">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">比赛赔率</p>
              <h2>{{ selectedMatch?.matchName || '赔率详情' }}</h2>
            </div>
            <span v-if="selectedMatch" class="status-pill">竞彩 {{ selectedMatch.jcCode || '待定' }}</span>
          </div>

          <div v-if="detailError" class="alert-panel" role="alert">{{ detailError }}</div>
          <p v-else-if="detailLoading && !selectedMatch" class="empty-copy">正在加载赔率详情...</p>
          <p v-else-if="!selectedMatch" class="empty-copy">当前未选中市场快照。</p>

          <template v-else>
            <ScoreboardCard
              class="detail-scoreboard-card"
              :home-team="selectedMatch.homeTeam"
              :away-team="selectedMatch.awayTeam"
              :scoreboard="selectedMatch.scoreboard"
              :match-name="selectedMatch.matchName"
              :meta="formatDateTime(selectedMatch.matchday)"
              status="赔率证据"
              :evidence-count="selectedMatch.markets.length"
            />

            <section class="odds-visual-card" aria-label="当前市场解读">
              <div>
                <p class="eyebrow">市场信号</p>
                <h3>{{ marketInsight.title }}</h3>
                <p>{{ marketInsight.body }}</p>
              </div>
              <div class="odds-visual-rings" tabindex="0" aria-label="当前玩法市场图">
                <CoverageDonut
                  label="市场倾向峰值"
                  :value="marketLeaderProbability"
                  :tone="marketLeaderTone"
                  size="compact"
                  :caption="marketLeaderCaption"
                />
                <CoverageDonut
                  label="概率覆盖"
                  :value="probabilityCoverageScore"
                  tone="accent"
                  size="compact"
                  :caption="probabilityCoverageCaption"
                />
              </div>
              <div class="probability-list">
                <MetricBar
                  v-for="selection in currentSelections"
                  :key="`lean-${selection.id}`"
                  :label="selection.selectionName"
                  :value="probabilityValue(selection)"
                  :max="100"
                  unit="%"
                  :tone="selectionTone(selection)"
                  :caption="`${oddsFormatLabel(selection.oddsValue)} ${oddsValueText(selection.oddsValue)} · ${enumLabel('oddsSelectionStatus', selection.selectionStatus)}`"
                />
              </div>
            </section>

            <section class="market-structure-card" aria-label="比赛市场结构">
              <div class="panel-heading">
                <div>
                  <p class="eyebrow">市场结构</p>
                  <h3>比赛市场结构</h3>
                </div>
                <span class="status-pill">{{ selectedMatch.markets.length }} 个市场快照</span>
              </div>
              <div class="market-structure-rings" tabindex="0" aria-label="比赛市场结构环形图">
                <CoverageDonut
                  v-for="ring in marketStructureRings"
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

            <section class="market-card-grid" tabindex="0" aria-label="比赛市场">
              <button
                v-for="market in selectedMatch.markets"
                :key="market.id"
                class="market-card"
                :class="{ 'market-card--active': market.id === currentMarket?.id }"
                type="button"
                @click="selectMarket(market)"
              >
                <span>{{ market.bookmaker }} · {{ snapshotTypeText(market.snapshotType) }}</span>
                <strong>{{ marketLabel(market.marketCode, market.marketName) }}</strong>
                <small>{{ formatMarketLine(market.lineValue, market.handicapLine) }} · {{ formatDateTime(market.capturedAt) }}</small>
              </button>
            </section>

            <section class="info-card">
              <p class="eyebrow">选项</p>
              <h3>当前玩法选项赔率</h3>
              <div v-if="currentMarket?.selections.length" class="selection-grid" tabindex="0">
                <article v-for="selection in currentMarket.selections" :key="selection.id" class="selection-card">
                  <span>{{ selection.selectionCode }}</span>
                  <strong>{{ selection.selectionName }}</strong>
                  <b>{{ oddsValueText(selection.oddsValue) }}</b>
                  <small>{{ enumLabel('oddsSelectionStatus', selection.selectionStatus) }} · {{ oddsFormatLabel(selection.oddsValue) }} · 隐含概率 {{ percentText(selection.impliedProbability) }}</small>
                  <MetricBar
                    :label="`${selection.selectionName} 市场倾向`"
                    :value="probabilityValue(selection)"
                    :max="100"
                    unit="%"
                    :tone="selectionTone(selection)"
                    :caption="`市场倾向 ${percentText(selection.impliedProbability)}`"
                  />
                </article>
              </div>
              <p v-else class="empty-copy">当前玩法暂无选项赔率。</p>
            </section>

            <aside class="public-boundary" aria-label="赔率公开边界">
              <strong>公开边界</strong>
              <span>本页只解释赔率快照、市场线和隐含概率；不展示个人执行明细，也不输出“买哪边”的指令。</span>
            </aside>
          </template>
        </article>
      </section>
    </section>
  </section>
</template>

<style scoped>
.evidence-page { max-width: 100%; overflow-x: hidden; }
.odds-page__content { display: grid; gap: 18px; min-width: 0; }
.evidence-hero,
.stat-card,
.odds-overview-card,
.market-timeline-audit,
.filter-panel,
.side-panel,
.detail-panel,
.info-card,
.market-structure-card,
.odds-visual-card,
.public-boundary,
.alert-panel {
  background: var(--wc-glass);
  border: 1px solid var(--wc-border);
  border-radius: var(--wc-radius-lg);
  color: var(--wc-text);
  min-width: 0;
  padding: 16px;
}
.evidence-hero {
  align-items: center;
  display: grid;
  gap: 18px;
  grid-template-columns: minmax(0, 1fr) auto;
  padding: clamp(20px, 4vw, 38px);
}
.evidence-hero > div {
  min-width: 0;
}
.evidence-hero h1 {
  font-family: var(--wc-font-display);
  font-size: clamp(34px, 6vw, 68px);
  line-height: 1;
  margin: 0 0 12px;
  overflow-wrap: anywhere;
}
.evidence-hero p:not(.eyebrow),
.empty-copy,
.list-card span,
.list-card small,
.market-card span,
.market-card small,
.selection-card small,
.odds-visual-card p,
.public-boundary span {
  color: var(--wc-text-muted);
  overflow-wrap: anywhere;
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
.stat-card {
  display: grid;
  gap: 6px;
}
.stat-card span,
.stat-card small {
  color: var(--wc-text-muted);
}
.stat-card strong {
  font-family: var(--wc-font-display);
  font-size: 30px;
  line-height: 1;
}
.odds-overview-card {
  display: grid;
  gap: 14px;
}
.odds-overview-rings {
  display: grid;
  gap: 13px;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  min-width: 0;
}
.market-timeline-audit {
  display: grid;
  gap: 14px;
}
.timeline-audit-grid {
  align-items: center;
  display: grid;
  gap: 14px;
  grid-template-columns: minmax(0, 220px) minmax(0, 1fr);
  min-width: 0;
}
.timeline-audit-grid article {
  background: rgba(15, 23, 42, .42);
  border: 1px solid rgba(147, 197, 253, .16);
  border-radius: 16px;
  display: grid;
  gap: 8px;
  min-width: 0;
  padding: 14px;
}
.timeline-audit-grid p {
  color: var(--wc-text-muted);
  line-height: 1.6;
  margin: 0;
  overflow-wrap: anywhere;
}
.filter-panel {
  align-items: end;
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}
.filter-panel label {
  color: var(--wc-text-muted);
  display: grid;
  font-size: 13px;
  font-weight: 800;
  gap: 8px;
}
.filter-panel input,
.filter-panel select {
  background: rgba(15, 23, 42, .66);
  border: 1px solid rgba(147, 197, 253, .22);
  border-radius: 14px;
  color: var(--wc-text);
  font: inherit;
  min-height: 44px;
  min-width: 0;
  padding: 0 12px;
}
.filter-summary {
  align-items: center;
  color: var(--wc-text-muted);
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  grid-column: 1 / -1;
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
.evidence-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: minmax(0, 350px) minmax(0, 1fr);
  min-width: 0;
}
.side-panel {
  align-self: start;
  display: grid;
  gap: 14px;
  max-height: none;
  overflow: visible;
  position: sticky;
  top: 18px;
}
.detail-panel,
.info-card {
  display: grid;
  gap: 16px;
}
.panel-heading {
  align-items: center;
  display: flex;
  gap: 12px;
  justify-content: space-between;
}
.panel-heading h2, .info-card h3, .market-structure-card h3 { margin: 0; }
.odds-list-scroll {
  display: grid;
  gap: 10px;
  max-height: 720px;
  min-width: 0;
  overflow: auto;
  padding-right: 2px;
}
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
.market-card strong {
  display: block;
  max-width: 100%;
  min-width: 0;
  overflow-wrap: anywhere;
  word-break: break-word;
  white-space: normal;
}
.list-card, .market-card {
  cursor: pointer;
  transition: border-color 180ms ease, transform 180ms ease;
}
.list-card--active, .market-card--active { border-color: rgba(217, 119, 6, .62); }
.list-card:hover, .market-card:hover {
  border-color: rgba(147, 197, 253, .46);
  transform: translateY(-1px);
}
.list-card:focus-visible,
.market-card:focus-visible,
.action-button:focus-visible,
.filter-panel input:focus-visible,
.filter-panel select:focus-visible,
.ghost-button:focus-visible,
.odds-list-scroll:focus-visible,
.odds-pager button:focus-visible,
.odds-overview-rings:focus-visible,
.market-timeline-audit:focus-visible,
.side-panel:focus-visible,
.detail-panel:focus-visible,
.odds-visual-rings:focus-visible,
.market-structure-rings:focus-visible,
.market-card-grid:focus-visible,
.selection-grid:focus-visible {
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
.odds-pager {
  align-items: center;
  display: grid;
  gap: 10px;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
}
.odds-pager button {
  background: rgba(147, 197, 253, .1);
  border: 1px solid rgba(147, 197, 253, .22);
  border-radius: 999px;
  color: var(--wc-text);
  cursor: pointer;
  font-weight: 900;
  min-height: 44px;
}
.odds-pager button:disabled {
  cursor: not-allowed;
  opacity: .45;
}
.odds-pager span {
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
  white-space: nowrap;
}
.market-card-grid, .selection-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  min-width: 0;
}
.market-structure-card {
  display: grid;
  gap: 14px;
}
.market-structure-rings {
  display: grid;
  gap: 13px;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  min-width: 0;
}
.odds-visual-card {
  align-items: start;
  display: grid;
  gap: 16px;
  grid-template-columns: minmax(0, .92fr) minmax(0, .72fr) minmax(0, 1.1fr);
  min-width: 0;
}
.odds-visual-card > div {
  min-width: 0;
}
.odds-visual-rings,
.probability-list {
  display: grid;
  gap: 12px;
  min-width: 0;
}
.selection-card b {
  color: var(--wc-accent);
  font-family: var(--wc-font-mono);
  font-size: 26px;
}
.public-boundary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  border-color: rgba(217, 119, 6, .32);
}
@media (max-width: 1024px) {
  .evidence-hero, .evidence-grid, .odds-visual-card { grid-template-columns: 1fr; }
  .timeline-audit-grid { grid-template-columns: 1fr; }
  .stat-grid, .market-card-grid, .selection-grid, .filter-panel { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .odds-visual-rings { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .side-panel { max-height: none; position: static; }
}
@media (max-width: 640px) {
  .evidence-hero,
  .filter-panel,
  .evidence-grid,
  .odds-visual-card,
  .odds-pager {
    grid-template-columns: 1fr;
  }

  .stat-grid,
  .market-structure-rings,
  .odds-overview-rings,
  .selection-grid,
  .odds-visual-rings {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .market-card-grid {
    grid-template-columns: 1fr;
  }

  .evidence-hero,
  .stat-card,
  .odds-overview-card,
  .filter-panel,
  .side-panel,
  .detail-panel,
  .info-card,
  .market-structure-card,
  .odds-visual-card,
  .public-boundary,
  .alert-panel {
    border-radius: var(--wc-radius-md);
    gap: 10px;
    padding: 12px;
  }

  .evidence-hero h1 {
    font-size: clamp(28px, 12vw, 42px);
    margin-bottom: 8px;
  }

  .evidence-hero p:not(.eyebrow),
  .odds-visual-card p:not(.eyebrow),
  .selection-card small,
  .public-boundary span,
  .empty-filter-state p {
    display: none;
  }

  .stat-grid,
  .market-card-grid,
  .market-structure-rings,
  .odds-overview-rings,
  .selection-grid,
  .odds-visual-rings,
  .probability-list {
    gap: 10px;
  }

  .odds-overview-rings :deep(.coverage-donut),
  .market-structure-rings :deep(.coverage-donut),
  .odds-visual-rings :deep(.coverage-donut) {
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

  .odds-overview-rings :deep(.coverage-donut__ring),
  .market-structure-rings :deep(.coverage-donut__ring),
  .odds-visual-rings :deep(.coverage-donut__ring) {
    width: 66px;
  }

  .odds-overview-rings :deep(.coverage-donut__ring span),
  .market-structure-rings :deep(.coverage-donut__ring span),
  .odds-visual-rings :deep(.coverage-donut__ring span) {
    font-size: 20px;
  }

  .odds-overview-rings :deep(.coverage-donut__copy),
  .market-structure-rings :deep(.coverage-donut__copy),
  .odds-visual-rings :deep(.coverage-donut__copy) {
    gap: 3px;
  }

  .odds-overview-rings :deep(.coverage-donut__copy strong),
  .market-structure-rings :deep(.coverage-donut__copy strong),
  .odds-visual-rings :deep(.coverage-donut__copy strong) {
    font-size: 12px;
    line-height: 1.2;
  }

  .odds-overview-rings :deep(.coverage-donut__copy small),
  .market-structure-rings :deep(.coverage-donut__copy small),
  .odds-visual-rings :deep(.coverage-donut__copy small) {
    font-size: 11px;
    line-height: 1.25;
  }

  .stat-card {
    min-height: 102px;
  }

  .stat-card strong,
  .selection-card b {
    font-size: 28px;
  }

  .side-panel {
    max-height: none;
    overflow: visible;
    scrollbar-gutter: auto;
  }

  .detail-panel {
    max-height: none;
    overflow: visible;
    scrollbar-gutter: auto;
  }

  .odds-list-scroll {
    max-height: min(48dvh, 420px);
    overflow: auto;
    padding-right: 0;
    scrollbar-gutter: auto;
    scrollbar-width: none;
  }
  .odds-list-scroll::-webkit-scrollbar {
    display: none;
  }

  .market-card-grid,
  .selection-grid,
  .probability-list {
    max-height: none;
    overflow: visible;
    padding-right: 0;
    scrollbar-gutter: auto;
  }

  .market-structure-rings,
  .odds-overview-rings {
    max-height: min(46dvh, 360px);
    overflow: auto;
    padding-right: 4px;
    scrollbar-color: rgba(147, 197, 253, .38) transparent;
  }

  .list-card,
  .market-card,
  .selection-card {
    border-radius: 14px;
    gap: 5px;
    padding: 10px;
  }

  .list-card :deep(.scoreboard-card__main),
  .detail-scoreboard-card:deep(.scoreboard-card__main) {
    grid-template-columns: minmax(0, 1fr) 84px minmax(0, 1fr) !important;
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
    font-size: 12px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .list-card :deep(.scoreboard-card__score),
  .detail-scoreboard-card:deep(.scoreboard-card__score) {
    min-width: 0;
    padding: 7px 5px;
    width: 84px;
  }

  .list-card :deep(.scoreboard-card__score strong),
  .detail-scoreboard-card:deep(.scoreboard-card__score strong) {
    font-size: 18px;
  }

  .detail-scoreboard-card:deep(.scoreboard-card__signals) {
    grid-template-columns: 1fr auto;
  }

  .detail-scoreboard-card:deep(.scoreboard-card__signals .metric-bar small),
  .probability-list :deep(.metric-bar small),
  .selection-grid :deep(.metric-bar small) {
    display: none;
  }

  .panel-heading { align-items: stretch; flex-direction: column; }
  .ghost-button, .odds-pager button { width: 100%; }
  .action-button { width: 100%; }
}
@media (prefers-reduced-motion: reduce) {
  .list-card, .market-card { transition: none; }
}
</style>
