<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { fetchSystemSettings, type SystemSettings } from '@/api/system';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const settings = ref<SystemSettings | null>(null);
const loading = ref(false);
const error = ref('');

const canAdminWrite = computed(() => authStore.canWrite);

async function loadSettings() {
  if (!canAdminWrite.value) {
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    const response = await fetchSystemSettings(authStore.basicAuthHeader);
    settings.value = response.data;
  } catch (cause) {
    settings.value = null;
    error.value = cause instanceof Error ? cause.message : '无法读取系统设置，请确认已登录并启动后端服务。';
  } finally {
    loading.value = false;
  }
}

onMounted(loadSettings);
</script>

<template>
  <section class="page-shell admin-page" aria-labelledby="system-settings-title">
    <section class="page-content admin-page__content">
      <header class="admin-hero">
        <div>
          <p class="eyebrow">Admin · Settings</p>
          <h1 id="system-settings-title">系统设置</h1>
          <p>系统配置仅供 Basic 管理员查看，用于确认归档路径和分析体系保护边界。</p>
        </div>
        <RouterLink v-if="!canAdminWrite" class="login-link" to="/login">去登录</RouterLink>
        <button v-else class="action-button" type="button" :disabled="loading" @click="loadSettings">
          {{ loading ? '刷新中' : '刷新设置' }}
        </button>
      </header>

      <section v-if="!canAdminWrite" class="permission-card" role="note">
        <strong>需要管理员登录</strong>
        <p>系统设置属于后台配置，请使用 Basic 管理员账号登录后查看。</p>
      </section>

      <template v-else>
        <div v-if="error" class="notice-card notice-card--danger" role="alert">{{ error }}</div>
        <p v-if="loading && !settings" class="empty-copy">正在读取系统设置...</p>
        <section v-else-if="settings" class="settings-grid" aria-label="系统设置详情">
          <article class="setting-card">
            <p class="eyebrow">Archive</p>
            <h2>JSON 档案路径</h2>
            <strong>{{ settings.archivePath }}</strong>
          </article>
          <article class="setting-card">
            <p class="eyebrow">Protection</p>
            <h2>分析体系保护</h2>
            <strong>{{ settings.analysisSystemProtected ? '已启用' : '未启用' }}</strong>
          </article>
          <article class="setting-card setting-card--wide">
            <p class="eyebrow">Boundary</p>
            <h2>边界说明</h2>
            <p>{{ settings.boundaryDescription }}</p>
          </article>
        </section>
        <p v-else class="empty-copy">暂无系统设置。</p>
      </template>
    </section>
  </section>
</template>

<style scoped>
.admin-page { max-width: 100%; overflow-x: hidden; }
.admin-page__content { display: grid; gap: 18px; min-width: 0; }
.admin-hero, .permission-card, .notice-card, .setting-card {
  background: var(--wc-glass);
  border: 1px solid var(--wc-border);
  border-radius: var(--wc-radius-lg);
  color: var(--wc-text);
}
.admin-hero { align-items: center; display: grid; gap: 18px; grid-template-columns: minmax(0, 1fr) auto; padding: clamp(20px, 4vw, 38px); }
.admin-hero h1 { font-family: var(--wc-font-display); font-size: clamp(34px, 6vw, 64px); line-height: 1; margin: 0 0 12px; }
.admin-hero p:not(.eyebrow), .permission-card p, .setting-card p, .empty-copy { color: var(--wc-text-muted); }
.eyebrow { color: var(--wc-warning); font-family: var(--wc-font-mono); font-size: 12px; font-weight: 800; letter-spacing: .08em; margin: 0 0 8px; text-transform: uppercase; }
.login-link, .action-button {
  background: var(--wc-accent);
  border: 0;
  border-radius: 999px;
  color: var(--wc-on-accent);
  cursor: pointer;
  display: inline-flex;
  font-weight: 800;
  justify-content: center;
  min-height: 44px;
  padding: 12px 16px;
  text-decoration: none;
}
button:disabled { cursor: not-allowed; opacity: .48; }
.permission-card, .notice-card, .setting-card { display: grid; gap: 12px; min-width: 0; padding: 18px; }
.notice-card--danger { border-color: rgba(239, 68, 68, .36); }
.settings-grid { display: grid; gap: 14px; grid-template-columns: repeat(2, minmax(0, 1fr)); min-width: 0; }
.setting-card h2 { margin: 0; }
.setting-card strong { color: var(--wc-primary); font-family: var(--wc-font-mono); font-size: clamp(20px, 4vw, 34px); overflow-wrap: anywhere; }
.setting-card--wide { grid-column: 1 / -1; }
@media (max-width: 1024px) {
  .admin-hero { grid-template-columns: 1fr; }
}
@media (max-width: 640px) {
  .admin-hero, .settings-grid { grid-template-columns: 1fr; }
  .login-link, .action-button { width: 100%; }
}
</style>
