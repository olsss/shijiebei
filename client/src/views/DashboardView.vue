<template>
  <main class="page-shell">
    <section class="page-content">
      <el-row :gutter="16">
        <el-col :span="24">
          <el-card>
            <h1>数据分析工作台</h1>
            <p>当前阶段已建立 Java 系统骨架。比赛分析仍由 `skill/` 和 JSON 档案负责。</p>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="module-grid">
        <el-col v-for="item in modules" :key="item.title" :span="6">
          <el-card shadow="hover" class="module-card">
            <RouterLink :to="item.to" class="module-link">
              <h3>{{ item.title }}</h3>
              <p>{{ item.description }}</p>
              <span>进入模块 →</span>
            </RouterLink>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="overview-row">
        <el-col :span="24">
          <el-card class="overview-card">
            <template #header>
              <div class="overview-header">
                <span>正式业务数据概览</span>
                <el-button size="small" :loading="coreDataLoading" @click="loadCoreDataOverview">刷新</el-button>
              </div>
            </template>

            <el-empty v-if="!authStore.basicAuthHeader" description="登录后显示正式业务库概览" />
            <el-skeleton v-else-if="coreDataLoading" :rows="3" animated />
            <el-alert v-else-if="coreDataError" :title="coreDataError" type="warning" show-icon :closable="false" />
            <div v-else-if="coreDataOverview" class="core-stats-grid">
              <div v-for="stat in coreDataStats" :key="stat.label" class="core-stat">
                <strong>{{ stat.value }}</strong>
                <span>{{ stat.label }}</span>
              </div>
            </div>
            <el-empty v-else description="暂无正式业务数据" />
          </el-card>
        </el-col>
      </el-row>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { fetchCoreDataOverview, type CoreDataOverview } from '@/api/coreData';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const coreDataOverview = ref<CoreDataOverview | null>(null);
const coreDataLoading = ref(false);
const coreDataError = ref('');

const modules = [
  { title: 'JSON 审核中心', description: '扫描、校验、批准 JSON 暂存入库', to: '/import-review' },
  { title: '比赛中心', description: '赛程、阵容、事件、统计、证据与冲突', to: '/matches' },
  { title: '球队画像中心', description: '球队特性、进球时间点和证据链', to: '/profiles/teams' },
  { title: '球员画像中心', description: '状态、伤病、红黄牌和更衣室情况', to: '/profiles/players' },
  { title: '赛后复盘中心', description: 'ROI、CLV 和五层复盘', to: '/' },
];

const coreDataStats = computed(() => [
  { label: '球队', value: coreDataOverview.value?.teams ?? 0 },
  { label: '球员', value: coreDataOverview.value?.players ?? 0 },
  { label: '比赛', value: coreDataOverview.value?.matches ?? 0 },
  { label: '分析报告', value: coreDataOverview.value?.analysisReports ?? 0 },
  { label: '下注记录', value: coreDataOverview.value?.bets ?? 0 },
  { label: '赔率快照', value: coreDataOverview.value?.oddsSnapshots ?? 0 },
  { label: '证据链', value: coreDataOverview.value?.evidence ?? 0 },
  { label: '导入映射', value: coreDataOverview.value?.mappings ?? 0 },
]);

async function loadCoreDataOverview() {
  coreDataError.value = '';
  if (!authStore.basicAuthHeader) {
    coreDataOverview.value = null;
    return;
  }
  coreDataLoading.value = true;
  try {
    const response = await fetchCoreDataOverview(authStore.basicAuthHeader);
    coreDataOverview.value = response.data;
  } catch (cause) {
    coreDataOverview.value = null;
    coreDataError.value = cause instanceof Error ? cause.message : '无法读取正式业务库概览。';
  } finally {
    coreDataLoading.value = false;
  }
}

onMounted(loadCoreDataOverview);
</script>

<style scoped>
.module-grid,
.overview-row {
  margin-top: 16px;
}
.module-card {
  min-height: 178px;
}
.module-link {
  color: inherit;
  display: block;
  min-height: 126px;
  text-decoration: none;
}
.module-link span {
  color: #2563eb;
  font-weight: 600;
}
.overview-card {
  border-radius: 14px;
}
.overview-header {
  align-items: center;
  display: flex;
  justify-content: space-between;
}
.core-stats-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(4, 1fr);
}
.core-stat {
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px;
}
.core-stat strong {
  color: #1d4ed8;
  display: block;
  font-size: 26px;
}
.core-stat span {
  color: #6b7280;
}
</style>


