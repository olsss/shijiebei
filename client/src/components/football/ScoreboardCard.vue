<script setup lang="ts">
import { computed } from 'vue';
import FlagTeamName from './FlagTeamName.vue';
import MetricBar from './MetricBar.vue';
import {
  normalizeTeamVisual,
  scoreboardFallback,
  scoreboardMetaStatus,
  scoreTone,
  teamNameFromMatchName,
  winnerClass,
  type Scoreboard,
  type TeamVisual,
} from '@/utils/football-visuals';

const props = withDefaults(
  defineProps<{
    homeTeam?: TeamVisual | null;
    awayTeam?: TeamVisual | null;
    scoreboard?: Scoreboard | null;
    matchName?: string;
    meta?: string;
    status?: string;
    compact?: boolean;
    integrityScore?: number | null;
    riskCount?: number | null;
    evidenceCount?: number | null;
  }>(),
  {
    homeTeam: null,
    awayTeam: null,
    scoreboard: null,
    matchName: '',
    meta: '',
    status: '',
    compact: false,
    integrityScore: null,
    riskCount: null,
    evidenceCount: null,
  },
);

const normalizedHome = computed(() => normalizeTeamVisual(props.homeTeam, teamNameFromMatchName(props.matchName, 'HOME')));
const normalizedAway = computed(() => normalizeTeamVisual(props.awayTeam, teamNameFromMatchName(props.matchName, 'AWAY')));
const visualScoreboard = computed(() =>
  scoreboardFallback({ scoreboard: props.scoreboard, matchName: props.matchName, status: props.status }, props.status),
);
const winnerSide = computed(() => visualScoreboard.value.winnerSide || 'UNKNOWN');
const scoreCaption = computed(() => visualScoreboard.value.resultText || '赛果待同步');
const scoreDisplayText = computed(() => {
  const text = visualScoreboard.value.scoreDisplay || '待同步';
  if (!props.compact) {
    return text;
  }
  if (text === '比分待同步') {
    return '待同步';
  }
  if (text === '比分待校验') {
    return '待校验';
  }
  return text;
});
const isScoreStatusText = computed(() => !/[0-9０-９]/.test(scoreDisplayText.value));
const integrityTone = computed(() => scoreTone(props.integrityScore));
const riskTone = computed(() => (props.riskCount && props.riskCount > 0 ? 'warning' : 'success'));
const displayStatus = computed(() => scoreboardMetaStatus(props.status, visualScoreboard.value));
</script>

<template>
  <article class="scoreboard-card" :class="{ 'scoreboard-card--compact': compact }">
    <span v-if="matchName" class="scoreboard-card__match-name">{{ matchName }}</span>

    <div v-if="meta || displayStatus" class="scoreboard-card__meta">
      <span>{{ meta || '世界杯' }}</span>
      <strong v-if="displayStatus">{{ displayStatus }}</strong>
    </div>

    <div class="scoreboard-card__main">
      <div class="scoreboard-card__team" :class="winnerClass('HOME', winnerSide)">
        <FlagTeamName :team="normalizedHome" :compact="compact" :winner="winnerSide === 'HOME'" side="主队" />
        <small v-if="winnerSide === 'HOME'">胜</small>
      </div>

      <div
        class="scoreboard-card__score"
        :class="{
          'scoreboard-card__score--draw': winnerSide === 'DRAW',
          'scoreboard-card__score--status': isScoreStatusText,
        }"
      >
        <strong>{{ scoreDisplayText }}</strong>
        <span>{{ scoreCaption }}</span>
      </div>

      <div class="scoreboard-card__team scoreboard-card__team--away" :class="winnerClass('AWAY', winnerSide)">
        <FlagTeamName :team="normalizedAway" :compact="compact" :winner="winnerSide === 'AWAY'" side="客队" />
        <small v-if="winnerSide === 'AWAY'">胜</small>
      </div>
    </div>

    <div v-if="!compact && (integrityScore != null || riskCount != null || evidenceCount != null)" class="scoreboard-card__signals">
      <MetricBar
        v-if="integrityScore != null"
        label="资料准备度"
        :value="integrityScore"
        unit="%"
        :tone="integrityTone"
        caption="比分、证据和外因公开资料覆盖"
      />
      <span v-if="riskCount != null" class="scoreboard-card__chip" :class="`scoreboard-card__chip--${riskTone}`">风险 {{ riskCount }}</span>
      <span v-if="evidenceCount != null" class="scoreboard-card__chip">证据 {{ evidenceCount }}</span>
    </div>
  </article>
</template>

<style scoped>
.scoreboard-card {
  background:
    radial-gradient(circle at 18% 0%, rgba(96, 165, 250, 0.16), transparent 28%),
    rgba(15, 23, 42, 0.66);
  border: 1px solid rgba(147, 197, 253, 0.22);
  border-radius: var(--wc-radius-md);
  color: var(--wc-text);
  display: grid;
  gap: 13px;
  min-width: 0;
  padding: 16px;
}

.scoreboard-card__meta {
  align-items: center;
  color: var(--wc-text-muted);
  display: flex;
  font-family: var(--wc-font-mono);
  font-size: 12px;
  gap: 8px;
  justify-content: space-between;
}

.scoreboard-card__match-name {
  color: var(--wc-text-muted);
  font-family: var(--wc-font-mono);
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.scoreboard-card__meta strong {
  background: rgba(147, 197, 253, 0.12);
  border: 1px solid rgba(147, 197, 253, 0.2);
  border-radius: 999px;
  color: var(--wc-primary);
  padding: 5px 8px;
}

.scoreboard-card__main {
  align-items: center;
  display: grid;
  gap: 12px;
  grid-template-columns: minmax(88px, 1fr) minmax(104px, auto) minmax(88px, 1fr);
}

.scoreboard-card__team {
  border: 1px solid rgba(148, 163, 184, 0.12);
  border-radius: 16px;
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 11px;
}

.scoreboard-card__team :deep(.flag-team) {
  align-items: flex-start;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.scoreboard-card__team :deep(.flag-team__copy) {
  width: 100%;
}

.scoreboard-card__team :deep(.flag-team__copy strong) {
  overflow: visible;
  text-overflow: clip;
  white-space: normal;
}

.scoreboard-card__team--away {
  justify-items: end;
  text-align: right;
}

.scoreboard-card__team--away :deep(.flag-team) {
  align-items: flex-end;
}

.scoreboard-card__team--away :deep(.flag-team__copy) {
  justify-items: end;
}

.scoreboard-card__team.is-winner {
  background: rgba(251, 191, 36, 0.1);
  border-color: rgba(251, 191, 36, 0.42);
}

.scoreboard-card__team.is-loser {
  opacity: 0.72;
}

.scoreboard-card__team small {
  color: var(--wc-warning);
  font-family: var(--wc-font-mono);
  font-size: 11px;
  font-weight: 900;
}

.scoreboard-card__score {
  align-items: center;
  background: rgba(2, 6, 23, 0.58);
  border: 1px solid rgba(147, 197, 253, 0.2);
  border-radius: 18px;
  display: grid;
  justify-items: center;
  min-width: 104px;
  padding: 11px 12px;
  text-align: center;
}

.scoreboard-card__score strong {
  color: var(--wc-warning);
  font-family: var(--wc-font-display);
  font-size: clamp(28px, 4vw, 42px);
  font-variant-numeric: tabular-nums;
  line-height: 0.95;
  white-space: nowrap;
}

.scoreboard-card__score--status strong {
  font-size: clamp(23px, 3.2vw, 34px);
  letter-spacing: 0;
  line-height: 1.04;
}

.scoreboard-card__score span {
  color: var(--wc-text-muted);
  font-size: 12px;
  margin-top: 6px;
}

.scoreboard-card__score--draw strong {
  color: var(--wc-primary);
}

.scoreboard-card__signals {
  align-items: end;
  border-top: 1px solid rgba(148, 163, 184, 0.14);
  display: grid;
  gap: 10px;
  grid-template-columns: minmax(0, 1fr) auto auto;
  padding-top: 12px;
}

.scoreboard-card__chip {
  background: rgba(147, 197, 253, 0.11);
  border: 1px solid rgba(147, 197, 253, 0.2);
  border-radius: 999px;
  color: var(--wc-text);
  font-family: var(--wc-font-mono);
  font-size: 12px;
  font-weight: 800;
  padding: 7px 9px;
  white-space: nowrap;
}

.scoreboard-card__chip--success {
  background: rgba(134, 239, 172, 0.12);
  color: var(--wc-success);
}

.scoreboard-card__chip--warning {
  background: rgba(253, 230, 138, 0.12);
  color: var(--wc-warning);
}

.scoreboard-card--compact {
  border-radius: 14px;
  gap: 8px;
  padding: 10px;
}

.scoreboard-card--compact .scoreboard-card__main {
  gap: 8px;
  grid-template-columns: minmax(0, 1fr) 90px minmax(0, 1fr);
}

.scoreboard-card--compact .scoreboard-card__team {
  border: 0;
  padding: 0;
}

.scoreboard-card--compact .scoreboard-card__team :deep(.flag-team) {
  align-items: center;
  flex-direction: row;
  gap: 7px;
}

.scoreboard-card--compact .scoreboard-card__team--away :deep(.flag-team) {
  flex-direction: row-reverse;
}

.scoreboard-card--compact .scoreboard-card__team :deep(.flag-team__copy) {
  max-width: 100%;
  min-width: 0;
}

.scoreboard-card--compact .scoreboard-card__team :deep(.flag-team__copy strong) {
  display: -webkit-box;
  line-height: 1.14;
  overflow: hidden;
  overflow-wrap: anywhere;
  text-overflow: ellipsis;
  white-space: normal;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.scoreboard-card--compact .scoreboard-card__score {
  border-radius: 13px;
  min-width: 0;
  overflow: hidden;
  padding: 7px 6px;
  width: 90px;
}

.scoreboard-card--compact .scoreboard-card__score strong {
  font-size: clamp(15px, 1.3vw, 18px);
  line-height: 1.06;
  overflow-wrap: anywhere;
  white-space: normal;
}

.scoreboard-card--compact .scoreboard-card__score--status strong {
  font-size: clamp(14px, 1.18vw, 17px);
}

.scoreboard-card--compact .scoreboard-card__score span {
  display: none;
}

@media (max-width: 680px) {
  .scoreboard-card__main,
  .scoreboard-card--compact .scoreboard-card__main {
    grid-template-columns: 1fr;
  }

  .scoreboard-card__team,
  .scoreboard-card__team--away {
    justify-items: start;
    text-align: left;
  }

  .scoreboard-card__signals {
    grid-template-columns: 1fr;
  }
}
</style>
