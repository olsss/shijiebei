<template>
  <section class="page-shell more-page" aria-labelledby="more-title">
    <section class="page-content more-page__content">
      <div class="more-page__hero">
        <p>More · Admin gateway</p>
        <h1 id="more-title">更多入口</h1>
        <span>公开数据继续保持只读；管理后台需要 Basic 管理员登录。</span>
      </div>

      <div class="more-page__grid">
        <RouterLink class="more-card" to="/evidence/teams">
          <strong>球队画像</strong>
          <span>查看已批准的球队事实、阵容和历史表现。</span>
        </RouterLink>
        <RouterLink class="more-card" to="/evidence/players">
          <strong>球员画像</strong>
          <span>查看已批准的球员状态、伤停和纪律信息。</span>
        </RouterLink>
        <RouterLink v-if="authStore.canWrite" class="more-card more-card--admin" to="/admin/import-review">
          <strong>JSON 审核中心</strong>
          <span>管理员登录后进行扫描、批准、驳回和入库。</span>
        </RouterLink>
        <RouterLink v-if="authStore.canWrite" class="more-card more-card--admin" to="/admin/collection-review">
          <strong>采集审核中心</strong>
          <span>管理员登录后批准或驳回球队/球员画像采集项。</span>
        </RouterLink>
        <RouterLink v-if="authStore.canWrite" class="more-card more-card--admin" to="/admin/settings">
          <strong>系统设置</strong>
          <span>管理员维护系统配置与归档路径。</span>
        </RouterLink>
        <div v-else class="more-card more-card--readonly" role="note">
          <strong>管理员入口已隐藏</strong>
          <span>JSON 审核、系统设置和入库操作仅 Basic 管理员登录后显示。</span>
        </div>
      </div>
    </section>
  </section>
</template>

<script setup lang="ts">
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
</script>

<style scoped>
.more-page__content {
  display: grid;
  gap: 18px;
  min-width: 0;
}

.more-page__hero,
.more-card {
  background: var(--wc-glass);
  border: 1px solid var(--wc-border);
  border-radius: var(--wc-radius-lg);
  color: var(--wc-text);
}

.more-page__hero {
  padding: clamp(18px, 4vw, 34px);
}

.more-page__hero p {
  color: var(--wc-warning);
  font-family: var(--wc-font-mono);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  margin: 0 0 8px;
  text-transform: uppercase;
}

.more-page__hero h1 {
  font-family: var(--wc-font-display);
  font-size: clamp(30px, 6vw, 56px);
  line-height: 1.05;
  margin: 0 0 10px;
}

.more-page__hero span {
  color: var(--wc-text-muted);
}

.more-page__grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.more-card {
  display: grid;
  gap: 8px;
  min-height: 112px;
  min-width: 0;
  padding: 18px;
  text-decoration: none;
  transition: border-color 180ms ease, transform 180ms ease;
}

.more-card:hover {
  border-color: rgba(147, 197, 253, 0.48);
  transform: translateY(-2px);
}

.more-card:focus-visible {
  box-shadow: var(--wc-focus-ring);
  outline: none;
}

.more-card strong {
  font-size: 18px;
}

.more-card span {
  color: var(--wc-text-muted);
  line-height: 1.6;
}

.more-card--admin {
  border-color: rgba(217, 119, 6, 0.34);
}

.more-card--readonly {
  border-color: rgba(147, 197, 253, 0.24);
}

@media (max-width: 767px) {
  .more-page__grid {
    grid-template-columns: 1fr;
  }
}

@media (prefers-reduced-motion: reduce) {
  .more-card {
    transition: none;
  }
}
</style>
