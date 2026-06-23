<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import {
  getPublicPrematchWorkbenchMatch,
  listPublicPrematchWorkbenchMatches,
  type PublicIntegrityCheck,
  type PublicPrematchWorkbenchDetail,
  type PublicWorkbenchMatchSummary,
  type PublicWorkbenchSentimentRisk,
} from '@/api/prematchWorkbench';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const loading = ref(false);
const detailLoading = ref(false);
const error = ref('');
const detailError = ref('');
const matches = ref<PublicWorkbenchMatchSummary[]>([]);
const selected = ref<PublicPrematchWorkbenchDetail | null>(null);
const selectedMatchId = ref<number | null>(null);

const stats = computed(() => ({
  matches: matches.value.length,
  avgScore: matches.value.length
    ? Math.round(matches.value.reduce((sum, match) => sum + match.integrityScore, 0) / matches.value.length)
    : 0,
  missing: matches.value.reduce((sum, match) => sum + match.missingCount, 0),
  conflicts: matches.value.reduce((sum, match) => sum + match.conflictCount, 0),
}));

const decisionSteps = computed(() => {
  const summary = selected.value?.summary;
  return [
    {
      label: '证据完整性',
      value: summary ? `${summary.integrityScore}%` : '-',
      meta: summary ? `缺 ${summary.missingCount} · 旧 ${summary.staleCount} · 冲 ${summary.conflictCount}` : '等待选择比赛',
    },
    {
      label: '阵容与状态',
      value: summary ? `${summary.teamProfileCount}/${summary.playerProfileCount}` : '-',
      meta: '球队画像 / 球员画像',
    },
    {
      label: '赔率市场',
      value: summary?.oddsMarketCount ?? 0,
      meta: '公开赔率快照',
    },
    {
      label: '舆情外因',
      value: summary?.sentimentFactorCount ?? 0,
      meta: '风险与外部变量',
    },
    {
      label: '分析结论',
      value: summary?.analysisReportCount ?? 0,
      meta: '已批准报告摘要',
    },
  ];
});

const publicSensitivePattern =
  /\b(?:ticketNo|ticket|stakeSuggestion|stake|betPlan|rawPayload|profitLoss|profit|loss|budgetAmount|returnAmount|ROI|CLV|closingOdds|closing[_\s-]?odds)\b\s*(?::|=|：|为|是)?\s*(?:[+\-]?\d+(?:\.\d+)?%?|[^\s,;，。；、/]+)?|票号|投入|返还|盈亏|预算|下注|金额建议|原始\s*JSON/gi;

function publicText(value?: string, fallback = '暂无公开摘要。'): string {
  const text = value?.trim();
  if (!text) {
    return fallback;
  }
  return text.replace(publicSensitivePattern, '已脱敏指标');
}

function matchTitle(match?: PublicWorkbenchMatchSummary | null): string {
  if (!match) {
    return '赛前分析作战室';
  }
  if (match.homeTeamName || match.awayTeamName) {
    return `${match.homeTeamName || '主队待定'} vs ${match.awayTeamName || '客队待定'}`;
  }
  return match.matchName;
}

function formatDateTime(value?: string): string {
  if (!value) {
    return '待同步';
  }
  return value.replace('T', ' ').slice(0, 16);
}

function numberText(value?: number): string {
  if (value == null) {
    return '-';
  }
  return Number(value).toFixed(4).replace(/0+$/, '').replace(/\.$/, '');
}

function percentText(value?: number): string {
  if (value == null) {
    return '-';
  }
  return `${(Number(value) * 100).toFixed(1).replace(/\.0$/, '')}%`;
}

function scoreTone(score: number): string {
  if (score >= 90) {
    return 'tone-success';
  }
  if (score >= 70) {
    return 'tone-warning';
  }
  return 'tone-danger';
}

function statusTone(status?: string): string {
  switch (status) {
    case 'PASS':
    case 'RESOLVED':
    case 'FIT':
    case 'OPEN':
      return 'tone-success';
    case 'STALE':
    case 'PENDING':
    case 'MEDIUM':
      return 'tone-warning';
    case 'MISSING':
    case 'CONFLICT':
    case 'HIGH':
      return 'tone-danger';
    default:
      return 'tone-info';
  }
}

function riskTone(risk?: PublicWorkbenchSentimentRisk): string {
  return statusTone(risk?.riskLevel);
}

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const response = await listPublicPrematchWorkbenchMatches();
    matches.value = response.data;
    const nextMatch = selectedMatchId.value
      ? matches.value.find((match) => match.matchId === selectedMatchId.value) ?? matches.value[0]
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
    error.value = cause instanceof Error ? cause.message : '无法读取公开赛前作战室数据。';
  } finally {
    loading.value = false;
  }
}

async function openMatch(match: PublicWorkbenchMatchSummary) {
  selectedMatchId.value = match.matchId;
  detailLoading.value = true;
  detailError.value = '';
  try {
    const response = await getPublicPrematchWorkbenchMatch(match.matchId);
    selected.value = response.data;
  } catch (cause) {
    selected.value = null;
    detailError.value = cause instanceof Error ? cause.message : '无法读取公开作战室详情。';
  } finally {
    detailLoading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <section class="page-shell prematch-page" aria-labelledby="prematch-title">
    <section class="page-content prematch-page__content">
      <header class="prematch-hero">
        <div>
          <p class="eyebrow">Prematch Decision Flow</p>
          <h1 id="prematch-title">赛前分析作战室</h1>
          <p>
            以公开赛前 API 聚合完整性、阵容、赔率、舆情、证据与分析摘要。访客可只读查看；管理类动作只在 Basic 管理员登录后显示。
          </p>
        </div>
        <div class="hero-actions">
          <RouterLink class="action-link action-link--ghost" to="/">返回总览</RouterLink>
          <RouterLink v-if="authStore.canWrite" class="action-link action-link--admin" to="/admin/import-review">
            管理员审核入口
          </RouterLink>
        </div>
      </header>

      <section class="stat-grid" aria-label="赛前作战室统计">
        <article class="stat-card">
          <span>公开比赛</span>
          <strong>{{ stats.matches }}</strong>
          <small>待分析队列</small>
        </article>
        <article class="stat-card">
          <span>平均准备度</span>
          <strong>{{ stats.avgScore }}%</strong>
          <small>完整性均值</small>
        </article>
        <article class="stat-card">
          <span>缺失项</span>
          <strong>{{ stats.missing }}</strong>
          <small>需补证据</small>
        </article>
        <article class="stat-card">
          <span>冲突项</span>
          <strong>{{ stats.conflicts }}</strong>
          <small>需核对来源</small>
        </article>
      </section>

      <div v-if="error" class="alert-panel" role="alert">
        <strong>赛前作战室暂不可用</strong>
        <span>{{ error }}</span>
        <button class="action-link action-link--button" type="button" @click="load">重试</button>
      </div>

      <section class="workbench-grid">
        <aside class="match-rail" aria-label="比赛准备队列">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">Queue</p>
              <h2>比赛准备清单</h2>
            </div>
            <button class="refresh-button" type="button" :disabled="loading" @click="load">
              {{ loading ? '刷新中' : '刷新' }}
            </button>
          </div>

          <div v-if="loading && !matches.length" class="loading-card" aria-live="polite">正在加载公开赛前队列...</div>
          <p v-else-if="!matches.length" class="empty-copy">暂无公开赛前比赛。</p>
          <button
            v-for="match in matches"
            v-else
            :key="match.matchId"
            class="match-card"
            :class="{ 'match-card--active': match.matchId === selectedMatchId }"
            type="button"
            data-test="workbench-match-card"
            @click="openMatch(match)"
          >
            <span>{{ match.competition || '世界杯' }} · {{ match.stage || '待定阶段' }}</span>
            <strong>{{ matchTitle(match) }}</strong>
            <small>{{ formatDateTime(match.kickoffTime || match.matchday) }} · JC {{ match.jcCode || '待定' }}</small>
            <em :class="scoreTone(match.integrityScore)">完整性 {{ match.integrityScore }}%</em>
            <small>缺 {{ match.missingCount }} · 旧 {{ match.staleCount }} · 冲 {{ match.conflictCount }}</small>
          </button>
        </aside>

        <article class="decision-board">
          <div class="panel-heading decision-board__heading">
            <div>
              <p class="eyebrow">Selected Match</p>
              <h2>{{ matchTitle(selected?.summary) }}</h2>
            </div>
            <span v-if="selected" class="status-pill" :class="scoreTone(selected.summary.integrityScore)">
              完整性 {{ selected.summary.integrityScore }}%
            </span>
          </div>

          <div v-if="detailError" class="alert-panel" role="alert">
            <strong>详情加载失败</strong>
            <span>{{ detailError }}</span>
          </div>
          <div v-else-if="detailLoading && !selected" class="loading-card" aria-live="polite">正在加载比赛详情...</div>
          <p v-else-if="!selected" class="empty-copy">请选择一场比赛查看赛前决策流。</p>

          <template v-else>
            <section class="match-summary-card" aria-label="比赛摘要">
              <div>
                <span>赛事</span>
                <strong>{{ selected.summary.competition || '世界杯' }}</strong>
              </div>
              <div>
                <span>阶段</span>
                <strong>{{ selected.summary.stage || '待定阶段' }}</strong>
              </div>
              <div>
                <span>开球</span>
                <strong>{{ formatDateTime(selected.summary.kickoffTime) }}</strong>
              </div>
              <div>
                <span>状态</span>
                <strong>{{ selected.summary.status || '待同步' }}</strong>
              </div>
            </section>

            <section class="decision-flow" data-test="decision-flow" aria-label="赛前决策流">
              <article v-for="step in decisionSteps" :key="step.label" class="flow-step">
                <span>{{ step.label }}</span>
                <strong>{{ step.value }}</strong>
                <small>{{ step.meta }}</small>
              </article>
            </section>

            <section class="section-block" aria-labelledby="integrity-title">
              <div class="section-title">
                <p class="eyebrow">Integrity</p>
                <h3 id="integrity-title">完整性检查</h3>
              </div>
              <div class="card-grid card-grid--checks">
                <article v-for="check in selected.integrityChecks" :key="check.code" class="info-card">
                  <div class="info-card__title">
                    <strong>{{ check.label }}</strong>
                    <span class="status-pill" :class="statusTone(check.status)">{{ check.status }}</span>
                  </div>
                  <p>{{ check.message }}</p>
                  <small>证据 {{ check.evidenceCount }} · {{ check.severity }} · {{ formatDateTime(check.lastUpdatedAt) }}</small>
                </article>
              </div>
            </section>

            <section class="evidence-grid" aria-label="赛前公开证据卡片">
              <article class="info-card" data-test="team-card">
                <div class="section-title">
                  <p class="eyebrow">Teams</p>
                  <h3>球队画像</h3>
                </div>
                <div v-for="team in selected.teams" :key="team.teamId" class="stack-item">
                  <strong>{{ team.teamName }} <small>{{ team.fifaCode || '' }}</small></strong>
                  <p>{{ team.styleTags || team.attackProfile || '暂无风格标签' }}</p>
                  <span v-for="fact in team.facts" :key="fact.factId" class="fact-chip">{{ fact.title }}</span>
                </div>
              </article>

              <article class="info-card" data-test="player-card">
                <div class="section-title">
                  <p class="eyebrow">Players</p>
                  <h3>球员状态</h3>
                </div>
                <div v-for="player in selected.players" :key="player.playerId" class="stack-item">
                  <strong>{{ player.playerName }} <small>{{ player.teamName || '' }} · {{ player.position || '位置待定' }}</small></strong>
                  <p>{{ player.injuryStatus || player.status || '状态待同步' }} · {{ player.cardStatus || '纪律待同步' }}</p>
                  <span v-for="fact in player.facts" :key="fact.factId" class="fact-chip">{{ fact.title }}</span>
                </div>
              </article>

              <article class="info-card">
                <div class="section-title">
                  <p class="eyebrow">Lineup</p>
                  <h3>阵容线索</h3>
                </div>
                <div v-for="lineup in selected.lineups" :key="lineup.id" class="stack-item">
                  <strong>{{ lineup.playerName || '球员待定' }} <small>{{ lineup.teamName || '' }}</small></strong>
                  <p>{{ lineup.position || '位置待定' }} · {{ lineup.role || '角色待定' }} · {{ lineup.starter ? '首发' : '替补' }}</p>
                </div>
              </article>

              <article class="info-card" data-test="odds-card">
                <div class="section-title">
                  <p class="eyebrow">Odds</p>
                  <h3>赔率与盘口</h3>
                </div>
                <div v-for="market in selected.oddsMarkets" :key="market.marketId" class="stack-item">
                  <strong>{{ market.bookmaker }} <small>{{ market.marketCode }} · {{ market.snapshotType || '快照' }}</small></strong>
                  <p>{{ market.marketName || '玩法待定' }} · {{ market.lineValue || '无盘口线' }} · {{ formatDateTime(market.capturedAt) }}</p>
                  <span v-for="selection in market.selections" :key="selection.selectionId" class="fact-chip">
                    {{ selection.selectionName }} {{ numberText(selection.oddsValue) }} / {{ percentText(selection.impliedProbability) }}
                  </span>
                </div>
              </article>

              <article class="info-card" data-test="sentiment-card">
                <div class="section-title">
                  <p class="eyebrow">Sentiment</p>
                  <h3>舆情与外部因素</h3>
                </div>
                <div v-for="factor in selected.sentimentFactors" :key="factor.factorId" class="stack-item">
                  <strong>{{ factor.title }} <small>{{ factor.factorCategory }} · {{ factor.impactDirection || '影响待定' }}</small></strong>
                  <p>{{ factor.summary || '暂无摘要' }}</p>
                  <span v-for="risk in factor.risks" :key="risk.riskId" class="fact-chip" :class="riskTone(risk)">
                    {{ risk.title }} {{ risk.riskLevel || '' }}
                  </span>
                </div>
              </article>

              <article class="info-card">
                <div class="section-title">
                  <p class="eyebrow">Evidence</p>
                  <h3>多源证据</h3>
                </div>
                <div v-for="item in selected.evidence" :key="item.evidenceId" class="stack-item">
                  <strong>{{ item.sourceName }} <small>{{ item.sourceType }}</small></strong>
                  <p>{{ item.summary || '暂无摘要' }}</p>
                  <small>可信度 {{ numberText(item.reliabilityScore) }} · {{ formatDateTime(item.evidenceTime) }}</small>
                </div>
              </article>

              <article class="info-card">
                <div class="section-title">
                  <p class="eyebrow">Conflicts</p>
                  <h3>数据冲突</h3>
                </div>
                <div v-for="conflict in selected.conflicts" :key="conflict.conflictId" class="stack-item">
                  <strong>{{ conflict.conflictType }} <small>{{ conflict.fieldName || '字段待定' }}</small></strong>
                  <p>状态：{{ conflict.resolutionStatus }}</p>
                </div>
                <p v-if="!selected.conflicts.length" class="empty-copy">暂无公开冲突项。</p>
              </article>

              <article class="info-card" data-test="analysis-card">
                <div class="section-title">
                  <p class="eyebrow">Reports</p>
                  <h3>分析摘要</h3>
                </div>
                <div v-for="report in selected.analysisReports" :key="report.reportId" class="stack-item">
                  <strong>{{ report.conclusionType || '结论待定' }} <small>{{ report.confidence || '置信度待定' }}</small></strong>
                  <p>{{ publicText(report.riskSummary, '暂无风险摘要') }}</p>
                  <small>{{ publicText(report.recommendedMarkets, '推荐玩法待定') }} &middot; {{ publicText(report.dimensions, '维度待定') }}</small>
                </div>
              </article>
            </section>

            <aside class="public-boundary" aria-label="公开数据边界">
              <strong>公开边界</strong>
              <span>本页只展示已批准的赛前摘要、证据、赔率与风险信号；资金和出票明细不在公开作战室展示。</span>
            </aside>
          </template>
        </article>
      </section>
    </section>
  </section>
</template>

<style scoped>
.prematch-page {
  max-width: 100%;
  overflow-x: hidden;
}

.prematch-page__content {
  display: grid;
  gap: 18px;
  min-width: 0;
}

.prematch-hero,
.stat-card,
.match-rail,
.decision-board,
.info-card,
.match-summary-card,
.alert-panel,
.loading-card,
.public-boundary {
  background: var(--wc-glass);
  border: 1px solid var(--wc-border);
  border-radius: var(--wc-radius-lg);
  color: var(--wc-text);
}

.prematch-hero {
  align-items: stretch;
  display: grid;
  gap: 18px;
  grid-template-columns: minmax(0, 1fr) auto;
  padding: clamp(20px, 4vw, 38px);
}

.prematch-hero h1 {
  font-family: var(--wc-font-display);
  font-size: clamp(34px, 6vw, 70px);
  line-height: 0.98;
  margin: 0 0 14px;
}

.prematch-hero p:not(.eyebrow) {
  color: var(--wc-text-muted);
  font-size: clamp(16px, 2vw, 19px);
  line-height: 1.7;
  margin: 0;
  max-width: 860px;
}

.eyebrow {
  color: var(--wc-warning);
  font-family: var(--wc-font-mono);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  margin: 0 0 8px;
  text-transform: uppercase;
}

.hero-actions,
.panel-heading,
.decision-board__heading,
.info-card__title {
  align-items: center;
  display: flex;
  gap: 12px;
  justify-content: space-between;
}

.hero-actions {
  align-content: start;
  flex-wrap: wrap;
  justify-content: end;
}

.action-link,
.refresh-button {
  align-items: center;
  border-radius: 999px;
  cursor: pointer;
  display: inline-flex;
  font-weight: 800;
  justify-content: center;
  min-height: 44px;
  padding: 0 16px;
  text-decoration: none;
  transition: border-color 180ms ease, opacity 180ms ease, transform 180ms ease;
}

.action-link--ghost,
.refresh-button {
  background: rgba(147, 197, 253, 0.1);
  border: 1px solid rgba(147, 197, 253, 0.28);
  color: var(--wc-text);
}

.action-link--admin,
.action-link--button {
  background: var(--wc-accent);
  border: 1px solid transparent;
  color: var(--wc-on-accent);
}

.refresh-button:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

.action-link:focus-visible,
.refresh-button:focus-visible,
.match-card:focus-visible {
  box-shadow: var(--wc-focus-ring);
  outline: none;
}

.action-link:hover,
.refresh-button:hover:not(:disabled),
.match-card:hover {
  border-color: rgba(147, 197, 253, 0.5);
  transform: translateY(-2px);
}

.stat-grid,
.decision-flow,
.match-summary-card {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.stat-card {
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 18px;
}

.stat-card span,
.stat-card small,
.match-card span,
.match-card small,
.flow-step small,
.match-summary-card span,
.stack-item small,
.public-boundary span,
.empty-copy {
  color: var(--wc-text-muted);
}

.stat-card strong {
  color: var(--wc-primary);
  font-family: var(--wc-font-mono);
  font-size: clamp(30px, 5vw, 44px);
  line-height: 1;
}

.workbench-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: minmax(0, 360px) minmax(0, 1fr);
  min-width: 0;
}

.match-rail,
.decision-board,
.alert-panel,
.loading-card,
.public-boundary {
  display: grid;
  gap: 14px;
  min-width: 0;
  padding: 18px;
}

.match-rail {
  align-content: start;
}

.panel-heading h2,
.section-title h3 {
  margin: 0;
}

.match-card {
  background: rgba(15, 23, 42, 0.58);
  border: 1px solid rgba(147, 197, 253, 0.18);
  border-radius: var(--wc-radius-md);
  color: var(--wc-text);
  display: grid;
  gap: 7px;
  min-height: 132px;
  min-width: 0;
  padding: 15px;
  text-align: left;
}

.match-card--active {
  border-color: rgba(217, 119, 6, 0.56);
  box-shadow: inset 0 0 0 1px rgba(217, 119, 6, 0.22);
}

.match-card strong,
.info-card strong {
  font-size: 18px;
}

.match-card em,
.status-pill,
.fact-chip {
  border-radius: 999px;
  display: inline-flex;
  font-family: var(--wc-font-mono);
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
  justify-self: start;
  padding: 6px 9px;
}

.decision-board {
  align-content: start;
}

.decision-board__heading h2 {
  font-size: clamp(24px, 4vw, 36px);
  line-height: 1.1;
  margin: 0;
}

.match-summary-card {
  padding: 16px;
}

.match-summary-card div,
.flow-step {
  background: rgba(15, 23, 42, 0.48);
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: var(--wc-radius-sm);
  display: grid;
  gap: 5px;
  min-width: 0;
  padding: 14px;
}

.match-summary-card strong,
.flow-step strong {
  font-family: var(--wc-font-mono);
}

.decision-flow {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.flow-step span {
  color: var(--wc-text);
  font-weight: 800;
}

.flow-step strong {
  color: var(--wc-primary);
  font-size: 24px;
}

.section-block {
  display: grid;
  gap: 12px;
}

.section-title {
  display: grid;
  gap: 2px;
}

.card-grid,
.evidence-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  min-width: 0;
}

.card-grid--checks {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.info-card {
  display: grid;
  gap: 12px;
  min-width: 0;
  padding: 18px;
}

.stack-item {
  background: rgba(15, 23, 42, 0.42);
  border: 1px solid rgba(148, 163, 184, 0.14);
  border-radius: var(--wc-radius-sm);
  display: grid;
  gap: 7px;
  min-width: 0;
  padding: 13px;
}

.stack-item p,
.info-card p {
  color: var(--wc-text-muted);
  line-height: 1.6;
  margin: 0;
}

.stack-item small {
  display: block;
  font-size: 12px;
  margin-top: 3px;
}

.fact-chip {
  background: rgba(147, 197, 253, 0.11);
  border: 1px solid rgba(147, 197, 253, 0.2);
  color: var(--wc-text);
  margin: 0 6px 6px 0;
}

.tone-success {
  background: rgba(134, 239, 172, 0.14);
  color: var(--wc-success);
}

.tone-warning {
  background: rgba(253, 230, 138, 0.14);
  color: var(--wc-warning);
}

.tone-danger {
  background: rgba(252, 165, 165, 0.16);
  color: var(--wc-danger);
}

.tone-info {
  background: rgba(147, 197, 253, 0.12);
  color: var(--wc-primary);
}

.alert-panel {
  border-color: rgba(252, 165, 165, 0.32);
}

.loading-card {
  color: var(--wc-text-muted);
}

.public-boundary {
  border-color: rgba(217, 119, 6, 0.32);
}

@media (max-width: 1100px) {
  .prematch-hero,
  .workbench-grid,
  .decision-flow {
    grid-template-columns: 1fr;
  }

  .stat-grid,
  .match-summary-card {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .prematch-page__content {
    gap: 14px;
  }

  .prematch-hero,
  .stat-grid,
  .workbench-grid,
  .decision-flow,
  .match-summary-card,
  .card-grid,
  .card-grid--checks,
  .evidence-grid {
    grid-template-columns: 1fr;
  }

  .prematch-hero,
  .stat-card,
  .match-rail,
  .decision-board,
  .info-card,
  .match-summary-card,
  .public-boundary {
    border-radius: var(--wc-radius-md);
    padding: 16px;
  }

  .hero-actions,
  .panel-heading,
  .decision-board__heading,
  .info-card__title {
    align-items: stretch;
    flex-direction: column;
  }

  .action-link,
  .refresh-button {
    width: 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .action-link,
  .refresh-button,
  .match-card {
    transition: none;
  }
}
</style>
