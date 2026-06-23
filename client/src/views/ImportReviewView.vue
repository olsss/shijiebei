<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { importCoreDataItem, listCoreDataMappings, type CoreDataMapping } from '@/api/coreData';
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
const detailLoading = ref(false);
const items = ref<ImportItemResponse[]>([]);
const selectedIds = ref<Set<number>>(new Set());
const statusFilter = ref<ImportItemStatus | ''>('');
const typeFilter = ref<ImportItemType | ''>('');
const lastJob = ref('');
const lastCoreImport = ref('');
const error = ref('');
const detail = ref<ImportItemDetailResponse | null>(null);
const importingItemIds = ref<Set<number>>(new Set());
const coreDataMappings = ref<Record<number, CoreDataMapping[]>>({});

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

const canAdminWrite = computed(() => authStore.canWrite);

const reviewStats = computed(() => {
  const total = items.value.length;
  const valid = items.value.filter((item) => item.validJson).length;
  const invalid = total - valid;
  const pending = items.value.filter((item) => item.status === 'PENDING_REVIEW').length;
  return { total, valid, invalid, pending };
});

const selectedItems = computed(() => items.value.filter((item) => selectedIds.value.has(item.id)));
const approvableSelection = computed(() => selectedItems.value.filter((item) => item.validJson && item.status === 'PENDING_REVIEW'));

function authHeader(): string {
  if (!authStore.basicAuthHeader) {
    throw new Error('需要管理员登录后才可以审核批准入库。');
  }
  return authStore.basicAuthHeader;
}

function statusLabel(status: ImportItemStatus): string {
  return statusOptions.find((option) => option.value === status)?.label ?? status;
}

function typeLabel(type: ImportItemType): string {
  return typeOptions.find((option) => option.value === type)?.label ?? type;
}

function mappingCount(row: ImportItemResponse): number {
  return coreDataMappings.value[row.id]?.length ?? 0;
}

function coreDataStatusLabel(row: ImportItemResponse): string {
  if (row.status !== 'APPROVED') {
    return '未批准';
  }
  return mappingCount(row) > 0 ? '已导入' : '未导入';
}

function isImportingCoreData(id: number): boolean {
  return importingItemIds.value.has(id);
}

function toggleSelection(row: ImportItemResponse, checked: boolean) {
  const next = new Set(selectedIds.value);
  if (checked) {
    next.add(row.id);
  } else {
    next.delete(row.id);
  }
  selectedIds.value = next;
}

async function loadCoreDataMappingsForApproved(rows: ImportItemResponse[]) {
  const approvedRows = rows.filter((item) => item.status === 'APPROVED' && item.validJson);
  if (approvedRows.length === 0) {
    coreDataMappings.value = {};
    return;
  }
  const header = authHeader();
  const nextMappings: Record<number, CoreDataMapping[]> = {};
  await Promise.all(approvedRows.map(async (item) => {
    try {
      const response = await listCoreDataMappings(header, item.id);
      nextMappings[item.id] = response.data;
    } catch {
      nextMappings[item.id] = [];
    }
  }));
  coreDataMappings.value = nextMappings;
}

async function loadItems() {
  if (!canAdminWrite.value) {
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    const response = await listImportItems(authHeader(), {
      status: statusFilter.value || undefined,
      type: typeFilter.value || undefined,
    });
    items.value = response.data;
    selectedIds.value = new Set();
    await loadCoreDataMappingsForApproved(response.data);
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
    const response = await scanArchive(authHeader(), archivePath.value);
    lastJob.value = `任务 #${response.data.id}：共 ${response.data.totalItems} 条，可批准 ${response.data.validItems} 条，无效 ${response.data.invalidItems} 条`;
    await loadItems();
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '扫描失败，请确认路径和后端服务。';
  } finally {
    scanning.value = false;
  }
}

async function openDetail(row: ImportItemResponse) {
  detailLoading.value = true;
  detail.value = null;
  try {
    const response = await getImportItem(authHeader(), row.id);
    detail.value = response.data;
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '无法读取 JSON 明细。';
  } finally {
    detailLoading.value = false;
  }
}

async function approve(row: ImportItemResponse) {
  if (!row.validJson) {
    error.value = '无效 JSON 不能批准入库。';
    return;
  }
  await approveImportItem(authHeader(), row.id);
  await loadItems();
}

async function approveBatch() {
  if (approvableSelection.value.length === 0) {
    error.value = '请选择待审核且 JSON 有效的条目。';
    return;
  }
  await batchApproveImportItems(authHeader(), approvableSelection.value.map((item) => item.id));
  await loadItems();
}

async function reject(row: ImportItemResponse) {
  const reason = window.prompt('请输入驳回原因，便于后续修正 JSON 档案。', row.validationMessage === 'ok' ? '' : row.validationMessage);
  if (reason == null) {
    return;
  }
  await rejectImportItem(authHeader(), row.id, reason);
  await loadItems();
}

async function importToCoreData(row: ImportItemResponse) {
  if (!row.validJson || row.status !== 'APPROVED') {
    error.value = '只有已批准且有效的 JSON 可以导入正式库。';
    return;
  }
  importingItemIds.value = new Set([...importingItemIds.value, row.id]);
  lastCoreImport.value = '';
  try {
    const response = await importCoreDataItem(authHeader(), row.id);
    const mappingResponse = await listCoreDataMappings(authHeader(), row.id);
    coreDataMappings.value = {
      ...coreDataMappings.value,
      [row.id]: mappingResponse.data,
    };
    lastCoreImport.value = `导入项 #${response.data.importItemId}：${response.data.message}`;
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '导入正式库失败。';
  } finally {
    const next = new Set(importingItemIds.value);
    next.delete(row.id);
    importingItemIds.value = next;
  }
}

onMounted(loadItems);
</script>

<template>
  <section class="page-shell admin-page" aria-labelledby="import-review-title">
    <section class="page-content admin-page__content">
      <header class="admin-hero">
        <div>
          <p class="eyebrow">Admin · JSON review</p>
          <h1 id="import-review-title">JSON 审核中心</h1>
          <p>仅 Basic 管理员可以扫描、批准、驳回、批量批准并导入正式库；公开页面不会暴露这些写入动作。</p>
        </div>
        <RouterLink v-if="!canAdminWrite" class="login-link" to="/login">去登录</RouterLink>
        <button v-else class="action-button" type="button" :disabled="loading" @click="loadItems">
          {{ loading ? '刷新中' : '刷新列表' }}
        </button>
      </header>

      <section v-if="!canAdminWrite" class="permission-card" role="note">
        <strong>需要管理员登录</strong>
        <p>JSON 审核、批准入库和正式库导入属于写入操作，请使用 Basic 管理员账号登录后继续。</p>
      </section>

      <template v-else>
        <section class="scan-card">
          <div>
            <p class="eyebrow">Archive scan</p>
            <h2>档案扫描</h2>
            <p>扫描已人工确认的 JSON 档案进入审核暂存区。</p>
          </div>
          <label>
            JSON 路径
            <input v-model="archivePath" type="text" autocomplete="off">
          </label>
          <button class="action-button" type="button" :disabled="scanning" @click="handleScan">
            {{ scanning ? '扫描中' : '扫描入审核池' }}
          </button>
        </section>

        <section class="stat-grid" aria-label="审核概览">
          <article class="stat-card"><span>总条目</span><strong>{{ reviewStats.total }}</strong></article>
          <article class="stat-card"><span>有效 JSON</span><strong>{{ reviewStats.valid }}</strong></article>
          <article class="stat-card"><span>无效 JSON</span><strong>{{ reviewStats.invalid }}</strong></article>
          <article class="stat-card"><span>待审核</span><strong>{{ reviewStats.pending }}</strong></article>
        </section>

        <div v-if="lastJob" class="notice-card notice-card--success" role="status">{{ lastJob }}</div>
        <div v-if="lastCoreImport" class="notice-card notice-card--success" role="status">{{ lastCoreImport }}</div>
        <div v-if="error" class="notice-card notice-card--danger" role="alert">{{ error }}</div>

        <section class="toolbar-card" aria-label="审核筛选">
          <label>
            状态
            <select v-model="statusFilter" @change="loadItems">
              <option value="">全部状态</option>
              <option v-for="option in statusOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
            </select>
          </label>
          <label>
            类型
            <select v-model="typeFilter" @change="loadItems">
              <option value="">全部类型</option>
              <option v-for="option in typeOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
            </select>
          </label>
          <button class="secondary-button" type="button" @click="loadItems">刷新</button>
          <button class="success-button" type="button" :disabled="approvableSelection.length === 0" @click="approveBatch">
            批量批准 {{ approvableSelection.length || '' }}
          </button>
        </section>

        <section class="review-card-list" aria-label="JSON 审核条目">
          <p v-if="loading && !items.length" class="empty-copy">正在加载审核条目...</p>
          <p v-else-if="!items.length" class="empty-copy">暂无 JSON 审核条目，请先扫描档案。</p>
          <article v-for="item in items" v-else :key="item.id" class="review-card">
            <div class="review-card__main">
              <label class="select-line">
                <input
                  type="checkbox"
                  :checked="selectedIds.has(item.id)"
                  :disabled="!item.validJson || item.status !== 'PENDING_REVIEW'"
                  @change="toggleSelection(item, ($event.target as HTMLInputElement).checked)"
                >
                <span>{{ typeLabel(item.itemType) }} · {{ statusLabel(item.status) }}</span>
              </label>
              <h3>{{ item.summaryTitle || item.relativePath }}</h3>
              <p>{{ item.relativePath }}</p>
              <small>SHA-256 {{ item.sha256 }} · 校验 {{ item.validJson ? 'JSON 有效' : 'JSON 无效' }} · {{ item.validationMessage }}</small>
              <small>正式库：{{ coreDataStatusLabel(item) }} <template v-if="mappingCount(item) > 0">· 映射 {{ mappingCount(item) }} 条</template></small>
            </div>
            <div class="review-card__actions">
              <button class="secondary-button" type="button" @click="openDetail(item)">查看</button>
              <button class="success-button" type="button" :disabled="!item.validJson || item.status === 'APPROVED'" @click="approve(item)">批准入库</button>
              <button class="danger-button" type="button" :disabled="item.status === 'REJECTED'" @click="reject(item)">驳回</button>
              <button
                class="action-button"
                type="button"
                :disabled="!item.validJson || item.status !== 'APPROVED'"
                @click="importToCoreData(item)"
              >
                {{ isImportingCoreData(item.id) ? '导入中' : '导入正式库' }}
              </button>
            </div>
          </article>
        </section>

        <section class="detail-card" aria-label="JSON 明细">
          <div class="panel-heading">
            <div><p class="eyebrow">Detail</p><h2>JSON 原文与审核上下文</h2></div>
          </div>
          <p v-if="detailLoading" class="empty-copy">正在读取 JSON 明细...</p>
          <template v-else-if="detail">
            <div class="summary-grid">
              <div><span>文件</span><strong>{{ detail.item.relativePath }}</strong></div>
              <div><span>状态</span><strong>{{ statusLabel(detail.item.status) }}</strong></div>
              <div><span>校验</span><strong>{{ detail.item.validationMessage }}</strong></div>
              <div><span>映射</span><strong>{{ coreDataMappings[detail.item.id]?.length ?? 0 }} 条</strong></div>
            </div>
            <p v-if="detail.rejectionReason">驳回原因：{{ detail.rejectionReason }}</p>
            <pre class="raw-json">{{ detail.rawJson }}</pre>
          </template>
          <p v-else class="empty-copy">请选择一条 JSON 查看。</p>
        </section>
      </template>
    </section>
  </section>
</template>

<style scoped>
.admin-page { max-width: 100%; overflow-x: hidden; }
.admin-page__content { display: grid; gap: 18px; min-width: 0; }
.admin-hero, .permission-card, .scan-card, .stat-card, .notice-card, .toolbar-card, .review-card, .detail-card, .summary-grid div {
  background: var(--wc-glass);
  border: 1px solid var(--wc-border);
  border-radius: var(--wc-radius-lg);
  color: var(--wc-text);
}
.admin-hero { align-items: center; display: grid; gap: 18px; grid-template-columns: minmax(0, 1fr) auto; padding: clamp(20px, 4vw, 38px); }
.admin-hero h1 { font-family: var(--wc-font-display); font-size: clamp(34px, 6vw, 64px); line-height: 1; margin: 0 0 12px; }
.admin-hero p:not(.eyebrow), .permission-card p, .scan-card p, .review-card p, .review-card small, .empty-copy, .summary-grid span { color: var(--wc-text-muted); }
.eyebrow { color: var(--wc-warning); font-family: var(--wc-font-mono); font-size: 12px; font-weight: 800; letter-spacing: .08em; margin: 0 0 8px; text-transform: uppercase; }
.login-link, .action-button, .secondary-button, .success-button, .danger-button {
  border: 0;
  border-radius: 999px;
  cursor: pointer;
  display: inline-flex;
  font-weight: 800;
  justify-content: center;
  min-height: 44px;
  padding: 12px 16px;
  text-decoration: none;
}
.login-link, .action-button { background: var(--wc-accent); color: var(--wc-on-accent); }
.secondary-button { background: rgba(147, 197, 253, .14); color: var(--wc-primary); }
.success-button { background: rgba(34, 197, 94, .18); color: #bbf7d0; }
.danger-button { background: rgba(239, 68, 68, .18); color: #fecaca; }
button:disabled { cursor: not-allowed; opacity: .48; }
.permission-card, .scan-card, .notice-card, .toolbar-card, .detail-card { display: grid; gap: 14px; min-width: 0; padding: 18px; }
.scan-card { align-items: end; grid-template-columns: minmax(0, 1fr) minmax(0, 320px) auto; }
label { color: var(--wc-text-muted); display: grid; font-size: 13px; font-weight: 800; gap: 8px; min-width: 0; }
input, select { background: rgba(15, 23, 42, .66); border: 1px solid rgba(147, 197, 253, .22); border-radius: 14px; color: var(--wc-text); min-height: 44px; min-width: 0; padding: 0 12px; }
.stat-grid, .summary-grid { display: grid; gap: 14px; grid-template-columns: repeat(4, minmax(0, 1fr)); }
.stat-card, .summary-grid div { display: grid; gap: 8px; min-width: 0; padding: 18px; }
.stat-card strong { color: var(--wc-primary); font-family: var(--wc-font-mono); font-size: 34px; }
.notice-card--success { border-color: rgba(34, 197, 94, .36); }
.notice-card--danger { border-color: rgba(239, 68, 68, .36); }
.toolbar-card { align-items: end; grid-template-columns: repeat(4, minmax(0, 1fr)); }
.review-card-list { display: grid; gap: 14px; min-width: 0; }
.review-card { display: grid; gap: 14px; grid-template-columns: minmax(0, 1fr) auto; min-width: 0; padding: 18px; }
.review-card__main { display: grid; gap: 8px; min-width: 0; }
.review-card__main h3 { margin: 0; }
.select-line { align-items: center; display: flex; flex-direction: row; gap: 10px; min-height: 44px; }
.select-line input { min-height: 20px; min-width: 20px; }
.review-card__actions { align-content: start; display: grid; gap: 8px; min-width: 0; }
.panel-heading { align-items: center; display: flex; justify-content: space-between; }
.raw-json { background: #0f172a; border: 1px solid rgba(147, 197, 253, .18); border-radius: var(--wc-radius-md); color: #e5e7eb; line-height: 1.6; max-height: 320px; min-width: 0; overflow: auto; padding: 18px; white-space: pre-wrap; }
@media (max-width: 1024px) {
  .admin-hero, .scan-card, .review-card { grid-template-columns: 1fr; }
  .stat-grid, .summary-grid, .toolbar-card { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .review-card__actions { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 640px) {
  .admin-hero, .scan-card, .stat-grid, .summary-grid, .toolbar-card, .review-card, .review-card__actions { grid-template-columns: 1fr; }
  .login-link, .action-button, .secondary-button, .success-button, .danger-button { width: 100%; }
}
</style>
