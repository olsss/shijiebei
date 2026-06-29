<script setup lang="ts">
import { computed } from 'vue';
import { clampPercent } from '@/utils/football-visuals';

const props = withDefaults(
  defineProps<{
    label: string;
    value?: number | null;
    max?: number;
    unit?: string;
    caption?: string;
    tone?: 'success' | 'warning' | 'danger' | 'info' | 'accent' | string;
  }>(),
  {
    value: 0,
    max: 100,
    unit: '',
    caption: '',
    tone: 'info',
  },
);

const percent = computed(() => clampPercent(props.value, props.max));
const displayValue = computed(() => {
  if (props.value == null) {
    return '-';
  }
  const value = Number(props.value).toFixed(1).replace(/\.0$/, '');
  return `${value}${props.unit}`;
});
const fillStyle = computed(() => ({
  width: `${percent.value}%`,
  minWidth: percent.value > 0 ? '3px' : '0',
}));
</script>

<template>
  <div class="metric-bar" :class="`metric-bar--${tone}`" :aria-label="`${label} ${displayValue}`">
    <div class="metric-bar__top">
      <span>{{ label }}</span>
      <strong>{{ displayValue }}</strong>
    </div>
    <div class="metric-bar__track" aria-hidden="true">
      <span class="metric-bar__fill" :style="fillStyle"></span>
    </div>
    <small v-if="caption">{{ caption }}</small>
  </div>
</template>

<style scoped>
.metric-bar {
  display: grid;
  gap: 7px;
  min-width: 0;
}

.metric-bar__top {
  align-items: baseline;
  display: flex;
  gap: 10px;
  justify-content: space-between;
}

.metric-bar__top span,
.metric-bar small {
  color: var(--wc-text-muted);
  font-size: 12px;
}

.metric-bar__top strong {
  color: var(--wc-text);
  font-family: var(--wc-font-mono);
  font-size: 14px;
}

.metric-bar__track {
  background: rgba(15, 23, 42, 0.82);
  border: 1px solid rgba(148, 163, 184, 0.14);
  border-radius: 999px;
  height: 9px;
  overflow: hidden;
}

.metric-bar__fill {
  background: linear-gradient(90deg, var(--wc-primary), var(--wc-cyan));
  border-radius: inherit;
  display: block;
  height: 100%;
}

.metric-bar--success .metric-bar__fill { background: linear-gradient(90deg, var(--wc-success), #bbf7d0); }
.metric-bar--warning .metric-bar__fill { background: linear-gradient(90deg, var(--wc-warning), #fef08a); }
.metric-bar--danger .metric-bar__fill { background: linear-gradient(90deg, var(--wc-danger), #fecaca); }
.metric-bar--accent .metric-bar__fill { background: linear-gradient(90deg, var(--wc-accent), var(--wc-warning)); }
</style>
