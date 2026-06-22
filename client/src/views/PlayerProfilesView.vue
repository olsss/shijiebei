<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
  approveCollectionItem,
  getPlayerProfile,
  listCollectionItems,
  listPlayerProfiles,
  rejectCollectionItem,
  type CollectionItem,
  type PlayerProfileDetail,
  type PlayerProfileSummary,
} from '@/api/profiles';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const loading = ref(false);
const reviewingId = ref<number | null>(null);
const players = ref<PlayerProfileSummary[]>([]);
const selected = ref<PlayerProfileDetail | null>(null);
const pendingItems = ref<CollectionItem[]>([]);
const error = ref('');

const stats = computed(() => ({
  players: players.value.length,
  facts: players.value.reduce((sum, player) => sum + player.factCount, 0),
  injured: players.value.filter((player) => player.injuryStatus && player.injuryStatus !== '无').length,
  pending: pendingItems.value.length,
}));

function requireAuthHeader(): string {
  if (!authStore.basicAuthHeader) {
    throw new Error('请先登录后查看球员画像。');
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
    const [playerResponse, itemResponse] = await Promise.all([
      listPlayerProfiles(authHeader),
      listCollectionItems(authHeader, 'PENDING_REVIEW'),
    ]);
    players.value = playerResponse.data;
    pendingItems.value = itemResponse.data.filter((item) => item.entityType === 'PLAYER');
    if (players.value.length > 0) {
      await openPlayer(players.value[0]);
    } else {
      selected.value = null;
    }
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '无法读取球员画像。';
  } finally {
    loading.value = false;
  }
}

async function openPlayer(player: PlayerProfileSummary) {
  try {
    const response = await getPlayerProfile(requireAuthHeader(), player.id);
    selected.value = response.data;
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '无法读取球员详情。');
  }
}

async function approve(item: CollectionItem) {
  reviewingId.value = item.id;
  try {
    await approveCollectionItem(requireAuthHeader(), item.id);
    ElMessage.success('球员画像采集项已批准。');
    await load();
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '批准球员画像采集项失败。');
  } finally {
    reviewingId.value = null;
  }
}

async function reject(item: CollectionItem) {
  reviewingId.value = item.id;
  try {
    await rejectCollectionItem(requireAuthHeader(), item.id, '页面快速驳回：来源或内容待补充');
    ElMessage.success('球员画像采集项已驳回。');
    await load();
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '驳回球员画像采集项失败。');
  } finally {
    reviewingId.value = null;
  }
}

onMounted(load);
</script>

<template>
  <main class="page-shell profile-page">
    <section class="page-content">
      <el-page-header content="球员画像中心" @back="$router.push('/')" />

      <el-alert v-if="error" :title="error" type="warning" show-icon class="top-alert" />

      <el-row :gutter="16" class="stat-row">
        <el-col :span="6"><el-card><strong>{{ stats.players }}</strong><span>球员</span></el-card></el-col>
        <el-col :span="6"><el-card><strong>{{ stats.facts }}</strong><span>画像事实</span></el-card></el-col>
        <el-col :span="6"><el-card><strong>{{ stats.injured }}</strong><span>伤病关注</span></el-card></el-col>
        <el-col :span="6"><el-card><strong>{{ stats.pending }}</strong><span>待审核采集</span></el-card></el-col>
      </el-row>

      <el-row :gutter="16" class="content-row">
        <el-col :span="9">
          <el-card class="panel-card">
            <template #header>
              <div class="card-header">
                <span>球员列表</span>
                <el-button size="small" :loading="loading" @click="load">刷新</el-button>
              </div>
            </template>
            <el-table :data="players" v-loading="loading" height="560" @row-click="openPlayer">
              <el-table-column prop="displayName" label="球员" min-width="110" />
              <el-table-column prop="teamName" label="球队" min-width="100" />
              <el-table-column prop="position" label="位置" width="72" />
              <el-table-column prop="factCount" label="事实" width="72" />
            </el-table>
          </el-card>
        </el-col>

        <el-col :span="15">
          <el-card class="panel-card">
            <template #header>
              <div class="card-header">
                <span>{{ selected?.player.displayName || '球员详情' }}</span>
                <el-tag type="info">{{ selected?.player.teamName || '未绑定球队' }}</el-tag>
              </div>
            </template>

            <el-empty v-if="!selected" description="请选择球员" />
            <template v-else>
              <el-descriptions :column="2" border>
                <el-descriptions-item label="号码">{{ selected.player.shirtNumber ?? '-' }}</el-descriptions-item>
                <el-descriptions-item label="位置">{{ selected.player.position || '-' }}</el-descriptions-item>
                <el-descriptions-item label="状态">{{ selected.player.status || '-' }}</el-descriptions-item>
                <el-descriptions-item label="伤病">{{ selected.player.injuryStatus || '-' }}</el-descriptions-item>
                <el-descriptions-item label="红黄牌">{{ selected.player.cardStatus || '-' }}</el-descriptions-item>
                <el-descriptions-item label="更衣室">{{ selected.player.lockerRoomStatus || '-' }}</el-descriptions-item>
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
            </template>
          </el-card>
        </el-col>
      </el-row>

      <el-card class="panel-card pending-card">
        <template #header>球员画像待审核采集项</template>
        <el-table :data="pendingItems" border empty-text="暂无待审核球员画像采集项">
          <el-table-column prop="entityKey" label="球员Key" width="140" />
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
.profile-page { background: radial-gradient(circle at top right, rgba(16, 185, 129, 0.12), transparent 30rem), #f5f7fb; }
.top-alert, .stat-row, .content-row, .pending-card { margin-top: 16px; }
.stat-row :deep(.el-card__body) { display: flex; flex-direction: column; gap: 6px; }
.stat-row strong { color: #047857; font-size: 30px; }
.stat-row span, small { color: #6b7280; }
.panel-card { border-radius: 14px; }
.card-header { align-items: center; display: flex; justify-content: space-between; }
h3 { margin-top: 22px; }
p { color: #374151; line-height: 1.7; }
</style>
