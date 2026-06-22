<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
  approveCollectionItem,
  getTeamProfile,
  listCollectionItems,
  listTeamProfiles,
  rejectCollectionItem,
  type CollectionItem,
  type TeamProfileDetail,
  type TeamProfileSummary,
} from '@/api/profiles';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const loading = ref(false);
const reviewingId = ref<number | null>(null);
const teams = ref<TeamProfileSummary[]>([]);
const selected = ref<TeamProfileDetail | null>(null);
const pendingItems = ref<CollectionItem[]>([]);
const error = ref('');

const stats = computed(() => ({
  teams: teams.value.length,
  facts: teams.value.reduce((sum, team) => sum + team.factCount, 0),
  players: teams.value.reduce((sum, team) => sum + team.playerCount, 0),
  pending: pendingItems.value.length,
}));

function requireAuthHeader(): string {
  if (!authStore.basicAuthHeader) {
    throw new Error('请先登录后查看球队画像。');
  }
  return authStore.basicAuthHeader;
}

function reliabilityLabel(value?: number): string {
  return value == null ? '未评分' : `${value.toFixed(1)} / 10`;
}

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const authHeader = requireAuthHeader();
    const [teamResponse, itemResponse] = await Promise.all([
      listTeamProfiles(authHeader),
      listCollectionItems(authHeader, 'PENDING_REVIEW'),
    ]);
    teams.value = teamResponse.data;
    pendingItems.value = itemResponse.data.filter((item) => item.entityType === 'TEAM');
    if (teams.value.length > 0) {
      await openTeam(teams.value[0]);
    } else {
      selected.value = null;
    }
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '无法读取球队画像。';
  } finally {
    loading.value = false;
  }
}

async function openTeam(team: TeamProfileSummary) {
  try {
    const response = await getTeamProfile(requireAuthHeader(), team.id);
    selected.value = response.data;
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '无法读取球队详情。');
  }
}

async function approve(item: CollectionItem) {
  reviewingId.value = item.id;
  try {
    await approveCollectionItem(requireAuthHeader(), item.id);
    ElMessage.success('球队画像采集项已批准。');
    await load();
  } finally {
    reviewingId.value = null;
  }
}

async function reject(item: CollectionItem) {
  reviewingId.value = item.id;
  try {
    await rejectCollectionItem(requireAuthHeader(), item.id, '页面快速驳回：来源或内容待补充');
    ElMessage.success('球队画像采集项已驳回。');
    await load();
  } finally {
    reviewingId.value = null;
  }
}

onMounted(load);
</script>

<template>
  <main class="page-shell profile-page">
    <section class="page-content">
      <el-page-header content="球队画像中心" @back="$router.push('/')" />

      <el-alert v-if="error" :title="error" type="warning" show-icon class="top-alert" />

      <el-row :gutter="16" class="stat-row">
        <el-col :span="6"><el-card><strong>{{ stats.teams }}</strong><span>球队</span></el-card></el-col>
        <el-col :span="6"><el-card><strong>{{ stats.players }}</strong><span>球员</span></el-card></el-col>
        <el-col :span="6"><el-card><strong>{{ stats.facts }}</strong><span>画像事实</span></el-card></el-col>
        <el-col :span="6"><el-card><strong>{{ stats.pending }}</strong><span>待审核采集</span></el-card></el-col>
      </el-row>

      <el-row :gutter="16" class="content-row">
        <el-col :span="8">
          <el-card class="panel-card">
            <template #header>
              <div class="card-header">
                <span>球队列表</span>
                <el-button size="small" :loading="loading" @click="load">刷新</el-button>
              </div>
            </template>
            <el-table :data="teams" v-loading="loading" height="520" @row-click="openTeam">
              <el-table-column prop="displayName" label="球队" min-width="120" />
              <el-table-column prop="playerCount" label="球员" width="72" />
              <el-table-column prop="factCount" label="事实" width="72" />
            </el-table>
          </el-card>
        </el-col>

        <el-col :span="16">
          <el-card class="panel-card">
            <template #header>
              <div class="card-header">
                <span>{{ selected?.team.displayName || '球队详情' }}</span>
                <el-tag type="info">证据 {{ selected?.evidenceCount ?? 0 }} / 冲突 {{ selected?.conflictCount ?? 0 }}</el-tag>
              </div>
            </template>

            <el-empty v-if="!selected" description="请选择球队" />
            <template v-else>
              <el-descriptions :column="2" border>
                <el-descriptions-item label="FIFA 代码">{{ selected.team.fifaCode || '-' }}</el-descriptions-item>
                <el-descriptions-item label="地区">{{ selected.team.countryRegion || '-' }}</el-descriptions-item>
                <el-descriptions-item label="风格标签">{{ selected.team.styleTags || '-' }}</el-descriptions-item>
                <el-descriptions-item label="舆情摘要">{{ selected.team.publicSentiment || '-' }}</el-descriptions-item>
                <el-descriptions-item label="进攻画像">{{ selected.team.attackProfile || '-' }}</el-descriptions-item>
                <el-descriptions-item label="防守画像">{{ selected.team.defenseProfile || '-' }}</el-descriptions-item>
              </el-descriptions>

              <h3>画像事实</h3>
              <el-timeline>
                <el-timeline-item v-for="fact in selected.facts" :key="fact.id" :timestamp="fact.capturedAt || '未标注时间'">
                  <h4>{{ fact.title }} <el-tag size="small">{{ fact.factType }}</el-tag></h4>
                  <p>{{ fact.summary }}</p>
                  <small>来源：{{ fact.sourceName }} ｜ 可信度：{{ reliabilityLabel(fact.reliabilityScore) }}</small>
                </el-timeline-item>
              </el-timeline>
              <el-empty v-if="selected.facts.length === 0" description="暂无正式画像事实" />

              <h3>球员名单</h3>
              <el-table :data="selected.players" border>
                <el-table-column prop="shirtNumber" label="#" width="58" />
                <el-table-column prop="displayName" label="球员" min-width="120" />
                <el-table-column prop="position" label="位置" width="80" />
                <el-table-column prop="status" label="状态" width="90" />
                <el-table-column prop="injuryStatus" label="伤病" min-width="120" />
                <el-table-column prop="cardStatus" label="红黄牌" min-width="120" />
              </el-table>
            </template>
          </el-card>
        </el-col>
      </el-row>

      <el-card class="panel-card pending-card">
        <template #header>球队画像待审核采集项</template>
        <el-table :data="pendingItems" border empty-text="暂无待审核球队画像采集项">
          <el-table-column prop="entityKey" label="球队Key" width="140" />
          <el-table-column prop="factType" label="事实类型" width="140" />
          <el-table-column prop="title" label="标题" min-width="160" />
          <el-table-column prop="summary" label="摘要" min-width="260" show-overflow-tooltip />
          <el-table-column prop="sourceName" label="来源" width="140" />
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button size="small" type="success" :loading="reviewingId === row.id" @click="approve(row)">批准</el-button>
              <el-button size="small" type="danger" :loading="reviewingId === row.id" @click="reject(row)">驳回</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </section>
  </main>
</template>

<style scoped>
.profile-page { background: radial-gradient(circle at top right, rgba(37, 99, 235, 0.12), transparent 30rem), #f5f7fb; }
.top-alert, .stat-row, .content-row, .pending-card { margin-top: 16px; }
.stat-row :deep(.el-card__body) { display: flex; flex-direction: column; gap: 6px; }
.stat-row strong { color: #1d4ed8; font-size: 30px; }
.stat-row span, small { color: #6b7280; }
.panel-card { border-radius: 14px; }
.card-header { align-items: center; display: flex; justify-content: space-between; }
h3 { margin-top: 22px; }
p { color: #374151; line-height: 1.7; }
</style>
