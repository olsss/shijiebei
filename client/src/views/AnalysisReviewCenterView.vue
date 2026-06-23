<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
  fetchAnalysisReviewOverview,
  getAnalysisReport,
  getBetPlan,
  listAnalysisReports,
  listBetPlans,
  listBetRecords,
  listPostMatchReviews,
  type AnalysisReportDetail,
  type AnalysisReportSummary,
  type AnalysisReviewOverview,
  type BetPlanDetail,
  type BetPlanSummary,
  type BetRecord,
  type PostMatchReview,
} from '@/api/analysisReview';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const loading = ref(false);
const detailLoading = ref(false);
const error = ref('');
const overview = ref<AnalysisReviewOverview | null>(null);
const reports = ref<AnalysisReportSummary[]>([]);
const betPlans = ref<BetPlanSummary[]>([]);
const bets = ref<BetRecord[]>([]);
const reviews = ref<PostMatchReview[]>([]);
const selectedReport = ref<AnalysisReportDetail | null>(null);
const selectedPlan = ref<BetPlanDetail | null>(null);
const activeTab = ref('reports');

const stats = computed(() => [
  { label: '分析报告', value: overview.value?.reportCount ?? 0 },
  { label: 'AI 下注方案', value: overview.value?.betPlanCount ?? 0 },
  { label: '实际出票', value: overview.value?.betCount ?? 0 },
  { label: '赛后复盘', value: overview.value?.reviewCount ?? 0 },
  { label: '总投入', value: moneyText(overview.value?.totalStake) },
  { label: '净盈亏', value: moneyText(overview.value?.netProfit) },
  { label: 'ROI', value: percentText(overview.value?.roi) },
  { label: '平均 CLV', value: percentText(overview.value?.averageClv) },
]);

function requireAuthHeader(): string {
  if (!authStore.basicAuthHeader) {
    throw new Error('请先登录后查看分析下注复盘中心。');
  }
  return authStore.basicAuthHeader;
}

function moneyText(value?: number): string {
  if (value == null) {
    return '0';
  }
  return Number(value).toFixed(2).replace(/\.00$/, '');
}

function oddsText(value?: number): string {
  if (value == null) {
    return '-';
  }
  return Number(value).toFixed(4).replace(/0+$/, '').replace(/\.$/, '');
}

function percentText(value?: number): string {
  if (value == null) {
    return '0%';
  }
  return `${(Number(value) * 100).toFixed(2).replace(/\.00$/, '')}%`;
}

function formatDateTime(value?: string): string {
  if (!value) {
    return '-';
  }
  return value.replace('T', ' ').slice(0, 16);
}

function tagType(value?: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  switch (value) {
    case 'HIT':
    case 'IMPORTED':
    case 'READY':
      return 'success';
    case 'MISS':
    case 'HIGH':
      return 'danger';
    case 'PENDING':
    case 'MEDIUM':
      return 'warning';
    default:
      return 'info';
  }
}

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const authHeader = requireAuthHeader();
    const [overviewResponse, reportsResponse, plansResponse, betsResponse, reviewsResponse] = await Promise.all([
      fetchAnalysisReviewOverview(authHeader),
      listAnalysisReports(authHeader),
      listBetPlans(authHeader),
      listBetRecords(authHeader),
      listPostMatchReviews(authHeader),
    ]);
    overview.value = overviewResponse.data;
    reports.value = reportsResponse.data;
    betPlans.value = plansResponse.data;
    bets.value = betsResponse.data;
    reviews.value = reviewsResponse.data;
    if (reports.value.length > 0) {
      await openReport(reports.value[0]);
    }
    if (betPlans.value.length > 0) {
      await openPlan(betPlans.value[0]);
    }
  } catch (cause) {
    overview.value = null;
    reports.value = [];
    betPlans.value = [];
    bets.value = [];
    reviews.value = [];
    selectedReport.value = null;
    selectedPlan.value = null;
    error.value = cause instanceof Error ? cause.message : '无法读取分析下注复盘中心数据。';
  } finally {
    loading.value = false;
  }
}

async function openReport(row: AnalysisReportSummary) {
  detailLoading.value = true;
  try {
    const response = await getAnalysisReport(requireAuthHeader(), row.id);
    selectedReport.value = response.data;
    activeTab.value = 'reports';
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '无法读取分析报告详情。');
  } finally {
    detailLoading.value = false;
  }
}

async function openPlan(row: BetPlanSummary) {
  detailLoading.value = true;
  try {
    const response = await getBetPlan(requireAuthHeader(), row.id);
    selectedPlan.value = response.data;
    activeTab.value = 'plans';
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '无法读取 AI 下注方案详情。');
  } finally {
    detailLoading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <section class="page-shell analysis-review-page">
    <section class="page-content">
      <el-page-header content="分析报告下注记录与赛后复盘中心" @back="$router.push('/')" />

      <el-alert
        class="boundary-alert"
        title="本页只展示已批准 JSON 归档：AI 下注方案来自 AI/Claude/Codex 生成并经用户批准；Java 不生成新推荐、不加注、不倍投。"
        type="info"
        show-icon
        :closable="false"
      />
      <el-alert v-if="error" :title="error" type="warning" show-icon class="top-alert" />

      <el-row :gutter="16" class="stat-row">
        <el-col v-for="item in stats" :key="item.label" :span="6">
          <el-card>
            <strong>{{ item.value }}</strong>
            <span>{{ item.label }}</span>
          </el-card>
        </el-col>
      </el-row>

      <el-card class="panel-card content-card">
        <template #header>
          <div class="card-header">
            <span>AI 方案归档 / 实际出票 / 赛后复盘</span>
            <el-button size="small" :loading="loading" @click="load">刷新</el-button>
          </div>
        </template>

        <el-tabs v-model="activeTab" type="border-card">
          <el-tab-pane label="分析报告" name="reports">
            <el-row :gutter="16">
              <el-col :span="11">
                <el-table :data="reports" v-loading="loading" height="560" @row-click="openReport">
                  <el-table-column prop="matchName" label="比赛" min-width="150" />
                  <el-table-column prop="analysisId" label="分析 ID" min-width="140" show-overflow-tooltip />
                  <el-table-column prop="conclusionType" label="结论" width="110" />
                  <el-table-column prop="confidence" label="置信度" width="90" />
                  <el-table-column label="关联" width="92">
                    <template #default="{ row }">{{ row.betPlanCount }} 方案 / {{ row.reviewCount }} 复盘</template>
                  </el-table-column>
                </el-table>
              </el-col>
              <el-col :span="13">
                <el-card v-loading="detailLoading" shadow="never">
                  <el-empty v-if="!selectedReport" description="请选择分析报告" />
                  <template v-else>
                    <h3>{{ selectedReport.report.matchName || selectedReport.report.analysisId }}</h3>
                    <el-descriptions :column="2" border size="small">
                      <el-descriptions-item label="结论类型">{{ selectedReport.report.conclusionType || '-' }}</el-descriptions-item>
                      <el-descriptions-item label="置信度">{{ selectedReport.report.confidence || '-' }}</el-descriptions-item>
                      <el-descriptions-item label="风险摘要" :span="2">{{ selectedReport.report.riskSummary || '-' }}</el-descriptions-item>
                      <el-descriptions-item label="长文" :span="2">{{ selectedReport.report.narrativeMd || '-' }}</el-descriptions-item>
                    </el-descriptions>
                    <h3>关联 AI 方案</h3>
                    <el-table :data="selectedReport.betPlans" border size="small" empty-text="暂无关联方案">
                      <el-table-column prop="planTitle" label="标题" min-width="150" />
                      <el-table-column prop="bettingMethod" label="下注方式" min-width="120" />
                      <el-table-column prop="budgetAmount" label="预算" width="90" />
                    </el-table>
                    <h3>报告原始 JSON</h3>
                    <pre class="raw-payload">{{ selectedReport.report.rawPayload || '无原始 JSON' }}</pre>
                  </template>
                </el-card>
              </el-col>
            </el-row>
          </el-tab-pane>

          <el-tab-pane label="AI 下注方案归档" name="plans">
            <el-row :gutter="16">
              <el-col :span="11">
                <el-table :data="betPlans" v-loading="loading" height="560" @row-click="openPlan">
                  <el-table-column prop="matchName" label="比赛" min-width="150" />
                  <el-table-column prop="planTitle" label="方案" min-width="160" show-overflow-tooltip />
                  <el-table-column prop="bettingMethod" label="下注方式" width="130" />
                  <el-table-column prop="strategyType" label="策略" width="120" />
                  <el-table-column label="明细" width="70">
                    <template #default="{ row }">{{ row.itemCount }}</template>
                  </el-table-column>
                </el-table>
              </el-col>
              <el-col :span="13">
                <el-card v-loading="detailLoading" shadow="never">
                  <el-empty v-if="!selectedPlan" description="请选择 AI 下注方案" />
                  <template v-else>
                    <h3>{{ selectedPlan.plan.planTitle }}</h3>
                    <el-alert title="AI 方案为已批准 JSON 归档，不代表 Java 系统新生成的下注建议。" type="success" show-icon :closable="false" />
                    <el-descriptions :column="2" border size="small" class="detail-block">
                      <el-descriptions-item label="下注方式">{{ selectedPlan.plan.bettingMethod || '-' }}</el-descriptions-item>
                      <el-descriptions-item label="策略类型">{{ selectedPlan.plan.strategyType || '-' }}</el-descriptions-item>
                      <el-descriptions-item label="预算">{{ moneyText(selectedPlan.plan.budgetAmount) }}</el-descriptions-item>
                      <el-descriptions-item label="状态"><el-tag :type="tagType(selectedPlan.plan.status)">{{ selectedPlan.plan.status }}</el-tag></el-descriptions-item>
                      <el-descriptions-item label="风险说明" :span="2">{{ selectedPlan.plan.riskSummary || '-' }}</el-descriptions-item>
                    </el-descriptions>
                    <h3>方案明细</h3>
                    <el-table :data="selectedPlan.items" border size="small">
                      <el-table-column prop="marketType" label="玩法" width="90" />
                      <el-table-column prop="selectionText" label="选项" min-width="110" />
                      <el-table-column label="金额建议" width="100">
                        <template #default="{ row }">{{ moneyText(row.stakeSuggestion) }}</template>
                      </el-table-column>
                      <el-table-column label="赔率" width="86">
                        <template #default="{ row }">{{ oddsText(row.odds) }}</template>
                      </el-table-column>
                      <el-table-column prop="playType" label="玩法表达" width="110" />
                      <el-table-column prop="passType" label="过关" width="90" />
                      <el-table-column prop="riskLevel" label="风险" width="90" />
                    </el-table>
                    <h3>方案原始 JSON</h3>
                    <pre class="raw-payload">{{ selectedPlan.plan.rawPayload || '无原始 JSON' }}</pre>
                  </template>
                </el-card>
              </el-col>
            </el-row>
          </el-tab-pane>

          <el-tab-pane label="实际出票记录" name="bets">
            <el-alert title="实际出票以票号、出票赔率、返还和结算字段为准；不要与 AI 方案金额建议混用。" type="warning" show-icon :closable="false" />
            <el-table :data="bets" v-loading="loading" height="580" class="tab-table">
              <el-table-column prop="ticketNo" label="票号" min-width="120" />
              <el-table-column prop="matchName" label="比赛" min-width="150" />
              <el-table-column prop="marketType" label="玩法" width="90" />
              <el-table-column prop="selectionText" label="投注项" min-width="120" />
              <el-table-column label="投入" width="90"><template #default="{ row }">{{ moneyText(row.stake) }}</template></el-table-column>
              <el-table-column label="出票赔率" width="100"><template #default="{ row }">{{ oddsText(row.odds) }}</template></el-table-column>
              <el-table-column label="收盘赔率" width="100"><template #default="{ row }">{{ oddsText(row.closingOdds) }}</template></el-table-column>
              <el-table-column label="CLV" width="90"><template #default="{ row }">{{ percentText(row.clv) }}</template></el-table-column>
              <el-table-column label="返还" width="90"><template #default="{ row }">{{ moneyText(row.returnAmount) }}</template></el-table-column>
              <el-table-column label="盈亏" width="90"><template #default="{ row }">{{ moneyText(row.profitLoss) }}</template></el-table-column>
              <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="tagType(row.hitStatus)">{{ row.hitStatus }}</el-tag></template></el-table-column>
              <el-table-column label="复盘" width="100"><template #default="{ row }"><el-tag type="info">{{ row.reviewStatus }}</el-tag></template></el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="赛后复盘" name="reviews">
            <el-table :data="reviews" v-loading="loading" height="580" class="tab-table" row-key="id">
              <el-table-column type="expand">
                <template #default="{ row }">
                  <el-descriptions :column="1" border size="small">
                    <el-descriptions-item label="数学层">{{ row.mathReview || '-' }}</el-descriptions-item>
                    <el-descriptions-item label="足球层">{{ row.footballReview || '-' }}</el-descriptions-item>
                    <el-descriptions-item label="盘口层">{{ row.handicapReview || '-' }}</el-descriptions-item>
                    <el-descriptions-item label="大赛气质层">{{ row.tournamentTemperamentReview || '-' }}</el-descriptions-item>
                    <el-descriptions-item label="赔率价值层">{{ row.oddsValueReview || '-' }}</el-descriptions-item>
                    <el-descriptions-item label="总评">{{ row.overallSummary || '-' }}</el-descriptions-item>
                  </el-descriptions>
                  <h3>规则沉淀</h3>
                  <el-tag v-for="lesson in row.lessons" :key="lesson.id" class="lesson-chip" :type="tagType(lesson.severity)">
                    {{ lesson.lessonType }}：{{ lesson.lessonText }}
                  </el-tag>
                  <pre class="raw-payload">{{ row.rawPayload || '无原始 JSON' }}</pre>
                </template>
              </el-table-column>
              <el-table-column prop="matchName" label="比赛" min-width="150" />
              <el-table-column prop="reviewTitle" label="复盘标题" min-width="180" />
              <el-table-column prop="reviewKey" label="复盘 Key" min-width="150" />
              <el-table-column label="规则" width="80"><template #default="{ row }">{{ row.lessons.length }}</template></el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </section>
  </section>
</template>

<style scoped>
.analysis-review-page {
  background: radial-gradient(circle at top right, rgba(37, 99, 235, 0.14), transparent 34rem), #f5f7fb;
}
.boundary-alert,
.top-alert,
.stat-row,
.content-card {
  margin-top: 16px;
}
.stat-row :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.stat-row strong {
  color: #1d4ed8;
  font-size: 28px;
}
.stat-row span {
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
.detail-block {
  margin-top: 12px;
}
.tab-table {
  margin-top: 12px;
}
h3 {
  margin-top: 18px;
}
.lesson-chip {
  margin: 0 8px 8px 0;
}
.raw-payload {
  background: #111827;
  border-radius: 10px;
  color: #e5e7eb;
  max-height: 220px;
  overflow: auto;
  padding: 14px;
  white-space: pre-wrap;
}
</style>
