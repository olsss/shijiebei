const labelMaps = {
  matchStatus: {
    SCHEDULED: '未开赛',
    PRE_MATCH: '赛前',
    LIVE: '进行中',
    IN_PROGRESS: '进行中',
    FINISHED: '已完赛',
    FINAL: '已完赛',
    POSTPONED: '延期',
    CANCELLED: '取消',
    UNKNOWN: '待确认',
  },
  integrityStatus: {
    PASS: '通过',
    COMPLETE: '完整',
    PARTIAL: '部分完整',
    STALE: '需更新',
    PENDING: '待确认',
    MISSING: '缺失',
    CONFLICT: '有冲突',
    BLOCKED: '阻塞',
    UNKNOWN: '待确认',
  },
  severity: {
    CRITICAL: '极高',
    HIGH: '高',
    MEDIUM: '中',
    LOW: '低',
    INFO: '提示',
    UNKNOWN: '待评估',
  },
  riskLevel: {
    CRITICAL: '极高风险',
    HIGH: '高风险',
    MEDIUM: '中风险',
    LOW: '低风险',
    UNKNOWN: '待评估',
  },
  oddsSnapshot: {
    LIVE: '实时盘口',
    PRE_MATCH: '赛前快照',
    POST_MATCH_ARCHIVED: '赛后归档',
  },
  oddsSelectionStatus: {
    OPEN: '可投注',
    ACTIVE: '可用',
    AVAILABLE: '可用',
    AVAILABLE_OR_ARCHIVED: '可用/归档',
    SUSPENDED: '暂停',
    CLOSED: '已关闭',
    SETTLED: '已结算',
  },
  marketCode: {
    H2H_1X2: '胜平负',
    SPREAD: '让球/让分',
    TOTAL_GOALS: '总进球',
    HAD: '胜平负',
    HHAD: '让球胜平负',
  },
  impactDirection: {
    POSITIVE: '利好',
    NEGATIVE: '利空',
    NEUTRAL: '中性',
    MIXED: '多空混合',
    UNKNOWN: '影响待评估',
  },
  evidenceLevel: {
    OFFICIAL: '官方来源',
    VERIFIED: '已验证',
    HIGH: '高可信',
    MEDIUM: '中可信',
    LOW: '低可信',
    STRUCTURED_API: '结构化接口',
    WEB_PAGE: '网页来源',
    UNKNOWN: '待确认',
  },
  factorCategory: {
    WEATHER: '天气',
    VENUE: '场地',
    REFEREE: '裁判',
    INJURY: '伤停',
    TRAVEL: '旅途',
    PUBLIC_OPINION: '舆论',
    OTHER: '其他',
  },
  factorType: {
    BROADCAST: '转播/赛程信息',
    STADIUM: '球场',
    REFEREE: '裁判',
    ATTENDANCE: '上座/观众',
    WEATHER: '天气',
    INJURY: '伤停',
    NEWS: '新闻',
    OTHER: '其他',
  },
  riskType: {
    PACE: '比赛节奏',
    RAIN: '降雨',
    PUBLIC_OVERHEAT: '热度过高',
    INJURY: '伤停',
    WEATHER: '天气',
    LINEUP: '阵容',
    ODDS: '赔率',
    TRAVEL: '旅途',
    PUBLIC_OPINION: '舆论',
    UNKNOWN: '待评估',
  },
  sourceType: {
    OFFICIAL: '官方来源',
    SCHEDULE_RESULT: '赛程赛果',
    WEB_MATCH_PAGE: '比赛网页',
    STRUCTURED_SUMMARY: '结构化摘要',
    STRUCTURED_API: '结构化接口',
    SCOREBOARD_STATUS: '比分牌状态',
    NEWS: '新闻来源',
    ODDS: '赔率来源',
    SENTIMENT: '舆情来源',
  },
  eventType: {
    KICKOFF: '开球',
    START_DELAY: '开赛延迟',
    END_DELAY: '结束延迟',
    START_2ND_HALF: '下半场开始',
    HALFTIME: '半场结束',
    END_REGULAR_TIME: '常规时间结束',
    GOAL: '进球',
    GOAL_HEADER: '头球进球',
    GOAL_VOLLEY: '凌空进球',
    GOAL_FREE_KICK: '任意球进球',
    OWN_GOAL: '乌龙球',
    PENALTY_GOAL: '点球进球',
    PENALTY_SCORED: '点球命中',
    PENALTY_MISSED: '点球罚失',
    YELLOW_CARD: '黄牌',
    RED_CARD: '红牌',
    VAR_RED_CARD_UPGRADE: 'VAR 升级红牌',
    SUBSTITUTION: '换人',
    HALF_TIME: '半场',
    FULL_TIME: '全场结束',
  },
  statsType: {
    ESPN_FULL_TIME: 'ESPN 全场统计',
    RECENT: '近期表现',
    FULL_TIME: '全场统计',
  },
  conflictType: {
    KICKOFF: '开球时间',
    SCORE: '比分',
    LINEUP: '阵容',
    ODDS: '赔率',
    STATUS: '比赛状态',
    SOURCE: '来源差异',
    MATCH_HOME_AWAY_ORDER_CONFLICT: '主客队顺序冲突',
    MATCH_RESULT_STATUS_CONFLICT: '赛果状态冲突',
    MATCH_STATUS_CONFLICT: '比赛状态冲突',
  },
  fieldName: {
    'home_team_key/away_team_key': '主客队标识',
    result_status: '赛果状态',
    status: '比赛状态',
    kickoffTime: '开球时间',
    kickoff_time: '开球时间',
  },
  resolutionStatus: {
    PENDING: '待处理',
    RESOLVED: '已处理',
    REJECTED: '已驳回',
    AUTO_RESOLVED: '自动处理',
    UNKNOWN: '待确认',
  },
  position: {
    SUB: '替补席',
    GK: '门将',
    DF: '后卫',
    MF: '中场',
    FW: '前锋',
    G: '门将',
    D: '后卫',
    M: '中场',
    F: '前锋',
    CD: '中后卫',
    'CD-R': '右中卫',
    'CD-L': '左中卫',
    RB: '右后卫',
    LB: '左后卫',
    SW: '清道夫',
    DM: '防守型中场',
    CM: '中前卫',
    'CM-R': '右中前卫',
    'CM-L': '左中前卫',
    LM: '左中场',
    RM: '右中场',
    AM: '前腰',
    'AM-R': '右前腰',
    'AM-L': '左前腰',
    CF: '中锋',
    'CF-R': '右中锋',
    'CF-L': '左中锋',
    RCF: '右中锋',
    RF: '右边锋',
    LF: '左边锋',
  },
  playerStatus: {
    SQUAD_LISTED: '名单内',
    FIT: '可出场',
    INJURED: '伤停',
    SUSPENDED: '停赛',
    DOUBTFUL: '出战存疑',
    UNKNOWN: '待同步',
  },
  lineupRole: {
    STARTER: '首发',
    SUBSTITUTE: '替补',
    BENCH: '替补',
    CAPTAIN: '队长',
    CORE: '核心',
  },
  factType: {
    STYLE: '风格',
    FORM: '状态',
    INJURY: '伤病',
    TACTIC: '战术',
    PROFILE: '画像',
    HISTORY: '历史',
    NEWS: '新闻',
    OTHER: '其他',
  },
  conclusionType: {
    LEAN_HOME: '倾向主队',
    LEAN_AWAY: '倾向客队',
    LEAN_DRAW: '倾向平局',
    WATCH_ONLY: '仅观察',
    NO_BET: '不建议介入',
    UNKNOWN: '结论待定',
  },
  lessonType: {
    MARKET: '盘口经验',
    RISK: '风险经验',
    TEAM: '球队经验',
    PLAYER: '球员经验',
    TACTIC: '战术经验',
    DATA: '数据经验',
    OTHER: '其他经验',
  },
} as const;

type LabelMapName = keyof typeof labelMaps;

function toDisplayText(value?: string | number | null): string {
  if (value == null || value === '') {
    return '';
  }
  return String(value);
}

export function enumLabel(mapName: LabelMapName, value?: string | number | null, fallback = '待同步'): string {
  const text = toDisplayText(value);
  if (!text) {
    return fallback;
  }
  const map = labelMaps[mapName] as Record<string, string>;
  return map[text] ?? text;
}

export function positionLabel(value?: string | null): string {
  const text = toDisplayText(value);
  if (!text) {
    return '位置待定';
  }
  return enumLabel('position', text, '位置待定');
}

export function lineupRoleLabel(value?: string | null): string {
  const text = toDisplayText(value);
  if (!text) {
    return '角色待定';
  }
  return enumLabel('lineupRole', text, '角色待定');
}

export function matchStatusLabel(value?: string | null): string {
  return enumLabel('matchStatus', value, '状态待同步');
}

export function sourceTypeLabel(value?: string | null): string {
  return enumLabel('sourceType', value, '来源类型待定');
}

export function factTypeLabel(value?: string | null): string {
  return enumLabel('factType', value, '事实类型待定');
}

export function marketLabel(code?: string | null, name?: string | null): string {
  const codeLabel = enumLabel('marketCode', code, '');
  if (name && codeLabel && name !== code) {
    return `${codeLabel}（${name}）`;
  }
  return codeLabel || name || '玩法待定';
}

export function fieldNameLabel(value?: string | null): string {
  return enumLabel('fieldName', value, '字段待定');
}

export function readablePublicText(value?: string | null, fallback = '暂无公开摘要。'): string {
  const text = value?.trim();
  if (!text) {
    return fallback;
  }
  return text
    .replace(/ESPN状态=/gi, 'ESPN 状态：')
    .replace(/ESPN status=/gi, 'ESPN 状态：')
    .replace(/比分=/g, '比分：')
    .replace(/detail=/gi, '详情：')
    .replace(/None-None/gi, '待定')
    .replace(/\bSCHEDULED\b/g, '未开赛')
    .replace(/\bFINISHED\b/g, '已完赛')
    .replace(/\bFINAL\b/g, '已完赛')
    .replace(/\bUNKNOWN\b/g, '待确认')
    .replace(/;/g, '；');
}
