<template>
  <section class="page-shell more-page" aria-labelledby="more-title">
    <section class="page-content more-page__content">
      <div class="more-page__hero">
        <p>更多入口</p>
        <h1 id="more-title">全部功能入口</h1>
      </div>

      <section class="more-entry-structure" data-test="more-entry-structure" aria-label="入口结构总览" tabindex="0">
        <div class="more-page__section-title">
          <p class="more-page__eyebrow">入口结构</p>
          <h2>公开功能覆盖</h2>
        </div>
        <div class="more-entry-structure__rings" tabindex="0" aria-label="入口结构环形图">
          <CoverageDonut
            v-for="ring in entryStructureRings"
            :key="ring.label"
            :label="ring.label"
            :value="ring.value"
            :max="ring.max"
            :unit="ring.unit"
            :caption="ring.caption"
            :tone="ring.tone"
            size="compact"
          />
        </div>
      </section>

      <div class="more-page__section-title">
        <p class="more-page__eyebrow">细分入口</p>
        <h2>公开数据与管理入口</h2>
      </div>

      <div class="more-page__grid" tabindex="0" aria-label="全部功能入口列表">
        <RouterLink v-for="entry in publicEntries" :key="entry.to" class="more-card" :to="entry.to">
          <small class="more-card__group">{{ entry.group }}</small>
          <strong>{{ entry.title }}</strong>
          <span>{{ entry.description }}</span>
        </RouterLink>
        <RouterLink
          v-for="entry in visibleAdminEntries"
          :key="entry.to"
          class="more-card more-card--admin"
          :to="entry.to"
        >
          <small class="more-card__group">{{ entry.group }}</small>
          <strong>{{ entry.title }}</strong>
          <span>{{ entry.description }}</span>
        </RouterLink>
        <div v-if="!authStore.canWrite" class="more-card more-card--readonly" role="note">
          <small class="more-card__group">管理入口</small>
          <strong>管理员入口已隐藏</strong>
          <span>数据审核、系统设置和入库操作仅管理员登录后显示。</span>
        </div>
      </div>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import CoverageDonut from '@/components/football/CoverageDonut.vue';
import { useAuthStore } from '@/stores/auth';

type MoreEntry = {
  title: string;
  description: string;
  group: string;
  to: string;
};

type EntryRing = {
  label: string;
  value: number;
  max: number;
  unit: string;
  caption: string;
  tone: 'success' | 'warning' | 'danger' | 'info' | 'accent';
};

const authStore = useAuthStore();

const publicEntries: MoreEntry[] = [
  { title: '赛事总览', group: '总览', to: '/', description: '查看公开态势、焦点比赛、比分状态和资料准备度。' },
  { title: '赛前作战', group: '赛前', to: '/workbench', description: '查看单场资料准备度、风险压力、市场快照和证据覆盖。' },
  { title: '证据中心总览', group: '证据', to: '/evidence', description: '查看比赛、赔率、舆情、球队和球员资料覆盖。' },
  { title: '比赛中心', group: '比赛', to: '/evidence/matches', description: '展示对阵、国旗、比分/待开球、胜负对比和证据质量。' },
  { title: '赔率与市场', group: '市场', to: '/evidence/odds', description: '查看已批准的市场快照、隐含概率和市场倾向，只做公开解释。' },
  { title: '舆情与外部因素', group: '外因', to: '/evidence/sentiment', description: '校验伤停、天气、裁判、赛程旅行、战意轮换和媒体球迷热度。' },
  { title: '球队画像', group: '画像', to: '/evidence/teams', description: '查看球队国旗、小组、名单、攻防画像和资料准备度。' },
  { title: '球员画像', group: '画像', to: '/evidence/players', description: '查看球员所属球队、位置、伤停、训练和比赛影响线索。' },
  { title: '决策复盘', group: '复盘', to: '/decisions', description: '查看完赛后的比分、风险命中、证据和规则沉淀。' },
];

const adminEntries: MoreEntry[] = [
  { title: '数据审核中心', group: '管理入口', to: '/admin/import-review', description: '管理员登录后进行扫描、批准、驳回和入库。' },
  { title: '采集审核中心', group: '管理入口', to: '/admin/collection-review', description: '管理员登录后批准或驳回球队/球员画像采集项。' },
  { title: '系统设置', group: '管理入口', to: '/admin/settings', description: '管理员维护系统配置与归档路径。' },
];

const visibleAdminEntries = computed(() => (authStore.canWrite ? adminEntries : []));

const entryStructureRings = computed<EntryRing[]>(() => {
  const evidenceEntries = publicEntries.filter((entry) => entry.to.startsWith('/evidence')).length;
  const matchContextEntries = publicEntries.filter((entry) =>
    ['/', '/workbench', '/evidence/matches', '/decisions'].includes(entry.to),
  ).length;
  const profileEntries = publicEntries.filter((entry) => ['/evidence/teams', '/evidence/players'].includes(entry.to)).length;
  const adminVisible = visibleAdminEntries.value.length;

  return [
    { label: '公开入口', value: publicEntries.length, max: publicEntries.length, unit: '项', caption: `${publicEntries.length} 项公开只读功能`, tone: 'success' },
    { label: '证据子页', value: evidenceEntries, max: evidenceEntries, unit: '项', caption: `${evidenceEntries} 项证据与画像入口`, tone: 'info' },
    { label: '比赛上下文', value: matchContextEntries, max: 4, unit: '项', caption: `${matchContextEntries} 项含比分、状态或复盘语境`, tone: 'accent' },
    { label: '球队球员', value: profileEntries, max: 2, unit: '项', caption: `${profileEntries} 项画像入口`, tone: 'info' },
    { label: '管理入口', value: adminVisible, max: adminEntries.length, unit: '项', caption: authStore.canWrite ? `${adminVisible} 项管理功能已显示` : '访客状态下隐藏', tone: authStore.canWrite ? 'warning' : 'danger' },
  ];
});
</script>

<style scoped>
.more-page__content {
  display: grid;
  gap: 20px;
  min-width: 0;
}

.more-page__hero,
.more-entry-structure,
.more-card {
  background: var(--wc-glass);
  border: 1px solid var(--wc-border);
  border-radius: var(--wc-radius-lg);
  color: var(--wc-text);
}

.more-page__hero {
  padding: clamp(18px, 4vw, 34px);
}

.more-entry-structure {
  display: grid;
  gap: 16px;
  max-height: min(56dvh, 420px);
  overflow: auto;
  padding: 18px;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}

.more-entry-structure__rings {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  max-height: 360px;
  min-width: 0;
  overflow: auto;
  padding-right: 4px;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}

.more-page__hero p,
.more-page__eyebrow {
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
  display: block;
  line-height: 1.7;
  max-width: 880px;
}

.more-page__section-title h2 {
  font-family: var(--wc-font-display);
  line-height: 1.12;
  margin: 0;
}

.more-page__section-title h2 {
  font-size: clamp(22px, 3vw, 32px);
}

.more-page__section-title {
  min-width: 0;
}

.more-page__grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.more-card {
  display: grid;
  gap: 8px;
  min-height: 138px;
  min-width: 0;
  padding: 18px;
  text-decoration: none;
  transition: border-color 180ms ease, transform 180ms ease;
}

.more-card:hover {
  border-color: rgba(147, 197, 253, 0.48);
  transform: translateY(-2px);
}

.more-card:focus-visible,
.more-entry-structure:focus-visible,
.more-entry-structure__rings:focus-visible,
.more-page__grid:focus-visible {
  box-shadow: var(--wc-focus-ring);
  outline: none;
}

.more-card__group {
  align-self: start;
  background: rgba(147, 197, 253, 0.1);
  border: 1px solid rgba(147, 197, 253, 0.18);
  border-radius: 999px;
  color: var(--wc-warning);
  font-family: var(--wc-font-mono);
  font-size: 12px;
  font-weight: 800;
  justify-self: start;
  letter-spacing: 0.04em;
  padding: 4px 9px;
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
  .more-page__content { gap: 14px; }
  .more-page__hero,
  .more-entry-structure,
  .more-card {
    border-radius: var(--wc-radius-md);
    padding: 12px;
  }
  .more-entry-structure { max-height: min(42dvh, 320px); }
  .more-entry-structure__rings {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    max-height: min(36dvh, 260px);
  }
  .more-entry-structure__rings :deep(.coverage-donut__copy small),
  .more-card span {
    display: none;
  }
  .more-page__grid {
    gap: 10px;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .more-card {
    min-height: 104px;
  }
  .more-card strong {
    font-size: 15px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .more-card {
    transition: none;
  }
}
</style>
