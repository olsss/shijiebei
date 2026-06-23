<template>
  <div class="app-shell">
    <a class="skip-link" href="#app-main">跳到主要内容</a>

    <aside class="app-shell__sidebar" aria-label="桌面端导航栏">
      <RouterLink class="brand" to="/" aria-label="返回赛事总览">
        <span class="brand__crest" aria-hidden="true">WC</span>
        <span>
          <strong>世界杯情报舰桥</strong>
          <small>Decision Command</small>
        </span>
      </RouterLink>

      <nav class="side-nav" role="navigation" aria-label="主导航">
        <RouterLink
          v-for="item in primaryNav"
          :key="item.to"
          class="side-nav__item"
          :to="item.to"
          :aria-current="isNavActive(item.to) ? 'page' : undefined"
        >
          <span class="side-nav__mark" aria-hidden="true">{{ item.mark }}</span>
          <span>
            <strong>{{ item.label }}</strong>
            <small>{{ item.description }}</small>
          </span>
        </RouterLink>
      </nav>

      <section v-if="authStore.canWrite" class="admin-panel" aria-label="管理后台导航">
        <p>管理后台</p>
        <RouterLink class="admin-panel__link" to="/admin/import-review">JSON 审核中心</RouterLink>
        <RouterLink class="admin-panel__link" to="/admin/collection-review">采集审核中心</RouterLink>
        <RouterLink class="admin-panel__link" to="/admin/settings">系统设置</RouterLink>
      </section>
    </aside>

    <section class="app-shell__workspace">
      <header class="topbar">
        <div>
          <p class="topbar__eyebrow">Public read model · Beijing time</p>
          <p class="topbar__title">赛事总览与赛前决策流</p>
        </div>
        <div class="topbar__actions">
          <span class="identity-pill">{{ authStore.admin?.displayName ?? '访客' }}</span>
          <RouterLink v-if="!authStore.isAuthenticated" class="shell-button" to="/login">管理员登录</RouterLink>
          <button v-else class="shell-button shell-button--ghost" type="button" @click="handleLogout">退出</button>
        </div>
      </header>

      <ReadonlyNotice class="app-shell__notice" :can-write="authStore.canWrite" />

      <main id="app-main" class="app-shell__main" tabindex="-1" data-test="app-main">
        <slot />
      </main>
    </section>

    <MobileTabbar :items="mobileNav" />
  </div>
</template>

<script setup lang="ts">
import MobileTabbar from '@/layout/MobileTabbar.vue';
import ReadonlyNotice from '@/components/common/ReadonlyNotice.vue';
import { useAuthStore } from '@/stores/auth';
import { useRoute, useRouter } from 'vue-router';

const authStore = useAuthStore();
const route = useRoute();
const router = useRouter();

const primaryNav = [
  { label: '赛事总览', description: '公开态势与入口', to: '/', mark: '01' },
  { label: '赛前作战', description: '单场综合研判', to: '/workbench', mark: '02' },
  { label: '证据中心', description: '赛程 · 赔率 · 舆情 · 画像', to: '/evidence/matches', mark: '03' },
  { label: '决策复盘', description: '分析报告与复盘经验', to: '/decisions', mark: '04' },
];

const mobileNav: Array<{
  section: 'overview' | 'workbench' | 'evidence' | 'decisions' | 'more';
  label: string;
  to: string;
  mark: string;
}> = [
  { section: 'overview', label: '总览', to: '/', mark: '01' },
  { section: 'workbench', label: '赛前', to: '/workbench', mark: '02' },
  { section: 'evidence', label: '证据', to: '/evidence/matches', mark: '03' },
  { section: 'decisions', label: '决策', to: '/decisions', mark: '04' },
  { section: 'more', label: '更多', to: '/more', mark: '05' },
];

function isNavActive(to: string): boolean {
  const currentPath = route.path ?? route.fullPath ?? '/';
  if (to === '/') {
    return currentPath === '/';
  }
  if (to === '/evidence/matches') {
    return currentPath.startsWith('/evidence');
  }
  return currentPath === to;
}

async function handleLogout() {
  const redirect = route.fullPath;
  const wasAdminRoute = route.meta.requiresAdmin === true;
  authStore.logout();
  if (wasAdminRoute) {
    await router.replace({ path: '/login', query: { redirect } });
  }
}
</script>

<style scoped>
.app-shell {
  background:
    radial-gradient(circle at top left, rgba(59, 130, 246, 0.28), transparent 34rem),
    radial-gradient(circle at top right, rgba(217, 119, 6, 0.22), transparent 30rem),
    linear-gradient(135deg, var(--wc-bg) 0%, #09172b 54%, #0e1726 100%);
  color: var(--wc-text);
  display: grid;
  grid-template-columns: 292px minmax(0, 1fr);
  min-height: 100dvh;
}

.skip-link {
  background: var(--wc-accent);
  border-radius: 999px;
  color: var(--wc-on-accent);
  font-weight: 800;
  left: 14px;
  padding: 10px 14px;
  position: fixed;
  top: 10px;
  transform: translateY(-150%);
  z-index: 120;
}

.skip-link:focus {
  transform: translateY(0);
}

.app-shell__sidebar {
  border-right: 1px solid var(--wc-border);
  display: flex;
  flex-direction: column;
  gap: 22px;
  min-height: 100dvh;
  padding: 22px 18px;
  position: sticky;
  top: 0;
}

.brand,
.side-nav__item,
.admin-panel__link,
.shell-button {
  min-height: 44px;
}

.brand {
  align-items: center;
  color: var(--wc-text);
  display: flex;
  gap: 12px;
  text-decoration: none;
}

.brand:focus-visible,
.side-nav__item:focus-visible,
.admin-panel__link:focus-visible,
.shell-button:focus-visible {
  box-shadow: var(--wc-focus-ring);
  outline: none;
}

.brand__crest {
  align-items: center;
  background: linear-gradient(135deg, var(--wc-primary-strong), var(--wc-accent));
  border-radius: 18px;
  box-shadow: 0 18px 44px rgba(59, 130, 246, 0.28);
  color: #fff;
  display: inline-flex;
  font-family: var(--wc-font-mono);
  font-weight: 800;
  height: 48px;
  justify-content: center;
  width: 48px;
}

.brand strong,
.brand small {
  display: block;
}

.brand small,
.side-nav__item small,
.topbar__eyebrow {
  color: var(--wc-text-muted);
}

.side-nav {
  display: grid;
  gap: 10px;
}

.side-nav__item {
  align-items: center;
  border: 1px solid transparent;
  border-radius: var(--wc-radius-lg);
  color: var(--wc-text-muted);
  display: grid;
  gap: 12px;
  grid-template-columns: 34px 1fr;
  padding: 13px 12px;
  text-decoration: none;
  transition: background 180ms ease, border-color 180ms ease, color 180ms ease;
}

.side-nav__item:hover,
.side-nav__item.router-link-active,
.side-nav__item.router-link-exact-active {
  background: rgba(147, 197, 253, 0.1);
  border-color: rgba(147, 197, 253, 0.26);
  color: var(--wc-text);
}

.side-nav__item strong,
.side-nav__item small {
  display: block;
}

.side-nav__mark {
  color: var(--wc-warning);
  font-family: var(--wc-font-mono);
  font-size: 12px;
  font-weight: 800;
}

.admin-panel {
  background: rgba(15, 23, 42, 0.64);
  border: 1px solid var(--wc-border);
  border-radius: var(--wc-radius-lg);
  margin-top: auto;
  padding: 14px;
}

.admin-panel p {
  color: var(--wc-warning);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  margin: 0 0 10px;
}

.admin-panel__link {
  align-items: center;
  border-radius: 12px;
  color: var(--wc-text-muted);
  display: flex;
  padding: 10px 12px;
  text-decoration: none;
}

.admin-panel__link:hover {
  background: rgba(217, 119, 6, 0.14);
  color: var(--wc-text);
}

.app-shell__workspace {
  min-width: 0;
  padding: 22px clamp(14px, 3vw, 34px) 34px;
}

.topbar {
  align-items: center;
  display: flex;
  gap: 18px;
  justify-content: space-between;
  margin: 0 auto 18px;
  max-width: 1480px;
}

.topbar__title {
  font-family: var(--wc-font-display);
  font-size: clamp(24px, 3vw, 42px);
  line-height: 1.05;
  margin: 0;
}

.topbar__eyebrow {
  font-family: var(--wc-font-mono);
  font-size: 12px;
  letter-spacing: 0.08em;
  margin: 0 0 8px;
  text-transform: uppercase;
}

.topbar__actions {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

.identity-pill,
.shell-button {
  align-items: center;
  border-radius: 999px;
  display: inline-flex;
  font-weight: 800;
  justify-content: center;
}

.identity-pill {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid var(--wc-border);
  color: var(--wc-text-muted);
  min-height: 38px;
  padding: 0 14px;
}

.shell-button {
  background: var(--wc-accent);
  border: 0;
  color: var(--wc-on-accent);
  cursor: pointer;
  padding: 0 16px;
  text-decoration: none;
}

.shell-button--ghost {
  background: rgba(255, 255, 255, 0.08);
  color: var(--wc-text);
}

.app-shell__notice,
.app-shell__main {
  margin-inline: auto;
  max-width: 1480px;
}

.app-shell__notice {
  margin-bottom: 18px;
}

.app-shell__main {
  min-width: 0;
}

@media (max-width: 1023px) {
  .app-shell {
    grid-template-columns: 1fr;
  }

  .app-shell__sidebar {
    display: none;
  }

  .app-shell__workspace {
    padding-bottom: calc(var(--mobile-tabbar-height) + env(safe-area-inset-bottom) + 24px);
  }
}

@media (max-width: 767px) {
  .app-shell__workspace {
    padding: 14px 12px calc(var(--mobile-tabbar-height) + env(safe-area-inset-bottom) + 18px);
  }

  .topbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .topbar__actions {
    justify-content: flex-start;
    width: 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .side-nav__item,
  .shell-button {
    transition: none;
  }
}
</style>
