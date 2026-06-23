<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import {
  approveCollectionItem,
  listCollectionItems,
  rejectCollectionItem,
  type CollectionItem,
  type CollectionItemStatus,
  type ProfileEntityType,
} from '@/api/profiles';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const loading = ref(false);
const reviewingId = ref<number | null>(null);
const statusFilter = ref<CollectionItemStatus | ''>('PENDING_REVIEW');
const entityFilter = ref<ProfileEntityType | ''>('');
const items = ref<CollectionItem[]>([]);
const error = ref('');
const lastAction = ref('');

const canAdminWrite = computed(() => authStore.canWrite);

const filteredItems = computed(() => items.value.filter((item) => {
  const entityOk = !entityFilter.value || item.entityType === entityFilter.value;
  return entityOk;
}));

const stats = computed(() => ({
  total: items.value.length,
  teams: items.value.filter((item) => item.entityType === 'TEAM').length,
  players: items.value.filter((item) => item.entityType === 'PLAYER').length,
  pending: items.value.filter((item) => item.status === 'PENDING_REVIEW').length,
}));

function authHeader(): string {
  if (!authStore.basicAuthHeader) {
    throw new Error('需要管理员登录后才可以审核采集项。');
  }
  return authStore.basicAuthHeader;
}

function statusLabel(status: CollectionItemStatus): string {
  switch (status) {
    case 'APPROVED':
      return '已批准';
    case 'REJECTED':
      return '已驳回';
    default:
      return '待审核';
  }
}

function entityLabel(entity: ProfileEntityType): string {
  return entity === 'TEAM' ? '球队' : '球员';
}

function scoreText(value?: number): string {
  return value == null ? '-' : Number(value).toFixed(1).replace(/\.0$/, '');
}

function formatDateTime(value?: string): string {
  return value ? value.replace('T', ' ').slice(0, 16) : '待同步';
}

async function loadItems() {
  if (!canAdminWrite.value) {
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    const response = await listCollectionItems(authHeader(), statusFilter.value || undefined);
    items.value = response.data;
  } catch (cause) {
    items.value = [];
    error.value = cause instanceof Error ? cause.message : '无法读取采集审核列表。';
  } finally {
    loading.value = false;
  }
}

async function approve(item: CollectionItem) {
  reviewingId.value = item.id;
  error.value = '';
  try {
    const response = await approveCollectionItem(authHeader(), item.id);
    lastAction.value = `已批准 ${item.title}：${response.data.message}`;
    await loadItems();
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '批准采集项失败。';
  } finally {
    reviewingId.value = null;
  }
}

async function reject(item: CollectionItem) {
  const reason = window.prompt('请输入驳回原因，便于后续补充来源或内容。', item.reviewNote || '');
  if (reason == null) {
    return;
  }
  reviewingId.value = item.id;
  error.value = '';
  try {
    const response = await rejectCollectionItem(authHeader(), item.id, reason);
    lastAction.value = `已驳回 ${item.title}：${response.data.message}`;
    await loadItems();
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '驳回采集项失败。';
  } finally {
    reviewingId.value = null;
  }
}

onMounted(loadItems);
</script>

<template>
  <section class="page-shell admin-page" aria-labelledby="collection-review-title">
    <section class="page-content admin-page__content">
      <header class="admin-hero">
        <div>
          <p class="eyebrow">Admin · Collection review</p>
          <h1 id="collection-review-title">采集审核中心</h1>
          <p>用于审核球队/球员画像采集事实，批准后进入公开画像；公开画像页不再承担写入操作。</p>
        </div>
        <RouterLink v-if="!canAdminWrite" class="login-link" to="/login">去登录</RouterLink>
        <button v-else class="action-button" type="button" :disabled="loading" @click="loadItems">
          {{ loading ? '刷新中' : '刷新列表' }}
        </button>
      </header>

      <section v-if="!canAdminWrite" class="permission-card" role="note">
        <strong>需要管理员登录</strong>
        <p>采集项批准/驳回会修改数据状态，请使用 Basic 管理员账号登录后继续。</p>
      </section>

      <template v-else>
        <section class="stat-grid" aria-label="采集审核统计">
          <article class="stat-card"><span>条目</span><strong>{{ stats.total }}</strong></article>
          <article class="stat-card"><span>球队</span><strong>{{ stats.teams }}</strong></article>
          <article class="stat-card"><span>球员</span><strong>{{ stats.players }}</strong></article>
          <article class="stat-card"><span>待审核</span><strong>{{ stats.pending }}</strong></article>
        </section>

        <div v-if="lastAction" class="notice-card notice-card--success" role="status">{{ lastAction }}</div>
        <div v-if="error" class="notice-card notice-card--danger" role="alert">{{ error }}</div>

        <section class="toolbar-card" aria-label="采集审核筛选">
          <label>
            状态
            <select v-model="statusFilter" @change="loadItems">
              <option value="">全部状态</option>
              <option value="PENDING_REVIEW">待审核</option>
              <option value="APPROVED">已批准</option>
              <option value="REJECTED">已驳回</option>
            </select>
          </label>
          <label>
            对象
            <select v-model="entityFilter">
              <option value="">全部对象</option>
              <option value="TEAM">球队</option>
              <option value="PLAYER">球员</option>
            </select>
          </label>
          <button class="secondary-button" type="button" @click="loadItems">刷新</button>
        </section>

        <section class="review-card-list" aria-label="采集审核条目">
          <p v-if="loading && !items.length" class="empty-copy">正在加载采集审核条目...</p>
          <p v-else-if="!filteredItems.length" class="empty-copy">暂无符合条件的采集审核条目。</p>
          <article v-for="item in filteredItems" v-else :key="item.id" class="review-card">
            <div class="review-card__main">
              <span class="meta-line">{{ entityLabel(item.entityType) }} · {{ statusLabel(item.status) }} · {{ item.factType }}</span>
              <h3>{{ item.title }}</h3>
              <p>{{ item.summary }}</p>
              <small>{{ item.entityKey }} · 来源 {{ item.sourceName }} · 可信度 {{ scoreText(item.reliabilityScore) }} · {{ formatDateTime(item.capturedAt) }}</small>
              <small v-if="item.reviewNote">审核备注：{{ item.reviewNote }}</small>
            </div>
            <div class="review-card__actions">
              <button
                class="success-button"
                type="button"
                :disabled="item.status === 'APPROVED' || reviewingId === item.id"
                @click="approve(item)"
              >
                {{ reviewingId === item.id ? '处理中' : '批准' }}
              </button>
              <button
                class="danger-button"
                type="button"
                :disabled="item.status === 'REJECTED' || reviewingId === item.id"
                @click="reject(item)"
              >
                驳回
              </button>
            </div>
          </article>
        </section>
      </template>
    </section>
  </section>
</template>

<style scoped>
.admin-page { max-width: 100%; overflow-x: hidden; }
.admin-page__content { display: grid; gap: 18px; min-width: 0; }
.admin-hero, .permission-card, .stat-card, .notice-card, .toolbar-card, .review-card {
  background: var(--wc-glass);
  border: 1px solid var(--wc-border);
  border-radius: var(--wc-radius-lg);
  color: var(--wc-text);
}
.admin-hero { align-items: center; display: grid; gap: 18px; grid-template-columns: minmax(0, 1fr) auto; padding: clamp(20px, 4vw, 38px); }
.admin-hero h1 { font-family: var(--wc-font-display); font-size: clamp(34px, 6vw, 64px); line-height: 1; margin: 0 0 12px; }
.admin-hero p:not(.eyebrow), .permission-card p, .review-card p, .review-card small, .empty-copy, .meta-line { color: var(--wc-text-muted); }
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
.permission-card, .notice-card, .toolbar-card { display: grid; gap: 14px; min-width: 0; padding: 18px; }
.stat-grid { display: grid; gap: 14px; grid-template-columns: repeat(4, minmax(0, 1fr)); }
.stat-card { display: grid; gap: 8px; min-width: 0; padding: 18px; }
.stat-card strong { color: var(--wc-primary); font-family: var(--wc-font-mono); font-size: 34px; }
.notice-card--success { border-color: rgba(34, 197, 94, .36); }
.notice-card--danger { border-color: rgba(239, 68, 68, .36); }
.toolbar-card { align-items: end; grid-template-columns: repeat(3, minmax(0, 1fr)); }
label { color: var(--wc-text-muted); display: grid; font-size: 13px; font-weight: 800; gap: 8px; min-width: 0; }
select { background: rgba(15, 23, 42, .66); border: 1px solid rgba(147, 197, 253, .22); border-radius: 14px; color: var(--wc-text); min-height: 44px; min-width: 0; padding: 0 12px; }
.review-card-list { display: grid; gap: 14px; min-width: 0; }
.review-card { display: grid; gap: 14px; grid-template-columns: minmax(0, 1fr) auto; min-width: 0; padding: 18px; }
.review-card__main { display: grid; gap: 8px; min-width: 0; }
.review-card__main h3 { margin: 0; }
.review-card__actions { align-content: start; display: grid; gap: 8px; min-width: 0; }
@media (max-width: 1024px) {
  .admin-hero, .review-card { grid-template-columns: 1fr; }
  .stat-grid, .toolbar-card { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .review-card__actions { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 640px) {
  .admin-hero, .stat-grid, .toolbar-card, .review-card, .review-card__actions { grid-template-columns: 1fr; }
  .login-link, .action-button, .secondary-button, .success-button, .danger-button { width: 100%; }
}
</style>
