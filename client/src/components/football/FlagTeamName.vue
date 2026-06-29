<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { flagEmojiForFifaCode, normalizeTeamVisual, type TeamVisual } from '@/utils/football-visuals';

const props = withDefaults(
  defineProps<{
    team?: TeamVisual | null;
    side?: string;
    compact?: boolean;
    winner?: boolean;
  }>(),
  {
    team: null,
    side: '',
    compact: false,
    winner: false,
  },
);

const flagVisible = ref(true);
const normalizedTeam = computed(() => normalizeTeamVisual(props.team));
const flagUrl = computed(() => normalizedTeam.value.flagUrl);
const flagEmoji = computed(() => flagEmojiForFifaCode(normalizedTeam.value.countryIso2 || normalizedTeam.value.fifaCode));
const fallbackCode = computed(() => normalizedTeam.value.fifaCode || (props.side ? props.side.slice(0, 2).toUpperCase() : '队'));

watch(flagUrl, () => {
  flagVisible.value = true;
});

function hideBrokenFlag() {
  flagVisible.value = false;
}
</script>

<template>
  <span class="flag-team" :class="{ 'flag-team--compact': compact, 'flag-team--winner': winner }">
    <span class="flag-team__media" aria-hidden="true">
      <span v-if="flagEmoji" class="flag-team__emoji">{{ flagEmoji }}</span>
      <span v-else class="flag-team__fallback">{{ fallbackCode }}</span>
      <img
        v-if="flagUrl && flagVisible"
        class="flag-team__flag"
        :src="flagUrl"
        :alt="`${normalizedTeam.teamName}国旗`"
        loading="lazy"
        @error="hideBrokenFlag"
      />
    </span>
    <span class="flag-team__copy">
      <strong>{{ normalizedTeam.teamName }}</strong>
      <small v-if="!compact">{{ normalizedTeam.fifaCode || normalizedTeam.countryRegion || '国家代码待同步' }}</small>
    </span>
  </span>
</template>

<style scoped>
.flag-team {
  align-items: center;
  display: inline-flex;
  gap: 10px;
  min-width: 0;
}

.flag-team__media {
  align-items: center;
  background: rgba(15, 23, 42, 0.72);
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 12px;
  display: inline-flex;
  flex: 0 0 auto;
  height: 34px;
  justify-content: center;
  overflow: hidden;
  position: relative;
  width: 46px;
}

.flag-team__flag {
  inset: 0;
  height: 100%;
  object-fit: cover;
  position: absolute;
  width: 100%;
}

.flag-team__emoji {
  font-size: 23px;
  line-height: 1;
}

.flag-team__fallback {
  color: var(--wc-warning);
  font-family: var(--wc-font-mono);
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.04em;
}

.flag-team__copy {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.flag-team__copy strong {
  color: var(--wc-text);
  font-size: 16px;
  line-height: 1.15;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.flag-team__copy small {
  color: var(--wc-text-muted);
  font-family: var(--wc-font-mono);
  font-size: 11px;
}

.flag-team--compact {
  gap: 7px;
}

.flag-team--compact .flag-team__media {
  border-radius: 10px;
  height: 26px;
  width: 36px;
}

.flag-team--compact .flag-team__copy strong {
  font-size: 13px;
}

.flag-team--winner .flag-team__media {
  border-color: rgba(251, 191, 36, 0.66);
  box-shadow: 0 0 0 2px rgba(251, 191, 36, 0.1);
}
</style>
