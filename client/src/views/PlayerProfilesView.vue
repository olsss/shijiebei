<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import {
  getPublicPlayerProfile,
  listPublicPlayerProfiles,
  type PublicPlayerProfileDetail,
  type PublicPlayerProfileSummary,
} from '@/api/profiles';
import { enumLabel, factTypeLabel, positionLabel, readablePublicText } from '@/utils/display-labels';

const loading = ref(false);
const detailLoading = ref(false);
const error = ref('');
const detailError = ref('');
const players = ref<PublicPlayerProfileSummary[]>([]);
const selected = ref<PublicPlayerProfileDetail | null>(null);
const selectedPlayerId = ref<number | null>(null);

const stats = computed(() => ({
  players: players.value.length,
  facts: players.value.reduce((sum, player) => sum + player.factCount, 0),
  teams: new Set(players.value.map((player) => player.teamName).filter(Boolean)).size,
  watch: players.value.filter((player) => player.injuryStatus && !['无', '健康'].includes(player.injuryStatus)).length,
}));

function reliabilityLabel(value?: number): string {
  return value == null ? '未评分' : `${Number(value).toFixed(1).replace(/\.0$/, '')} / 10`;
}

function formatDateTime(value?: string): string {
  return value ? value.replace('T', ' ').slice(0, 16) : '待同步';
}

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const response = await listPublicPlayerProfiles();
    players.value = response.data;
    const nextPlayer = selectedPlayerId.value
      ? players.value.find((player) => player.id === selectedPlayerId.value) ?? players.value[0]
      : players.value[0];
    if (nextPlayer) {
      await openPlayer(nextPlayer);
    } else {
      selected.value = null;
      selectedPlayerId.value = null;
    }
  } catch (cause) {
    players.value = [];
    selected.value = null;
    selectedPlayerId.value = null;
    error.value = cause instanceof Error ? cause.message : '无法读取公开球员画像数据。';
  } finally {
    loading.value = false;
  }
}

async function openPlayer(player: PublicPlayerProfileSummary) {
  selectedPlayerId.value = player.id;
  detailLoading.value = true;
  detailError.value = '';
  try {
    const response = await getPublicPlayerProfile(player.id);
    selected.value = response.data;
  } catch (cause) {
    selected.value = null;
    detailError.value = cause instanceof Error ? cause.message : '无法读取公开球员详情。';
  } finally {
    detailLoading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <section class="page-shell evidence-page profile-page" aria-labelledby="player-profile-title">
    <section class="page-content profile-page__content">
      <header class="evidence-hero">
        <div>
          <p class="eyebrow">证据 · 球员</p>
          <h1 id="player-profile-title">球员画像中心</h1>
          <p>公开展示球员基础状态、球队归属、伤停牌面、训练与表现事实，帮助移动端快速读取关键证据。</p>
        </div>
        <button class="action-button" type="button" :disabled="loading" @click="load">
          {{ loading ? '刷新中' : '刷新公开数据' }}
        </button>
      </header>

      <section class="stat-grid" aria-label="球员画像统计">
        <article class="stat-card"><span>球员</span><strong>{{ stats.players }}</strong><small>公开画像</small></article>
        <article class="stat-card"><span>球队</span><strong>{{ stats.teams }}</strong><small>所属队伍</small></article>
        <article class="stat-card"><span>事实</span><strong>{{ stats.facts }}</strong><small>画像事实</small></article>
        <article class="stat-card"><span>关注</span><strong>{{ stats.watch }}</strong><small>伤停或状态提示</small></article>
      </section>

      <div v-if="error" class="alert-panel" role="alert">{{ error }}</div>

      <section class="evidence-grid">
        <aside class="side-panel" aria-label="球员列表">
          <div class="panel-heading">
            <div><p class="eyebrow">球员</p><h2>球员列表</h2></div>
            <span class="count-pill">{{ players.length }}</span>
          </div>
          <p v-if="loading && !players.length" class="empty-copy">正在加载公开球员...</p>
          <p v-else-if="!players.length" class="empty-copy">暂无公开球员画像。</p>
          <button
            v-for="player in players"
            v-else
            :key="player.id"
            class="list-card"
            :class="{ 'list-card--active': player.id === selectedPlayerId }"
            type="button"
            @click="openPlayer(player)"
          >
            <span>{{ player.teamName || '球队待同步' }} · #{{ player.shirtNumber ?? '-' }}</span>
            <strong>{{ player.displayName }}</strong>
            <small>{{ positionLabel(player.position) }} · {{ enumLabel('playerStatus', player.status, '状态待同步') }}</small>
            <small>{{ player.factCount }} 条事实 · {{ formatDateTime(player.latestProfileUpdate) }}</small>
          </button>
        </aside>

        <article class="detail-panel">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">球员详情</p>
              <h2>{{ selected?.player.displayName || '球员详情' }}</h2>
            </div>
            <span v-if="selected" class="status-pill">{{ selected.player.teamName || '球队待同步' }}</span>
          </div>

          <div v-if="detailError" class="alert-panel" role="alert">{{ detailError }}</div>
          <p v-else-if="detailLoading && !selected" class="empty-copy">正在加载球员详情...</p>
          <p v-else-if="!selected" class="empty-copy">请选择左侧球员。</p>

          <template v-else>
            <section class="summary-grid" aria-label="球员摘要">
              <div><span>号码</span><strong>{{ selected.player.shirtNumber ?? '-' }}</strong></div>
              <div><span>位置</span><strong>{{ positionLabel(selected.player.position) }}</strong></div>
              <div><span>状态</span><strong>{{ enumLabel('playerStatus', selected.player.status, '-') }}</strong></div>
              <div><span>更衣室</span><strong>{{ selected.player.lockerRoomStatus || '-' }}</strong></div>
            </section>

            <section class="card-grid" aria-label="球员画像内容">
              <article class="info-card">
                <p class="eyebrow">可用性</p>
                <h3>可用性与牌面</h3>
                <div class="stack-item">
                  <strong>伤病状态</strong>
                  <span>{{ selected.player.injuryStatus || '待同步' }}</span>
                </div>
                <div class="stack-item">
                  <strong>红黄牌状态</strong>
                  <span>{{ selected.player.cardStatus || '待同步' }}</span>
                </div>
              </article>

              <article class="info-card">
                <p class="eyebrow">事实</p>
                <h3>画像事实</h3>
                <div v-for="fact in selected.facts" :key="fact.id" class="stack-item">
                  <strong>{{ fact.title }} <small>{{ factTypeLabel(fact.factType) }}</small></strong>
                  <span>{{ readablePublicText(fact.summary) }}</span>
                  <small>{{ fact.sourceName }} · 可信度 {{ reliabilityLabel(fact.reliabilityScore) }} · {{ formatDateTime(fact.capturedAt) }}</small>
                </div>
                <p v-if="!selected.facts.length" class="empty-copy">暂无画像事实。</p>
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
.profile-page__content { display: grid; gap: 18px; min-width: 0; }
.evidence-hero, .stat-card, .side-panel, .detail-panel, .info-card, .summary-grid div, .alert-panel {
  background: var(--wc-glass);
  border: 1px solid var(--wc-border);
  border-radius: var(--wc-radius-lg);
  color: var(--wc-text);
}
.evidence-hero {
  align-items: center;
  display: grid;
  gap: 18px;
  grid-template-columns: minmax(0, 1fr) auto;
  padding: clamp(20px, 4vw, 38px);
}
.evidence-hero h1 {
  font-family: var(--wc-font-display);
  font-size: clamp(34px, 6vw, 68px);
  line-height: 1;
  margin: 0 0 12px;
}
.evidence-hero p:not(.eyebrow), .empty-copy, .list-card span, .list-card small, .stack-item span, .stack-item small, .summary-grid span {
  color: var(--wc-text-muted);
}
.eyebrow {
  color: var(--wc-warning);
  font-family: var(--wc-font-mono);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: .08em;
  margin: 0 0 8px;
  text-transform: uppercase;
}
.action-button {
  background: var(--wc-accent);
  border: 0;
  border-radius: 999px;
  color: var(--wc-on-accent);
  cursor: pointer;
  font-weight: 800;
  min-height: 44px;
  padding: 0 16px;
}
.stat-grid, .summary-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}
.stat-card, .side-panel, .detail-panel, .info-card, .alert-panel, .summary-grid div {
  display: grid;
  gap: 12px;
  min-width: 0;
  padding: 18px;
}
.stat-card strong {
  color: var(--wc-primary);
  font-family: var(--wc-font-mono);
  font-size: 36px;
}
.evidence-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: minmax(0, 340px) minmax(0, 1fr);
  min-width: 0;
}
.panel-heading {
  align-items: center;
  display: flex;
  gap: 12px;
  justify-content: space-between;
}
.panel-heading h2, .info-card h3 { margin: 0; }
.list-card, .stack-item {
  background: rgba(15, 23, 42, .5);
  border: 1px solid rgba(147, 197, 253, .18);
  border-radius: var(--wc-radius-md);
  color: var(--wc-text);
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 14px;
  text-align: left;
}
.list-card {
  cursor: pointer;
  transition: border-color 180ms ease, transform 180ms ease;
}
.list-card--active { border-color: rgba(217, 119, 6, .62); }
.count-pill, .status-pill {
  background: rgba(147, 197, 253, .12);
  border-radius: 999px;
  color: var(--wc-primary);
  font-family: var(--wc-font-mono);
  font-size: 12px;
  font-weight: 800;
  padding: 6px 9px;
}
.card-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  min-width: 0;
}
@media (max-width: 1024px) {
  .evidence-hero, .evidence-grid, .summary-grid { grid-template-columns: 1fr; }
  .stat-grid, .card-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 640px) {
  .evidence-hero, .stat-grid, .evidence-grid, .summary-grid, .card-grid { grid-template-columns: 1fr; }
  .panel-heading { align-items: stretch; flex-direction: column; }
  .action-button { width: 100%; }
}
@media (prefers-reduced-motion: reduce) {
  .list-card { transition: none; }
}
</style>
