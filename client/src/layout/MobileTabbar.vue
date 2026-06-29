<template>
  <nav class="mobile-tabbar" role="navigation" aria-label="移动端主导航" data-test="mobile-tabbar">
    <RouterLink
      v-for="item in items"
      :key="item.section"
      class="mobile-tabbar__item"
      :data-section="item.section"
      :to="item.to"
      :aria-current="isActive(item.to) ? 'page' : undefined"
    >
      <span class="mobile-tabbar__mark" aria-hidden="true">{{ item.mark }}</span>
      <span>{{ item.label }}</span>
    </RouterLink>
  </nav>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router';

type MobileSection = 'overview' | 'workbench' | 'evidence' | 'decisions' | 'more';

defineProps<{
  items: Array<{
    section: MobileSection;
    label: string;
    to: string;
    mark: string;
  }>;
}>();

const route = useRoute();

function isActive(to: string): boolean {
  const currentPath = route.path ?? route.fullPath ?? '/';
  if (to === '/') {
    return currentPath === '/';
  }
  if (to === '/evidence') {
    return currentPath.startsWith('/evidence');
  }
  return currentPath === to;
}
</script>

<style scoped>
.mobile-tabbar {
  align-items: stretch;
  backdrop-filter: blur(18px);
  background: rgba(7, 17, 31, 0.94);
  border-top: 1px solid var(--wc-border);
  bottom: 0;
  box-shadow: 0 -18px 40px rgba(0, 0, 0, 0.34);
  display: none;
  gap: 4px;
  height: calc(var(--mobile-tabbar-height) + env(safe-area-inset-bottom));
  inset-inline: 0;
  padding: 7px 8px calc(7px + env(safe-area-inset-bottom));
  position: fixed;
  z-index: 40;
}

.mobile-tabbar__item {
  align-items: center;
  border-radius: 16px;
  color: var(--wc-text-muted);
  display: flex;
  flex: 1 1 0;
  flex-direction: column;
  font-size: 11px;
  font-weight: 700;
  gap: 3px;
  justify-content: center;
  min-height: 48px;
  text-decoration: none;
  transition: background 180ms ease, color 180ms ease, transform 180ms ease;
}

.mobile-tabbar__item:focus-visible {
  box-shadow: var(--wc-focus-ring);
  outline: none;
}

.mobile-tabbar__item:hover,
.mobile-tabbar__item.router-link-active,
.mobile-tabbar__item.router-link-exact-active {
  background: rgba(217, 119, 6, 0.18);
  color: var(--wc-warning);
}

.mobile-tabbar__item:active {
  transform: scale(0.97);
}

.mobile-tabbar__mark {
  align-items: center;
  border: 1px solid rgba(184, 199, 221, 0.28);
  border-radius: 999px;
  display: inline-flex;
  font-size: 10px;
  height: 20px;
  justify-content: center;
  letter-spacing: 0.04em;
  width: 20px;
}

@media (max-width: 1023px) {
  .mobile-tabbar {
    display: flex;
  }
}

@media (prefers-reduced-motion: reduce) {
  .mobile-tabbar__item {
    transition: none;
  }
}
</style>
