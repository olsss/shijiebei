<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { getMatchDetail, listMatches, type MatchDetail, type MatchSummary } from '@/api/matches';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const loading = ref(false);
const detailLoading = ref(false);
const matches = ref<MatchSummary[]>([]);
const selected = ref<MatchDetail | null>(null);
const error = ref('');

const stats = computed(() => ({
  matches: matches.value.length,
  events: matches.value.reduce((sum, match) => sum + match.eventCount, 0),
  lineups: matches.value.reduce((sum, match) => sum + match.lineupCount, 0),
  conflicts: matches.value.reduce((sum, match) => sum + match.conflictCount, 0),
}));

function requireAuthHeader(): string {
  if (!authStore.basicAuthHeader) {
    throw new Error('请先登录后查看比赛中心。');
  }
  return authStore.basicAuthHeader;
}

function matchTitle(match: MatchSummary): string {
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

function reliabilityLabel(value?: number): string {
  return value == null ? '未评分' : `${Number(value).toFixed(1)} / 10`;
}

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const response = await listMatches(requireAuthHeader());
    matches.value = response.data;
    if (matches.value.length > 0) {
      await openMatch(matches.value[0]);
    } else {
      selected.value = null;
    }
  } catch (cause) {
    matches.value = [];
    selected.value = null;
    error.value = cause instanceof Error ? cause.message : '无法读取比赛中心数据。';
  } finally {
    loading.value = false;
  }
}

async function openMatch(match: MatchSummary) {
  detailLoading.value = true;
  try {
    const response = await getMatchDetail(requireAuthHeader(), match.id);
    selected.value = response.data;
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '无法读取比赛详情。');
  } finally {
    detailLoading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <main class="page-shell match-page">
    <section class="page-content">
      <el-page-header content="比赛中心" @back="$router.push('/')" />

      <el-alert v-if="error" :title="error" type="warning" show-icon class="top-alert" />

      <el-row :gutter="16" class="stat-row">
        <el-col :span="6"><el-card><strong>{{ stats.matches }}</strong><span>比赛</span></el-card></el-col>
        <el-col :span="6"><el-card><strong>{{ stats.events }}</strong><span>事件</span></el-card></el-col>
        <el-col :span="6"><el-card><strong>{{ stats.lineups }}</strong><span>阵容条目</span></el-card></el-col>
        <el-col :span="6"><el-card><strong>{{ stats.conflicts }}</strong><span>待核冲突</span></el-card></el-col>
      </el-row>

      <el-row :gutter="16" class="content-row">
        <el-col :span="8">
          <el-card class="panel-card">
            <template #header>
              <div class="card-header">
                <span>赛程列表</span>
                <el-button size="small" :loading="loading" @click="load">刷新</el-button>
              </div>
            </template>
            <el-table :data="matches" v-loading="loading" height="590" @row-click="openMatch">
              <el-table-column label="比赛" min-width="180">
                <template #default="{ row }">
                  <strong>{{ matchTitle(row) }}</strong>
                  <small>{{ row.matchday || '-' }} ｜ {{ row.jcCode || '无编号' }}</small>
                </template>
              </el-table-column>
              <el-table-column prop="stage" label="阶段" width="90" />
              <el-table-column label="状态" width="92">
                <template #default="{ row }">
                  <el-tag size="small">{{ row.status }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>

        <el-col :span="16">
          <el-card class="panel-card" v-loading="detailLoading">
            <template #header>
              <div class="card-header">
                <span>{{ selected?.summary.matchName || '比赛详情' }}</span>
                <el-space>
                  <el-tag type="success">证据 {{ selected?.summary.evidenceCount ?? 0 }}</el-tag>
                  <el-tag type="warning">冲突 {{ selected?.summary.conflictCount ?? 0 }}</el-tag>
                </el-space>
              </div>
            </template>

            <el-empty v-if="!selected" description="请选择比赛" />
            <template v-else>
              <el-descriptions :column="2" border>
                <el-descriptions-item label="比赛">{{ matchTitle(selected.summary) }}</el-descriptions-item>
                <el-descriptions-item label="竞彩编号">{{ selected.summary.jcCode || '-' }}</el-descriptions-item>
                <el-descriptions-item label="比赛日">{{ selected.summary.matchday || '-' }}</el-descriptions-item>
                <el-descriptions-item label="开球时间">{{ formatDateTime(selected.summary.kickoffTime) }}</el-descriptions-item>
                <el-descriptions-item label="赛事">{{ selected.summary.competition || '-' }}</el-descriptions-item>
                <el-descriptions-item label="阶段">{{ selected.summary.stage || '-' }}</el-descriptions-item>
                <el-descriptions-item label="场地">{{ selected.summary.venue || '-' }}</el-descriptions-item>
                <el-descriptions-item label="状态">{{ selected.summary.status }} / {{ selected.summary.resultStatus }}</el-descriptions-item>
              </el-descriptions>

              <el-alert
                v-if="selected.externalFactors"
                class="external-alert"
                title="外部因素"
                :description="selected.externalFactors"
                type="info"
                show-icon
                :closable="false"
              />

              <h3>阵容 / 首发</h3>
              <el-table :data="selected.lineups" border empty-text="暂无阵容数据">
                <el-table-column prop="teamName" label="球队" min-width="110" />
                <el-table-column prop="playerName" label="球员" min-width="120" />
                <el-table-column prop="position" label="位置" width="80" />
                <el-table-column prop="role" label="角色" width="100" />
                <el-table-column label="首发" width="86">
                  <template #default="{ row }">
                    <el-tag :type="row.starter ? 'success' : 'info'" size="small">{{ row.starter ? '首发' : '替补' }}</el-tag>
                  </template>
                </el-table-column>
              </el-table>

              <h3>比赛事件</h3>
              <el-table :data="selected.events" border empty-text="暂无事件数据">
                <el-table-column prop="eventMinute" label="分钟" width="80" />
                <el-table-column prop="eventType" label="事件" width="110" />
                <el-table-column prop="teamName" label="球队" min-width="110" />
                <el-table-column prop="playerName" label="球员" min-width="120" />
                <el-table-column prop="payload" label="原始字段" min-width="180" show-overflow-tooltip />
              </el-table>

              <h3>球队统计 / 进球时间点</h3>
              <el-table :data="selected.teamStats" border empty-text="暂无球队统计">
                <el-table-column prop="teamName" label="球队" min-width="110" />
                <el-table-column prop="statsType" label="类型" width="110" />
                <el-table-column prop="goalsFor" label="进球" width="70" />
                <el-table-column prop="goalsAgainst" label="失球" width="70" />
                <el-table-column prop="firstGoalMinute" label="首球" width="80" />
                <el-table-column prop="scoringMinutes" label="进球分钟" min-width="120" />
                <el-table-column prop="payload" label="统计扩展" min-width="180" show-overflow-tooltip />
              </el-table>

              <h3>球员统计</h3>
              <el-table :data="selected.playerStats" border empty-text="暂无球员统计">
                <el-table-column prop="playerName" label="球员" min-width="120" />
                <el-table-column prop="teamName" label="球队" min-width="110" />
                <el-table-column prop="minutesPlayed" label="分钟" width="70" />
                <el-table-column prop="goals" label="进球" width="70" />
                <el-table-column prop="assists" label="助攻" width="70" />
                <el-table-column prop="yellowCards" label="黄牌" width="70" />
                <el-table-column prop="redCards" label="红牌" width="70" />
              </el-table>

              <h3>证据链</h3>
              <el-table :data="selected.evidence" border empty-text="暂无证据链">
                <el-table-column prop="sourceName" label="来源" width="120" />
                <el-table-column prop="sourceType" label="类型" width="100" />
                <el-table-column prop="summary" label="摘要" min-width="220" show-overflow-tooltip />
                <el-table-column label="可信度" width="110">
                  <template #default="{ row }">{{ reliabilityLabel(row.reliabilityScore) }}</template>
                </el-table-column>
                <el-table-column prop="sourceUrl" label="链接" min-width="180" show-overflow-tooltip />
              </el-table>

              <h3>数据冲突</h3>
              <el-table :data="selected.conflicts" border empty-text="暂无数据冲突">
                <el-table-column prop="conflictType" label="类型" width="110" />
                <el-table-column prop="fieldName" label="字段" width="110" />
                <el-table-column prop="currentValue" label="当前值" min-width="160" show-overflow-tooltip />
                <el-table-column prop="incomingValue" label="新值" min-width="160" show-overflow-tooltip />
                <el-table-column prop="resolutionStatus" label="状态" width="110" />
              </el-table>
            </template>
          </el-card>
        </el-col>
      </el-row>
    </section>
  </main>
</template>

<style scoped>
.match-page { background: radial-gradient(circle at top right, rgba(59, 130, 246, 0.14), transparent 30rem), #f5f7fb; }
.top-alert, .stat-row, .content-row { margin-top: 16px; }
.stat-row :deep(.el-card__body) { display: flex; flex-direction: column; gap: 6px; }
.stat-row strong { color: #1d4ed8; font-size: 30px; }
.stat-row span, small { color: #6b7280; }
.panel-card { border-radius: 14px; }
.card-header { align-items: center; display: flex; justify-content: space-between; }
.external-alert { margin-top: 16px; }
h3 { margin-top: 22px; }
small { display: block; margin-top: 4px; }
</style>
