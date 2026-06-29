export type WinnerSide = 'HOME' | 'AWAY' | 'DRAW' | 'UNKNOWN' | string;

export interface TeamVisual {
  teamId?: number | null;
  teamName?: string | null;
  fifaCode?: string | null;
  countryIso2?: string | null;
  flagUrl?: string | null;
  countryRegion?: string | null;
}

export interface Scoreboard {
  homeScore?: number | null;
  awayScore?: number | null;
  scoreDisplay?: string | null;
  winnerSide?: WinnerSide | null;
  resultText?: string | null;
  scoreSource?: string | null;
}

const FIFA_TO_ISO: Record<string, string> = {
  AFG: 'af', ALB: 'al', ALG: 'dz', AND: 'ad', ANG: 'ao', ARG: 'ar', ARM: 'am', ARU: 'aw', AUS: 'au', AUT: 'at', AZE: 'az',
  BAH: 'bs', BHR: 'bh', BAN: 'bd', BAR: 'bb', BEL: 'be', BEN: 'bj', BER: 'bm', BIH: 'ba', BLR: 'by', BOL: 'bo', BRA: 'br', BUL: 'bg', BFA: 'bf',
  CAM: 'kh', CMR: 'cm', CAN: 'ca', CPV: 'cv', CHI: 'cl', CHN: 'cn', COL: 'co', COM: 'km', CGO: 'cg', COD: 'cd', CRC: 'cr', CRO: 'hr', CUB: 'cu', CUW: 'cw', CYP: 'cy', CZE: 'cz',
  DEN: 'dk', DOM: 'do', ECU: 'ec', EGY: 'eg', ENG: 'gb-eng', EQG: 'gq', ESP: 'es', EST: 'ee', ETH: 'et',
  FIN: 'fi', FRA: 'fr', GAB: 'ga', GAM: 'gm', GEO: 'ge', GER: 'de', GHA: 'gh', GRE: 'gr', GUA: 'gt', GUI: 'gn',
  HAI: 'ht', HON: 'hn', HUN: 'hu', IDN: 'id', IND: 'in', IRN: 'ir', IRQ: 'iq', IRL: 'ie', ISL: 'is', ISR: 'il', ITA: 'it', CIV: 'ci',
  JAM: 'jm', JPN: 'jp', JOR: 'jo', KAZ: 'kz', KEN: 'ke', KOR: 'kr', KSA: 'sa', KUW: 'kw', KGZ: 'kg',
  LBN: 'lb', LBR: 'lr', LBY: 'ly', LTU: 'lt', LUX: 'lu', MAD: 'mg', MAS: 'my', MLI: 'ml', MAR: 'ma', MEX: 'mx', MDA: 'md', MKD: 'mk', MNE: 'me', MOZ: 'mz',
  NED: 'nl', NZL: 'nz', NGA: 'ng', NIR: 'gb-nir', NOR: 'no', OMA: 'om', PAK: 'pk', PAN: 'pa', PAR: 'py', PER: 'pe', PHI: 'ph', POL: 'pl', POR: 'pt',
  QAT: 'qa', ROU: 'ro', RSA: 'za', RUS: 'ru', SCO: 'gb-sct', SEN: 'sn', SRB: 'rs', SVK: 'sk', SVN: 'si', SUI: 'ch', SWE: 'se',
  THA: 'th', TUN: 'tn', TUR: 'tr', UKR: 'ua', UAE: 'ae', URU: 'uy', USA: 'us', UZB: 'uz', VEN: 've', VIE: 'vn', WAL: 'gb-wls', ZAM: 'zm',
};

const TEAM_NAME_TO_FIFA: Record<string, string> = {
  阿尔及利亚: 'ALG',
  阿根廷: 'ARG',
  澳大利亚: 'AUS',
  奥地利: 'AUT',
  比利时: 'BEL',
  巴西: 'BRA',
  波黑: 'BIH',
  波斯尼亚和黑塞哥维那: 'BIH',
  保加利亚: 'BUL',
  加拿大: 'CAN',
  佛得角: 'CPV',
  智利: 'CHI',
  中国: 'CHN',
  哥伦比亚: 'COL',
  哥斯达黎加: 'CRC',
  克罗地亚: 'CRO',
  库拉索: 'CUW',
  捷克: 'CZE',
  丹麦: 'DEN',
  民主刚果: 'COD',
  刚果民主共和国: 'COD',
  厄瓜多尔: 'ECU',
  埃及: 'EGY',
  英格兰: 'ENG',
  西班牙: 'ESP',
  芬兰: 'FIN',
  法国: 'FRA',
  德国: 'GER',
  加纳: 'GHA',
  希腊: 'GRE',
  洪都拉斯: 'HON',
  匈牙利: 'HUN',
  印度尼西亚: 'IDN',
  伊朗: 'IRN',
  爱尔兰: 'IRL',
  意大利: 'ITA',
  伊拉克: 'IRQ',
  日本: 'JPN',
  约旦: 'JOR',
  韩国: 'KOR',
  沙特: 'KSA',
  沙特阿拉伯: 'KSA',
  摩洛哥: 'MAR',
  墨西哥: 'MEX',
  荷兰: 'NED',
  新西兰: 'NZL',
  北爱尔兰: 'NIR',
  尼日利亚: 'NGA',
  挪威: 'NOR',
  巴拿马: 'PAN',
  巴拉圭: 'PAR',
  秘鲁: 'PER',
  波兰: 'POL',
  葡萄牙: 'POR',
  卡塔尔: 'QAT',
  罗马尼亚: 'ROU',
  俄罗斯: 'RUS',
  苏格兰: 'SCO',
  塞内加尔: 'SEN',
  塞尔维亚: 'SRB',
  斯洛伐克: 'SVK',
  斯洛文尼亚: 'SVN',
  南非: 'RSA',
  科特迪瓦: 'CIV',
  科特迪瓦共和国: 'CIV',
  瑞典: 'SWE',
  瑞士: 'SUI',
  泰国: 'THA',
  突尼斯: 'TUN',
  土耳其: 'TUR',
  乌克兰: 'UKR',
  阿联酋: 'UAE',
  乌拉圭: 'URU',
  美国: 'USA',
  乌兹别克斯坦: 'UZB',
  威尔士: 'WAL',
};

const SCORE_PATTERN = /(?:比分\s*[=：:]?\s*)?(\d{1,2})\s*[-:：比]\s*(\d{1,2})/;
const SCORE_DISPLAY_PATTERN = /^\s*\d{1,2}\s*[-:：]\s*\d{1,2}\s*$/;
const HTTP_URL_PATTERN = /^https?:\/\//i;
const PLACEHOLDER_TEAM_NAMES = new Set([
  'unknown home team',
  'unknown away team',
  'unknown team',
  '主队待定',
  '客队待定',
  '球队待定',
  '待定',
  '待同步',
]);

function normalizeCode(value?: string | null): string {
  return value?.trim().toUpperCase().replace(/[^A-Z0-9-]/g, '') ?? '';
}

function normalizeTeamNameKey(value?: string | null): string {
  return (
    value
      ?.trim()
      .replace(/[（(].*?[）)]/g, '')
      .replace(/国家队|男子足球队|女子足球队|男足|女足|足球队|代表队|队|待定|待同步/g, '')
      .replace(/\s+/g, '') ?? ''
  );
}

function inferFifaCodeFromTeamName(value?: string | null): string {
  const key = normalizeTeamNameKey(value);
  return key ? TEAM_NAME_TO_FIFA[key] ?? '' : '';
}

export function flagUrlForFifaCode(code?: string | null): string | null {
  const normalized = normalizeCode(code);
  if (!normalized) {
    return null;
  }
  const iso = FIFA_TO_ISO[normalized] ?? (normalized.length === 2 ? normalized.toLowerCase() : null);
  return iso ? `https://flagcdn.com/w80/${iso.toLowerCase()}.png` : null;
}

export function flagEmojiForFifaCode(code?: string | null): string | null {
  const normalized = normalizeCode(code);
  if (!normalized) {
    return null;
  }
  const iso = FIFA_TO_ISO[normalized] ?? (normalized.length === 2 ? normalized.toLowerCase() : null);
  if (!iso || !/^[a-z]{2}$/i.test(iso)) {
    return null;
  }
  return iso
    .toUpperCase()
    .split('')
    .map((letter) => String.fromCodePoint(127397 + letter.charCodeAt(0)))
    .join('');
}

export function teamFromLegacy(
  teamId?: number | null,
  teamName?: string | null,
  fifaCode?: string | null,
  countryRegion?: string | null,
): TeamVisual {
  const normalizedCode = normalizeCode(fifaCode);
  return {
    teamId: teamId ?? null,
    teamName: teamName?.trim() || '球队待定',
    fifaCode: normalizedCode || null,
    countryIso2: null,
    flagUrl: flagUrlForFifaCode(normalizedCode),
    countryRegion: countryRegion?.trim() || null,
  };
}

export function teamNameFromMatchName(matchName?: string | null, side: 'HOME' | 'AWAY' = 'HOME'): string {
  const fallback = side === 'HOME' ? '主队待定' : '客队待定';
  const text = matchName?.trim();
  if (!text) {
    return fallback;
  }
  const parts = text
    .split(/\s+(?:vs\.?|v\.?|VS|Vs|对|－|-|—)\s+/)
    .map((part) => part.trim())
    .filter(Boolean);
  if (parts.length >= 2) {
    return side === 'HOME' ? parts[0] : parts[1];
  }
  return side === 'HOME' ? text : fallback;
}

export function isPlaceholderTeamName(value?: string | null): boolean {
  const normalized = value?.trim();
  if (!normalized) {
    return true;
  }
  return PLACEHOLDER_TEAM_NAMES.has(normalized.toLowerCase()) || PLACEHOLDER_TEAM_NAMES.has(normalized);
}

export function normalizeTeamVisual(input?: TeamVisual | null, fallbackName?: string | null): TeamVisual {
  const inputTeamName = input?.teamName?.trim();
  const fallbackTeamName = fallbackName?.trim();
  const usableTeamName = inputTeamName && !isPlaceholderTeamName(inputTeamName) ? inputTeamName : fallbackTeamName;
  const inferredCode = inferFifaCodeFromTeamName(usableTeamName || fallbackTeamName);
  const rawCode = normalizeCode(
    input?.fifaCode || (!HTTP_URL_PATTERN.test(input?.flagUrl || '') ? input?.flagUrl : null) || inferredCode,
  );
  const directFlag = input?.flagUrl && HTTP_URL_PATTERN.test(input.flagUrl) ? input.flagUrl : null;
  return {
    teamId: input?.teamId ?? null,
    teamName: usableTeamName || '球队待定',
    fifaCode: rawCode || null,
    countryIso2: input?.countryIso2?.trim() || null,
    flagUrl: directFlag || flagUrlForFifaCode(input?.countryIso2 || rawCode),
    countryRegion: input?.countryRegion?.trim() || null,
  };
}

export function compareWinner(home?: number | null, away?: number | null): WinnerSide {
  if (home == null || away == null) {
    return 'UNKNOWN';
  }
  if (home > away) {
    return 'HOME';
  }
  if (away > home) {
    return 'AWAY';
  }
  return 'DRAW';
}

export function resultTextForWinner(winnerSide?: WinnerSide | null): string {
  switch (winnerSide) {
    case 'HOME':
      return '主队胜';
    case 'AWAY':
      return '客队胜';
    case 'DRAW':
      return '平局';
    default:
      return '赛果待同步';
  }
}

export function scoreboardFromNumbers(
  homeScore?: number | null,
  awayScore?: number | null,
  scoreSource = 'FRONTEND_DERIVED',
): Scoreboard {
  if (homeScore == null || awayScore == null) {
    return {
      homeScore: null,
      awayScore: null,
      scoreDisplay: '待同步',
      winnerSide: 'UNKNOWN',
      resultText: '比分待同步',
      scoreSource,
    };
  }
  const winnerSide = compareWinner(homeScore, awayScore);
  return {
    homeScore,
    awayScore,
    scoreDisplay: `${homeScore} - ${awayScore}`,
    winnerSide,
    resultText: resultTextForWinner(winnerSide),
    scoreSource,
  };
}

export function scoreboardFallback(match?: Record<string, unknown> | null, fallbackStatus?: string): Scoreboard {
  const existing = match?.scoreboard as Scoreboard | undefined;
  if (existing?.scoreDisplay) {
    const derivedWinner = existing.winnerSide ?? compareWinner(existing.homeScore, existing.awayScore);
    return {
      ...existing,
      winnerSide: derivedWinner,
      resultText: existing.resultText || resultTextForWinner(derivedWinner),
    };
  }

  const homeScore = typeof match?.homeScore === 'number' ? match.homeScore : null;
  const awayScore = typeof match?.awayScore === 'number' ? match.awayScore : null;
  if (homeScore != null && awayScore != null) {
    return scoreboardFromNumbers(homeScore, awayScore);
  }

  const summary = typeof match?.summary === 'string' ? match.summary : '';
  const parsed = SCORE_PATTERN.exec(summary);
  if (parsed) {
    return scoreboardFromNumbers(Number(parsed[1]), Number(parsed[2]), 'TEXT_PARSED');
  }

  const statusText = String(match?.resultStatus || match?.status || fallbackStatus || '').toUpperCase();
  if (statusText.includes('FINISHED') || statusText.includes('END') || statusText.includes('RESULT') || statusText.includes('已完赛')) {
    return {
      homeScore: null,
      awayScore: null,
      scoreDisplay: '比分待核对',
      winnerSide: 'UNKNOWN',
      resultText: '已完赛，等待比分核对',
      scoreSource: 'PENDING',
    };
  }
  const isScheduled =
    statusText.includes('SCHEDULED') ||
    statusText.includes('NOT_STARTED') ||
    statusText.includes('待开球') ||
    statusText.includes('赛前') ||
    Boolean(match?.kickoffTime);
  return {
    homeScore: null,
    awayScore: null,
    scoreDisplay: isScheduled ? '待开球' : '比分待同步',
    winnerSide: 'UNKNOWN',
    resultText: isScheduled ? '赛前' : '比分待同步',
    scoreSource: 'PENDING',
  };
}

export function scoreTone(score?: number | null): 'success' | 'warning' | 'danger' | 'info' {
  if (score == null) {
    return 'info';
  }
  if (score >= 85) {
    return 'success';
  }
  if (score >= 65) {
    return 'warning';
  }
  return 'danger';
}

export function winnerClass(side: 'HOME' | 'AWAY' | 'DRAW', winnerSide?: WinnerSide | null): string {
  if (winnerSide === 'DRAW' && side === 'DRAW') {
    return 'is-draw';
  }
  if (winnerSide === side) {
    return 'is-winner';
  }
  return winnerSide && winnerSide !== 'UNKNOWN' && winnerSide !== 'DRAW' ? 'is-loser' : '';
}

export function statusLabel(status?: string | null): string {
  switch ((status || '').toUpperCase()) {
    case 'SCHEDULED':
    case 'NOT_STARTED':
      return '待开球';
    case 'LIVE':
    case 'IN_PLAY':
      return '进行中';
    case 'FINISHED':
    case 'COMPLETED':
      return '已完赛';
    case 'POSTPONED':
      return '延期';
    case 'CANCELLED':
      return '取消';
    default:
      return status || '状态待同步';
  }
}


export function scoreboardHasResult(scoreboard?: Scoreboard | null): boolean {
  if (!scoreboard) {
    return false;
  }
  if (typeof scoreboard.homeScore === 'number' && typeof scoreboard.awayScore === 'number') {
    return true;
  }
  const display = scoreboard.scoreDisplay?.trim() || '';
  if (display && SCORE_DISPLAY_PATTERN.test(display)) {
    return true;
  }
  return ['HOME', 'AWAY', 'DRAW'].includes(String(scoreboard.winnerSide || '').toUpperCase());
}

export function scoreboardMetaStatus(status?: string | null, scoreboard?: Scoreboard | null): string {
  const label = statusLabel(status);
  if (!scoreboardHasResult(scoreboard)) {
    return label;
  }
  const normalized = `${label} ${status || ''}`.toUpperCase();
  if (
    !label ||
    label === '状态待同步' ||
    normalized.includes('待开球') ||
    normalized.includes('未开赛') ||
    normalized.includes('赛前') ||
    normalized.includes('SCHEDULED') ||
    normalized.includes('NOT_STARTED') ||
    normalized.includes('PENDING') ||
    normalized.includes('UNKNOWN')
  ) {
    return '比分记录';
  }
  return label;
}

export function parseScoringMinutes(value?: string | null): number[] {
  if (!value) {
    return [];
  }
  return value
    .split(/[^0-9+]+/)
    .map((item) => Number.parseInt(item, 10))
    .filter((minute) => Number.isFinite(minute) && minute >= 0)
    .map((minute) => Math.min(minute, 130));
}

export function clampPercent(value?: number | null, max = 100): number {
  if (value == null || Number.isNaN(value)) {
    return 0;
  }
  if (max <= 0) {
    return 0;
  }
  return Math.max(0, Math.min(100, (Number(value) / max) * 100));
}
