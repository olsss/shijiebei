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
    size?: 'compact' | 'regular';
  }>(),
  {
    value: 0,
    max: 100,
    unit: '%',
    caption: '',
    tone: 'info',
    size: 'regular',
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
const ringSweep = computed(() => {
  if (percent.value <= 0) {
    return 'var(--coverage-track)';
  }
  if (percent.value >= 100) {
    return 'var(--coverage-color)';
  }
  return `conic-gradient(from -90deg, var(--coverage-color) 0 ${percent.value}%, var(--coverage-track) ${percent.value}% 100%)`;
});
const styleVars = computed(() => ({
  '--coverage-sweep': ringSweep.value,
  '--coverage-percent': `${percent.value}%`,
}));
</script>

<template>
  <div
    class="coverage-donut"
    :class="[`coverage-donut--${tone}`, `coverage-donut--${size}`]"
    :style="styleVars"
    :aria-label="`${label} ${displayValue}`"
    role="img"
  >
    <div class="coverage-donut__ring" aria-hidden="true">
      <span>{{ displayValue }}</span>
    </div>
    <div class="coverage-donut__copy">
      <strong>{{ label }}</strong>
      <span class="coverage-donut__value" aria-hidden="true">{{ displayValue }}</span>
      <small v-if="caption">{{ caption }}</small>
      <span class="coverage-donut__meter" aria-hidden="true">
        <i></i>
      </span>
    </div>
  </div>
</template>

<style scoped>
.coverage-donut {
  --coverage-color: var(--wc-primary);
  --coverage-glow: rgba(147, 197, 253, 0.18);
  --coverage-track: rgba(71, 85, 105, 0.26);
  align-items: start;
  display: grid;
  gap: 8px;
  grid-template-columns: 1fr;
  min-width: 0;
}

.coverage-donut__ring {
  --coverage-hole: rgba(2, 6, 23, 0.92);
  --coverage-ring-size: 92px;
  align-items: center;
  aspect-ratio: 1;
  background:
    radial-gradient(circle at center, var(--coverage-hole) 0 72%, transparent 73%),
    var(--coverage-sweep);
  border: 1px solid rgba(147, 197, 253, 0.18);
  border-radius: 999px;
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.04),
    0 0 0 1px rgba(2, 6, 23, 0.45),
    0 12px 24px -20px var(--coverage-glow);
  display: none;
  justify-content: center;
  position: relative;
  width: var(--coverage-ring-size);
}

.coverage-donut--compact .coverage-donut__ring {
  --coverage-ring-size: 64px;
}

.coverage-donut__ring span {
  color: var(--wc-text);
  font-family: var(--wc-font-display);
  font-size: clamp(20px, 2.1vw, 26px);
  font-variant-numeric: tabular-nums;
  font-weight: 850;
  letter-spacing: -0.035em;
  line-height: 1;
  text-shadow: 0 1px 12px rgba(2, 6, 23, 0.75);
}

.coverage-donut--compact .coverage-donut__ring span {
  font-size: 18px;
}

.coverage-donut__copy {
  display: grid;
  gap: 6px;
  grid-column: 1 / -1;
  min-width: 0;
}

.coverage-donut__copy strong {
  color: var(--wc-text);
  font-size: 16px;
  line-height: 1.25;
}

.coverage-donut__copy small {
  color: var(--wc-text-muted);
  line-height: 1.55;
}

.coverage-donut__value {
  color: var(--wc-text);
  display: block;
  font-family: var(--wc-font-display);
  font-size: 20px;
  font-variant-numeric: tabular-nums;
  font-weight: 850;
  line-height: 1;
}

.coverage-donut__meter {
  background: rgba(15, 23, 42, 0.74);
  border: 1px solid rgba(148, 163, 184, 0.13);
  border-radius: 999px;
  display: block;
  height: 7px;
  margin-top: 1px;
  overflow: hidden;
  width: 100%;
}

.coverage-donut__meter i {
  background: linear-gradient(90deg, var(--coverage-color), color-mix(in srgb, var(--coverage-color) 58%, white));
  border-radius: inherit;
  box-shadow: 0 0 14px -8px var(--coverage-color);
  display: block;
  height: 100%;
  width: var(--coverage-percent);
}

.coverage-donut--success {
  --coverage-color: var(--wc-success);
  --coverage-glow: rgba(134, 239, 172, 0.22);
}
.coverage-donut--warning {
  --coverage-color: var(--wc-warning);
  --coverage-glow: rgba(253, 230, 138, 0.22);
}
.coverage-donut--danger {
  --coverage-color: var(--wc-danger);
  --coverage-glow: rgba(252, 165, 165, 0.22);
}
.coverage-donut--accent {
  --coverage-color: var(--wc-accent);
  --coverage-glow: rgba(217, 119, 6, 0.22);
}

@media (max-width: 640px) {
  .coverage-donut__copy {
    gap: 5px;
  }

  .coverage-donut__meter {
    height: 6px;
  }
}
</style>
