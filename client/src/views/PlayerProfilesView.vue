<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import {
  getPublicPlayerProfile,
  listPublicPlayerProfiles,
  type PublicPlayerProfileDetail,
  type PublicPlayerProfileSummary,
} from '@/api/profiles';
import CoverageDonut from '@/components/football/CoverageDonut.vue';
import FlagTeamName from '@/components/football/FlagTeamName.vue';
import MetricBar from '@/components/football/MetricBar.vue';
import { enumLabel, factTypeLabel, positionLabel, readablePublicText } from '@/utils/display-labels';
import { normalizeTeamVisual, type TeamVisual } from '@/utils/football-visuals';

const loading = ref(false);
const detailLoading = ref(false);
const error = ref('');
const detailError = ref('');
const players = ref<PublicPlayerProfileSummary[]>([]);
const selected = ref<PublicPlayerProfileDetail | null>(null);
const selectedPlayerId = ref<number | null>(null);
const searchQuery = ref('');
const teamFilter = ref('');
const positionFilter = ref('');
const statusFilter = ref('');
const playerDataFilter = ref('ALL');
const currentPage = ref(1);
const pageSize = 24;

const stats = computed(() => ({
  players: players.value.length,
  facts: players.value.reduce((sum, player) => sum + player.factCount, 0),
  teams: new Set(players.value.map((player) => player.teamName).filter(Boolean)).size,
  watch: players.value.filter((player) => player.injuryStatus && !['无', '健康'].includes(player.injuryStatus)).length,
}));

const playerOverviewRings = computed(() => {
  const currentFacts = selected.value?.facts.length ?? 0;
  const totalPlayers = Math.max(1, stats.value.players);
  const materialTotal = Math.max(1, stats.value.facts, stats.value.players);
  return [
    {
      label: '公开球员',
      value: stats.value.players,
      max: totalPlayers,
      unit: '名',
      tone: stats.value.players ? 'success' : 'info',
      caption: `${stats.value.players} 名公开球员画像`,
    },
    {
      label: '覆盖球队',
      value: stats.value.teams,
      max: Math.max(1, stats.value.teams),
      unit: '队',
      tone: stats.value.teams ? 'success' : 'info',
      caption: `${stats.value.teams} 支球队有关联球员`,
    },
    {
      label: '画像事实',
      value: stats.value.facts,
      max: materialTotal,
      unit: '条',
      tone: stats.value.facts ? 'success' : 'warning',
      caption: `${stats.value.facts} 条公开球员事实`,
    },
    {
      label: '可用性关注',
      value: stats.value.watch,
      max: totalPlayers,
      unit: '名',
      tone: stats.value.watch ? 'warning' : 'success',
      caption: `${stats.value.watch} 名伤停或状态关注`,
    },
    {
      label: '当前事实',
      value: currentFacts,
      max: Math.max(4, currentFacts),
      unit: '条',
      tone: currentFacts >= 4 ? 'success' : currentFacts > 0 ? 'warning' : 'danger',
      caption: `${currentFacts} 条当前球员事实`,
    },
  ];
});

const normalizedSearch = computed(() => searchQuery.value.trim().toLowerCase());
const teamOptions = computed(() => uniqueOptions(players.value.map((player) => player.teamName)));
const positionOptions = computed(() => uniqueOptions(players.value.map((player) => player.position)));
const statusOptions = computed(() => uniqueOptions(players.value.map((player) => player.status)));
const filteredPlayers = computed(() => players.value.filter((player) => {
  if (teamFilter.value && player.teamName !== teamFilter.value) {
    return false;
  }
  if (positionFilter.value && player.position !== positionFilter.value) {
    return false;
  }
  if (statusFilter.value && player.status !== statusFilter.value) {
    return false;
  }
  if (playerDataFilter.value === 'NO_FACTS' && player.factCount > 0) {
    return false;
  }
  if (playerDataFilter.value === 'HAS_FACTS' && player.factCount === 0) {
    return false;
  }
  if (playerDataFilter.value === 'NO_AVAILABILITY' && (player.injuryStatus || player.cardStatus || player.lockerRoomStatus)) {
    return false;
  }
  if (playerDataFilter.value === 'HAS_AVAILABILITY' && !(player.injuryStatus || player.cardStatus || player.lockerRoomStatus)) {
    return false;
  }
  if (!normalizedSearch.value) {
    return true;
  }
  const searchable = [
    player.displayName,
    player.teamName,
    player.team?.teamName,
    player.team?.fifaCode,
    player.shirtNumber == null ? '' : `#${player.shirtNumber}`,
    player.shirtNumber == null ? '' : `${player.shirtNumber}`,
    player.position,
    positionLabel(player.position),
    player.status,
    enumLabel('playerStatus', player.status, ''),
    player.injuryStatus,
    player.cardStatus,
  ].filter(Boolean).join(' ').toLowerCase();
  return searchable.includes(normalizedSearch.value);
}));
const totalPages = computed(() => Math.max(1, Math.ceil(filteredPlayers.value.length / pageSize)));
const pagedPlayers = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return filteredPlayers.value.slice(start, start + pageSize);
});
const filterSummary = computed(() => {
  if (!players.value.length) {
    return '暂无球员画像';
  }
  return `已筛出 ${filteredPlayers.value.length} / ${players.value.length} 名球员`;
});
const hasActiveFilters = computed(() => Boolean(searchQuery.value || teamFilter.value || positionFilter.value || statusFilter.value || playerDataFilter.value !== 'ALL'));
const playerDataOverview = computed(() => {
  const total = Math.max(1, players.value.length);
  const withFacts = players.value.filter((player) => player.factCount > 0).length;
  const withTeam = players.value.filter((player) => player.teamName || player.team?.teamName).length;
  const withPosition = players.value.filter((player) => player.position).length;
  const withAvailability = players.value.filter((player) => player.injuryStatus || player.cardStatus || player.lockerRoomStatus).length;
  const readinessScore = Math.round(((withFacts / total) * 0.42 + (withAvailability / total) * 0.24 + (withPosition / total) * 0.18 + (withTeam / total) * 0.16) * 100);
  const missingFacts = Math.max(0, players.value.length - withFacts);
  const missingAvailability = Math.max(0, players.value.length - withAvailability);
  return {
    score: players.value.length ? readinessScore : 0,
    level: readinessScore >= 80 ? '准备充分' : readinessScore >= 55 ? '基础可读' : '偏薄弱',
    withFacts,
    withTeam,
    withPosition,
    withAvailability,
    missingFacts,
    missingAvailability,
    bars: [
      { label: '画像事实覆盖', value: withFacts, max: total, tone: withFacts / total >= 0.7 ? 'success' : 'danger', caption: `${missingFacts} 名球员缺近期事实、训练或表现来源。` },
      { label: '可用性线索', value: withAvailability, max: total, tone: withAvailability / total >= 0.7 ? 'success' : 'warning', caption: `${missingAvailability} 名球员缺伤停、牌面或更衣室状态。` },
      { label: '位置覆盖', value: withPosition, max: total, tone: withPosition / total >= 0.9 ? 'success' : 'warning', caption: '位置覆盖门将/后卫/中场/前锋。' },
      { label: '球队归属', value: withTeam, max: total, tone: withTeam / total >= 0.95 ? 'success' : 'warning', caption: '球队国旗和归属资料同步。' },
    ],
    priorityGaps: [
      missingFacts ? `主要缺口：${missingFacts} 名球员缺近期表现、训练、伤停恢复和新闻事实。` : '画像事实已覆盖主要球员。',
      missingAvailability ? `${missingAvailability} 名球员缺伤停、停赛、训练负荷与可用性。` : '可用性线索资料较齐。',
      '缺口包括出场分钟、xG/xA、关键传球、预计首发概率和定位球职责。',
    ],
    priorityActions: [
      {
        id: 'facts',
        title: missingFacts ? '筛缺事实球员' : '事实覆盖已可读',
        body: missingFacts ? `${missingFacts} 名球员缺近期表现、训练或新闻来源。` : '关键球员来源时效已显示。',
        filter: missingFacts ? 'NO_FACTS' : 'HAS_FACTS',
        cta: missingFacts ? '筛缺事实' : '看已有事实',
      },
      {
        id: 'availability',
        title: missingAvailability ? '筛缺可用性球员' : '可用性线索已可读',
        body: missingAvailability ? `${missingAvailability} 名球员缺伤停、停赛、牌面或训练状态。` : '需核风险球员的临场名单。',
        filter: missingAvailability ? 'NO_AVAILABILITY' : 'HAS_AVAILABILITY',
        cta: missingAvailability ? '筛可用性缺口' : '看可用性线索',
      },
      {
        id: 'team-context',
        title: '按国家队关联',
        body: '国家队、国旗、位置分布和可用性线索合并展示。',
        filter: 'TEAM_CONTEXT',
        cta: '跳到国家队',
      },
    ],
  };
});
const playerTeamContextCards = computed(() => {
  const contexts = new Map<string, {
    visual: TeamVisual;
    playerCount: number;
    factCount: number;
    availabilityCount: number;
    positionSet: Set<string>;
  }>();
  for (const player of players.value) {
    const visual = playerTeamVisual(player);
    const key = visual.fifaCode || visual.teamName || `team-${player.teamId ?? 'unknown'}`;
    const current = contexts.get(key) ?? {
      visual,
      playerCount: 0,
      factCount: 0,
      availabilityCount: 0,
      positionSet: new Set<string>(),
    };
    current.playerCount += 1;
    current.factCount += player.factCount;
    if (player.injuryStatus || player.cardStatus || player.lockerRoomStatus) {
      current.availabilityCount += 1;
    }
    if (player.position) {
      current.positionSet.add(positionLabel(player.position));
    }
    contexts.set(key, current);
  }
  return Array.from(contexts.values())
    .sort((first, second) => second.playerCount - first.playerCount || (first.visual.teamName || '').localeCompare(second.visual.teamName || '', 'zh-CN'))
    .slice(0, 6)
    .map((context) => ({
      ...context,
      missingFacts: Math.max(0, context.playerCount - context.factCount),
      positionText: Array.from(context.positionSet).slice(0, 4).join(' / ') || '位置待同步',
    }));
});
const maxTeamContextPlayers = computed(() => Math.max(1, ...playerTeamContextCards.value.map((context) => context.playerCount)));
const playerTeamContextRings = computed(() => {
  const total = Math.max(1, players.value.length);
  const teamKeys = new Set(players.value.map((player) => {
    const visual = playerTeamVisual(player);
    return visual.fifaCode || visual.teamName || `team-${player.teamId ?? 'unknown'}`;
  }));
  const teamTotal = Math.max(1, teamKeys.size);
  const withFacts = players.value.filter((player) => player.factCount > 0).length;
  const withAvailability = players.value.filter((player) => player.injuryStatus || player.cardStatus || player.lockerRoomStatus).length;
  const withPosition = players.value.filter((player) => Boolean(player.position)).length;
  const missingAvailability = Math.max(0, players.value.length - withAvailability);

  return [
    {
      label: '国家队卡片',
      value: playerTeamContextCards.value.length,
      max: teamTotal,
      unit: '队',
      tone: playerTeamContextCards.value.length >= teamTotal ? 'success' : 'info',
      caption: `${playerTeamContextCards.value.length} / ${teamTotal} 个国家队已展示`,
    },
    {
      label: '位置覆盖',
      value: withPosition,
      max: total,
      unit: '名',
      tone: withPosition / total >= 0.9 ? 'success' : 'warning',
      caption: `${withPosition} / ${players.value.length || 0} 名球员有位置`,
    },
    {
      label: '画像事实',
      value: withFacts,
      max: total,
      unit: '名',
      tone: withFacts / total >= 0.7 ? 'success' : 'warning',
      caption: `${withFacts} / ${players.value.length || 0} 名球员有事实`,
    },
    {
      label: '可用性线索',
      value: withAvailability,
      max: total,
      unit: '名',
      tone: withAvailability / total >= 0.7 ? 'success' : 'warning',
      caption: `${withAvailability} / ${players.value.length || 0} 名球员有线索`,
    },
    {
      label: '可用性缺口',
      value: missingAvailability,
      max: total,
      unit: '名',
      tone: missingAvailability ? 'danger' : 'success',
      caption: `${missingAvailability} / ${players.value.length || 0} 名球员缺伤停/牌面/训练状态`,
    },
  ];
});

const playerCoverageBars = computed(() => {
  if (!selected.value) {
    return [];
  }
  const player = selected.value.player;
  return [
    { label: '画像事实', value: selected.value.facts.length, max: Math.max(4, selected.value.facts.length), tone: selected.value.facts.length >= 4 ? 'success' : 'warning', caption: '训练、比赛、状态、新闻至少各一条' },
    { label: '伤病状态', value: player.injuryStatus ? 1 : 0, max: 1, tone: player.injuryStatus ? 'success' : 'warning', caption: '伤病状态字段' },
    { label: '红黄牌', value: player.cardStatus ? 1 : 0, max: 1, tone: player.cardStatus ? 'success' : 'warning', caption: '停赛/累计黄牌状态单独展示' },
    { label: '更衣室', value: player.lockerRoomStatus ? 1 : 0, max: 1, tone: player.lockerRoomStatus ? 'success' : 'warning', caption: '状态和士气类线索' },
  ];
});
const playerReadiness = computed(() => selected.value?.readiness ?? {
  score: 0,
  level: 'UNKNOWN',
  summary: '球员资料准备度待同步。',
  strengths: [],
  missingDimensions: ['球员资料准备度待同步'],
  nextActions: ['公开只读评分待同步'],
});
const playerReadinessRings = computed(() => {
  const detail = selected.value;
  if (!detail) {
    return [];
  }
  const player = detail.player;
  const metric = detail.latestMetric;
  const availabilitySignalCount = [
    player.injuryStatus,
    player.cardStatus,
    player.lockerRoomStatus,
    metric?.availabilityScore,
  ].filter((value) => value != null && value !== '').length;
  const identityCount = [
    player.teamName || player.team?.teamName,
    player.position,
    player.shirtNumber,
  ].filter((value) => value != null && value !== '').length;
  const metricCount = [
    metric?.minutesPlayed,
    metric?.xg,
    metric?.xa,
    metric?.expectedStartingProbability,
    metric?.availabilityScore,
    metric?.trainingLoad,
    metric?.keyPasses,
  ].filter((value) => value != null).length;
  return [
    {
      label: '资料准备度',
      value: playerReadiness.value.score,
      max: 100,
      unit: '%',
      tone: impactTone(playerReadiness.value.score, 80, 55),
      caption: enumLabel('profileReadinessLevel', playerReadiness.value.level, '待评估'),
    },
    {
      label: '事实支撑',
      value: detail.facts.length,
      max: Math.max(4, detail.facts.length),
      unit: '条',
      tone: detail.facts.length >= 4 ? 'success' : detail.facts.length > 0 ? 'warning' : 'danger',
      caption: `${detail.facts.length} 条画像事实`,
    },
    {
      label: '可用性线索',
      value: availabilitySignalCount,
      max: 4,
      unit: '项',
      tone: availabilitySignalCount >= 3 ? 'success' : availabilitySignalCount > 0 ? 'warning' : 'danger',
      caption: '伤停、牌面、更衣室、可用性评分',
    },
    {
      label: '身份上下文',
      value: identityCount,
      max: 3,
      unit: '项',
      tone: identityCount >= 3 ? 'success' : identityCount >= 2 ? 'warning' : 'danger',
      caption: '球队、位置、号码',
    },
    {
      label: '指标覆盖',
      value: metricCount,
      max: 7,
      unit: '项',
      tone: metricCount >= 5 ? 'success' : metricCount >= 2 ? 'warning' : 'danger',
      caption: '出场、xG、xA、首发概率等',
    },
  ];
});
const playerMetricRows = computed(() => {
  const metric = selected.value?.latestMetric;
  if (!metric) {
    return [];
  }
  return [
    { label: '出场时间', value: metric.minutesPlayed, max: 90, tone: 'info', unit: '分', caption: '近场或周期内累计分钟' },
    { label: 'xG 预期进球', value: metric.xg, max: 2, tone: 'success', unit: '', caption: '射门机会质量' },
    { label: 'xA 预期助攻', value: metric.xa, max: 2, tone: 'accent', unit: '', caption: '创造机会质量' },
    { label: '预计首发', value: metric.expectedStartingProbability == null ? null : metric.expectedStartingProbability * 100, max: 100, tone: 'warning', unit: '%', caption: '公开预计首发概率' },
    { label: '可用性', value: metric.availabilityScore, max: 100, tone: 'success', unit: '%', caption: '综合伤停/训练/状态指标' },
    { label: '训练负荷', value: metric.trainingLoad, max: 100, tone: 'info', unit: '', caption: '训练量与恢复状态' },
  ].filter((row) => row.value != null);
});
const playerImpactRows = computed(() => {
  const detail = selected.value;
  if (!detail) {
    return [];
  }
  const metric = detail.latestMetric;
  const expectedStart = metric?.expectedStartingProbability == null ? null : metric.expectedStartingProbability * 100;
  const goalThreat = metric?.xg == null ? null : Math.min(100, metric.xg * 100);
  const chanceCreation = metric?.xa == null && metric?.keyPasses == null
    ? null
    : Math.min(100, (metric?.xa ?? 0) * 100 + (metric?.keyPasses ?? 0) * 8);
  const availability = metric?.availabilityScore ?? fallbackAvailabilityScore(detail.player);
  const factSupport = Math.min(100, detail.facts.length * 25);
  return [
    {
      label: '出场可能性',
      value: expectedStart,
      max: 100,
      tone: expectedStart == null ? 'warning' : expectedStart >= 70 ? 'success' : expectedStart >= 45 ? 'warning' : 'danger',
      unit: '%',
      caption: expectedStart == null ? '预计首发概率待同步。' : '首发或核心轮换概率。',
    },
    {
      label: '进球威胁',
      value: goalThreat,
      max: 100,
      tone: goalThreat == null ? 'warning' : goalThreat >= 60 ? 'success' : goalThreat >= 25 ? 'warning' : 'info',
      unit: '',
      caption: metric?.xg == null ? 'xG 待同步，机会质量为空。' : `xG ${metricText(metric.xg)}，射门机会质量。`,
    },
    {
      label: '创造机会',
      value: chanceCreation,
      max: 100,
      tone: chanceCreation == null ? 'warning' : chanceCreation >= 45 ? 'success' : chanceCreation >= 18 ? 'warning' : 'info',
      unit: '',
      caption: metric?.xa == null && metric?.keyPasses == null ? 'xA/关键传球待同步。' : `xA ${metricText(metric?.xa)} · 关键传球 ${metric?.keyPasses ?? '待同步'}`,
    },
    {
      label: '可用性稳定度',
      value: availability,
      max: 100,
      tone: availability == null ? 'warning' : availability >= 75 ? 'success' : availability >= 50 ? 'warning' : 'danger',
      unit: '%',
      caption: availability == null ? '缺伤停/训练可用性评分，需核健康、停赛和训练负荷。' : availabilityCaption(detail.player),
    },
    {
      label: '事实支撑',
      value: factSupport,
      max: 100,
      tone: factSupport >= 75 ? 'success' : factSupport >= 35 ? 'warning' : 'danger',
      unit: '%',
      caption: `${detail.facts.length} 条画像事实；训练、新闻和比赛事实覆盖。`,
    },
  ];
});
const playerImpactRings = computed(() => {
  const detail = selected.value;
  if (!detail) {
    return [];
  }
  const metric = detail.latestMetric;
  const expectedStart = metric?.expectedStartingProbability == null ? null : metric.expectedStartingProbability * 100;
  const goalThreat = metric?.xg == null ? null : Math.min(100, metric.xg * 100);
  const chanceCreation = metric?.xa == null && metric?.keyPasses == null
    ? null
    : Math.min(100, (metric?.xa ?? 0) * 100 + (metric?.keyPasses ?? 0) * 8);
  const availability = metric?.availabilityScore ?? fallbackAvailabilityScore(detail.player);
  return [
    {
      label: '出场概率',
      value: expectedStart,
      tone: impactTone(expectedStart, 70, 45),
      caption: expectedStart == null ? '预计首发待同步' : `预计首发 ${metricText(expectedStart, '%')}`,
    },
    {
      label: '进球威胁',
      value: goalThreat,
      tone: impactTone(goalThreat, 60, 25),
      caption: metric?.xg == null ? 'xG 待同步' : `xG ${metricText(metric.xg)}`,
    },
    {
      label: '创造机会',
      value: chanceCreation,
      tone: impactTone(chanceCreation, 45, 18),
      caption: metric?.xa == null && metric?.keyPasses == null ? 'xA / 关键传球待同步' : `xA ${metricText(metric?.xa)} · 关键传球 ${metric?.keyPasses ?? '待同步'}`,
    },
    {
      label: '可用性',
      value: availability,
      tone: impactTone(availability, 75, 50),
      caption: availability == null ? '可用性评分待同步' : availabilityCaption(detail.player),
    },
  ];
});
const playerImpactVerdict = computed(() => {
  const detail = selected.value;
  if (!detail) {
    return '当前未选中球员，影响雷达为空。';
  }
  const metric = detail.latestMetric;
  if (!metric) {
    return '这个球员还缺高阶指标；出场时间、xG/xA、预计首发和可用性为空。';
  }
  const highlights = [];
  if ((metric.expectedStartingProbability ?? 0) >= 0.7) {
    highlights.push('出场可能性较高');
  }
  if ((metric.xg ?? 0) >= 0.5) {
    highlights.push('有直接进球威胁');
  }
  if ((metric.xa ?? 0) >= 0.25 || (metric.keyPasses ?? 0) >= 3) {
    highlights.push('能创造机会');
  }
  if ((metric.availabilityScore ?? fallbackAvailabilityScore(detail.player) ?? 0) < 60) {
    highlights.push('可用性待校验');
  }
  return highlights.length
    ? `关键影响：${highlights.join('、')}。出场、进攻、创造和可用性同步展示。`
    : '当前指标没有明显突出项，最近出场、训练和角色信息缺口。';
});
const playerImpactChips = computed(() => {
  const detail = selected.value;
  if (!detail) {
    return [];
  }
  return [
    detail.latestMetric?.expectedStartingProbability == null
      ? '预计首发待同步'
      : `预计首发 ${metricText(detail.latestMetric.expectedStartingProbability * 100, '%')}`,
    detail.latestMetric?.xg == null && detail.latestMetric?.xa == null
      ? '进攻指标待同步'
      : `xG ${metricText(detail.latestMetric?.xg)} · xA ${metricText(detail.latestMetric?.xa)}`,
    hasAvailabilityRisk(detail.player)
      ? '可用性存在风险'
      : '可用性暂无高风险',
  ];
});

function reliabilityLabel(value?: number): string {
  return value == null ? '未评分' : `${Number(value).toFixed(1).replace(/\.0$/, '')} / 10`;
}

function formatDateTime(value?: string): string {
  return value ? value.replace('T', ' ').slice(0, 16) : '待同步';
}

function metricText(value?: number, unit = ''): string {
  if (value == null) {
    return '待同步';
  }
  return `${Number(value).toFixed(2).replace(/\.00$/, '').replace(/0$/, '')}${unit}`;
}

function impactTone(value: number | null | undefined, high: number, mid: number): 'success' | 'warning' | 'danger' | 'info' {
  if (value == null) {
    return 'info';
  }
  if (value >= high) {
    return 'success';
  }
  if (value >= mid) {
    return 'warning';
  }
  return 'danger';
}

function normalizedStatusText(value?: string): string {
  return (value || '').trim().toLowerCase();
}

function hasAvailabilityRisk(player?: PublicPlayerProfileSummary): boolean {
  const injury = normalizedStatusText(player?.injuryStatus);
  const card = normalizedStatusText(player?.cardStatus);
  const status = normalizedStatusText(player?.status);
  const riskyInjury = Boolean(injury && !['无', '健康', 'fit', 'available', 'normal', '正常'].includes(injury));
  const riskyCard = Boolean(card && !['无', 'none', 'clear', '正常'].includes(card));
  const riskyStatus = Boolean(status && ['doubtful', 'injured', 'suspended', '停赛', '伤停', '待定'].some((keyword) => status.includes(keyword)));
  return riskyInjury || riskyCard || riskyStatus;
}

function fallbackAvailabilityScore(player?: PublicPlayerProfileSummary): number | null {
  if (!player) {
    return null;
  }
  return hasAvailabilityRisk(player) ? 45 : (player.status || player.injuryStatus || player.cardStatus ? 78 : null);
}

function availabilityCaption(player?: PublicPlayerProfileSummary): string {
  if (hasAvailabilityRisk(player)) {
    return '存在伤停、停赛或状态疑点，可用性待校验。';
  }
  return '暂无明显伤停/牌面风险，临场名单待同步。';
}

function playerTeamVisual(player?: PublicPlayerProfileSummary | null): TeamVisual {
  return normalizeTeamVisual(player?.team ?? {
    teamId: player?.teamId ?? null,
    teamName: player?.teamName ?? null,
  });
}

function uniqueOptions(values: Array<string | undefined>): string[] {
  return Array.from(new Set(values.filter((value): value is string => Boolean(value)))).sort((first, second) => first.localeCompare(second, 'zh-CN'));
}

function resetFilters() {
  searchQuery.value = '';
  teamFilter.value = '';
  positionFilter.value = '';
  statusFilter.value = '';
  playerDataFilter.value = 'ALL';
  currentPage.value = 1;
}


function applyPlayerPriorityAction(filter: string) {
  if (filter === 'TEAM_CONTEXT') {
    document.querySelector('#player-team-context')?.scrollIntoView({ block: 'start' });
    return;
  }
  searchQuery.value = '';
  teamFilter.value = '';
  positionFilter.value = '';
  statusFilter.value = '';
  playerDataFilter.value = filter;
  currentPage.value = 1;
}

function goPage(offset: number) {
  currentPage.value = Math.min(totalPages.value, Math.max(1, currentPage.value + offset));
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
    void scrollHashIntoView();
  }
}

async function scrollHashIntoView() {
  if (window.location.hash !== '#player-team-context') {
    return;
  }
  await nextTick();
  document.querySelector('#player-team-context')?.scrollIntoView({ block: 'start' });
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

watch([searchQuery, teamFilter, positionFilter, statusFilter, playerDataFilter], () => {
  currentPage.value = 1;
});

watch(filteredPlayers, () => {
  if (currentPage.value > totalPages.value) {
    currentPage.value = totalPages.value;
  }
});

onMounted(load);
</script>

<template>
  <section class="page-shell evidence-page profile-page" aria-labelledby="player-profile-title">
    <section class="page-content profile-page__content">
      <header class="evidence-hero">
        <div>
          <p class="eyebrow">证据 · 球员</p>
          <h1 id="player-profile-title">球员画像中心</h1>
        </div>
        <button class="action-button" type="button" :disabled="loading" @click="load">
          {{ loading ? '刷新中' : '刷新公开数据' }}
        </button>
      </header>

      <section class="stat-grid" aria-label="球员画像统计">
        <article class="stat-card"><span>球员</span><strong>{{ stats.players }}</strong><small>公开画像</small></article>
        <article class="stat-card"><span>球队</span><strong>{{ stats.teams }}</strong><small>所属队伍</small></article>
        <article class="stat-card"><span>事实</span><strong>{{ stats.facts }}</strong><small>画像事实</small></article>
        <article class="stat-card"><span>关注</span><strong>{{ stats.watch }}</strong><small>伤停或状态说明</small></article>
      </section>

      <section class="player-overview-structure" data-test="player-overview-rings" aria-label="球员画像结构" tabindex="0">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">画像结构</p>
            <h2>球员画像结构</h2>
          </div>
          <span class="count-pill">{{ stats.players }} 名球员</span>
        </div>
        <div class="player-overview-rings" tabindex="0" aria-label="球员画像结构环形图">
          <CoverageDonut
            v-for="ring in playerOverviewRings"
            :key="ring.label"
            :label="ring.label"
            :value="ring.value"
            :max="ring.max"
            :unit="ring.unit"
            :tone="ring.tone"
            size="compact"
            :caption="ring.caption"
          />
        </div>
      </section>

      <section class="player-overview-board" data-test="player-data-overview" aria-label="球员资料准备度总览" tabindex="0">
        <article class="overview-score-card">
          <p class="eyebrow">重点</p>
          <h2>球员资料准备度总览</h2>
          <CoverageDonut
            label="球员资料度"
            :value="playerDataOverview.score"
            unit="%"
            :tone="playerDataOverview.score >= 80 ? 'success' : playerDataOverview.score >= 55 ? 'warning' : 'danger'"
            :caption="playerDataOverview.level"
          />
        </article>
        <div class="overview-bars" aria-label="球员数据覆盖条形图">
          <MetricBar
            v-for="bar in playerDataOverview.bars"
            :key="bar.label"
            :label="bar.label"
            :value="bar.value"
            :max="bar.max"
            :tone="bar.tone"
            :caption="bar.caption"
          />
        </div>
        <article class="overview-gap-card">
          <p class="eyebrow">数据缺口</p>
          <h3>球员数据缺口</h3>
          <div class="gap-action-list" aria-label="球员数据缺口筛选">
            <button
              v-for="action in playerDataOverview.priorityActions"
              :key="action.id"
              class="gap-action-button"
              type="button"
              @click="applyPlayerPriorityAction(action.filter)"
            >
              <span>{{ action.title }}</span>
              <small>{{ action.body }}</small>
              <strong>{{ action.cta }}</strong>
            </button>
          </div>
          <ol>
            <li v-for="gap in playerDataOverview.priorityGaps" :key="gap">{{ gap }}</li>
          </ol>
        </article>
      </section>

      <section id="player-team-context" class="player-team-context" data-test="player-team-context" aria-label="球员国家队上下文" tabindex="0">
        <div class="context-heading">
          <div>
            <p class="eyebrow">国家队上下文</p>
            <h2>国家队球员结构</h2>
          </div>
        </div>
        <div class="team-context-rings" aria-label="国家队球员结构环形图">
          <CoverageDonut
            v-for="ring in playerTeamContextRings"
            :key="ring.label"
            :label="ring.label"
            :value="ring.value"
            :max="ring.max"
            :unit="ring.unit"
            :tone="ring.tone"
            :caption="ring.caption"
            size="compact"
          />
        </div>
        <div class="team-context-grid" tabindex="0" aria-label="国家队球员结构卡片">
          <article v-for="context in playerTeamContextCards" :key="context.visual.fifaCode || context.visual.teamName || 'team-context'" class="team-context-card">
            <FlagTeamName :team="context.visual" />
            <MetricBar
              label="名单人数"
              :value="context.playerCount"
              :max="maxTeamContextPlayers"
              tone="info"
              :caption="`${context.positionText} · ${context.factCount} 条画像事实`"
            />
            <MetricBar
              label="可用性线索"
              :value="context.availabilityCount"
              :max="context.playerCount || 1"
              tone="warning"
              :caption="context.availabilityCount ? '已有伤停/牌面/更衣室线索' : '伤停、牌面和训练状态待同步'"
            />
          </article>
        </div>
      </section>

      <div v-if="error" class="alert-panel" role="alert">{{ error }}</div>

      <section class="evidence-grid">
        <aside class="side-panel" aria-label="球员列表" tabindex="0">
          <div class="panel-heading">
            <div><p class="eyebrow">球员</p><h2>球员列表</h2></div>
            <span class="count-pill">{{ filteredPlayers.length }} / {{ players.length }}</span>
          </div>
          <form class="player-filter-panel" aria-label="球员列表筛选" @submit.prevent>
            <label class="filter-field filter-field--wide">
              <span>搜索球员</span>
              <input
                v-model="searchQuery"
                aria-label="搜索球员姓名、球队或号码"
                autocomplete="off"
                placeholder="输入姓名、球队、号码"
                type="search"
              >
            </label>
            <label class="filter-field">
              <span>球队</span>
              <select v-model="teamFilter" aria-label="按球队筛选球员">
                <option value="">全部球队</option>
                <option v-for="team in teamOptions" :key="team" :value="team">{{ team }}</option>
              </select>
            </label>
            <label class="filter-field">
              <span>位置</span>
              <select v-model="positionFilter" aria-label="按位置筛选球员">
                <option value="">全部位置</option>
                <option v-for="position in positionOptions" :key="position" :value="position">{{ positionLabel(position) }}</option>
              </select>
            </label>
            <label class="filter-field">
              <span>状态</span>
              <select v-model="statusFilter" aria-label="按状态筛选球员">
                <option value="">全部状态</option>
                <option v-for="status in statusOptions" :key="status" :value="status">{{ enumLabel('playerStatus', status, status) }}</option>
              </select>
            </label>
            <label class="filter-field">
              <span>资料</span>
              <select v-model="playerDataFilter" aria-label="按球员资料缺口筛选">
                <option value="ALL">全部资料</option>
                <option value="NO_FACTS">缺画像事实</option>
                <option value="HAS_FACTS">已有画像事实</option>
                <option value="NO_AVAILABILITY">缺可用性线索</option>
                <option value="HAS_AVAILABILITY">已有可用性线索</option>
              </select>
            </label>
          </form>
          <div class="filter-result-strip" aria-live="polite">
            <span>{{ filterSummary }}</span>
            <button v-if="hasActiveFilters" type="button" @click="resetFilters">清除筛选</button>
          </div>
          <p v-if="loading && !players.length" class="empty-copy">正在加载公开球员...</p>
          <p v-else-if="!players.length" class="empty-copy">暂无公开球员画像。</p>
          <p v-else-if="!filteredPlayers.length" class="empty-copy">没有匹配的球员。</p>
          <div v-else class="player-list-scroll" tabindex="0" aria-label="筛选后的球员结果">
            <button
              v-for="player in pagedPlayers"
              :key="player.id"
              class="list-card"
              :class="{ 'list-card--active': player.id === selectedPlayerId }"
              type="button"
              @click="openPlayer(player)"
            >
              <FlagTeamName
                :team="playerTeamVisual(player)"
                compact
              />
              <strong>{{ player.displayName }}</strong>
              <small>{{ positionLabel(player.position) }} · {{ enumLabel('playerStatus', player.status, '状态待同步') }}</small>
              <small>{{ player.factCount }} 条事实 · {{ formatDateTime(player.latestProfileUpdate) }}</small>
            </button>
          </div>
          <nav v-if="filteredPlayers.length > pageSize" class="pager" aria-label="球员列表分页">
            <button type="button" :disabled="currentPage <= 1" @click="goPage(-1)">上一页</button>
            <span>第 {{ currentPage }} / {{ totalPages }} 页</span>
            <button type="button" :disabled="currentPage >= totalPages" @click="goPage(1)">下一页</button>
          </nav>
        </aside>

        <article class="detail-panel" tabindex="0">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">球员详情</p>
              <h2>{{ selected?.player.displayName || '球员详情' }}</h2>
            </div>
            <span v-if="selected" class="status-pill">{{ selected.player.teamName || '球队待同步' }}</span>
          </div>

          <div v-if="detailError" class="alert-panel" role="alert">{{ detailError }}</div>
          <p v-else-if="detailLoading && !selected" class="empty-copy">正在加载球员详情...</p>
          <p v-else-if="!selected" class="empty-copy">当前未选中球员。</p>

          <template v-else>
            <section class="player-identity-card" aria-label="球员身份与所属球队">
              <div>
                <p class="eyebrow">球员名片</p>
                <h3>{{ selected.player.displayName }}</h3>
              </div>
              <FlagTeamName
                :team="playerTeamVisual(selected.player)"
              />
            </section>

            <section class="summary-grid" aria-label="球员摘要">
              <div><span>号码</span><strong>{{ selected.player.shirtNumber ?? '-' }}</strong></div>
              <div><span>位置</span><strong>{{ positionLabel(selected.player.position) }}</strong></div>
              <div><span>状态</span><strong>{{ enumLabel('playerStatus', selected.player.status, '-') }}</strong></div>
              <div><span>更衣室</span><strong>{{ selected.player.lockerRoomStatus || '-' }}</strong></div>
            </section>

            <section class="impact-board" data-test="player-impact-board" aria-label="关键球员影响雷达" tabindex="0">
              <article class="impact-summary-card">
                <p class="eyebrow">关键影响</p>
                <h3>球员影响指标</h3>
                <p>{{ playerImpactVerdict }}</p>
                <div class="impact-chips" aria-label="球员影响摘要">
                  <span v-for="item in playerImpactChips" :key="item">{{ item }}</span>
                </div>
              </article>
              <div class="impact-rings" aria-label="球员影响环形图" tabindex="0">
                <CoverageDonut
                  v-for="ring in playerImpactRings"
                  :key="ring.label"
                  :label="ring.label"
                  :value="ring.value"
                  :tone="ring.tone"
                  size="compact"
                  :caption="ring.caption"
                />
              </div>
              <div class="impact-grid" aria-label="球员影响力条形图" tabindex="0">
                <MetricBar
                  v-for="row in playerImpactRows"
                  :key="row.label"
                  :label="row.label"
                  :value="row.value"
                  :max="row.max"
                  :tone="row.tone"
                  :unit="row.unit"
                  :caption="row.caption"
                />
              </div>
            </section>

            <section class="readiness-board" aria-label="球员资料准备度结论" tabindex="0">
              <article class="readiness-score">
                <p class="eyebrow">资料判断</p>
                <strong>{{ playerReadiness.score }}</strong>
                <span>{{ enumLabel('profileReadinessLevel', playerReadiness.level, '待评估') }}</span>
                <MetricBar
                  label="资料准备度"
                  :value="playerReadiness.score"
                  :max="100"
                  unit="%"
                  :tone="playerReadiness.score >= 80 ? 'success' : playerReadiness.score >= 55 ? 'warning' : 'danger'"
                  :caption="playerReadiness.summary"
                />
              </article>
              <article class="readiness-ring-card" tabindex="0" aria-label="球员资料准备度环形图">
                <CoverageDonut
                  v-for="ring in playerReadinessRings"
                  :key="ring.label"
                  :label="ring.label"
                  :value="ring.value"
                  :max="ring.max"
                  :unit="ring.unit"
                  :tone="ring.tone"
                  size="compact"
                  :caption="ring.caption"
                />
              </article>
              <article class="readiness-list readiness-list--good">
                <strong>已具备</strong>
                <ul>
                  <li v-for="item in playerReadiness.strengths.slice(0, 5)" :key="item">{{ item }}</li>
                  <li v-if="!playerReadiness.strengths.length">暂无明确优势维度。</li>
                </ul>
              </article>
              <article class="readiness-list readiness-list--missing">
                <strong>缺口维度</strong>
                <ul>
                  <li v-for="item in playerReadiness.missingDimensions.slice(0, 5)" :key="item">{{ item }}</li>
                  <li v-if="!playerReadiness.missingDimensions.length">关键维度已基本覆盖。</li>
                </ul>
              </article>
            </section>

            <section class="coverage-board" aria-label="球员数据缺口情况">
              <div class="panel-heading">
                <div><p class="eyebrow">资料缺口</p><h3>球员数据缺口清单</h3></div>
              </div>
              <div class="coverage-grid" tabindex="0">
                <MetricBar
                  v-for="bar in playerCoverageBars"
                  :key="bar.label"
                  :label="bar.label"
                  :value="bar.value"
                  :max="bar.max"
                  :tone="bar.tone"
                  :caption="bar.caption"
                />
              </div>
              <p class="coverage-note">
                持续缺口：预计首发概率、近三场出场时间、伤病恢复、训练负荷、停赛风险、点球/定位球职责和与队友配合状态。
              </p>
            </section>

            <section class="metric-board" aria-label="球员高阶指标">
              <div class="panel-heading">
                <div><p class="eyebrow">高阶指标</p><h3>出场 / xG / 可用性</h3></div>
                <span class="status-pill">{{ selected.latestMetric?.sourceName || '指标待同步' }}</span>
              </div>
              <div v-if="playerMetricRows.length" class="metric-grid" tabindex="0">
                <MetricBar
                  v-for="row in playerMetricRows"
                  :key="row.label"
                  :label="row.label"
                  :value="row.value"
                  :max="row.max"
                  :tone="row.tone"
                  :unit="row.unit"
                  :caption="row.caption"
                />
              </div>
              <div class="metric-summary">
                <span>分钟 {{ selected.latestMetric?.minutesPlayed ?? '待同步' }}</span>
                <span>xG {{ metricText(selected.latestMetric?.xg) }}</span>
                <span>xA {{ metricText(selected.latestMetric?.xa) }}</span>
                <span>预计首发 {{ metricText(selected.latestMetric?.expectedStartingProbability == null ? undefined : selected.latestMetric.expectedStartingProbability * 100, '%') }}</span>
                <span>更新时间 {{ formatDateTime(selected.latestMetric?.capturedAt) }}</span>
              </div>
              <p v-if="!selected.latestMetric" class="empty-copy">暂无球员高阶指标；出场分钟、xG/xA、射门、关键传球、训练负荷、可用性评分和预计首发概率缺口。</p>
            </section>

            <section class="card-grid" aria-label="球员画像内容" tabindex="0">
              <article class="info-card" tabindex="0">
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

              <article class="info-card" tabindex="0">
                <p class="eyebrow">事实</p>
                <h3>画像事实</h3>
                <div v-for="fact in selected.facts" :key="fact.id" class="stack-item">
                  <strong>{{ fact.title }} <small>{{ factTypeLabel(fact.factType) }}</small></strong>
                  <span>{{ readablePublicText(fact.summary) }}</span>
                  <small>{{ fact.sourceName }} · 可信度 {{ reliabilityLabel(fact.reliabilityScore) }} · {{ formatDateTime(fact.capturedAt) }}</small>
                </div>
                <p v-if="!selected.facts.length" class="empty-copy">暂无画像事实；近期表现、训练情况、伤停恢复、出场概率和新闻来源缺口。</p>
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
.evidence-hero, .stat-card, .player-overview-structure, .player-overview-board, .player-team-context, .overview-score-card, .overview-gap-card, .side-panel, .detail-panel, .player-identity-card, .impact-board, .impact-summary-card, .readiness-board, .coverage-board, .metric-board, .info-card, .summary-grid div, .alert-panel {
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
.evidence-hero p:not(.eyebrow), .empty-copy, .list-card span, .list-card small, .stack-item span, .stack-item small, .summary-grid span, .coverage-note, .player-identity-card p, .readiness-list li {
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
.stat-card, .overview-score-card, .overview-gap-card, .side-panel, .detail-panel, .player-identity-card, .impact-board, .impact-summary-card, .readiness-board, .coverage-board, .metric-board, .info-card, .alert-panel, .summary-grid div {
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
.player-overview-structure {
  display: grid;
  gap: 14px;
  min-width: 0;
  padding: 18px;
}
.player-overview-rings {
  display: grid;
  gap: 13px;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  min-width: 0;
}
.player-overview-board {
  display: grid;
  gap: 16px;
  grid-template-columns: minmax(0, .9fr) minmax(0, 1.35fr) minmax(0, .8fr);
  min-width: 0;
  padding: 18px;
}
.overview-score-card {
  background:
    radial-gradient(circle at 16% 0%, rgba(96, 165, 250, .14), transparent 30%),
    rgba(15, 23, 42, .5);
}
.overview-score-card h2,
.overview-gap-card h3 {
  font-family: var(--wc-font-display);
  font-size: clamp(22px, 2.45vw, 30px);
  line-height: 1.12;
  margin: 0;
}
.overview-score-card strong {
  color: var(--wc-primary);
  font-family: var(--wc-font-mono);
  font-size: clamp(36px, 4.2vw, 54px);
  line-height: 1;
}
.overview-score-card span {
  color: var(--wc-warning);
  font-weight: 900;
}
.overview-score-card p,
.overview-gap-card li {
  color: var(--wc-text-muted);
  line-height: 1.65;
  margin: 0;
}
.overview-bars {
  display: grid;
  gap: 12px;
  max-height: 520px;
  min-width: 0;
  overflow: auto;
  padding-right: 2px;
}

.gap-action-list {
  display: grid;
  gap: 10px;
  max-height: 360px;
  min-width: 0;
  overflow: auto;
  padding-right: 2px;
}

.gap-action-button {
  background: rgba(15, 23, 42, .48);
  border: 1px solid rgba(217, 119, 6, .26);
  border-radius: var(--wc-radius-md);
  color: var(--wc-text);
  cursor: pointer;
  display: grid;
  gap: 5px;
  min-height: 72px;
  padding: 12px;
  text-align: left;
  transition: border-color 180ms ease, background 180ms ease, transform 180ms ease;
}

.gap-action-button:hover {
  background: rgba(30, 41, 59, .72);
  border-color: rgba(217, 119, 6, .52);
}

.gap-action-button:focus-visible {
  box-shadow: var(--wc-focus-ring);
  outline: none;
}

.gap-action-button span,
.gap-action-button strong {
  font-weight: 900;
}

.gap-action-button small {
  color: var(--wc-text-muted);
  line-height: 1.5;
}

.gap-action-button strong {
  color: var(--wc-warning);
  font-size: 12px;
}
.overview-gap-card ol {
  display: grid;
  gap: 9px;
  margin: 0;
  padding-left: 20px;
}
.player-team-context {
  display: grid;
  gap: 16px;
  min-width: 0;
  padding: 18px;
}
.context-heading {
  align-items: end;
  display: grid;
  gap: 14px;
  grid-template-columns: minmax(0, .75fr) minmax(0, 1fr);
}
.context-heading h2 {
  font-family: var(--wc-font-display);
  font-size: clamp(24px, 3vw, 34px);
  line-height: 1.08;
  margin: 0;
}
.context-heading p:not(.eyebrow) {
  color: var(--wc-text-muted);
  line-height: 1.65;
  margin: 0;
}
.team-context-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  min-width: 0;
}
.team-context-rings {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(176px, 1fr));
  min-width: 0;
}
.team-context-rings :deep(.coverage-donut) {
  background: rgba(15, 23, 42, .44);
  border: 1px solid rgba(147, 197, 253, .16);
  border-radius: var(--wc-radius-md);
  padding: 12px;
}
.team-context-card {
  background: rgba(15, 23, 42, .45);
  border: 1px solid rgba(147, 197, 253, .16);
  border-radius: var(--wc-radius-md);
  display: grid;
  gap: 12px;
  min-width: 0;
  padding: 14px;
}
.evidence-grid {
  align-items: start;
  display: grid;
  gap: 16px;
  grid-template-columns: minmax(0, 340px) minmax(0, 1fr);
  min-width: 0;
}
.player-overview-structure,
.player-overview-rings,
.player-overview-board,
.overview-bars,
.gap-action-list,
.player-team-context,
.team-context-rings,
.team-context-grid,
.side-panel,
.detail-panel,
.impact-board,
.impact-rings,
.impact-grid,
.readiness-board,
.readiness-ring-card,
.coverage-grid,
.metric-grid,
.card-grid {
  scrollbar-color: rgba(217, 119, 6, .42) rgba(15, 23, 42, .28);
  scrollbar-gutter: stable both-edges;
}
.detail-panel, .impact-board, .readiness-board, .coverage-board, .metric-board { align-content: start; }
.side-panel {
  max-height: min(76dvh, 680px);
  overflow: auto;
}
.detail-panel {
  max-height: min(78dvh, 700px);
  overflow: auto;
}
.player-overview-structure:focus-visible,
.player-overview-rings:focus-visible,
.player-overview-board:focus-visible,
.overview-bars:focus-visible,
.gap-action-list:focus-visible,
.player-team-context:focus-visible,
.team-context-rings:focus-visible,
.team-context-grid:focus-visible,
.side-panel:focus-visible,
.detail-panel:focus-visible,
.impact-board:focus-visible,
.impact-rings:focus-visible,
.impact-grid:focus-visible,
.readiness-board:focus-visible,
.readiness-ring-card:focus-visible,
.coverage-grid:focus-visible,
.metric-grid:focus-visible,
.card-grid:focus-visible {
  outline: 3px solid rgba(217, 119, 6, .45);
  outline-offset: 2px;
}
.panel-heading {
  align-items: center;
  display: flex;
  gap: 12px;
  justify-content: space-between;
}
.panel-heading h2, .info-card h3 { margin: 0; }
.player-filter-panel {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}
.filter-field {
  display: grid;
  gap: 6px;
  min-width: 0;
}
.filter-field--wide {
  grid-column: 1 / -1;
}
.filter-field span {
  color: var(--wc-text-muted);
  font-size: 12px;
  font-weight: 800;
}
.filter-field input,
.filter-field select {
  background: rgba(15, 23, 42, .64);
  border: 1px solid rgba(147, 197, 253, .2);
  border-radius: 14px;
  color: var(--wc-text);
  font: inherit;
  min-height: 44px;
  min-width: 0;
  outline: none;
  padding: 0 12px;
}
.filter-field input:focus,
.filter-field select:focus {
  border-color: rgba(59, 130, 246, .72);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, .18);
}
.filter-result-strip,
.pager {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: space-between;
}
.filter-result-strip span,
.pager span {
  color: var(--wc-text-muted);
  font-family: var(--wc-font-mono);
  font-size: 12px;
  font-weight: 800;
}
.filter-result-strip button,
.pager button {
  background: rgba(217, 119, 6, .14);
  border: 1px solid rgba(217, 119, 6, .34);
  border-radius: 999px;
  color: var(--wc-warning);
  cursor: pointer;
  font-weight: 900;
  min-height: 44px;
  padding: 0 12px;
}
.pager button:disabled {
  cursor: not-allowed;
  opacity: .45;
}
.player-list-scroll {
  display: grid;
  gap: 12px;
  max-height: min(58dvh, 640px);
  min-width: 0;
  overflow: auto;
  padding-right: 4px;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}
.player-list-scroll:focus {
  outline: 3px solid rgba(59, 130, 246, .32);
  outline-offset: 3px;
}
.team-context-grid:focus-visible,
.readiness-ring-card:focus-visible,
.info-card:focus-visible {
  box-shadow: var(--wc-focus-ring);
  outline: none;
}
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
.player-identity-card {
  align-items: center;
  grid-template-columns: minmax(0, 1fr) auto;
}
.player-identity-card h3 {
  font-size: clamp(26px, 4vw, 44px);
  line-height: 1.05;
  margin: 0 0 10px;
}
.player-identity-card p {
  line-height: 1.62;
  margin: 0;
}
.impact-board {
  background:
    radial-gradient(circle at 0% 0%, rgba(217, 119, 6, .16), transparent 30%),
    rgba(15, 23, 42, .48);
  grid-template-columns: minmax(0, .86fr) minmax(0, 1.14fr);
  max-height: min(82dvh, 720px);
  overflow: auto;
}
.impact-summary-card {
  background: rgba(15, 23, 42, .46);
  border-color: rgba(217, 119, 6, .24);
  border-radius: var(--wc-radius-md);
}
.impact-summary-card h3 {
  font-family: var(--wc-font-display);
  font-size: clamp(22px, 2.5vw, 30px);
  line-height: 1.12;
  margin: 0;
  overflow-wrap: anywhere;
}
.impact-summary-card p,
.impact-chips span {
  color: var(--wc-text-muted);
  line-height: 1.65;
  margin: 0;
}
.impact-chips,
.impact-rings {
  display: grid;
  gap: 10px;
  max-height: 520px;
  min-width: 0;
  overflow: auto;
  padding-right: 2px;
}
.impact-chips span {
  background: rgba(147, 197, 253, .1);
  border: 1px solid rgba(147, 197, 253, .16);
  border-radius: 999px;
  color: var(--wc-text);
  font-family: var(--wc-font-mono);
  font-size: 12px;
  font-weight: 900;
  padding: 7px 10px;
}
.impact-grid {
  align-content: start;
  display: grid;
  gap: 13px;
  grid-column: 1 / -1;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  max-height: 460px;
  min-width: 0;
  overflow: auto;
  padding-right: 2px;
}
.readiness-board {
  grid-template-columns: minmax(0, .9fr) minmax(0, 1fr) minmax(0, 1fr);
  max-height: min(82dvh, 720px);
  overflow: auto;
}
.readiness-score, .readiness-list, .readiness-ring-card {
  background: rgba(15, 23, 42, .45);
  border: 1px solid rgba(147, 197, 253, .16);
  border-radius: var(--wc-radius-md);
  display: grid;
  gap: 10px;
  min-width: 0;
  padding: 14px;
}
.readiness-ring-card {
  grid-column: 2 / -1;
  grid-template-columns: repeat(auto-fit, minmax(178px, 1fr));
  max-height: 520px;
  overflow: auto;
}
.readiness-score > strong {
  color: var(--wc-primary);
  font-family: var(--wc-font-mono);
  font-size: clamp(42px, 6vw, 72px);
  line-height: .95;
}
.readiness-score > span {
  color: var(--wc-warning);
  font-weight: 900;
}
.readiness-list strong {
  color: var(--wc-text);
  font-size: 16px;
}
.readiness-list ul {
  display: grid;
  gap: 8px;
  margin: 0;
  padding-left: 18px;
}
.readiness-list--good { border-color: rgba(34, 197, 94, .26); }
.readiness-list--missing { border-color: rgba(245, 158, 11, .28); }
.coverage-grid {
  display: grid;
  gap: 13px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  max-height: 520px;
  min-width: 0;
  overflow: auto;
  padding-right: 2px;
}
.coverage-note {
  line-height: 1.62;
  margin: 0;
}
.metric-grid {
  display: grid;
  gap: 13px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  max-height: 460px;
  min-width: 0;
  overflow: auto;
  padding-right: 2px;
}
.metric-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.metric-summary span {
  background: rgba(147, 197, 253, .1);
  border: 1px solid rgba(147, 197, 253, .16);
  border-radius: 999px;
  color: var(--wc-text-muted);
  font-family: var(--wc-font-mono);
  font-size: 12px;
  font-weight: 800;
  padding: 7px 10px;
}
.card-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  max-height: min(84dvh, 820px);
  min-width: 0;
  overflow: auto;
  padding-right: 4px;
}
@media (max-width: 1024px) {
  .evidence-hero, .player-overview-board, .context-heading, .evidence-grid, .summary-grid, .player-identity-card, .impact-board, .readiness-board { grid-template-columns: 1fr; }
  .readiness-ring-card { grid-column: auto; }
  .stat-grid, .team-context-grid, .coverage-grid, .card-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 640px) {
  .evidence-hero, .player-overview-board, .evidence-grid, .card-grid { grid-template-columns: 1fr; }
  .stat-grid, .player-overview-rings, .team-context-rings, .team-context-grid, .impact-rings, .impact-grid, .readiness-ring-card, .coverage-grid, .metric-grid, .summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .evidence-hero, .stat-card, .player-overview-structure, .player-overview-board, .player-team-context, .overview-score-card, .overview-gap-card, .side-panel, .detail-panel, .player-identity-card, .impact-board, .impact-summary-card, .readiness-board, .coverage-board, .metric-board, .info-card, .summary-grid div {
    border-radius: var(--wc-radius-md);
    gap: 10px;
    padding: 12px;
  }
  .evidence-hero h1 {
    font-size: clamp(28px, 12vw, 42px);
    margin-bottom: 8px;
  }
  .evidence-hero p:not(.eyebrow) {
    display: none;
  }
  .stat-grid {
    gap: 10px;
  }
  .stat-card strong {
    font-size: 28px;
  }
  .stat-card small {
    font-size: 12px;
  }
  .player-overview-rings,
  .team-context-rings,
  .impact-rings,
  .readiness-ring-card {
    gap: 10px;
  }
  .player-overview-rings :deep(.coverage-donut),
  .team-context-rings :deep(.coverage-donut),
  .impact-rings :deep(.coverage-donut),
  .readiness-ring-card :deep(.coverage-donut) {
    align-content: start;
    background: rgba(15, 23, 42, .42);
    border: 1px solid rgba(147, 197, 253, .14);
    border-radius: var(--wc-radius-md);
    gap: 6px;
    grid-template-columns: 1fr !important;
    justify-items: center;
    min-height: 126px;
    padding: 9px;
    text-align: center;
  }
  .player-overview-rings :deep(.coverage-donut__ring),
  .team-context-rings :deep(.coverage-donut__ring),
  .impact-rings :deep(.coverage-donut__ring),
  .readiness-ring-card :deep(.coverage-donut__ring),
  .overview-score-card :deep(.coverage-donut__ring) {
    width: 66px;
  }
  .player-overview-rings :deep(.coverage-donut__ring span),
  .team-context-rings :deep(.coverage-donut__ring span),
  .impact-rings :deep(.coverage-donut__ring span),
  .readiness-ring-card :deep(.coverage-donut__ring span),
  .overview-score-card :deep(.coverage-donut__ring span) {
    font-size: 20px;
  }
  .player-overview-rings :deep(.coverage-donut__copy),
  .team-context-rings :deep(.coverage-donut__copy),
  .impact-rings :deep(.coverage-donut__copy),
  .readiness-ring-card :deep(.coverage-donut__copy) {
    gap: 3px;
  }
  .player-overview-rings :deep(.coverage-donut__copy strong),
  .team-context-rings :deep(.coverage-donut__copy strong),
  .impact-rings :deep(.coverage-donut__copy strong),
  .readiness-ring-card :deep(.coverage-donut__copy strong) {
    font-size: 12px;
    line-height: 1.2;
  }
  .player-overview-rings :deep(.coverage-donut__copy small),
  .team-context-rings :deep(.coverage-donut__copy small),
  .impact-rings :deep(.coverage-donut__copy small),
  .readiness-ring-card :deep(.coverage-donut__copy small) {
    display: none;
  }
  .overview-score-card {
    align-items: center;
    grid-template-columns: minmax(0, 1fr) auto;
  }
  .overview-score-card .eyebrow,
  .overview-score-card h2 {
    grid-column: 1;
  }
  .overview-score-card h2,
  .overview-gap-card h3,
  .context-heading h2,
  .impact-summary-card h3 {
    font-size: 22px;
  }
  .overview-score-card :deep(.coverage-donut) {
    grid-column: 2;
    grid-row: 1 / span 2;
    grid-template-columns: 1fr !important;
    justify-items: center;
    text-align: center;
  }
  .overview-score-card :deep(.coverage-donut__copy strong) {
    font-size: 12px;
  }
  .overview-score-card :deep(.coverage-donut__copy small) {
    font-size: 11px;
  }
  .overview-bars,
  .gap-action-list,
  .coverage-grid,
  .metric-grid,
  .impact-grid {
    gap: 10px;
  }
  .overview-bars :deep(.metric-bar small),
  .team-context-card :deep(.metric-bar small),
  .coverage-grid :deep(.metric-bar small),
  .metric-grid :deep(.metric-bar small),
  .impact-grid :deep(.metric-bar small),
  .readiness-score :deep(.metric-bar small) {
    display: none;
  }
  .overview-gap-card ol {
    display: none;
  }
  .gap-action-button {
    gap: 3px;
    min-height: 0;
    padding: 10px;
  }
  .gap-action-button small {
    display: none;
  }
  .context-heading {
    align-items: start;
    grid-template-columns: 1fr;
  }
  .context-heading p:not(.eyebrow) {
    display: none;
  }
  .team-context-card {
    gap: 9px;
    padding: 10px;
  }
  .team-context-card :deep(.metric-bar__top) {
    align-items: start;
    display: grid;
    gap: 3px;
  }
  .team-context-card :deep(.metric-bar__top strong) {
    font-size: 13px;
  }
  .side-panel {
    max-height: none;
    overflow: visible;
    scrollbar-gutter: auto;
  }
  .player-filter-panel { grid-template-columns: 1fr; }
  .filter-field input,
  .filter-field select,
  .filter-result-strip button,
  .pager button,
  .action-button {
    min-height: 44px;
  }
  .player-list-scroll {
    gap: 10px;
    max-height: min(48dvh, 420px);
    overflow: auto;
    padding-right: 0;
    scrollbar-gutter: auto;
    scrollbar-width: none;
  }
  .player-list-scroll::-webkit-scrollbar {
    display: none;
  }
  .list-card,
  .stack-item {
    border-radius: 14px;
    gap: 5px;
    padding: 10px;
  }
  .summary-grid {
    gap: 10px;
  }
  .summary-grid div {
    min-height: 92px;
  }
  .player-identity-card {
    grid-template-columns: minmax(0, 1fr) auto;
  }
  .player-identity-card h3 {
    font-size: 26px;
  }
  .player-identity-card p {
    display: none;
  }
  .impact-board,
  .readiness-board {
    grid-template-columns: 1fr;
  }
  .impact-summary-card p {
    line-height: 1.5;
  }
  .impact-chips {
    gap: 7px;
  }
  .impact-chips span {
    font-size: 11px;
    padding: 6px 8px;
  }
  .readiness-score {
    align-items: center;
    grid-template-columns: auto minmax(0, 1fr);
  }
  .readiness-score > strong {
    font-size: 42px;
    grid-row: 1 / span 2;
  }
  .readiness-score :deep(.metric-bar) {
    grid-column: 1 / -1;
  }
  .readiness-ring-card {
    max-height: none;
    overflow: visible;
    scrollbar-gutter: auto;
  }
  .readiness-list {
    max-height: none;
    overflow: visible;
    scrollbar-gutter: auto;
  }
  .readiness-list ul {
    gap: 6px;
  }
  .coverage-note {
    font-size: 12px;
    line-height: 1.5;
  }
  .metric-summary {
    gap: 6px;
  }
  .metric-summary span {
    font-size: 11px;
    padding: 6px 8px;
  }
  .card-grid > .info-card {
    max-height: none;
    overflow: visible;
    scrollbar-gutter: auto;
  }
  .impact-board,
  .readiness-board {
    max-height: none;
    overflow: visible;
    scrollbar-gutter: auto;
  }
  .detail-panel {
    max-height: none;
    overflow: visible;
    scrollbar-gutter: auto;
  }
  .card-grid {
    max-height: none;
    overflow: visible;
    scrollbar-gutter: auto;
  }
  .panel-heading { align-items: stretch; flex-direction: column; }
  .action-button { width: 100%; }
}
@media (prefers-reduced-motion: reduce) {
  .list-card { transition: none; }
}
</style>
