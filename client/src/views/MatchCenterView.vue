<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import {
  getPublicMatchDetail,
  listPublicMatches,
  type PublicMatchDetail,
  type PublicMatchSummary,
} from '@/api/matches';
import {
  enumLabel,
  fieldNameLabel,
  lineupRoleLabel,
  positionLabel,
  readablePublicText,
  sourceTypeLabel,
} from '@/utils/display-labels';

const loading = ref(false);
const detailLoading = ref(false);
const error = ref('');
const detailError = ref('');
const matches = ref<PublicMatchSummary[]>([]);
const selected = ref<PublicMatchDetail | null>(null);
const selectedMatchId = ref<number | null>(null);

const stats = computed(() => ({
  matches: matches.value.length,
  events: matches.value.reduce((sum, match) => sum + match.eventCount, 0),
  lineups: matches.value.reduce((sum, match) => sum + match.lineupCount, 0),
  conflicts: matches.value.reduce((sum, match) => sum + match.conflictCount, 0),
}));

function matchTitle(match?: PublicMatchSummary | null): string {
  if (!match) {
    return '比赛中心';
  }
  if (match.homeTeamName || match.awayTeamName) {
    return `${match.homeTeamName || '主队待定'} vs ${match.awayTeamName || '客队待定'}`;
  }
  return match.matchName;
}

function formatDateTime(value?: string): string {
  return value ? value.replace('T', ' ').slice(0, 16) : '待同步';
}

function scoreText(value?: number): string {
  return value == null ? '-' : Number(value).toFixed(1).replace(/\.0$/, '');
}

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const response = await listPublicMatches();
    matches.value = response.data;
    const nextMatch = selectedMatchId.value
      ? matches.value.find((match) => match.id === selectedMatchId.value) ?? matches.value[0]
      : matches.value[0];
    if (nextMatch) {
      await openMatch(nextMatch);
    } else {
      selected.value = null;
      selectedMatchId.value = null;
    }
  } catch (cause) {
    matches.value = [];
    selected.value = null;
    selectedMatchId.value = null;
    error.value = cause instanceof Error ? cause.message : '无法读取公开比赛中心数据。';
  } finally {
    loading.value = false;
  }
}

async function openMatch(match: PublicMatchSummary) {
  selectedMatchId.value = match.id;
  detailLoading.value = true;
  detailError.value = '';
  try {
    const response = await getPublicMatchDetail(match.id);
    selected.value = response.data;
  } catch (cause) {
    selected.value = null;
    detailError.value = cause instanceof Error ? cause.message : '无法读取公开比赛详情。';
  } finally {
    detailLoading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <section class="page-shell evidence-page" aria-labelledby="match-center-title">
    <section class="page-content evidence-page__content">
      <header class="evidence-hero">
        <div>
          <p class="eyebrow">证据 · 比赛</p>
          <h1 id="match-center-title">比赛中心</h1>
          <p>公开展示已入库的赛程、阵容、事件、统计、证据链与冲突状态；采集底稿和冲突明细值不在公开页渲染。</p>
        </div>
        <button class="action-button" type="button" :disabled="loading" @click="load">
          {{ loading ? '刷新中' : '刷新公开数据' }}
        </button>
      </header>

      <section class="stat-grid" aria-label="比赛中心统计">
        <article class="stat-card"><span>比赛</span><strong>{{ stats.matches }}</strong><small>公开赛程</small></article>
        <article class="stat-card"><span>事件</span><strong>{{ stats.events }}</strong><small>进球 / 红黄牌等</small></article>
        <article class="stat-card"><span>阵容</span><strong>{{ stats.lineups }}</strong><small>首发与角色</small></article>
        <article class="stat-card"><span>冲突</span><strong>{{ stats.conflicts }}</strong><small>仅显示状态</small></article>
      </section>

      <div v-if="error" class="alert-panel" role="alert">{{ error }}</div>

      <section class="evidence-grid">
        <aside class="side-panel" aria-label="赛程列表">
          <div class="panel-heading">
            <div><p class="eyebrow">赛程</p><h2>赛程列表</h2></div>
          </div>
          <p v-if="loading && !matches.length" class="empty-copy">正在加载公开赛程...</p>
          <p v-else-if="!matches.length" class="empty-copy">暂无公开比赛。</p>
          <button
            v-for="match in matches"
            v-else
            :key="match.id"
            class="list-card"
            :class="{ 'list-card--active': match.id === selectedMatchId }"
            type="button"
            @click="openMatch(match)"
          >
            <span>{{ match.competition || '世界杯' }} · {{ match.stage || '待定阶段' }}</span>
            <strong>{{ matchTitle(match) }}</strong>
            <small>{{ formatDateTime(match.kickoffTime || match.matchday) }} · 竞彩 {{ match.jcCode || '待定' }}</small>
          </button>
        </aside>

        <article class="detail-panel">
          <div class="panel-heading">
            <div><p class="eyebrow">比赛详情</p><h2>{{ matchTitle(selected?.summary) }}</h2></div>
            <span v-if="selected" class="status-pill">证据 {{ selected.summary.evidenceCount }} · 冲突 {{ selected.summary.conflictCount }}</span>
          </div>

          <div v-if="detailError" class="alert-panel" role="alert">{{ detailError }}</div>
          <p v-else-if="detailLoading && !selected" class="empty-copy">正在加载比赛详情...</p>
          <p v-else-if="!selected" class="empty-copy">请选择一场比赛。</p>

          <template v-else>
            <section class="summary-grid" aria-label="比赛摘要">
              <div><span>赛事</span><strong>{{ selected.summary.competition || '世界杯' }}</strong></div>
              <div><span>阶段</span><strong>{{ selected.summary.stage || '待定阶段' }}</strong></div>
              <div><span>场地</span><strong>{{ selected.summary.venue || '待同步' }}</strong></div>
              <div><span>开球</span><strong>{{ formatDateTime(selected.summary.kickoffTime) }}</strong></div>
            </section>

            <section v-if="selected.externalFactors" class="info-card">
              <p class="eyebrow">外部因素</p>
              <h3>外部因素</h3>
              <p>{{ readablePublicText(selected.externalFactors) }}</p>
            </section>

            <section class="card-grid" aria-label="比赛证据卡片">
              <article class="info-card">
                <p class="eyebrow">阵容</p>
                <h3>阵容 / 首发</h3>
                <div v-for="lineup in selected.lineups" :key="lineup.id" class="stack-item">
                  <strong>{{ lineup.playerName || '球员待定' }} <small>{{ lineup.teamName || '' }}</small></strong>
                  <span>{{ positionLabel(lineup.position) }} · {{ lineupRoleLabel(lineup.role) }} · {{ lineup.starter ? '首发' : '替补' }}</span>
                </div>
              </article>

              <article class="info-card">
                <p class="eyebrow">事件</p>
                <h3>比赛事件</h3>
                <div v-for="event in selected.events" :key="event.id" class="stack-item">
                  <strong>{{ enumLabel('eventType', event.eventType) }} <small>{{ event.eventMinute ?? '-' }}'</small></strong>
                  <span>{{ event.teamName || '球队待定' }} · {{ event.playerName || '球员待定' }}</span>
                </div>
              </article>

              <article class="info-card">
                <p class="eyebrow">球队统计</p>
                <h3>球队统计 / 进球时间点</h3>
                <div v-for="stat in selected.teamStats" :key="stat.id" class="stack-item">
                  <strong>{{ stat.teamName || '球队待定' }} <small>{{ enumLabel('statsType', stat.statsType) }}</small></strong>
                  <span>进 {{ stat.goalsFor ?? '-' }} / 失 {{ stat.goalsAgainst ?? '-' }} · {{ stat.scoringMinutes || '进球分钟待同步' }}</span>
                </div>
              </article>

              <article class="info-card">
                <p class="eyebrow">球员统计</p>
                <h3>球员统计</h3>
                <div v-for="stat in selected.playerStats" :key="stat.id" class="stack-item">
                  <strong>{{ stat.playerName || '球员待定' }} <small>{{ stat.teamName || '' }}</small></strong>
                  <span>{{ stat.minutesPlayed ?? '-' }} 分钟 · 进球 {{ stat.goals ?? 0 }} · 助攻 {{ stat.assists ?? 0 }}</span>
                </div>
              </article>

              <article class="info-card">
                <p class="eyebrow">证据链</p>
                <h3>证据链</h3>
                <div v-for="item in selected.evidence" :key="item.id" class="stack-item">
                  <strong>{{ item.sourceName }} <small>{{ sourceTypeLabel(item.sourceType) }}</small></strong>
                  <span>{{ readablePublicText(item.summary, '暂无摘要') }}</span>
                  <small>可信度 {{ scoreText(item.reliabilityScore) }} · {{ formatDateTime(item.evidenceTime) }}</small>
                </div>
              </article>

              <article class="info-card">
                <p class="eyebrow">冲突</p>
                <h3>数据冲突</h3>
                <div v-for="conflict in selected.conflicts" :key="conflict.id" class="stack-item">
                  <strong>{{ enumLabel('conflictType', conflict.conflictType) }} <small>{{ fieldNameLabel(conflict.fieldName) }}</small></strong>
                  <span>状态：{{ enumLabel('resolutionStatus', conflict.resolutionStatus) }}</span>
                </div>
                <p v-if="!selected.conflicts.length" class="empty-copy">暂无公开冲突状态。</p>
              </article>
            </section>
          </template>
        </article>
      </section>
    </section>
  </section>
</template>

<style scoped>
.evidence-page { max-width: 100%; overflow-x: hidden; }
.evidence-page__content { display: grid; gap: 18px; min-width: 0; }
.evidence-hero, .stat-card, .side-panel, .detail-panel, .info-card, .summary-grid div, .alert-panel {
  background: var(--wc-glass); border: 1px solid var(--wc-border); border-radius: var(--wc-radius-lg); color: var(--wc-text);
}
.evidence-hero { align-items: center; display: grid; gap: 18px; grid-template-columns: minmax(0, 1fr) auto; padding: clamp(20px, 4vw, 38px); }
.evidence-hero h1 { font-family: var(--wc-font-display); font-size: clamp(34px, 6vw, 68px); line-height: 1; margin: 0 0 12px; }
.evidence-hero p:not(.eyebrow), .empty-copy, .stack-item span, .summary-grid span, small { color: var(--wc-text-muted); }
.eyebrow { color: var(--wc-warning); font-family: var(--wc-font-mono); font-size: 12px; font-weight: 800; letter-spacing: .08em; margin: 0 0 8px; text-transform: uppercase; }
.action-button { background: var(--wc-accent); border: 0; border-radius: 999px; color: var(--wc-on-accent); cursor: pointer; font-weight: 800; min-height: 44px; padding: 0 16px; }
.stat-grid, .summary-grid { display: grid; gap: 14px; grid-template-columns: repeat(4, minmax(0, 1fr)); }
.stat-card, .side-panel, .detail-panel, .info-card, .alert-panel { display: grid; gap: 12px; min-width: 0; padding: 18px; }
.stat-card strong { color: var(--wc-primary); font-family: var(--wc-font-mono); font-size: 36px; }
.evidence-grid { display: grid; gap: 16px; grid-template-columns: minmax(0, 340px) minmax(0, 1fr); min-width: 0; }
.panel-heading { align-items: center; display: flex; gap: 12px; justify-content: space-between; }
.panel-heading h2, .info-card h3 { margin: 0; }
.list-card, .stack-item { background: rgba(15, 23, 42, .5); border: 1px solid rgba(147, 197, 253, .18); border-radius: var(--wc-radius-md); color: var(--wc-text); display: grid; gap: 6px; min-width: 0; padding: 14px; text-align: left; }
.list-card { cursor: pointer; transition: border-color 180ms ease, transform 180ms ease; }
.list-card--active { border-color: rgba(217, 119, 6, .56); }
.status-pill { background: rgba(147, 197, 253, .12); border-radius: 999px; color: var(--wc-primary); font-family: var(--wc-font-mono); font-size: 12px; font-weight: 800; padding: 6px 9px; }
.summary-grid { grid-template-columns: repeat(4, minmax(0, 1fr)); }
.summary-grid div { display: grid; gap: 5px; min-width: 0; padding: 14px; }
.card-grid { display: grid; gap: 14px; grid-template-columns: repeat(2, minmax(0, 1fr)); min-width: 0; }
@media (max-width: 1024px) { .evidence-hero, .evidence-grid, .summary-grid { grid-template-columns: 1fr; } .stat-grid, .card-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 640px) { .evidence-hero, .stat-grid, .evidence-grid, .summary-grid, .card-grid { grid-template-columns: 1fr; } .panel-heading { align-items: stretch; flex-direction: column; } .action-button { width: 100%; } }
@media (prefers-reduced-motion: reduce) { .list-card { transition: none; } }
</style>
