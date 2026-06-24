<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import {
  getPublicTeamProfile,
  listPublicTeamProfiles,
  type PublicTeamProfileDetail,
  type PublicTeamProfileSummary,
} from '@/api/profiles';
import {
  enumLabel,
  factTypeLabel,
  lineupRoleLabel,
  matchStatusLabel,
  positionLabel,
  readablePublicText,
} from '@/utils/display-labels';

const loading = ref(false);
const detailLoading = ref(false);
const error = ref('');
const detailError = ref('');
const teams = ref<PublicTeamProfileSummary[]>([]);
const selected = ref<PublicTeamProfileDetail | null>(null);
const selectedTeamId = ref<number | null>(null);

const stats = computed(() => ({
  teams: teams.value.length,
  facts: teams.value.reduce((sum, team) => sum + team.factCount, 0),
  players: teams.value.reduce((sum, team) => sum + team.playerCount, 0),
  evidence: selected.value?.evidenceCount ?? 0,
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
    const response = await listPublicTeamProfiles();
    teams.value = response.data;
    const nextTeam = selectedTeamId.value
      ? teams.value.find((team) => team.id === selectedTeamId.value) ?? teams.value[0]
      : teams.value[0];
    if (nextTeam) {
      await openTeam(nextTeam);
    } else {
      selected.value = null;
      selectedTeamId.value = null;
    }
  } catch (cause) {
    teams.value = [];
    selected.value = null;
    selectedTeamId.value = null;
    error.value = cause instanceof Error ? cause.message : '无法读取公开球队画像数据。';
  } finally {
    loading.value = false;
  }
}

async function openTeam(team: PublicTeamProfileSummary) {
  selectedTeamId.value = team.id;
  detailLoading.value = true;
  detailError.value = '';
  try {
    const response = await getPublicTeamProfile(team.id);
    selected.value = response.data;
  } catch (cause) {
    selected.value = null;
    detailError.value = cause instanceof Error ? cause.message : '无法读取公开球队详情。';
  } finally {
    detailLoading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <section class="page-shell evidence-page profile-page" aria-labelledby="team-profile-title">
    <section class="page-content profile-page__content">
      <header class="evidence-hero">
        <div>
          <p class="eyebrow">证据 · 球队</p>
          <h1 id="team-profile-title">球队画像中心</h1>
          <p>公开呈现球队风格、人员、阵容、进球时间点、外部因素和历史比赛，用卡片帮助移动端快速定位证据。</p>
        </div>
        <button class="action-button" type="button" :disabled="loading" @click="load">
          {{ loading ? '刷新中' : '刷新公开数据' }}
        </button>
      </header>

      <section class="stat-grid" aria-label="球队画像统计">
        <article class="stat-card"><span>球队</span><strong>{{ stats.teams }}</strong><small>公开画像</small></article>
        <article class="stat-card"><span>球员</span><strong>{{ stats.players }}</strong><small>名单覆盖</small></article>
        <article class="stat-card"><span>事实</span><strong>{{ stats.facts }}</strong><small>画像事实</small></article>
        <article class="stat-card"><span>证据</span><strong>{{ stats.evidence }}</strong><small>当前球队链路</small></article>
      </section>

      <div v-if="error" class="alert-panel" role="alert">{{ error }}</div>

      <section class="evidence-grid">
        <aside class="side-panel" aria-label="球队列表">
          <div class="panel-heading">
            <div><p class="eyebrow">球队</p><h2>球队列表</h2></div>
            <span class="count-pill">{{ teams.length }}</span>
          </div>
          <p v-if="loading && !teams.length" class="empty-copy">正在加载公开球队...</p>
          <p v-else-if="!teams.length" class="empty-copy">暂无公开球队画像。</p>
          <button
            v-for="team in teams"
            v-else
            :key="team.id"
            class="list-card"
            :class="{ 'list-card--active': team.id === selectedTeamId }"
            type="button"
            @click="openTeam(team)"
          >
            <span>{{ team.fifaCode || team.teamKey }} · {{ team.countryRegion || '地区待同步' }}</span>
            <strong>{{ team.displayName }}</strong>
            <small>{{ team.styleTags || '风格待同步' }}</small>
            <small>{{ team.playerCount }} 名球员 · {{ team.factCount }} 条事实</small>
          </button>
        </aside>

        <article class="detail-panel">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">球队详情</p>
              <h2>{{ selected?.team.displayName || '球队详情' }}</h2>
            </div>
            <span v-if="selected" class="status-pill">证据 {{ selected.evidenceCount }} · 冲突 {{ selected.conflictCount }}</span>
          </div>

          <div v-if="detailError" class="alert-panel" role="alert">{{ detailError }}</div>
          <p v-else-if="detailLoading && !selected" class="empty-copy">正在加载球队详情...</p>
          <p v-else-if="!selected" class="empty-copy">请选择左侧球队。</p>

          <template v-else>
            <section class="summary-grid" aria-label="球队摘要">
              <div><span>FIFA</span><strong>{{ selected.team.fifaCode || '-' }}</strong></div>
              <div><span>地区</span><strong>{{ selected.team.countryRegion || '-' }}</strong></div>
              <div><span>攻击画像</span><strong>{{ selected.team.attackProfile || '-' }}</strong></div>
              <div><span>防守画像</span><strong>{{ selected.team.defenseProfile || '-' }}</strong></div>
            </section>

            <section class="info-card">
              <p class="eyebrow">公开情绪</p>
              <h3>公开情绪与风格</h3>
              <p>{{ selected.team.publicSentiment || '暂无公开情绪摘要' }}</p>
              <small>{{ selected.team.styleTags || '暂无风格标签' }}</small>
            </section>

            <section class="card-grid" aria-label="球队画像内容">
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

              <article class="info-card">
                <p class="eyebrow">球员</p>
                <h3>球员名单</h3>
                <div v-for="player in selected.players" :key="player.id" class="stack-item">
                  <strong>{{ player.displayName }} <small>#{{ player.shirtNumber ?? '-' }}</small></strong>
                  <span>{{ positionLabel(player.position) }} · {{ enumLabel('playerStatus', player.status, '状态待同步') }}</span>
                  <small>伤病 {{ player.injuryStatus || '-' }} · 牌面 {{ player.cardStatus || '-' }}</small>
                </div>
                <p v-if="!selected.players.length" class="empty-copy">暂无球员名单。</p>
              </article>

              <article class="info-card">
                <p class="eyebrow">阵容</p>
                <h3>上阵人员 / 首发阵容</h3>
                <div v-for="lineup in selected.lineups" :key="`${lineup.matchId}-${lineup.playerName}`" class="stack-item">
                  <strong>{{ lineup.playerName }} <small>{{ positionLabel(lineup.position) }}</small></strong>
                  <span>{{ lineup.matchName }} · {{ lineupRoleLabel(lineup.role) }}</span>
                  <small>{{ lineup.starter ? '首发' : '替补/上阵' }}</small>
                </div>
                <p v-if="!selected.lineups.length" class="empty-copy">暂无阵容数据。</p>
              </article>

              <article class="info-card">
                <p class="eyebrow">进球</p>
                <h3>历史进球时间点</h3>
                <div v-for="pattern in selected.scoringPatterns" :key="pattern.matchId" class="stack-item">
                  <strong>{{ pattern.matchName }} <small>{{ pattern.goalsFor ?? '-' }} / {{ pattern.goalsAgainst ?? '-' }}</small></strong>
                  <span>首球 {{ pattern.firstGoalMinute ?? '-' }} 分钟</span>
                  <small>{{ pattern.scoringMinutes || '进球分钟待同步' }}</small>
                </div>
                <p v-if="!selected.scoringPatterns.length" class="empty-copy">暂无进球时间数据。</p>
              </article>

              <article class="info-card">
                <p class="eyebrow">外部</p>
                <h3>外部因素</h3>
                <div v-for="factor in selected.externalFactors" :key="factor.matchId" class="stack-item">
                  <strong>{{ factor.matchName }}</strong>
                  <span>{{ readablePublicText(factor.externalFactors) }}</span>
                  <small>{{ formatDateTime(factor.matchday) }}</small>
                </div>
                <p v-if="!selected.externalFactors.length" class="empty-copy">暂无外部因素。</p>
              </article>

              <article class="info-card">
                <p class="eyebrow">历史</p>
                <h3>历史比赛</h3>
                <div v-for="match in selected.matchHistory" :key="match.matchId" class="stack-item">
                  <strong>{{ match.matchName }} <small>{{ match.stage || '阶段待定' }}</small></strong>
                  <span>{{ match.competition || '赛事待定' }} · {{ match.venue || '场地待同步' }} · {{ matchStatusLabel(match.resultStatus) }}</span>
                  <small>进失球 {{ match.goalsFor ?? '-' }} / {{ match.goalsAgainst ?? '-' }} · {{ match.scoringMinutes || '分钟待同步' }}</small>
                </div>
                <p v-if="!selected.matchHistory.length" class="empty-copy">暂无历史比赛。</p>
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
.evidence-hero p:not(.eyebrow), .empty-copy, .list-card span, .list-card small, .stack-item span, .stack-item small, .summary-grid span, .info-card p {
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
