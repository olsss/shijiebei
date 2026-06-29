<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import {
  getPublicMatchSentiment,
  listPublicSentimentCategories,
  listPublicSentimentOverview,
  listPublicSentimentRiskTypes,
  type PublicSentimentFactorDetail,
  type PublicSentimentFactorSummary,
  type PublicSentimentMatchDetail,
  type PublicSentimentRisk,
} from '@/api/sentiment';
import CoverageDonut from '@/components/football/CoverageDonut.vue';
import MetricBar from '@/components/football/MetricBar.vue';
import ScoreboardCard from '@/components/football/ScoreboardCard.vue';
import { enumLabel, readablePublicText } from '@/utils/display-labels';

const loading = ref(false);
const detailLoading = ref(false);
const error = ref('');
const detailError = ref('');
const overview = ref<PublicSentimentFactorSummary[]>([]);
const categories = ref<string[]>([]);
const riskTypes = ref<string[]>([]);
const selectedCategory = ref('');
const selectedRiskLevel = ref('');
const searchQuery = ref('');
const staleOnly = ref(false);
const selectedMatch = ref<PublicSentimentMatchDetail | null>(null);
const selectedFactorId = ref<number | null>(null);
const currentPage = ref(1);
const pageSize = 16;

const riskLevels = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'UNKNOWN'];
const riskLevelOptions = riskLevels.map((level) => ({
  value: level,
  label: enumLabel('riskLevel', level),
}));

const publicSentimentSensitivePattern = /ROI|CLV|closing[_\s-]?odds|ticket(?:No)?|stake|profit|loss|budget|票号|投入|返还|盈亏|预算|资金|收益|本金|投注金额|金额建议|下注金额|下注|投注/gi;
const publicSentimentTermPattern = /公众投注|盘口变动|盘口|置信度|置信分|推荐玩法|投注建议|下注建议/gi;

const publicSentimentTermLabels: Record<string, string> = {
  公众投注: '公众热度',
  盘口变动: '市场价格快照',
  盘口: '市场线',
  置信度: '把握程度',
  置信分: '把握分',
  推荐玩法: '市场维度',
  投注建议: '公开数据边界',
  下注建议: '公开数据边界',
};

function replacePublicSentimentTerm(term: string): string {
  return publicSentimentTermLabels[term] ?? '已脱敏指标';
}

function publicSentimentText(value?: string | null, fallback = '暂无公开摘要。'): string {
  return readablePublicText(value, fallback)
    .replace(/\bREDUCE_STAKE\b/gi, '热度过高，第二来源缺口')
    .replace(/\bMONITOR\b/gi, '保持观察，等待更多公开来源')
    .replace(/\bVERIFY\b/gi, '第二来源缺口')
    .replace(publicSentimentTermPattern, replacePublicSentimentTerm)
    .replace(publicSentimentSensitivePattern, '已脱敏执行明细');
}

function publicRiskText(risk: PublicSentimentRisk): string {
  return publicSentimentText(risk.rationale || risk.suggestedAction, '保持观察，等待更多公开来源');
}

const dimensionDefinitions = [
  {
    key: 'weather_venue',
    label: '天气与场地',
    categories: ['WEATHER', 'VENUE', 'ATTENDANCE', 'BROADCAST', 'TEMPERATURE_PITCH', 'STADIUM', 'CROWD'],
    description: '温度、降雨、风速、草皮、场馆、上座和转播信息，帮助判断比赛环境资料是否完整。',
    priority: '中',
    nextAction: '温度、降雨、风速、草皮、场馆、上座和转播来源缺口。',
  },
  {
    key: 'referee',
    label: '裁判与判罚',
    categories: ['REFEREE', 'OFFICIAL', 'REFEREE_ASSIGNMENT', 'REFEREE_SOURCE_AUDIT', 'CARD_TENDENCY', 'DISCIPLINE', 'CARD', 'TEAM_CARD_ACCUMULATION', 'PLAYER_CARD_ACCUMULATION', 'NEXT_MATCH_DISCIPLINE_WATCH'],
    description: '裁判指派、执法人员来源、红黄牌/纪律累计与待核查判罚背景，帮助识别需要二次确认的比赛信息。',
    priority: '高',
    nextAction: '裁判指派来源、历史黄牌/红牌/点球样本、停赛官方名单和吹罚尺度样本缺口。',
  },
  {
    key: 'injury_training',
    label: '伤停与训练',
    categories: ['INJURY', 'TRAINING', 'PLAYER_INJURY', 'FITNESS'],
    description: '核心球员伤病、训练负荷、出场存疑和停赛信息。',
    priority: '高',
    nextAction: '核心球员伤停、预计回归、训练参与和停赛风险缺口。',
  },
  {
    key: 'schedule_travel',
    label: '赛程、旅行与休息',
    categories: ['SCHEDULE', 'TRAVEL', 'REST', 'TRAVEL_REST', 'REST_DAYS', 'WORKLOAD', 'PLAYING_TIME', 'TEAM_CONSECUTIVE_STARTERS', 'TEAM_HIGH_WORKLOAD_PLAYER_COUNT', 'MINUTES_DERIVED'],
    description: '休息天数、跨时区、旅途距离和连续比赛造成的疲劳。',
    priority: '高',
    nextAction: '休息天数、旅行距离、跨时区和连续比赛负荷缺口。',
  },
  {
    key: 'motivation_rotation',
    label: '战意、轮换与更衣室',
    categories: ['MOTIVATION', 'ROTATION', 'LOCKER_ROOM', 'GROUP_QUALIFICATION', 'QUALIFICATION', 'QUALIFICATION_STATUS', 'ADVANCEMENT', 'ADVANCEMENT_STATUS', 'KNOCKOUT', 'ROUND_OF_32_STAGE', 'ROUND_OF_32_PATH', 'ADVANCED_TO_ROUND_OF_16', 'ELIMINATED_IN_ROUND_OF_32', 'KNOCKOUT_STAGE', 'KNOCKOUT_ELIMINATION_CONTEXT', 'TOURNAMENT_FORMAT', 'ROTATION_PRESSURE', 'STARTING_CONTINUITY', 'NEXT_MATCH_WORKLOAD_WATCH'],
    description: '出线状态、淘汰赛阶段、晋级/出局赛制事实、轮换意愿和队内稳定性。',
    priority: '高',
    nextAction: '出线状态、淘汰赛阶段、轮换动机和队内稳定性缺口；避免把赛制事实写成市场方向。',
  },
  {
    key: 'news_press',
    label: '发布会与球队新闻',
    categories: ['PRESS_CONFERENCE', 'TEAM_NEWS', 'NEWS', 'OFFICIAL_TEAM_NEWS'],
    description: '主帅发布会、阵容线索、媒体记录的球队动态。',
    priority: '中',
    nextAction: '主帅发布会、官方公告、阵容线索和赛前新闻缺口。',
  },
  {
    key: 'media_fans',
    label: '媒体/球迷舆情',
    categories: ['PUBLIC_OPINION', 'PUBLIC_SENTIMENT', 'FAN_SENTIMENT', 'MEDIA'],
    description: '舆论热度、球迷预期、热门叙事是否过度集中。',
    priority: '中',
    nextAction: '媒体叙事、球迷热度、社媒情绪和过热风险缺口。',
  },
  {
    key: 'market_public',
    label: '赔率/市场价格',
    categories: ['MARKET', 'MARKET_SIGNAL', 'MARKET_PRICE_SNAPSHOT', 'MARKET_PRICE', 'IMPLIED_PROBABILITY', 'PUBLIC_BETTING', 'ODDS', 'MATCH_WIN', 'H2H_1X2', 'SPREAD', 'TOTAL_GOALS'],
    description: '赛前市场价格快照、赔率隐含概率，以及后续待补的价格时间线和公众热度比例。',
    priority: '高',
    nextAction: '开盘/当前价格时间线、公众热度比例和第二来源市场快照缺口。',
  },
  {
    key: 'history_tactic',
    label: '历史交锋与战术对位',
    categories: ['HISTORY', 'HEAD_TO_HEAD', 'TACTICAL_MATCHUP', 'TACTIC'],
    description: '历史对战、阵型克制、边路/中路对位和定位球匹配。',
    priority: '中',
    nextAction: '历史交锋、阵型对位、压迫体系和定位球匹配缺口。',
  },
  {
    key: 'form_results',
    label: '近期赛果/状态走势',
    categories: ['FORM', 'TOURNAMENT_FORM', 'RECENT_RESULTS', 'RESULT_TREND', 'FORM_TREND', 'UNBEATEN_STREAK', 'WINLESS_STREAK', 'CLEAN_SHEET_TREND'],
    description: '本届已完赛胜平负、近三场结果、进失球、零封和连续不败/未胜等赛果事实。',
    priority: '高',
    nextAction: '近期赛果走势、主客场样本和第二来源复核缺口；避免把赛果事实写成未来胜率。',
  },
  {
    key: 'key_player_contribution',
    label: '关键球员进球参与',
    categories: ['KEY_PLAYER', 'PLAYER_IMPACT', 'ATTACK_CONTRIBUTION', 'KEY_PLAYER_CONTRIBUTION', 'GOAL_CONTRIBUTION', 'TOP_SCORER_CONTRIBUTION', 'ASSIST_CONTRIBUTION', 'CONTRIBUTION_DISTRIBUTION'],
    description: '已完赛球员进球、助攻、主要参与者和贡献分布等事实资料。',
    priority: '中',
    nextAction: '关键球员贡献分布、球员可用性和第二来源缺口；避免写成未来进球概率或预计首发。',
  },

  {
    key: 'squad_profile',
    label: '阵容结构/年龄经验',
    categories: ['SQUAD_PROFILE', 'TEAM_SIDE_ROSTER_BASELINE', 'ROSTER_PROFILE', 'AGE_EXPERIENCE', 'AGE_EXPERIENCE_STRUCTURE', 'TEAM_EXPERIENCE', 'TEAM_EXPERIENCE_PROFILE', 'CLUB_DISTRIBUTION', 'CLUB_DISTRIBUTION_PROFILE', 'HEIGHT_PROFILE', 'POSITION_PROFILE', 'SQUAD_LIST'],
    description: '官方名单中的年龄、身高、位置分布、赛前国家队出场/进球和俱乐部注册地分布。',
    priority: '中',
    nextAction: '名单字段二次来源、球员临场可用性和出场名单确认缺口；避免写成阵容优劣或赛果预测。',
  },
  {
    key: 'lineup_structure',
    label: '首发位置结构',
    categories: ['LINEUP_STRUCTURE', 'CONFIRMED_STARTING_XI_STRUCTURE', 'LINEUP_STRUCTURE_PROFILE', 'STARTING_XI_PROFILE', 'LINEUP_STRUCTURE_DERIVED', 'CONFIRMED_LINEUP_DERIVED'],
    description: '已完赛确认首发、替补记录和位置编码聚合出的基础阵容样本。',
    priority: '中',
    nextAction: '下一场官方首发、临场变阵和第二来源确认缺口；避免写成预计首发或战术强弱。',
  },
  {
    key: 'goalkeeping_pressure',
    label: '门将/防守承压',
    categories: ['GOALKEEPING', 'SAVE_PRESSURE_PROFILE', 'SAVE_PROFILE', 'GOALKEEPING_SAVE_PROFILE', 'GOALKEEPER_SAVE_PROFILE', 'SHOT_STOPPING', 'SHOT_STOPPING_PROFILE', 'DEFENSIVE_PRESSURE', 'DEFENSIVE_PRESSURE_PROFILE'],
    description: '门将扑救、门将失球、零封和被射正样本等已入库基础统计。',
    priority: '中',
    nextAction: '门将扑救、被射正样本、xGOT/PSxG 和门将出场确认缺口；避免写成扑救质量或赛果预测。',
  },
  {
    key: 'infraction_profile',
    label: '犯规/越位样本',
    categories: ['INFRACTION_PROFILE', 'FOUL_OFFSIDE_PENALTY_PROFILE', 'FOUL_PROFILE', 'OFFSIDE_PROFILE', 'PENALTY_KICK_SAMPLE'],
    description: 'ESPN 完整技术样本中的犯规、越位、点球尝试与命中等基础规则行为统计。',
    priority: '中',
    nextAction: '犯规/越位样本第二来源、裁判历史判罚样本和点球尝试来源缺口；避免写成纪律好坏或后续事件概率。',
  },
  {
    key: 'scoring_pattern',
    label: '进球方式与时间',
    categories: [
      'SCORING',
      'SET_PIECE',
      'GOAL_TIMING',
      'ATTACK_PATTERN',
      'GOAL_TIME_BUCKET',
      'SPECIAL_SCORING_PROFILE',
      'FIRST_GOAL_MINUTE',
      'LATE_GOAL',
      'HEADER_GOAL',
      'FREE_KICK_GOAL',
      'PENALTY_SCORED',
      'PENALTY_MISSED',
      'OWN_GOAL',
      'CORNER_GOAL',
      'SET_PIECE_GOAL',
      'CROSS_GOAL',
      'THROUGH_BALL_GOAL',
      'FAST_BREAK_GOAL',
      'PLAYER_GOAL_INVOLVEMENT',
    ],
    description: '进球时间段、首球/末段进球、点球/任意球/头球/乌龙等已入库进球方式。',
    priority: '中',
    nextAction: '进球时间段、定位球来源、点球/任意球/头球和射手参与样本缺口。',
  },
];

function factorMatchesDimension(
  item: Pick<PublicSentimentFactorSummary, 'factorCategory' | 'factorType'>,
  dimension: { categories: string[] },
): boolean {
  const category = (item.factorCategory || '').toUpperCase();
  const type = (item.factorType || '').toUpperCase();
  return dimension.categories.includes(category) || dimension.categories.includes(type);
}

const filteredOverview = computed(() => overview.value.filter((item) => {
  const categoryOk = !selectedCategory.value || item.factorCategory === selectedCategory.value;
  const riskOk = !selectedRiskLevel.value || item.highestRiskLevel === selectedRiskLevel.value;
  const staleOk = !staleOnly.value || item.stale;
  const keyword = searchQuery.value.trim().toLowerCase();
  const textOk = !keyword || [
    item.matchName,
    item.title,
    item.summary,
    item.sourceName,
    item.sourceRef,
    item.factorCategory,
    item.factorType,
    enumLabel('factorCategory', item.factorCategory, ''),
    enumLabel('factorType', item.factorType, ''),
    enumLabel('riskLevel', item.highestRiskLevel || 'UNKNOWN', ''),
  ].filter(Boolean).join(' ').toLowerCase().includes(keyword);
  return categoryOk && riskOk && staleOk && textOk;
}));
const totalPages = computed(() => Math.max(1, Math.ceil(filteredOverview.value.length / pageSize)));
const pagedOverview = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return filteredOverview.value.slice(start, start + pageSize);
});
const hasActiveFilters = computed(() => Boolean(selectedCategory.value || selectedRiskLevel.value || searchQuery.value || staleOnly.value));

const stats = computed(() => ({
  matches: new Set(overview.value.map((item) => item.matchId).filter((id) => id != null)).size,
  factors: overview.value.length,
  risks: Math.max(
    overview.value.reduce((sum, item) => sum + item.riskCount, 0),
    selectedMatch.value?.risks.length ?? 0,
  ),
  stale: overview.value.filter((item) => item.stale).length,
}));

const dimensionCards = computed(() => dimensionDefinitions.map((dimension) => {
  const items = overview.value.filter((item) => factorMatchesDimension(item, dimension));
  const highestRisk = items
    .map((item) => item.highestRiskLevel || 'UNKNOWN')
    .sort((left, right) => riskRank(right) - riskRank(left))[0] || 'UNKNOWN';
  const covered = items.length > 0;
  return {
    ...dimension,
    count: items.length,
    covered,
    highestRisk,
    tone: !covered ? 'info' : riskRank(highestRisk) >= 3 ? 'danger' : riskRank(highestRisk) === 2 ? 'warning' : 'success',
    coverage: Math.min(100, items.length * 50),
  };
}));

const spotlightDimensionKey = ref('');

const globalIntelligence = computed(() => {
  const dimensions = dimensionCards.value;
  const covered = dimensions.filter((dimension) => dimension.covered);
  const missing = dimensions.filter((dimension) => !dimension.covered);
  const highPriorityMissing = missing.filter((dimension) => dimension.priority === '高');
  const score = Math.round((covered.length / Math.max(1, dimensionDefinitions.length)) * 100);
  const priorityLabels = (highPriorityMissing.length ? highPriorityMissing : missing)
    .map((dimension) => dimension.label)
    .slice(0, 3)
    .join('、');
  const coveredLabels = covered
    .map((dimension) => dimension.label)
    .slice(0, 3)
    .join('、');

  return {
    score,
    covered,
    missing,
    highPriorityMissing,
    summary: missing.length
      ? `全站已覆盖 ${covered.length} / ${dimensionDefinitions.length} 类外因，已覆盖 ${coveredLabels || '基础线索'}；缺失 ${priorityLabels || '伤停、裁判、赛程旅行'}。`
      : `${dimensionDefinitions.length} 类外部因素都有正式入库数据；高风险、过期和单来源状态已展示。`,
  };
});

const globalPriorityCards = computed(() => {
  const missing = globalIntelligence.value.highPriorityMissing.length
    ? globalIntelligence.value.highPriorityMissing
    : globalIntelligence.value.missing;
  if (missing.length) {
    return missing.slice(0, 3);
  }
  return [...globalIntelligence.value.covered]
    .sort((left, right) => riskRank(right.highestRisk) - riskRank(left.highestRisk) || right.count - left.count)
    .slice(0, 3);
});

const globalSentimentRings = computed(() => {
  const factorTotal = stats.value.factors;
  const highRiskFactors = overview.value.filter((item) => ['CRITICAL', 'HIGH'].includes((item.highestRiskLevel || '').toUpperCase())).length;
  const highPriorityTotal = dimensionDefinitions.filter((dimension) => dimension.priority === '高').length;
  const highPriorityMissing = globalIntelligence.value.highPriorityMissing.length;
  return [
    {
      label: '全站外因覆盖',
      value: globalIntelligence.value.score,
      max: 100,
      unit: '%',
      tone: coverageTone(globalIntelligence.value.score),
      caption: `${globalIntelligence.value.covered.length} / ${dimensionDefinitions.length} 类外因`,
    },
    {
      label: '已覆盖维度',
      value: globalIntelligence.value.covered.length,
      max: dimensionDefinitions.length,
      unit: '类',
      tone: globalIntelligence.value.covered.length >= 6 ? 'success' : globalIntelligence.value.covered.length >= 3 ? 'warning' : 'info',
      caption: `${globalIntelligence.value.covered.length} / ${dimensionDefinitions.length} 类外因`,
    },
    {
      label: '高风险因素',
      value: highRiskFactors,
      max: Math.max(1, factorTotal),
      unit: '条',
      tone: highRiskFactors ? 'danger' : 'success',
      caption: `${highRiskFactors} / ${factorTotal} 条因素`,
    },
    {
      label: '过期因素',
      value: stats.value.stale,
      max: Math.max(1, factorTotal),
      unit: '条',
      tone: stats.value.stale ? 'danger' : 'success',
      caption: `${stats.value.stale} / ${factorTotal} 条因素`,
    },
    {
      label: '高等级缺口',
      value: highPriorityMissing,
      max: Math.max(1, highPriorityTotal),
      unit: '类',
      tone: highPriorityMissing ? 'warning' : 'success',
      caption: `${highPriorityMissing} / ${highPriorityTotal} 类高等级维度`,
    },
  ];
});

function availableCategoryForDimension(dimension: { categories: string[] }): string {
  const matchingItem = overview.value.find((item) => factorMatchesDimension(item, dimension));
  return matchingItem?.factorCategory
    ?? dimension.categories.find((category) => categories.value.includes(category))
    ?? '';
}

function applyGlobalDimensionAction(dimension: { key: string; categories: string[]; covered?: boolean }) {
  spotlightDimensionKey.value = dimension.key;
  if (dimension.covered) {
    selectedCategory.value = availableCategoryForDimension(dimension);
    selectedRiskLevel.value = '';
    searchQuery.value = '';
    staleOnly.value = false;
    currentPage.value = 1;
    return;
  }

  selectedCategory.value = '';
  selectedRiskLevel.value = '';
  searchQuery.value = '';
  staleOnly.value = false;
  currentPage.value = 1;
  const scrollToDimensionPanel = () => {
    if (typeof document === 'undefined') {
      return;
    }
    document.querySelector('[data-test="sentiment-dimension-panel"]')?.scrollIntoView?.({ block: 'start' });
  };
  if (typeof window !== 'undefined' && typeof window.requestAnimationFrame === 'function') {
    window.requestAnimationFrame(scrollToDimensionPanel);
  } else {
    scrollToDimensionPanel();
  }
}

const selectedMatchDimensions = computed(() => dimensionDefinitions.map((dimension) => {
  const factors = selectedMatch.value?.factors ?? [];
  const items = factors.filter((item) => factorMatchesDimension(item, dimension));
  const risks = selectedMatch.value?.risks.filter((risk) => items.some((item) => item.id === risk.factorId)) ?? [];
  const highestRisk = risks
    .map((risk) => risk.riskLevel || 'UNKNOWN')
    .sort((left, right) => riskRank(right) - riskRank(left))[0] || 'UNKNOWN';
  return {
    ...dimension,
    count: items.length,
    riskCount: risks.length,
    covered: items.length > 0,
    highestRisk,
  };
}));

const matchIntelligence = computed(() => {
  const dimensions = selectedMatchDimensions.value;
  const covered = dimensions.filter((dimension) => dimension.covered);
  const missing = dimensions.filter((dimension) => !dimension.covered);
  const score = Math.round((covered.length / Math.max(1, dimensions.length)) * 100);
  const highPriorityMissing = missing.filter((dimension) => dimension.priority === '高');
  const priorityLabels = (highPriorityMissing.length ? highPriorityMissing : missing)
    .map((dimension) => dimension.label)
    .slice(0, 3)
    .join('、');
  return {
    score,
    covered,
    missing,
    highPriorityMissing,
    summary: missing.length
      ? `本场外部情报缺口：${priorityLabels || '关键线索'}；对应维度已在下方标出。`
      : '本场 10 类外部因素已基本覆盖，来源时效和冲突状态已展示。',
  };
});

const priorityGaps = computed(() => {
  const missing = matchIntelligence.value.highPriorityMissing.length
    ? matchIntelligence.value.highPriorityMissing
    : matchIntelligence.value.missing;
  return missing.slice(0, 6);
});

const headlineRisk = computed(() => {
  const top = [...overview.value]
    .sort((left, right) => riskRank(right.highestRiskLevel) - riskRank(left.highestRiskLevel))[0];
  if (!top) {
    return '当前主要风险：暂无已入库因素；伤停、天气、裁判、旅行休息和市场热度缺失。';
  }
  if (riskRank(top.highestRiskLevel) === 0) {
    const covered = dimensionCards.value
      .filter((dimension) => dimension.covered)
      .map((dimension) => dimension.label)
      .slice(0, 3)
      .join('、') || '基础外部因素';
    const missing = dimensionCards.value
      .filter((dimension) => !dimension.covered)
      .map((dimension) => dimension.label)
      .slice(0, 3)
      .join('、');
    return missing
      ? `当前主要风险：暂无高风险评分；已覆盖 ${covered}，待补 ${missing}。`
      : `当前主要风险：暂无高风险评分；已覆盖 ${covered}，来源时效和风险状态已展示。`;
  }
  return `当前主要风险：${enumLabel('factorCategory', top.factorCategory)} · ${publicSentimentText(top.title, '外部线索')}（${enumLabel('riskLevel', top.highestRiskLevel)}）`;
});

const currentFactor = computed<PublicSentimentFactorDetail | null>(() => {
  if (!selectedMatch.value) {
    return null;
  }
  if (selectedFactorId.value == null) {
    return selectedMatch.value.factors[0] ?? null;
  }
  return selectedMatch.value.factors.find((factor) => factor.id === selectedFactorId.value)
    ?? selectedMatch.value.factors[0]
    ?? null;
});

const currentFactorRisks = computed<PublicSentimentRisk[]>(() => {
  if (!currentFactor.value || !selectedMatch.value) {
    return [];
  }
  return selectedMatch.value.risks.filter((risk) => risk.factorId === currentFactor.value?.id);
});

const currentFactorRings = computed(() => {
  const factor = currentFactor.value;
  if (!factor) {
    return [];
  }
  const risks = currentFactorRisks.value;
  const riskScore = risks
    .map((risk) => scorePercent(risk.riskScore))
    .filter((value): value is number => value != null)
    .sort((left, right) => right - left)[0] ?? null;
  const confidence = scorePercent(factor.confidenceScore);
  const reliability = scorePercent(factor.reliabilityScore);
  const freshness = factor.stale ? 35 : 100;
  return [
    {
      label: '风险评分',
      value: riskScore,
      tone: riskScore == null ? 'info' : riskScore >= 70 ? 'danger' : riskScore >= 40 ? 'warning' : 'success',
      caption: risks.length ? `${risks.length} 条关联风险` : '暂无关联风险评分',
    },
    {
      label: '证据把握',
      value: confidence,
      tone: confidence == null ? 'info' : coverageTone(confidence),
      caption: `原始分 ${scoreText(factor.confidenceScore)}`,
    },
    {
      label: '来源可靠',
      value: reliability,
      tone: reliability == null ? 'info' : coverageTone(reliability),
      caption: `来源分 ${scoreText(factor.reliabilityScore)}`,
    },
    {
      label: '时效状态',
      value: freshness,
      tone: factor.stale ? 'danger' : 'success',
      caption: factor.stale ? '已过期' : '仍在有效期内',
    },
  ];
});

const matchLevelRisks = computed<PublicSentimentRisk[]>(() => {
  if (!selectedMatch.value) {
    return [];
  }
  return selectedMatch.value.risks.filter((risk) => risk.factorId == null);
});
const matchRiskRings = computed(() => {
  const risks = matchLevelRisks.value;
  const total = risks.length;
  const highCount = risks.filter((risk) => ['CRITICAL', 'HIGH'].includes((risk.riskLevel || '').toUpperCase())).length;
  const mediumCount = risks.filter((risk) => (risk.riskLevel || '').toUpperCase() === 'MEDIUM').length;
  const lowCount = risks.filter((risk) => (risk.riskLevel || '').toUpperCase() === 'LOW').length;
  const unknownCount = Math.max(0, total - highCount - mediumCount - lowCount);
  const peakScore = risks
    .map((risk) => scorePercent(risk.riskScore))
    .filter((value): value is number => value != null)
    .sort((left, right) => right - left)[0] ?? null;
  const countMax = Math.max(1, total);
  return [
    {
      label: '高风险',
      value: highCount,
      max: countMax,
      unit: '条',
      tone: highCount ? 'danger' : 'success',
      caption: `${highCount} / ${total} 条比赛级风险`,
    },
    {
      label: '中风险',
      value: mediumCount,
      max: countMax,
      unit: '条',
      tone: mediumCount ? 'warning' : 'success',
      caption: `${mediumCount} / ${total} 条比赛级风险`,
    },
    {
      label: '低风险',
      value: lowCount,
      max: countMax,
      unit: '条',
      tone: lowCount ? 'success' : 'info',
      caption: `${lowCount} / ${total} 条比赛级风险`,
    },
    {
      label: '未定风险',
      value: unknownCount,
      max: countMax,
      unit: '条',
      tone: unknownCount ? 'warning' : 'success',
      caption: `${unknownCount} / ${total} 条比赛级风险`,
    },
    {
      label: '风险峰值',
      value: peakScore,
      max: 100,
      unit: '%',
      tone: peakScore == null ? 'info' : peakScore >= 70 ? 'danger' : peakScore >= 40 ? 'warning' : 'success',
      caption: peakScore == null ? '暂无风险分' : `最高分 ${peakScore}%`,
    },
  ];
});

function formatDateTime(value?: string): string {
  return value ? value.replace('T', ' ').slice(0, 16) : '待同步';
}

function scoreText(value?: number): string {
  return value == null ? '-' : Number(value).toFixed(1).replace(/\.0$/, '');
}

function scorePercent(value?: number): number | null {
  if (value == null || !Number.isFinite(value)) {
    return null;
  }
  if (value <= 1) {
    return Math.round(value * 100);
  }
  if (value <= 10) {
    return Math.round(value * 10);
  }
  return Math.max(0, Math.min(100, Math.round(value)));
}

function riskRank(level?: string): number {
  switch (level) {
    case 'CRITICAL':
      return 4;
    case 'HIGH':
      return 3;
    case 'MEDIUM':
      return 2;
    case 'LOW':
      return 1;
    default:
      return 0;
  }
}

function riskClass(level?: string): string {
  switch (level) {
    case 'CRITICAL':
    case 'HIGH':
      return 'risk-pill--danger';
    case 'MEDIUM':
      return 'risk-pill--warning';
    case 'LOW':
      return 'risk-pill--success';
    default:
      return 'risk-pill--info';
  }
}

function coverageTone(value: number): 'success' | 'warning' | 'danger' {
  if (value >= 75) {
    return 'success';
  }
  if (value >= 45) {
    return 'warning';
  }
  return 'danger';
}

function factorRiskCount(factor: PublicSentimentFactorDetail): number {
  if (!selectedMatch.value) {
    return 0;
  }
  return selectedMatch.value.risks.filter((risk) => risk.factorId === factor.id).length;
}

function clearFilters() {
  selectedCategory.value = '';
  selectedRiskLevel.value = '';
  searchQuery.value = '';
  staleOnly.value = false;
  currentPage.value = 1;
}

function goPage(offset: number) {
  currentPage.value = Math.min(totalPages.value, Math.max(1, currentPage.value + offset));
}

async function load() {
  loading.value = true;
  error.value = '';
  try {
    const [overviewResponse, categoryResponse, riskTypeResponse] = await Promise.all([
      listPublicSentimentOverview(),
      listPublicSentimentCategories(),
      listPublicSentimentRiskTypes(),
    ]);
    overview.value = overviewResponse.data;
    categories.value = categoryResponse.data;
    riskTypes.value = riskTypeResponse.data;
    const first = filteredOverview.value[0] ?? overview.value[0];
    if (first) {
      await openFactor(first);
    } else {
      selectedMatch.value = null;
      selectedFactorId.value = null;
    }
  } catch (cause) {
    overview.value = [];
    categories.value = [];
    riskTypes.value = [];
    selectedMatch.value = null;
    selectedFactorId.value = null;
    error.value = cause instanceof Error ? cause.message : '无法读取公开舆情与外部因素数据。';
  } finally {
    loading.value = false;
  }
}

async function openFactor(row: PublicSentimentFactorSummary) {
  const matchId = row.matchId;
  if (matchId == null) {
    detailError.value = '该因素暂未绑定比赛，无法查看比赛维度详情。';
    return;
  }
  detailLoading.value = true;
  detailError.value = '';
  try {
    const response = await getPublicMatchSentiment(matchId);
    selectedMatch.value = response.data;
    selectedFactorId.value = row.id;
  } catch (cause) {
    selectedMatch.value = null;
    detailError.value = cause instanceof Error ? cause.message : '无法读取公开舆情详情。';
  } finally {
    detailLoading.value = false;
  }
}

function selectFactor(factor: PublicSentimentFactorDetail) {
  selectedFactorId.value = factor.id;
}

function matchMeta(match?: PublicSentimentMatchDetail | PublicSentimentFactorSummary | null): string {
  if (!match) {
    return '比赛待同步';
  }
  const parts = [match.matchday, match.jcCode ? `竞彩 ${match.jcCode}` : ''].filter(Boolean);
  return parts.length ? parts.join(' · ') : '比赛待同步';
}

watch([selectedCategory, selectedRiskLevel, searchQuery, staleOnly], () => {
  currentPage.value = 1;
});

watch(filteredOverview, () => {
  if (currentPage.value > totalPages.value) {
    currentPage.value = totalPages.value;
  }
});

onMounted(load);
</script>

<template>
  <section class="page-shell evidence-page sentiment-page" aria-labelledby="sentiment-center-title">
    <section class="page-content sentiment-page__content">
      <header class="evidence-hero">
        <div>
          <p class="eyebrow">证据 · 舆情</p>
          <h1 id="sentiment-center-title">舆情与外部因素中心</h1>
          <p class="hero-risk">{{ headlineRisk }}</p>
        </div>
        <div class="hero-side">
          <button class="action-button" type="button" :disabled="loading" @click="load">
            {{ loading ? '刷新中' : '刷新公开数据' }}
          </button>
          <ScoreboardCard
            v-if="selectedMatch"
            class="hero-scoreboard"
            :home-team="selectedMatch.homeTeam"
            :away-team="selectedMatch.awayTeam"
            :scoreboard="selectedMatch.scoreboard"
            :match-name="selectedMatch.matchName"
            :meta="matchMeta(selectedMatch)"
            compact
          />
        </div>
      </header>

      <section class="stat-grid" aria-label="舆情统计">
        <article class="stat-card"><span>比赛</span><strong>{{ stats.matches }}</strong><small>公开因素覆盖</small></article>
        <article class="stat-card"><span>因素</span><strong>{{ stats.factors }}</strong><small>事实记录</small></article>
        <article class="stat-card"><span>风险</span><strong>{{ stats.risks }}</strong><small>风险评分项</small></article>
        <article class="stat-card"><span>过期</span><strong>{{ stats.stale }}</strong><small>时效记录</small></article>
      </section>

      <section class="sentiment-command-board" data-test="sentiment-command-board" aria-label="舆情外部情报态势" tabindex="0">
        <article class="command-summary">
          <p class="eyebrow">外因覆盖</p>
          <h2>外因覆盖与缺口</h2>
          <div class="global-sentiment-rings" aria-label="全站外因结构环形图">
            <CoverageDonut
              v-for="ring in globalSentimentRings"
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
          <MetricBar
            label="10 类外因覆盖"
            :value="globalIntelligence.covered.length"
            :max="dimensionDefinitions.length"
            tone="info"
            :caption="`已覆盖 ${globalIntelligence.covered.length} 类，缺失 ${globalIntelligence.missing.length} 类；过期和高风险状态同步展示。`"
          />
        </article>
        <article class="command-priority">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">高等级缺口</p>
              <h3>主要外因缺口</h3>
            </div>
            <span class="status-pill">按缺口排序</span>
          </div>
          <button
            v-for="dimension in globalPriorityCards"
            :key="dimension.key"
            class="command-card"
            :class="{ 'command-card--missing': !dimension.covered }"
            type="button"
            @click="applyGlobalDimensionAction(dimension)"
          >
            <span>{{ dimension.priority }}等级 · {{ dimension.covered ? `${dimension.count} 条` : '缺失' }}</span>
            <strong>{{ dimension.label }}</strong>
            <small>{{ dimension.covered ? dimension.description : dimension.nextAction }}</small>
            <em>{{ dimension.covered ? '已覆盖' : '缺口' }}</em>
          </button>
        </article>
        <article class="command-coverage" aria-label="舆情覆盖状态条">
          <MetricBar
            label="已覆盖维度"
            :value="globalIntelligence.covered.length"
            :max="dimensionDefinitions.length"
            tone="success"
            caption="有正式入库因素，可进入列表核来源。"
          />
          <MetricBar
            label="缺失维度"
            :value="globalIntelligence.missing.length"
            :max="dimensionDefinitions.length"
            :tone="globalIntelligence.missing.length ? 'warning' : 'success'"
            caption="缺失维度不显示空图表，等待正式数据同步。"
          />
          <MetricBar
            label="过期线索"
            :value="stats.stale"
            :max="Math.max(1, stats.factors)"
            :tone="stats.stale ? 'danger' : 'success'"
            caption="过期信息缺第二来源或官方记录。"
          />
        </article>
      </section>

      <section class="dimension-panel" data-test="sentiment-dimension-panel" aria-label="足球分析外部因素覆盖" tabindex="0">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">覆盖矩阵</p>
            <h2>AI 分析的 {{ dimensionDefinitions.length }} 类外部因素</h2>
          </div>
          <span class="status-pill">缺失维度已标出</span>
        </div>
        <div class="dimension-grid" tabindex="0" aria-label="10 类外部因素覆盖矩阵">
          <article
            v-for="dimension in dimensionCards"
            :key="dimension.key"
            class="dimension-card"
            :class="{ 'dimension-card--missing': !dimension.covered, 'dimension-card--spotlight': spotlightDimensionKey === dimension.key }"
          >
            <div class="dimension-card__top">
              <strong>{{ dimension.label }}</strong>
              <span class="risk-pill" :class="dimension.covered ? riskClass(dimension.highestRisk) : 'risk-pill--info'">
                {{ dimension.covered ? `${dimension.count} 条 · ${enumLabel('riskLevel', dimension.highestRisk)}` : '缺失' }}
              </span>
            </div>
            <p>{{ dimension.description }}</p>
            <MetricBar
              label="公开覆盖"
              :value="dimension.coverage"
              unit="%"
              :tone="dimension.tone"
              :caption="dimension.covered ? '已有正式库因素与来源记录' : '没有正式入库数据'"
            />
          </article>
        </div>
      </section>

      <div v-if="error" class="alert-panel" role="alert">{{ error }}</div>

      <section class="filter-panel" aria-label="舆情筛选" tabindex="0">
        <label>
          搜索线索
          <input
            v-model="searchQuery"
            aria-label="搜索比赛、因素、来源或摘要"
            autocomplete="off"
            placeholder="比赛 / 因素 / 来源"
            type="search"
          >
        </label>
        <label>
          因素分类
          <select v-model="selectedCategory">
            <option value="">全部分类</option>
            <option v-for="category in categories" :key="category" :value="category">
              {{ enumLabel('factorCategory', category) }}
            </option>
          </select>
        </label>
        <label>
          风险等级
          <select v-model="selectedRiskLevel">
            <option value="">全部等级</option>
            <option v-for="level in riskLevelOptions" :key="level.value" :value="level.value">{{ level.label }}</option>
          </select>
        </label>
        <label class="check-row">
          <input v-model="staleOnly" type="checkbox">
          只看过期提醒
        </label>
        <div class="risk-type-row" aria-label="风险类型">
          <span v-for="riskType in riskTypes" :key="riskType" class="type-chip">{{ enumLabel('riskType', riskType) }}</span>
          <span v-if="riskTypes.length === 0" class="muted-text">暂无风险类型</span>
        </div>
        <div class="filter-result-strip" aria-live="polite">
          <span>已筛出 {{ filteredOverview.length }} / {{ overview.length }} 条因素</span>
          <button v-if="hasActiveFilters" type="button" @click="clearFilters">清除筛选</button>
        </div>
      </section>

      <section class="evidence-grid">
        <aside class="side-panel" aria-label="外部因素列表" tabindex="0">
          <div class="panel-heading">
            <div><p class="eyebrow">因素</p><h2>因素记录</h2></div>
            <span class="count-pill">{{ filteredOverview.length }}</span>
          </div>
          <p v-if="loading && !overview.length" class="empty-copy">正在加载公开舆情...</p>
          <p v-else-if="!filteredOverview.length" class="empty-copy">暂无符合筛选条件的因素。</p>
          <div v-else class="factor-list-scroll" tabindex="0" aria-label="筛选后的外部因素结果">
            <article
              v-for="item in pagedOverview"
              :key="item.id"
              class="list-card"
              :class="{ 'list-card--active': item.id === selectedFactorId }"
            >
              <ScoreboardCard
                class="factor-match-card"
                :home-team="item.homeTeam"
                :away-team="item.awayTeam"
                :scoreboard="item.scoreboard"
                :match-name="item.matchName"
                :meta="matchMeta(item)"
                compact
              />
              <button class="list-card__trigger" type="button" @click="openFactor(item)">
                <span>{{ enumLabel('factorCategory', item.factorCategory) }} · {{ item.sourceName || '来源待同步' }}</span>
                <strong>{{ publicSentimentText(item.title, '线索待同步') }}</strong>
                <small>{{ publicSentimentText(item.summary, '暂无摘要') }}</small>
                <small>{{ item.stale ? '已过期' : '仍在有效期内' }}</small>
                <span class="risk-pill" :class="riskClass(item.highestRiskLevel)">{{ enumLabel('riskLevel', item.highestRiskLevel || 'UNKNOWN') }}</span>
              </button>
            </article>
          </div>
          <nav v-if="filteredOverview.length > pageSize" class="pager" aria-label="外部因素列表分页">
            <button type="button" :disabled="currentPage <= 1" @click="goPage(-1)">上一页</button>
            <span>第 {{ currentPage }} / {{ totalPages }} 页</span>
            <button type="button" :disabled="currentPage >= totalPages" @click="goPage(1)">下一页</button>
          </nav>
        </aside>

        <article class="detail-panel" tabindex="0">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">比赛详情</p>
              <h2>{{ selectedMatch?.matchName || '舆情详情' }}</h2>
            </div>
            <span v-if="selectedMatch" class="status-pill">竞彩 {{ selectedMatch.jcCode || '待定' }}</span>
          </div>

          <div v-if="detailError" class="alert-panel" role="alert">{{ detailError }}</div>
          <p v-else-if="detailLoading && !selectedMatch" class="empty-copy">正在加载舆情详情...</p>
          <p v-else-if="!selectedMatch" class="empty-copy">当前未选中因素。</p>

          <template v-else>
            <ScoreboardCard
              class="sentiment-match-card"
              :home-team="selectedMatch.homeTeam"
              :away-team="selectedMatch.awayTeam"
              :scoreboard="selectedMatch.scoreboard"
              :match-name="selectedMatch.matchName"
              :meta="matchMeta(selectedMatch)"
              :risk-count="selectedMatch.risks.length"
              :evidence-count="selectedMatch.factors.length"
            />

            <section class="intelligence-board" aria-label="本场外部情报准备度" tabindex="0">
              <article class="intelligence-score">
                <CoverageDonut
                  label="外部情报准备度"
                  :value="matchIntelligence.score"
                  :tone="coverageTone(matchIntelligence.score)"
                  caption="天气、伤停、裁判、旅行、市场热度等资料覆盖状态。"
                />
                <MetricBar
                  label="10 类线索准备度"
                  :value="matchIntelligence.score"
                  :max="100"
                  unit="%"
                  :tone="coverageTone(matchIntelligence.score)"
                  :caption="matchIntelligence.summary"
                />
              </article>
              <article class="intelligence-list intelligence-list--covered">
                <strong>本场已覆盖</strong>
                <ul>
                  <li v-for="dimension in matchIntelligence.covered.slice(0, 5)" :key="dimension.key">
                    {{ dimension.label }} · {{ dimension.count }} 条
                  </li>
                  <li v-if="!matchIntelligence.covered.length">暂无正式入库外部线索。</li>
                </ul>
              </article>
              <article class="intelligence-list intelligence-list--missing">
                <strong>主要缺口</strong>
                <ul>
                  <li v-for="dimension in priorityGaps.slice(0, 5)" :key="dimension.key">
                    {{ dimension.label }}：{{ dimension.nextAction }}
                  </li>
                  <li v-if="!priorityGaps.length">关键外部线索已基本覆盖，时效和冲突状态已展示。</li>
                </ul>
              </article>
            </section>

            <section class="priority-gap-panel" aria-label="缺口等级清单" tabindex="0">
              <div class="panel-heading">
                <div>
                  <p class="eyebrow">缺口等级</p>
                  <h3>本场 AI 分析线索缺口</h3>
                </div>
                <span class="status-pill">公开数据边界</span>
              </div>
              <div v-if="priorityGaps.length" class="priority-gap-grid" tabindex="0">
                <article v-for="dimension in priorityGaps" :key="dimension.key" class="priority-gap-card">
                  <span class="priority-pill" :class="dimension.priority === '高' ? 'priority-pill--high' : 'priority-pill--mid'">{{ dimension.priority }}等级</span>
                  <strong>{{ dimension.label }}</strong>
                  <p>{{ dimension.description }}</p>
                  <small>{{ dimension.nextAction }}</small>
                </article>
              </div>
              <p v-else class="empty-copy">本场 10 类外部线索都有正式入库数据；来源可信度、更新时间和冲突项已展示。</p>
            </section>

            <section class="factor-card-grid" aria-label="比赛因素" tabindex="0">
              <button
                v-for="factor in selectedMatch.factors"
                :key="factor.id"
                class="factor-card"
                :class="{ 'factor-card--active': factor.id === currentFactor?.id }"
                type="button"
                @click="selectFactor(factor)"
              >
                <span>{{ enumLabel('factorCategory', factor.factorCategory) }} · {{ enumLabel('factorType', factor.factorType, '类型待定') }}</span>
                <strong>{{ publicSentimentText(factor.title, '线索待同步') }}</strong>
                <small>{{ factor.sourceName || factor.sourceRef || '来源待同步' }} · 风险 {{ factorRiskCount(factor) }}</small>
              </button>
            </section>

            <section class="detail-card-grid" tabindex="0">
              <article class="info-card">
                <p class="eyebrow">当前因素</p>
                <h3>当前因素摘要</h3>
                <template v-if="currentFactor">
                  <div class="factor-state-rings" aria-label="当前因素状态环形图" tabindex="0">
                    <CoverageDonut
                      v-for="ring in currentFactorRings"
                      :key="ring.label"
                      :label="ring.label"
                      :value="ring.value"
                      :tone="ring.tone"
                      size="compact"
                      :caption="ring.caption"
                    />
                  </div>
                  <div class="summary-grid">
                    <div><span>影响方向</span><strong>{{ enumLabel('impactDirection', currentFactor.impactDirection, '-') }}</strong></div>
                    <div><span>证据等级</span><strong>{{ enumLabel('evidenceLevel', currentFactor.evidenceLevel, '-') }}</strong></div>
                    <div><span>证据把握分</span><strong>{{ scoreText(currentFactor.confidenceScore) }}</strong></div>
                    <div><span>来源可靠分</span><strong>{{ scoreText(currentFactor.reliabilityScore) }}</strong></div>
                  </div>
                  <p>{{ publicSentimentText(currentFactor.summary, '暂无摘要') }}</p>
                  <small>{{ formatDateTime(currentFactor.observedAt) }} · 过期时间 {{ formatDateTime(currentFactor.expiresAt) }}</small>
                </template>
                <p v-else class="empty-copy">暂无当前因素。</p>
              </article>

              <article class="info-card">
                <p class="eyebrow">因素风险</p>
                <h3>关联风险评分</h3>
                <div v-if="currentFactorRisks.length" class="risk-grid" tabindex="0">
                  <article v-for="risk in currentFactorRisks" :key="risk.id" class="risk-card">
                    <span class="risk-pill" :class="riskClass(risk.riskLevel)">{{ enumLabel('riskLevel', risk.riskLevel) }}</span>
                    <strong>{{ publicSentimentText(risk.title, '风险待同步') }}</strong>
                    <small>{{ enumLabel('riskType', risk.riskType) }} · 分数 {{ scoreText(risk.riskScore) }}</small>
                    <p>{{ publicRiskText(risk) }}</p>
                  </article>
                </div>
                <p v-else class="empty-copy">当前因素暂无风险评分。</p>
              </article>
            </section>

            <section class="info-card">
              <p class="eyebrow">比赛风险</p>
              <h3>比赛级风险评分</h3>
              <div class="match-risk-rings" aria-label="比赛级风险结构环形图" tabindex="0">
                <CoverageDonut
                  v-for="ring in matchRiskRings"
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
              <div v-if="matchLevelRisks.length" class="risk-grid" tabindex="0">
                <article v-for="risk in matchLevelRisks" :key="risk.id" class="risk-card">
                  <span class="risk-pill" :class="riskClass(risk.riskLevel)">{{ enumLabel('riskLevel', risk.riskLevel) }}</span>
                  <strong>{{ publicSentimentText(risk.title, '风险待同步') }}</strong>
                  <small>{{ enumLabel('riskType', risk.riskType) }} · 分数 {{ scoreText(risk.riskScore) }}</small>
                  <p>{{ publicRiskText(risk) }}</p>
                </article>
              </div>
              <p v-else class="empty-copy">暂无比赛级风险评分。</p>
            </section>
          </template>
        </article>
      </section>
    </section>
  </section>
</template>

<style scoped>
.evidence-page { max-width: 100%; overflow-x: hidden; }
.sentiment-page__content { display: grid; gap: 18px; min-width: 0; }
.evidence-hero, .stat-card, .sentiment-command-board, .sentiment-data-panel, .sentiment-state-panel, .dimension-panel, .filter-panel, .side-panel, .detail-panel, .info-card, .alert-panel, .intelligence-board, .priority-gap-panel {
  background: var(--wc-glass);
  border: 1px solid var(--wc-border);
  border-radius: var(--wc-radius-lg);
  color: var(--wc-text);
}
.evidence-hero {
  align-items: center;
  display: grid;
  gap: 18px;
  grid-template-columns: minmax(0, 1fr) minmax(0, 420px);
  padding: clamp(20px, 4vw, 38px);
}
.evidence-hero h1 {
  font-family: var(--wc-font-display);
  font-size: clamp(34px, 5vw, 60px);
  line-height: 1;
  margin: 0 0 12px;
}
.evidence-hero p:not(.eyebrow), .empty-copy, .list-card span, .list-card small, .factor-card span, .factor-card small, .summary-grid span, .summary-grid strong, .risk-card small, .risk-card p, .muted-text {
  color: var(--wc-text-muted);
  overflow-wrap: anywhere;
}
.hero-risk {
  background: rgba(217, 119, 6, .12);
  border: 1px solid rgba(217, 119, 6, .22);
  border-radius: 16px;
  color: var(--wc-warning) !important;
  font-weight: 800;
  margin-top: 14px;
  padding: 10px 12px;
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
.hero-side {
  align-self: stretch;
  display: grid;
  gap: 12px;
  min-width: 0;
}
.hero-side .action-button {
  align-self: start;
  justify-self: end;
  width: auto;
}
.hero-scoreboard {
  align-self: end;
}
.stat-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}
.sentiment-data-panel {
  display: grid;
  gap: 14px;
  grid-template-columns: minmax(0, .9fr) minmax(0, 1.1fr);
  min-width: 0;
  padding: 18px;
}
.sentiment-bars {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  min-width: 0;
}
.sentiment-state-panel {
  background: rgba(15, 23, 42, .45);
  border-color: rgba(217, 119, 6, .22);
  display: grid;
  gap: 8px;
  padding: 16px;
}
.sentiment-state-panel h2 {
  font-size: 20px;
  margin: 0;
}
.sentiment-state-panel ol {
  color: var(--wc-text-muted);
  display: grid;
  gap: 6px;
  line-height: 1.55;
  margin: 0;
  padding-left: 18px;
}
.dimension-panel {
  display: grid;
  gap: 14px;
  min-width: 0;
  padding: 18px;
}
.dimension-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  min-width: 0;
}
.dimension-grid:focus {
  border-radius: var(--wc-radius-md);
  box-shadow: var(--wc-focus-ring);
  outline: none;
}
.dimension-card {
  background: rgba(15, 23, 42, .5);
  border: 1px solid rgba(147, 197, 253, .18);
  border-radius: var(--wc-radius-md);
  display: grid;
  gap: 10px;
  min-width: 0;
  padding: 14px;
}
.dimension-card--missing {
  background: rgba(15, 23, 42, .34);
  border-style: dashed;
}
.dimension-card--spotlight {
  border-color: rgba(217, 119, 6, .75);
  box-shadow: 0 0 0 3px rgba(217, 119, 6, .12);
}
.dimension-card__top {
  align-items: start;
  display: flex;
  gap: 10px;
  justify-content: space-between;
}
.dimension-card__top strong {
  line-height: 1.25;
}
.dimension-card p {
  color: var(--wc-text-muted);
  font-size: 13px;
  line-height: 1.55;
  margin: 0;
}
.stat-card, .side-panel, .detail-panel, .info-card, .alert-panel, .intelligence-board, .priority-gap-panel {
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
.sentiment-command-board {
  display: grid;
  gap: 14px;
  grid-template-columns: minmax(0, .9fr) minmax(0, 1.2fr) minmax(0, .95fr);
  min-width: 0;
  padding: 18px;
}
.command-summary,
.command-priority,
.command-coverage {
  background: rgba(15, 23, 42, .45);
  border: 1px solid rgba(147, 197, 253, .18);
  border-radius: var(--wc-radius-md);
  display: grid;
  gap: 12px;
  min-width: 0;
  padding: 16px;
}
.command-summary h2,
.command-priority h3 {
  margin: 0;
}
.command-summary > strong {
  color: #fde68a;
  font-family: var(--wc-font-mono);
  font-size: clamp(42px, 6vw, 68px);
  line-height: .95;
}
.command-summary p:not(.eyebrow),
.command-card small {
  color: var(--wc-text-muted);
  line-height: 1.55;
  margin: 0;
}
.global-sentiment-rings {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(132px, 1fr));
  min-width: 0;
}
.global-sentiment-rings :deep(.coverage-donut) {
  gap: 10px;
}
.global-sentiment-rings :deep(.coverage-donut__ring) {
  width: 84px;
}
.global-sentiment-rings :deep(.coverage-donut__copy small) {
  line-height: 1.35;
}
.command-card {
  background: rgba(15, 23, 42, .55);
  border: 1px solid rgba(147, 197, 253, .2);
  border-radius: var(--wc-radius-md);
  color: var(--wc-text);
  cursor: pointer;
  display: grid;
  gap: 6px;
  min-height: 118px;
  padding: 14px;
  text-align: left;
  transition: border-color 180ms ease, transform 180ms ease;
}
.command-card--missing {
  border-color: rgba(245, 158, 11, .36);
  border-style: dashed;
}
.command-card:hover {
  border-color: rgba(217, 119, 6, .62);
  transform: translateY(-1px);
}
.command-card:focus-visible {
  box-shadow: var(--wc-focus-ring);
  outline: none;
}
.command-card span,
.command-card em {
  color: var(--wc-warning);
  font-family: var(--wc-font-mono);
  font-size: 12px;
  font-style: normal;
  font-weight: 900;
}
.command-coverage {
  align-content: start;
}
.filter-panel {
  align-items: end;
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  max-height: min(64dvh, 520px);
  min-width: 0;
  overflow: auto;
  padding: 16px;
}
.filter-panel label {
  color: var(--wc-text-muted);
  display: grid;
  font-size: 13px;
  font-weight: 800;
  gap: 8px;
}
.filter-panel input,
.filter-panel select {
  background: rgba(15, 23, 42, .66);
  border: 1px solid rgba(147, 197, 253, .22);
  border-radius: 14px;
  color: var(--wc-text);
  font: inherit;
  min-height: 44px;
  min-width: 0;
  padding: 0 12px;
}
.filter-panel input:focus,
.filter-panel select:focus {
  box-shadow: var(--wc-focus-ring);
  outline: none;
}
.check-row {
  align-items: center;
  background: rgba(15, 23, 42, .5);
  border: 1px solid rgba(147, 197, 253, .18);
  border-radius: 14px;
  cursor: pointer;
  display: flex !important;
  gap: 8px;
  min-height: 44px;
  padding: 0 12px;
}
.check-row input {
  accent-color: var(--wc-accent);
  flex: 0 0 20px;
  height: 20px;
  min-height: 20px;
  min-width: 20px;
  width: 20px;
}
.check-row:focus-within {
  box-shadow: var(--wc-focus-ring);
}
.risk-type-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
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
.type-chip, .risk-pill, .count-pill, .status-pill {
  border-radius: 999px;
  font-family: var(--wc-font-mono);
  font-size: 12px;
  font-weight: 800;
  overflow-wrap: anywhere;
  padding: 6px 9px;
}
.type-chip, .count-pill, .status-pill {
  background: rgba(147, 197, 253, .12);
  color: var(--wc-primary);
}
.evidence-grid {
  align-items: start;
  display: grid;
  gap: 16px;
  grid-template-columns: minmax(0, 360px) minmax(0, 1fr);
  min-width: 0;
}
.sentiment-command-board,
.dimension-panel,
.dimension-grid,
.filter-panel,
.side-panel,
.detail-panel,
.intelligence-board,
.priority-gap-panel,
.priority-gap-grid,
.factor-card-grid,
.detail-card-grid,
.factor-state-rings,
.match-risk-rings,
.risk-grid {
  scrollbar-color: rgba(217, 119, 6, .42) rgba(15, 23, 42, .28);
  scrollbar-gutter: stable both-edges;
}
.detail-panel, .side-panel, .intelligence-board, .priority-gap-panel { align-content: start; }
.side-panel {
  max-height: min(76dvh, 680px);
  overflow: auto;
}
.detail-panel {
  max-height: min(78dvh, 720px);
  overflow: auto;
}
.sentiment-command-board:focus-visible,
.dimension-panel:focus-visible,
.dimension-grid:focus-visible,
.filter-panel:focus-visible,
.side-panel:focus-visible,
.detail-panel:focus-visible,
.intelligence-board:focus-visible,
.priority-gap-panel:focus-visible,
.priority-gap-grid:focus-visible,
.factor-card-grid:focus-visible,
.detail-card-grid:focus-visible,
.factor-state-rings:focus-visible,
.match-risk-rings:focus-visible,
.risk-grid:focus-visible {
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
.list-card, .factor-card, .risk-card, .summary-grid div {
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
.factor-match-card {
  padding: 12px;
}
.factor-list-scroll {
  display: grid;
  gap: 12px;
  max-height: min(62dvh, 700px);
  min-width: 0;
  overflow: auto;
  padding-right: 4px;
  scrollbar-color: rgba(147, 197, 253, .38) transparent;
}
.factor-list-scroll:focus {
  outline: 3px solid rgba(59, 130, 246, .32);
  outline-offset: 3px;
}
.list-card__trigger {
  background: transparent;
  border: 0;
  color: inherit;
  cursor: pointer;
  display: grid;
  gap: 6px;
  min-height: 44px;
  padding: 0;
  text-align: left;
}
.list-card__trigger:focus-visible {
  border-radius: 14px;
  box-shadow: var(--wc-focus-ring);
  outline: none;
}
.list-card__trigger .risk-pill {
  justify-self: start;
}
.factor-card {
  cursor: pointer;
  transition: border-color 180ms ease, transform 180ms ease;
}
.list-card--active, .factor-card--active { border-color: rgba(217, 119, 6, .62); }
.sentiment-match-card {
  margin-bottom: 4px;
}
.intelligence-board {
  grid-template-columns: minmax(0, .9fr) minmax(0, 1fr) minmax(0, 1.2fr);
  max-height: min(78dvh, 700px);
  overflow: auto;
}
.intelligence-score,
.intelligence-list {
  background: rgba(15, 23, 42, .45);
  border: 1px solid rgba(147, 197, 253, .16);
  border-radius: var(--wc-radius-md);
  display: grid;
  gap: 10px;
  min-width: 0;
  padding: 14px;
}
.intelligence-score > strong {
  color: var(--wc-primary);
  font-family: var(--wc-font-mono);
  font-size: clamp(42px, 6vw, 72px);
  line-height: .95;
}
.intelligence-score > span {
  color: var(--wc-warning);
  font-weight: 900;
}
.intelligence-score > small {
  color: var(--wc-text-muted);
  line-height: 1.5;
}
.intelligence-list strong {
  color: var(--wc-text);
  font-size: 16px;
}
.intelligence-list ul {
  display: grid;
  gap: 8px;
  margin: 0;
  padding-left: 18px;
}
.intelligence-list li {
  color: var(--wc-text-muted);
  line-height: 1.5;
}
.intelligence-list--covered { border-color: rgba(34, 197, 94, .26); }
.intelligence-list--missing { border-color: rgba(245, 158, 11, .28); }
.priority-gap-panel {
  gap: 14px;
  max-height: min(72dvh, 620px);
  overflow: auto;
}
.priority-gap-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  max-height: min(64dvh, 560px);
  min-width: 0;
  overflow: auto;
  padding-right: 2px;
}
.priority-gap-card {
  background: rgba(15, 23, 42, .45);
  border: 1px dashed rgba(245, 158, 11, .34);
  border-radius: var(--wc-radius-md);
  display: grid;
  gap: 8px;
  min-width: 0;
  padding: 14px;
}
.priority-gap-card p,
.priority-gap-card small {
  color: var(--wc-text-muted);
  line-height: 1.55;
  margin: 0;
}
.priority-pill {
  border-radius: 999px;
  font-family: var(--wc-font-mono);
  font-size: 12px;
  font-weight: 900;
  justify-self: start;
  padding: 6px 9px;
}
.priority-pill--high { background: rgba(239, 68, 68, .16); color: #fecaca; }
.priority-pill--mid { background: rgba(245, 158, 11, .18); color: #fde68a; }
.factor-card-grid, .detail-card-grid, .risk-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  min-width: 0;
}
.factor-card-grid {
  max-height: min(64dvh, 560px);
  overflow: auto;
  padding-right: 2px;
}
.detail-card-grid {
  max-height: min(74dvh, 640px);
  overflow: auto;
  padding-right: 2px;
}
.risk-grid {
  max-height: min(62dvh, 520px);
  overflow: auto;
  padding-right: 2px;
}
.summary-grid {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  min-width: 0;
}
.factor-state-rings {
  display: grid;
  gap: 13px;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  max-height: 520px;
  min-width: 0;
  overflow: auto;
  padding-right: 2px;
}
.match-risk-rings {
  display: grid;
  gap: 13px;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  max-height: 520px;
  min-width: 0;
  overflow: auto;
  padding-right: 2px;
}
.risk-pill--danger { background: rgba(239, 68, 68, .16); color: #fecaca; }
.risk-pill--warning { background: rgba(245, 158, 11, .18); color: #fde68a; }
.risk-pill--success { background: rgba(34, 197, 94, .16); color: #bbf7d0; }
.risk-pill--info { background: rgba(147, 197, 253, .12); color: var(--wc-primary); }
@media (max-width: 1024px) {
  .evidence-hero, .sentiment-command-board, .sentiment-data-panel, .evidence-grid, .intelligence-board { grid-template-columns: 1fr; }
  .hero-side .action-button { justify-self: start; }
  .stat-grid, .filter-panel, .sentiment-bars, .dimension-grid, .priority-gap-grid, .factor-card-grid, .detail-card-grid, .risk-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 640px) {
  .evidence-hero, .sentiment-command-board, .sentiment-data-panel, .filter-panel, .sentiment-bars, .evidence-grid, .intelligence-board, .priority-gap-grid, .factor-card-grid, .detail-card-grid, .risk-grid, .summary-grid { grid-template-columns: 1fr; }
  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .summary-grid,
  .priority-gap-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .stat-card {
    gap: 6px;
    padding: 14px;
  }
  .stat-card strong {
    font-size: 30px;
  }
  .global-sentiment-rings,
  .dimension-grid,
  .factor-state-rings,
  .match-risk-rings {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .global-sentiment-rings :deep(.coverage-donut__ring),
  .factor-state-rings :deep(.coverage-donut__ring),
  .match-risk-rings :deep(.coverage-donut__ring) {
    width: 72px;
  }
  .global-sentiment-rings :deep(.coverage-donut__ring span),
  .factor-state-rings :deep(.coverage-donut__ring span),
  .match-risk-rings :deep(.coverage-donut__ring span) {
    font-size: 22px;
  }
  .global-sentiment-rings :deep(.coverage-donut__copy strong),
  .factor-state-rings :deep(.coverage-donut__copy strong),
  .match-risk-rings :deep(.coverage-donut__copy strong) {
    font-size: 13px;
  }
  .global-sentiment-rings :deep(.coverage-donut__copy small),
  .factor-state-rings :deep(.coverage-donut__copy small),
  .match-risk-rings :deep(.coverage-donut__copy small) {
    display: none;
  }
  .evidence-hero p:not(.eyebrow):not(.hero-risk),
  .sentiment-command-board :deep(.metric-bar small),
  .dimension-grid :deep(.metric-bar small),
  .intelligence-score :deep(.coverage-donut__copy small),
  .intelligence-score :deep(.metric-bar small),
  .priority-gap-card p,
  .priority-gap-card small {
    display: none;
  }
  .evidence-hero,
  .stat-card,
  .sentiment-command-board,
  .dimension-panel,
  .filter-panel,
  .side-panel,
  .detail-panel,
  .info-card,
  .intelligence-board,
  .priority-gap-panel {
    padding: 14px;
  }
  .filter-panel,
  .side-panel,
  .detail-panel,
  .intelligence-board,
  .priority-gap-panel,
  .detail-card-grid {
    max-height: none;
    overflow: visible;
    scrollbar-gutter: auto;
  }
  .dimension-card {
    gap: 8px;
    padding: 10px;
  }
  .dimension-card p {
    display: none;
  }
  .dimension-card__top {
    align-items: start;
    display: grid;
  }
  .dimension-card__top .risk-pill {
    justify-self: start;
  }
  .command-priority {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .command-priority .panel-heading {
    grid-column: 1 / -1;
  }
  .command-card {
    min-height: 0;
    padding: 10px;
  }
  .command-card small {
    display: none;
  }

  .hero-scoreboard :deep(.scoreboard-card__main),
  .factor-match-card :deep(.scoreboard-card__main) {
    grid-template-columns: minmax(0, 1fr) 78px minmax(0, 1fr);
  }
  .hero-scoreboard :deep(.scoreboard-card__team),
  .factor-match-card :deep(.scoreboard-card__team) {
    justify-items: start;
    padding: 0;
    text-align: left;
  }
  .hero-scoreboard :deep(.scoreboard-card__team .flag-team),
  .factor-match-card :deep(.scoreboard-card__team .flag-team) {
    align-items: center;
    flex-direction: row;
    gap: 6px;
  }
  .hero-scoreboard :deep(.scoreboard-card__team--away .flag-team),
  .factor-match-card :deep(.scoreboard-card__team--away .flag-team) {
    flex-direction: row-reverse;
  }
  .hero-scoreboard :deep(.flag-team__flag),
  .factor-match-card :deep(.flag-team__flag) {
    width: 32px;
  }
  .hero-scoreboard :deep(.flag-team__copy small),
  .factor-match-card :deep(.flag-team__copy small),
  .factor-match-card :deep(.scoreboard-card__match-name) {
    display: none;
  }
  .hero-scoreboard :deep(.scoreboard-card__score),
  .factor-match-card :deep(.scoreboard-card__score) {
    min-width: 0;
    padding: 7px 5px;
    width: 78px;
  }
  .hero-scoreboard :deep(.scoreboard-card__score strong),
  .factor-match-card :deep(.scoreboard-card__score strong) {
    font-size: 18px;
  }

  .sentiment-match-card :deep(.scoreboard-card__main) {
    grid-template-columns: minmax(0, 1fr) 92px minmax(0, 1fr);
  }
  .sentiment-match-card :deep(.scoreboard-card__team) {
    padding: 8px;
  }
  .sentiment-match-card :deep(.scoreboard-card__team .flag-team) {
    align-items: center;
    flex-direction: row;
    gap: 6px;
  }
  .sentiment-match-card :deep(.scoreboard-card__team--away .flag-team) {
    flex-direction: row-reverse;
  }
  .sentiment-match-card :deep(.flag-team__flag) {
    width: 34px;
  }
  .sentiment-match-card :deep(.flag-team__copy small) {
    display: none;
  }
  .sentiment-match-card :deep(.scoreboard-card__score) {
    min-width: 0;
    padding: 8px 6px;
  }
  .sentiment-match-card :deep(.scoreboard-card__score strong) {
    font-size: 24px;
  }
  .sentiment-match-card :deep(.scoreboard-card__signals) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .sentiment-match-card :deep(.scoreboard-card__signals .metric-bar) {
    grid-column: 1 / -1;
  }
  .intelligence-score :deep(.coverage-donut) {
    align-items: center;
    grid-template-columns: auto minmax(0, 1fr);
  }
  .intelligence-score :deep(.coverage-donut__ring) {
    width: 82px;
  }
  .intelligence-score :deep(.coverage-donut__ring span) {
    font-size: 23px;
  }
  .factor-list-scroll {
    max-height: min(50dvh, 420px);
    overflow: auto;
    padding-right: 0;
    scrollbar-gutter: auto;
    scrollbar-width: none;
  }
  .factor-list-scroll::-webkit-scrollbar {
    display: none;
  }
  .intelligence-list {
    max-height: none;
    overflow: visible;
    scrollbar-gutter: auto;
  }
  .priority-gap-grid,
  .factor-card-grid,
  .risk-grid {
    max-height: none;
    overflow: visible;
    padding-right: 0;
    scrollbar-gutter: auto;
  }
  .detail-card-grid > .info-card,
  .detail-panel > .info-card {
    max-height: none;
    overflow: visible;
    padding-right: 14px;
    scrollbar-gutter: auto;
  }
  .panel-heading { align-items: stretch; flex-direction: column; }
  .action-button { width: 100%; }
}
@media (prefers-reduced-motion: reduce) {
  .factor-card, .command-card { transition: none; }
  .command-card:hover { transform: none; }
}
</style>
