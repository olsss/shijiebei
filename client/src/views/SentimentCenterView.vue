<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
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
const overview = ref<PublicSentimentFactorSummary[]>([]);
const categories = ref<string[]>([]);
const riskTypes = ref<string[]>([]);
const selectedCategory = ref('');
const selectedRiskLevel = ref('');
const staleOnly = ref(false);
const selectedMatch = ref<PublicSentimentMatchDetail | null>(null);
const selectedFactorId = ref<number | null>(null);
const error = ref('');

const riskLevels = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'UNKNOWN'];

const filteredOverview = computed(() => overview.value.filter((item) => {
  const categoryOk = !selectedCategory.value || item.factorCategory === selectedCategory.value;
  const riskOk = !selectedRiskLevel.value || item.highestRiskLevel === selectedRiskLevel.value;
  const staleOk = !staleOnly.value || item.stale;
  return categoryOk && riskOk && staleOk;
}));

const stats = computed(() => ({
  matches: new Set(overview.value.map((item) => item.matchId).filter(Boolean)).size,
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
  if (!value) {
    return '-';
  }
  return value.replace('T', ' ').slice(0, 16);
}

function scoreText(value?: number): string {
  if (value == null) {
    return '-';
  }
  return Number(value).toFixed(1).replace(/\.0$/, '');
}

function riskTagType(level?: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  switch (level) {
    case 'CRITICAL':
    case 'HIGH':
      return 'danger';
    case 'MEDIUM':
      return 'warning';
    case 'LOW':
      return 'success';
    default:
      return 'info';
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
    if (overview.value.length > 0) {
      await openFactor(overview.value[0]);
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
    error.value = cause instanceof Error ? cause.message : '无法读取舆情与外部因素数据。';
  } finally {
    loading.value = false;
  }
}

async function openFactor(row: PublicSentimentFactorSummary) {
  if (!row.matchId) {
    ElMessage.warning('该因素未绑定比赛，无法查看比赛维度详情。');
    return;
  }
  detailLoading.value = true;
  try {
    const response = await getPublicMatchSentiment(row.matchId);
    selectedMatch.value = response.data;
    selectedFactorId.value = row.id;
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '无法读取比赛舆情详情。');
  } finally {
    detailLoading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <section class="page-shell sentiment-page">
    <section class="page-content">
      <el-page-header content="舆情与外部因素中心" @back="$router.push('/')" />

      <el-alert
        class="boundary-alert"
        title="仅展示已批准 JSON 的事实记录、来源与风险评分，不生成方向或金额结论。"
        type="info"
        show-icon
        :closable="false"
      />
      <el-alert v-if="error" :title="error" type="warning" show-icon class="top-alert" />

      <el-row :gutter="16" class="stat-row">
        <el-col :span="6"><el-card><strong>{{ stats.matches }}</strong><span>比赛</span></el-card></el-col>
        <el-col :span="6"><el-card><strong>{{ stats.factors }}</strong><span>因素记录</span></el-card></el-col>
        <el-col :span="6"><el-card><strong>{{ stats.risks }}</strong><span>风险评分</span></el-card></el-col>
        <el-col :span="6"><el-card><strong>{{ stats.stale }}</strong><span>过期提醒</span></el-card></el-col>
      </el-row>

      <el-card class="panel-card filter-card">
        <template #header>
          <div class="card-header">
            <span>A1 事实记录 + A2 风险评分筛选</span>
            <el-button size="small" :loading="loading" @click="load">刷新</el-button>
          </div>
        </template>
        <el-form inline>
          <el-form-item label="因素分类">
            <el-select v-model="selectedCategory" clearable placeholder="全部分类" style="width: 180px">
              <el-option v-for="category in categories" :key="category" :label="category" :value="category" />
            </el-select>
          </el-form-item>
          <el-form-item label="最高风险">
            <el-select v-model="selectedRiskLevel" clearable placeholder="全部等级" style="width: 160px">
              <el-option v-for="level in riskLevels" :key="level" :label="level" :value="level" />
            </el-select>
          </el-form-item>
          <el-form-item label="时效">
            <el-switch v-model="staleOnly" active-text="只看过期" inactive-text="全部" />
          </el-form-item>
          <el-form-item label="风险类型">
            <el-tag v-for="riskType in riskTypes" :key="riskType" class="risk-chip" type="info" effect="plain">
              {{ riskType }}
            </el-tag>
            <span v-if="riskTypes.length === 0" class="muted-text">暂无</span>
          </el-form-item>
        </el-form>
      </el-card>

      <el-row :gutter="16" class="content-row">
        <el-col :span="11">
          <el-card class="panel-card">
            <template #header>外部因素与舆情记录</template>
            <el-table :data="filteredOverview" v-loading="loading" height="640" @row-click="openFactor">
              <el-table-column prop="matchName" label="比赛" min-width="150" />
              <el-table-column label="分类" width="120">
                <template #default="{ row }">
                  <strong>{{ row.factorCategory }}</strong>
                  <small>{{ row.factorType || '-' }}</small>
                </template>
              </el-table-column>
              <el-table-column prop="title" label="标题" min-width="150" show-overflow-tooltip />
              <el-table-column label="风险" width="110">
                <template #default="{ row }">
                  <el-tag :type="riskTagType(row.highestRiskLevel)" effect="dark">
                    {{ row.highestRiskLevel || 'UNKNOWN' }}
                  </el-tag>
                  <small>{{ row.riskCount }} 条</small>
                </template>
              </el-table-column>
              <el-table-column label="时效" width="92">
                <template #default="{ row }">
                  <el-tag :type="row.stale ? 'danger' : 'success'" effect="plain">
                    {{ row.stale ? '已过期' : '有效' }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>

        <el-col :span="13">
          <el-card class="panel-card" v-loading="detailLoading">
            <template #header>
              <div class="card-header">
                <span>{{ selectedMatch?.matchName || '比赛舆情详情' }}</span>
                <el-tag type="info">{{ selectedMatch?.jcCode || '无编号' }}</el-tag>
              </div>
            </template>

            <el-empty v-if="!selectedMatch" description="请选择左侧记录" />
            <template v-else>
              <el-table
                :data="selectedMatch.factors"
                border
                height="230"
                highlight-current-row
                @row-click="(row: PublicSentimentFactorDetail) => selectedFactorId = row.id"
              >
                <el-table-column prop="factorCategory" label="分类" width="110" />
                <el-table-column prop="factorType" label="类型" width="110" />
                <el-table-column prop="title" label="标题" min-width="140" show-overflow-tooltip />
                <el-table-column label="来源" min-width="130" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.sourceName || row.sourceRef || '-' }}</template>
                </el-table-column>
                <el-table-column label="可信度" width="92">
                  <template #default="{ row }">{{ scoreText(row.reliabilityScore) }}</template>
                </el-table-column>
                <el-table-column label="风险数" width="80">
                  <template #default="{ row }">{{ factorRiskCount(row) }}</template>
                </el-table-column>
              </el-table>

              <div class="detail-grid">
                <div>
                  <h3>当前因素摘要</h3>
                  <el-descriptions v-if="currentFactor" :column="2" border size="small">
                    <el-descriptions-item label="影响方向">{{ currentFactor.impactDirection || '-' }}</el-descriptions-item>
                    <el-descriptions-item label="对象">{{ currentFactor.entityType || '-' }}</el-descriptions-item>
                    <el-descriptions-item label="证据等级">{{ currentFactor.evidenceLevel || '-' }}</el-descriptions-item>
                    <el-descriptions-item label="置信分">{{ scoreText(currentFactor.confidenceScore) }}</el-descriptions-item>
                    <el-descriptions-item label="观测时间">{{ formatDateTime(currentFactor.observedAt) }}</el-descriptions-item>
                    <el-descriptions-item label="过期时间">
                      <el-tag v-if="currentFactor.stale" type="danger" effect="plain">已过期</el-tag>
                      {{ formatDateTime(currentFactor.expiresAt) }}
                    </el-descriptions-item>
                    <el-descriptions-item label="摘要" :span="2">{{ currentFactor.summary || '-' }}</el-descriptions-item>
                  </el-descriptions>
                </div>

                <div>
                  <h3>关联风险评分</h3>
                  <el-table :data="currentFactorRisks" border empty-text="当前因素暂无风险评分">
                    <el-table-column prop="riskType" label="类型" min-width="130" />
                    <el-table-column label="等级" width="92">
                      <template #default="{ row }">
                        <el-tag :type="riskTagType(row.riskLevel)" effect="dark">{{ row.riskLevel }}</el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column label="分数" width="80">
                      <template #default="{ row }">{{ scoreText(row.riskScore) }}</template>
                    </el-table-column>
                    <el-table-column prop="title" label="标题" min-width="120" />
                    <el-table-column prop="suggestedAction" label="动作代码" min-width="120" show-overflow-tooltip />
                  </el-table>
                </div>
              </div>

              <h3>比赛级风险评分</h3>
              <el-table :data="matchLevelRisks" border empty-text="暂无比赛级风险评分">
                <el-table-column prop="riskType" label="类型" min-width="130" />
                <el-table-column label="等级" width="92">
                  <template #default="{ row }">
                    <el-tag :type="riskTagType(row.riskLevel)" effect="dark">{{ row.riskLevel }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="分数" width="80">
                  <template #default="{ row }">{{ scoreText(row.riskScore) }}</template>
                </el-table-column>
                <el-table-column prop="title" label="标题" min-width="120" />
                <el-table-column prop="suggestedAction" label="动作代码" min-width="120" show-overflow-tooltip />
              </el-table>

            </template>
          </el-card>
        </el-col>
      </el-row>
    </section>
  </section>
</template>

<style scoped>
.sentiment-page {
  background: radial-gradient(circle at top right, rgba(14, 165, 233, 0.18), transparent 32rem), #f5f7fb;
}
.boundary-alert,
.top-alert,
.stat-row,
.filter-card,
.content-row {
  margin-top: 16px;
}
.stat-row :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.stat-row strong {
  color: #0369a1;
  font-size: 30px;
}
.stat-row span,
small,
.muted-text {
  color: #6b7280;
}
.panel-card {
  border-radius: 14px;
}
.card-header {
  align-items: center;
  display: flex;
  justify-content: space-between;
}
.risk-chip {
  margin-right: 6px;
}
h3 {
  margin-top: 22px;
}
small {
  display: block;
  margin-top: 4px;
}
.detail-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: 1fr;
}
</style>
