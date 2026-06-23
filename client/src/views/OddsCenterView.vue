<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
  getMatchOdds,
  listBookmakers,
  listOddsMarkets,
  listOddsOverview,
  type OddsMarketDetail,
  type OddsMarketDictionaryItem,
  type OddsMarketSummary,
  type OddsMatchDetail,
} from '@/api/odds';
import { useAuthStore } from '@/stores/auth';
import { formatMarketLine } from '@/utils/odds-format';

const authStore = useAuthStore();
const loading = ref(false);
const detailLoading = ref(false);
const overview = ref<OddsMarketSummary[]>([]);
const bookmakers = ref<string[]>([]);
const markets = ref<OddsMarketDictionaryItem[]>([]);
const selectedMatch = ref<OddsMatchDetail | null>(null);
const selectedMarketId = ref<number | null>(null);
const selectedBookmaker = ref('');
const selectedMarketCode = ref('');
const error = ref('');

const filteredOverview = computed(() => overview.value.filter((item) => {
  const bookmakerOk = !selectedBookmaker.value || item.bookmaker === selectedBookmaker.value;
  const marketOk = !selectedMarketCode.value || item.marketCode === selectedMarketCode.value;
  return bookmakerOk && marketOk;
}));

const stats = computed(() => ({
  matches: new Set(overview.value.map((item) => item.matchId).filter(Boolean)).size,
  bookmakers: bookmakers.value.length,
  markets: markets.value.length,
  selections: overview.value.reduce((sum, item) => sum + item.selectionCount, 0),
}));

const currentMarket = computed<OddsMarketDetail | null>(() => {
  if (!selectedMatch.value || selectedMarketId.value == null) {
    return selectedMatch.value?.markets[0] ?? null;
  }
  return selectedMatch.value.markets.find((market) => market.id === selectedMarketId.value) ?? selectedMatch.value.markets[0] ?? null;
});

function requireAuthHeader(): string {
  if (!authStore.basicAuthHeader) {
    throw new Error('请先登录后查看赔率中心。');
  }
  return authStore.basicAuthHeader;
}

function formatDateTime(value?: string): string {
  if (!value) {
    return '-';
  }
  return value.replace('T', ' ').slice(0, 16);
}

function oddsText(value?: number): string {
  return value == null ? '-' : Number(value).toFixed(4).replace(/0+$/, '').replace(/\.$/, '');
}

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const authHeader = requireAuthHeader();
    const [overviewResponse, bookmakerResponse, marketResponse] = await Promise.all([
      listOddsOverview(authHeader),
      listBookmakers(authHeader),
      listOddsMarkets(authHeader),
    ]);
    overview.value = overviewResponse.data;
    bookmakers.value = bookmakerResponse.data;
    markets.value = marketResponse.data;
    if (overview.value.length > 0) {
      await openMarket(overview.value[0]);
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
    error.value = cause instanceof Error ? cause.message : '无法读取赔率中心数据。';
  } finally {
    loading.value = false;
  }
}

async function openMarket(row: OddsMarketSummary) {
  if (!row.matchId) {
    ElMessage.warning('该赔率快照未绑定比赛，无法查看比赛详情。');
    return;
  }
  detailLoading.value = true;
  try {
    const response = await getMatchOdds(requireAuthHeader(), row.matchId);
    selectedMatch.value = response.data;
    selectedMarketId.value = row.id;
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '无法读取赔率详情。');
  } finally {
    detailLoading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <section class="page-shell odds-page">
    <section class="page-content">
      <el-page-header content="赔率中心" @back="$router.push('/')" />

      <el-alert v-if="error" :title="error" type="warning" show-icon class="top-alert" />

      <el-row :gutter="16" class="stat-row">
        <el-col :span="6"><el-card><strong>{{ stats.matches }}</strong><span>比赛</span></el-card></el-col>
        <el-col :span="6"><el-card><strong>{{ stats.bookmakers }}</strong><span>公司</span></el-card></el-col>
        <el-col :span="6"><el-card><strong>{{ stats.markets }}</strong><span>玩法</span></el-card></el-col>
        <el-col :span="6"><el-card><strong>{{ stats.selections }}</strong><span>选项赔率</span></el-card></el-col>
      </el-row>

      <el-card class="panel-card filter-card">
        <template #header>
          <div class="card-header">
            <span>盘口快照筛选</span>
            <el-button size="small" :loading="loading" @click="load">刷新</el-button>
          </div>
        </template>
        <el-form inline>
          <el-form-item label="公司">
            <el-select v-model="selectedBookmaker" clearable placeholder="全部公司" style="width: 180px">
              <el-option v-for="bookmaker in bookmakers" :key="bookmaker" :label="bookmaker" :value="bookmaker" />
            </el-select>
          </el-form-item>
          <el-form-item label="玩法">
            <el-select v-model="selectedMarketCode" clearable placeholder="全部玩法" style="width: 200px">
              <el-option
                v-for="market in markets"
                :key="market.marketCode"
                :label="`${market.marketCode} ${market.marketName || ''}`"
                :value="market.marketCode"
              />
            </el-select>
          </el-form-item>
        </el-form>
      </el-card>

      <el-row :gutter="16" class="content-row">
        <el-col :span="11">
          <el-card class="panel-card">
            <template #header>玩法市场快照</template>
            <el-table :data="filteredOverview" v-loading="loading" height="620" @row-click="openMarket">
              <el-table-column prop="matchName" label="比赛" min-width="150" />
              <el-table-column prop="bookmaker" label="公司" width="110" />
              <el-table-column label="玩法" min-width="120">
                <template #default="{ row }">
                  <strong>{{ row.marketCode }}</strong>
                  <small>{{ row.marketName || '-' }}</small>
                </template>
              </el-table-column>
              <el-table-column label="盘口" width="92">
                <template #default="{ row }">{{ formatMarketLine(row.lineValue, row.handicapLine) }}</template>
              </el-table-column>
              <el-table-column prop="snapshotType" label="快照" width="82" />
              <el-table-column label="选项" width="70">
                <template #default="{ row }">{{ row.selectionCount }}</template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>

        <el-col :span="13">
          <el-card class="panel-card" v-loading="detailLoading">
            <template #header>
              <div class="card-header">
                <span>{{ selectedMatch?.matchName || '赔率详情' }}</span>
                <el-tag type="info">{{ selectedMatch?.jcCode || '无编号' }}</el-tag>
              </div>
            </template>

            <el-empty v-if="!selectedMatch" description="请选择盘口快照" />
            <template v-else>
              <el-table :data="selectedMatch.markets" border height="220" highlight-current-row @row-click="(row: OddsMarketDetail) => selectedMarketId = row.id">
                <el-table-column prop="bookmaker" label="公司" width="110" />
                <el-table-column prop="marketCode" label="玩法" width="100" />
                <el-table-column prop="marketName" label="名称" min-width="120" />
                <el-table-column label="盘口" width="90">
                  <template #default="{ row }">{{ formatMarketLine(row.lineValue, row.handicapLine) }}</template>
                </el-table-column>
                <el-table-column prop="snapshotType" label="快照" width="88" />
                <el-table-column label="抓取时间" min-width="130">
                  <template #default="{ row }">{{ formatDateTime(row.capturedAt) }}</template>
                </el-table-column>
              </el-table>

              <h3>当前玩法选项赔率</h3>
              <el-table :data="currentMarket?.selections || []" border empty-text="暂无选项赔率">
                <el-table-column prop="selectionCode" label="选项代码" width="120" />
                <el-table-column prop="selectionName" label="选项" min-width="120" />
                <el-table-column label="赔率" width="100">
                  <template #default="{ row }">{{ oddsText(row.oddsValue) }}</template>
                </el-table-column>
                <el-table-column prop="selectionStatus" label="状态" width="100" />
                <el-table-column prop="rawPayload" label="原始字段" min-width="180" show-overflow-tooltip />
              </el-table>

              <h3>市场原始 JSON</h3>
              <pre class="raw-payload">{{ currentMarket?.rawPayload || '无原始字段' }}</pre>
            </template>
          </el-card>
        </el-col>
      </el-row>
    </section>
  </section>
</template>

<style scoped>
.odds-page { background: radial-gradient(circle at top right, rgba(245, 158, 11, 0.16), transparent 30rem), #f5f7fb; }
.top-alert, .stat-row, .filter-card, .content-row { margin-top: 16px; }
.stat-row :deep(.el-card__body) { display: flex; flex-direction: column; gap: 6px; }
.stat-row strong { color: #b45309; font-size: 30px; }
.stat-row span, small { color: #6b7280; }
.panel-card { border-radius: 14px; }
.card-header { align-items: center; display: flex; justify-content: space-between; }
h3 { margin-top: 22px; }
small { display: block; margin-top: 4px; }
.raw-payload { background: #111827; border-radius: 10px; color: #e5e7eb; max-height: 220px; overflow: auto; padding: 14px; white-space: pre-wrap; }
</style>
