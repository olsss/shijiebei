<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  approveImportItem,
  batchApproveImportItems,
  getImportItem,
  listImportItems,
  rejectImportItem,
  scanArchive,
  type ImportItemDetailResponse,
  type ImportItemResponse,
  type ImportItemStatus,
  type ImportItemType,
} from '@/api/importReview';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const archivePath = ref('../skill/archive');
const loading = ref(false);
const scanning = ref(false);
const items = ref<ImportItemResponse[]>([]);
const selectedItems = ref<ImportItemResponse[]>([]);
const statusFilter = ref<ImportItemStatus | ''>('');
const typeFilter = ref<ImportItemType | ''>('');
const lastJob = ref('');
const error = ref('');
const drawerVisible = ref(false);
const detailLoading = ref(false);
const detail = ref<ImportItemDetailResponse | null>(null);

const statusOptions: Array<{ label: string; value: ImportItemStatus }> = [
  { label: '待审核', value: 'PENDING_REVIEW' },
  { label: '已批准', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' },
];

const typeOptions: Array<{ label: string; value: ImportItemType }> = [
  { label: '下注汇总', value: 'BETS' },
  { label: '比赛分析', value: 'ANALYSIS' },
  { label: '赔率快照', value: 'ODDS' },
  { label: '来源证据', value: 'SOURCE' },
];

const reviewStats = computed(() => {
  const total = items.value.length;
  const valid = items.value.filter((item) => item.validJson).length;
  const invalid = total - valid;
  const pending = items.value.filter((item) => item.status === 'PENDING_REVIEW').length;
  return { total, valid, invalid, pending };
});

const approvableSelection = computed(() =>
  selectedItems.value.filter((item) => item.validJson && item.status === 'PENDING_REVIEW'),
);

function requireAuthHeader(): string {
  if (!authStore.basicAuthHeader) {
    throw new Error('请先登录后再操作 JSON 审核中心。');
  }
  return authStore.basicAuthHeader;
}

function statusLabel(status: ImportItemStatus): string {
  return statusOptions.find((option) => option.value === status)?.label ?? status;
}

function typeLabel(type: ImportItemType): string {
  return typeOptions.find((option) => option.value === type)?.label ?? type;
}

function statusTagType(status: ImportItemStatus) {
  if (status === 'APPROVED') return 'success';
  if (status === 'REJECTED') return 'danger';
  return 'warning';
}

async function loadItems() {
  loading.value = true;
  error.value = '';
  try {
    const response = await listImportItems(requireAuthHeader(), {
      status: statusFilter.value || undefined,
      type: typeFilter.value || undefined,
    });
    items.value = response.data;
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '无法读取 JSON 审核列表。';
  } finally {
    loading.value = false;
  }
}

async function handleScan() {
  scanning.value = true;
  error.value = '';
  try {
    const response = await scanArchive(requireAuthHeader(), archivePath.value);
    lastJob.value = `任务 #${response.data.id}：共 ${response.data.totalItems} 条，可批准 ${response.data.validItems} 条，无效 ${response.data.invalidItems} 条`;
    ElMessage.success('JSON 档案扫描完成，已进入审核暂存区。');
    await loadItems();
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '扫描失败，请确认路径和后端服务。';
  } finally {
    scanning.value = false;
  }
}

async function openDetail(row: ImportItemResponse) {
  detailLoading.value = true;
  drawerVisible.value = true;
  detail.value = null;
  try {
    const response = await getImportItem(requireAuthHeader(), row.id);
    detail.value = response.data;
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '无法读取 JSON 明细。');
  } finally {
    detailLoading.value = false;
  }
}

async function approve(row: ImportItemResponse) {
  if (!row.validJson) {
    ElMessage.warning('无效 JSON 不能批准入库。');
    return;
  }
  await approveImportItem(requireAuthHeader(), row.id);
  ElMessage.success(`已批准 ${row.relativePath}`);
  await loadItems();
}

async function approveBatch() {
  if (approvableSelection.value.length === 0) {
    ElMessage.warning('请选择待审核且 JSON 有效的条目。');
    return;
  }
  const ids = approvableSelection.value.map((item) => item.id);
  await batchApproveImportItems(requireAuthHeader(), ids);
  ElMessage.success(`已批量批准 ${ids.length} 条 JSON。`);
  selectedItems.value = [];
  await loadItems();
}

async function reject(row: ImportItemResponse) {
  const result = await ElMessageBox.prompt('请输入驳回原因，便于后续修正 JSON 档案。', '驳回 JSON', {
    confirmButtonText: '确认驳回',
    cancelButtonText: '取消',
    inputPlaceholder: '例如：字段缺失或数据来源不完整',
    inputValue: row.validationMessage === 'ok' ? '' : row.validationMessage,
  });
  await rejectImportItem(requireAuthHeader(), row.id, result.value);
  ElMessage.success(`已驳回 ${row.relativePath}`);
  await loadItems();
}

onMounted(loadItems);
</script>

<template>
  <main class="page-shell import-review-page">
    <section class="page-content">
      <el-page-header content="JSON 审核中心" @back="$router.push('/')" />

      <el-row :gutter="16" class="hero-grid">
        <el-col :span="16">
          <el-card class="hero-card">
            <template #header>
              <div class="card-header">
                <span>档案扫描</span>
                <el-tag type="info">只读扫描 skill/archive</el-tag>
              </div>
            </template>
            <p class="hero-copy">
              将 Codex/Claude 生成并经人工确认的 JSON 档案扫描进审核暂存区；批准后只写入 MySQL import staging 表，不修改比赛分析规则。
            </p>
            <el-input v-model="archivePath" placeholder="../skill/archive" clearable>
              <template #prepend>JSON 路径</template>
              <template #append>
                <el-button :loading="scanning" type="primary" @click="handleScan">扫描入审核池</el-button>
              </template>
            </el-input>
            <el-alert v-if="lastJob" :title="lastJob" type="success" show-icon class="job-alert" />
            <el-alert v-if="error" :title="error" type="error" show-icon class="job-alert" />
          </el-card>
        </el-col>

        <el-col :span="8">
          <el-card class="stats-card">
            <template #header>审核概览</template>
            <div class="stats-grid">
              <div>
                <strong>{{ reviewStats.total }}</strong>
                <span>总条目</span>
              </div>
              <div>
                <strong>{{ reviewStats.valid }}</strong>
                <span>有效 JSON</span>
              </div>
              <div>
                <strong>{{ reviewStats.invalid }}</strong>
                <span>无效 JSON</span>
              </div>
              <div>
                <strong>{{ reviewStats.pending }}</strong>
                <span>待审核</span>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-card class="table-card">
        <template #header>
          <div class="table-toolbar">
            <div class="filters">
              <el-select v-model="statusFilter" placeholder="状态" clearable @change="loadItems">
                <el-option v-for="option in statusOptions" :key="option.value" :label="option.label" :value="option.value" />
              </el-select>
              <el-select v-model="typeFilter" placeholder="类型" clearable @change="loadItems">
                <el-option v-for="option in typeOptions" :key="option.value" :label="option.label" :value="option.value" />
              </el-select>
              <el-button @click="loadItems">刷新</el-button>
            </div>
            <el-button type="success" :disabled="approvableSelection.length === 0" @click="approveBatch">
              批量批准 {{ approvableSelection.length || '' }}
            </el-button>
          </div>
        </template>

        <el-table
          v-loading="loading"
          :data="items"
          empty-text="暂无 JSON 审核条目，请先扫描档案"
          border
          @selection-change="(rows: ImportItemResponse[]) => (selectedItems = rows)"
        >
          <el-table-column type="selection" width="48" />
          <el-table-column prop="relativePath" label="文件" min-width="220" />
          <el-table-column prop="summaryTitle" label="摘要" min-width="160" />
          <el-table-column label="类型" width="120">
            <template #default="{ row }">
              <el-tag>{{ typeLabel(row.itemType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="校验" min-width="220">
            <template #default="{ row }">
              <el-tag :type="row.validJson ? 'success' : 'danger'">
                {{ row.validJson ? 'JSON 有效' : 'JSON 无效' }}
              </el-tag>
              <span class="validation-text">{{ row.validationMessage }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="sha256" label="SHA-256" min-width="260" show-overflow-tooltip />
          <el-table-column label="操作" fixed="right" width="250">
            <template #default="{ row }">
              <el-button size="small" @click="openDetail(row)">查看</el-button>
              <el-button
                size="small"
                type="success"
                :disabled="!row.validJson || row.status === 'APPROVED'"
                @click="approve(row)"
              >
                批准
              </el-button>
              <el-button size="small" type="danger" :disabled="row.status === 'REJECTED'" @click="reject(row)">
                驳回
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-drawer v-model="drawerVisible" title="JSON 原文与审核上下文" size="52%">
        <el-skeleton v-if="detailLoading" :rows="8" animated />
        <template v-else-if="detail">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="文件">{{ detail.item.relativePath }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ statusLabel(detail.item.status) }}</el-descriptions-item>
            <el-descriptions-item label="校验">{{ detail.item.validationMessage }}</el-descriptions-item>
            <el-descriptions-item v-if="detail.rejectionReason" label="驳回原因">{{ detail.rejectionReason }}</el-descriptions-item>
          </el-descriptions>
          <pre class="raw-json">{{ detail.rawJson }}</pre>
        </template>
        <el-empty v-else description="请选择一条 JSON 查看" />
      </el-drawer>
    </section>
  </main>
</template>

<style scoped>
.import-review-page {
  background:
    radial-gradient(circle at top left, rgba(37, 99, 235, 0.12), transparent 28rem),
    #f5f7fb;
}
.hero-grid,
.table-card {
  margin-top: 16px;
}
.hero-card,
.stats-card,
.table-card {
  border-radius: 14px;
}
.card-header,
.table-toolbar,
.filters {
  align-items: center;
  display: flex;
  gap: 12px;
  justify-content: space-between;
}
.filters {
  justify-content: flex-start;
}
.hero-copy {
  color: #4b5563;
  line-height: 1.7;
  margin-top: 0;
}
.job-alert {
  margin-top: 12px;
}
.stats-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(2, 1fr);
}
.stats-grid div {
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 18px;
}
.stats-grid strong {
  color: #1d4ed8;
  display: block;
  font-size: 28px;
}
.stats-grid span,
.validation-text {
  color: #6b7280;
  margin-left: 8px;
}
.raw-json {
  background: #0f172a;
  border-radius: 12px;
  color: #e5e7eb;
  line-height: 1.6;
  margin-top: 16px;
  overflow: auto;
  padding: 18px;
  white-space: pre-wrap;
}
</style>
