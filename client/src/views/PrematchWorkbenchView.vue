<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
  getPrematchWorkbenchMatch,
  listPrematchWorkbenchMatches,
  type PrematchWorkbenchDetail,
  type WorkbenchMatchSummary,
} from '@/api/prematchWorkbench';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const loading = ref(false);
const detailLoading = ref(false);
const error = ref('');
const matches = ref<WorkbenchMatchSummary[]>([]);
const selected = ref<PrematchWorkbenchDetail | null>(null);
const activeTab = ref('integrity');

const stats = computed(() => ({
  matches: matches.value.length,
  avgScore: matches.value.length
    ? Math.round(matches.value.reduce((sum, match) => sum + match.integrityScore, 0) / matches.value.length)
    : 0,
  missing: matches.value.reduce((sum, match) => sum + match.missingCount, 0),
  conflicts: matches.value.reduce((sum, match) => sum + match.conflictCount, 0),
}));

function requireAuthHeader(): string {
  if (!authStore.basicAuthHeader) {
    throw new Error('请先登录后查看赛前分析作战室。');
  }
  return authStore.basicAuthHeader;
}

function matchTitle(match?: WorkbenchMatchSummary): string {
  if (!match) {
    return '赛前作战室';
  }
  if (match.homeTeamName || match.awayTeamName) {
    return `${match.homeTeamName || '主队待定'} vs ${match.awayTeamName || '客队待定'}`;
  }
  return match.matchName;
}

function formatDateTime(value?: string): string {
  if (!value) {
    return '-';
  }
  return value.replace('T', ' ').slice(0, 16);
}

function numberText(value?: number): string {
  if (value == null) {
    return '-';
  }
  return Number(value).toFixed(4).replace(/0+$/, '').replace(/\.$/, '');
}

function moneyText(value?: number): string {
  if (value == null) {
    return '-';
  }
  return Number(value).toFixed(2).replace(/\.00$/, '');
}

function percentText(value?: number): string {
  if (value == null) {
    return '-';
  }
  return `${(Number(value) * 100).toFixed(2).replace(/\.00$/, '')}%`;
}

function statusType(status?: string): 'success' | 'warning' | 'danger' | 'info' | 'primary' {
  switch (status) {
    case 'PASS':
    case 'RESOLVED':
    case 'FIT':
      return 'success';
    case 'STALE':
    case 'PENDING':
    case 'MEDIUM':
      return 'warning';
    case 'MISSING':
    case 'CONFLICT':
    case 'HIGH':
      return 'danger';
    default:
      return 'info';
  }
}

function scoreType(score: number): 'success' | 'warning' | 'danger' {
  if (score >= 90) {
    return 'success';
  }
  if (score >= 70) {
    return 'warning';
  }
  return 'danger';
}

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const response = await listPrematchWorkbenchMatches(requireAuthHeader());
    matches.value = response.data;
    if (matches.value.length > 0) {
      await openMatch(matches.value[0]);
    } else {
      selected.value = null;
    }
  } catch (cause) {
    matches.value = [];
    selected.value = null;
    error.value = cause instanceof Error ? cause.message : '无法读取赛前作战室数据。';
  } finally {
    loading.value = false;
  }
}

async function openMatch(match: WorkbenchMatchSummary) {
  detailLoading.value = true;
  try {
    const response = await getPrematchWorkbenchMatch(requireAuthHeader(), match.matchId);
    selected.value = response.data;
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '无法读取作战室详情。');
  } finally {
    detailLoading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <main class="page-shell prematch-page">
    <section class="page-content">
      <el-page-header content="赛前分析作战室与数据完整性中心" @back="$router.push('/')" />

      <el-alert
        class="boundary-alert"
        title="本页只聚合正式业务库数据，完整性分数表示数据准备度，不代表胜率、投注价值或下注建议。"
        type="info"
        show-icon
        :closable="false"
      />
      <el-alert v-if="error" :title="error" type="warning" show-icon class="top-alert" />

      <el-row :gutter="16" class="stat-row">
        <el-col :span="6"><el-card><strong>{{ stats.matches }}</strong><span>待分析比赛</span></el-card></el-col>
        <el-col :span="6"><el-card><strong>{{ stats.avgScore }}%</strong><span>平均准备度</span></el-card></el-col>
        <el-col :span="6"><el-card><strong>{{ stats.missing }}</strong><span>缺失项</span></el-card></el-col>
        <el-col :span="6"><el-card><strong>{{ stats.conflicts }}</strong><span>冲突项</span></el-card></el-col>
      </el-row>

      <el-row :gutter="16" class="content-row">
        <el-col :span="8">
          <el-card class="panel-card">
            <template #header>
              <div class="card-header">
                <span>比赛准备清单</span>
                <el-button size="small" :loading="loading" @click="load">刷新</el-button>
              </div>
            </template>
            <el-table :data="matches" v-loading="loading" height="640" @row-click="openMatch">
              <el-table-column label="比赛" min-width="180">
                <template #default="{ row }">
                  <strong>{{ matchTitle(row) }}</strong>
                  <small>{{ row.matchday || '-' }} ｜ {{ row.jcCode || '无编号' }}</small>
                </template>
              </el-table-column>
              <el-table-column label="完整性" width="104">
                <template #default="{ row }">
                  <el-tag :type="scoreType(row.integrityScore)">{{ row.integrityScore }}%</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="异常" width="104">
                <template #default="{ row }">
                  <el-space wrap>
                    <el-tag v-if="row.missingCount" type="danger" size="small">缺 {{ row.missingCount }}</el-tag>
                    <el-tag v-if="row.staleCount" type="warning" size="small">旧 {{ row.staleCount }}</el-tag>
                    <el-tag v-if="row.conflictCount" type="danger" size="small">冲 {{ row.conflictCount }}</el-tag>
                    <el-tag v-if="!row.missingCount && !row.staleCount && !row.conflictCount" type="success" size="small">正常</el-tag>
                  </el-space>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>

        <el-col :span="16">
          <el-card class="panel-card detail-card" v-loading="detailLoading">
            <template #header>
              <div class="card-header">
                <span>{{ matchTitle(selected?.summary) }}</span>
                <el-space v-if="selected">
                  <el-tag :type="scoreType(selected.summary.integrityScore)">完整性 {{ selected.summary.integrityScore }}%</el-tag>
                  <el-tag type="warning">赔率 {{ selected.summary.oddsMarketCount }}</el-tag>
                  <el-tag type="success">AI方案 {{ selected.summary.betPlanCount }}</el-tag>
                </el-space>
              </div>
            </template>

            <el-empty v-if="!selected" description="请选择比赛" />
            <template v-else>
              <el-descriptions :column="3" border>
                <el-descriptions-item label="比赛">{{ matchTitle(selected.summary) }}</el-descriptions-item>
                <el-descriptions-item label="竞彩编号">{{ selected.summary.jcCode || '-' }}</el-descriptions-item>
                <el-descriptions-item label="开球">{{ formatDateTime(selected.summary.kickoffTime) }}</el-descriptions-item>
                <el-descriptions-item label="赛事">{{ selected.summary.competition || '-' }}</el-descriptions-item>
                <el-descriptions-item label="阶段">{{ selected.summary.stage || '-' }}</el-descriptions-item>
                <el-descriptions-item label="状态">{{ selected.summary.status || '-' }}</el-descriptions-item>
              </el-descriptions>

              <el-tabs v-model="activeTab" type="border-card" class="workbench-tabs">
                <el-tab-pane label="完整性检查" name="integrity">
                  <el-table :data="selected.integrityChecks" border>
                    <el-table-column prop="label" label="检查项" width="140" />
                    <el-table-column label="状态" width="110">
                      <template #default="{ row }">
                        <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column prop="message" label="说明" min-width="260" />
                    <el-table-column prop="evidenceCount" label="证据数" width="90" />
                    <el-table-column label="最后更新" width="150">
                      <template #default="{ row }">{{ formatDateTime(row.lastUpdatedAt) }}</template>
                    </el-table-column>
                  </el-table>
                </el-tab-pane>

                <el-tab-pane label="球队 / 球员 / 阵容" name="profile">
                  <h3>球队画像</h3>
                  <el-row :gutter="12">
                    <el-col v-for="team in selected.teams" :key="team.teamId" :span="12">
                      <el-card shadow="never" class="inner-card">
                        <h4>{{ team.teamName }} <small>{{ team.fifaCode || '' }}</small></h4>
                        <p>{{ team.styleTags || '暂无风格标签' }}</p>
                        <el-tag v-for="fact in team.facts" :key="fact.factId" class="fact-tag" type="info">
                          {{ fact.title }}
                        </el-tag>
                      </el-card>
                    </el-col>
                  </el-row>

                  <h3>首发阵容</h3>
                  <el-table :data="selected.lineups" border empty-text="暂无阵容">
                    <el-table-column prop="teamName" label="球队" width="110" />
                    <el-table-column prop="playerName" label="球员" min-width="120" />
                    <el-table-column prop="position" label="位置" width="80" />
                    <el-table-column prop="role" label="角色" width="100" />
                    <el-table-column label="首发" width="86">
                      <template #default="{ row }">
                        <el-tag :type="row.starter ? 'success' : 'info'">{{ row.starter ? '首发' : '替补' }}</el-tag>
                      </template>
                    </el-table-column>
                  </el-table>

                  <h3>球员状态</h3>
                  <el-table :data="selected.players" border empty-text="暂无球员状态">
                    <el-table-column prop="playerName" label="球员" min-width="120" />
                    <el-table-column prop="teamName" label="球队" width="110" />
                    <el-table-column prop="position" label="位置" width="80" />
                    <el-table-column prop="injuryStatus" label="伤病" width="120" />
                    <el-table-column prop="cardStatus" label="红黄牌" width="120" />
                    <el-table-column prop="lockerRoomStatus" label="更衣室" min-width="140" />
                    <el-table-column label="画像" min-width="180">
                      <template #default="{ row }">
                        <el-tag v-for="fact in row.facts" :key="fact.factId" class="fact-tag" type="info">{{ fact.title }}</el-tag>
                      </template>
                    </el-table-column>
                  </el-table>
                </el-tab-pane>

                <el-tab-pane label="赔率与盘口" name="odds">
                  <el-alert title="各玩法、各选项盘口快照均从 odds_market_snapshots / odds_selection_snapshots 读取。" type="success" show-icon :closable="false" />
                  <el-table :data="selected.oddsMarkets" border class="tab-table" row-key="marketId">
                    <el-table-column type="expand">
                      <template #default="{ row }">
                        <el-table :data="row.selections" border size="small">
                          <el-table-column prop="selectionCode" label="代码" width="90" />
                          <el-table-column prop="selectionName" label="选项" min-width="120" />
                          <el-table-column label="赔率" width="100"><template #default="{ row: selection }">{{ numberText(selection.oddsValue) }}</template></el-table-column>
                          <el-table-column prop="selectionStatus" label="状态" width="100" />
                        </el-table>
                      </template>
                    </el-table-column>
                    <el-table-column prop="bookmaker" label="公司" width="130" />
                    <el-table-column prop="marketCode" label="玩法" width="90" />
                    <el-table-column prop="marketName" label="名称" min-width="120" />
                    <el-table-column prop="snapshotType" label="快照" width="90" />
                    <el-table-column prop="lineValue" label="盘口线" width="100" />
                    <el-table-column label="采集时间" width="150"><template #default="{ row }">{{ formatDateTime(row.capturedAt) }}</template></el-table-column>
                  </el-table>
                </el-tab-pane>

                <el-tab-pane label="舆情 / 证据 / 冲突" name="sentiment">
                  <h3>舆情与外部因素</h3>
                  <el-table :data="selected.sentimentFactors" border empty-text="暂无舆情因素">
                    <el-table-column prop="factorCategory" label="分类" width="110" />
                    <el-table-column prop="factorType" label="类型" width="110" />
                    <el-table-column prop="title" label="标题" min-width="140" />
                    <el-table-column prop="summary" label="摘要" min-width="220" show-overflow-tooltip />
                    <el-table-column label="风险" min-width="180">
                      <template #default="{ row }">
                        <el-tag v-for="risk in row.risks" :key="risk.riskId" :type="statusType(risk.riskLevel)" class="fact-tag">
                          {{ risk.riskType }} {{ risk.riskLevel }}
                        </el-tag>
                      </template>
                    </el-table-column>
                  </el-table>

                  <h3>多源证据</h3>
                  <el-table :data="selected.evidence" border empty-text="暂无证据">
                    <el-table-column prop="sourceName" label="来源" width="120" />
                    <el-table-column prop="sourceType" label="类型" width="100" />
                    <el-table-column prop="summary" label="摘要" min-width="220" show-overflow-tooltip />
                    <el-table-column label="可信度" width="100"><template #default="{ row }">{{ numberText(row.reliabilityScore) }}</template></el-table-column>
                    <el-table-column prop="sourceUrl" label="链接" min-width="180" show-overflow-tooltip />
                  </el-table>

                  <h3>数据冲突</h3>
                  <el-table :data="selected.conflicts" border empty-text="暂无冲突">
                    <el-table-column prop="conflictType" label="类型" width="110" />
                    <el-table-column prop="fieldName" label="字段" width="120" />
                    <el-table-column prop="currentValue" label="当前值" min-width="160" show-overflow-tooltip />
                    <el-table-column prop="incomingValue" label="新值" min-width="160" show-overflow-tooltip />
                    <el-table-column label="状态" width="110">
                      <template #default="{ row }"><el-tag :type="statusType(row.resolutionStatus)">{{ row.resolutionStatus }}</el-tag></template>
                    </el-table-column>
                  </el-table>
                </el-tab-pane>

                <el-tab-pane label="分析 / 方案 / 出票" name="analysis">
                  <el-alert title="这里展示已批准 JSON 归档：分析报告、AI 下注方案和实际出票记录。" type="info" show-icon :closable="false" />
                  <h3>分析报告</h3>
                  <el-table :data="selected.analysisReports" border empty-text="暂无分析报告">
                    <el-table-column prop="analysisId" label="分析 ID" min-width="150" />
                    <el-table-column prop="conclusionType" label="结论" width="110" />
                    <el-table-column prop="confidence" label="置信度" width="100" />
                    <el-table-column prop="riskSummary" label="风险摘要" min-width="180" show-overflow-tooltip />
                  </el-table>

                  <h3>AI 下注方案</h3>
                  <el-table :data="selected.betPlans" border empty-text="暂无 AI 下注方案" row-key="planId">
                    <el-table-column type="expand">
                      <template #default="{ row }">
                        <el-table :data="row.items" border size="small">
                          <el-table-column prop="marketType" label="玩法" width="90" />
                          <el-table-column prop="selectionText" label="选项" min-width="120" />
                          <el-table-column label="建议金额" width="100"><template #default="{ row: item }">{{ moneyText(item.stakeSuggestion) }}</template></el-table-column>
                          <el-table-column label="赔率" width="90"><template #default="{ row: item }">{{ numberText(item.odds) }}</template></el-table-column>
                          <el-table-column prop="playType" label="表达" width="110" />
                          <el-table-column prop="passType" label="过关" width="90" />
                        </el-table>
                      </template>
                    </el-table-column>
                    <el-table-column prop="planKey" label="Key" min-width="150" />
                    <el-table-column prop="planTitle" label="标题" min-width="160" />
                    <el-table-column prop="bettingMethod" label="下注方式" min-width="130" />
                    <el-table-column prop="strategyType" label="策略" min-width="120" />
                    <el-table-column label="预算" width="100"><template #default="{ row }">{{ moneyText(row.budgetAmount) }}</template></el-table-column>
                  </el-table>

                  <h3>实际出票</h3>
                  <el-table :data="selected.bets" border empty-text="暂无出票">
                    <el-table-column prop="ticketNo" label="票号" min-width="110" />
                    <el-table-column prop="marketType" label="玩法" width="90" />
                    <el-table-column prop="selectionText" label="投注项" min-width="120" />
                    <el-table-column label="投入" width="90"><template #default="{ row }">{{ moneyText(row.stake) }}</template></el-table-column>
                    <el-table-column label="出票赔率" width="100"><template #default="{ row }">{{ numberText(row.odds) }}</template></el-table-column>
                    <el-table-column label="CLV" width="90"><template #default="{ row }">{{ percentText(row.clv) }}</template></el-table-column>
                    <el-table-column prop="hitStatus" label="状态" width="110" />
                  </el-table>
                </el-tab-pane>
              </el-tabs>
            </template>
          </el-card>
        </el-col>
      </el-row>
    </section>
  </main>
</template>

<style scoped>
.prematch-page {
  background: radial-gradient(circle at top right, rgba(14, 165, 233, 0.16), transparent 32rem), #f5f7fb;
}
.boundary-alert,
.top-alert,
.stat-row,
.content-row {
  margin-top: 16px;
}
.stat-row :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.stat-row strong {
  color: #0f766e;
  font-size: 30px;
}
.stat-row span,
small {
  color: #6b7280;
}
.panel-card {
  border-radius: 14px;
}
.detail-card {
  min-height: 720px;
}
.card-header {
  align-items: center;
  display: flex;
  justify-content: space-between;
}
.workbench-tabs {
  margin-top: 16px;
}
.inner-card,
.tab-table {
  margin-top: 12px;
}
.fact-tag {
  margin: 0 6px 6px 0;
}
h3 {
  margin-top: 22px;
}
h4 {
  margin: 0 0 8px;
}
small {
  display: block;
  margin-top: 4px;
}
</style>
