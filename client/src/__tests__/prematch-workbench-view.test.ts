import ElementPlus from 'element-plus';
import { createPinia, setActivePinia } from 'pinia';
import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import PrematchWorkbenchView from '@/views/PrematchWorkbenchView.vue';
import prematchWorkbenchSource from '@/views/PrematchWorkbenchView.vue?raw';
import {
  getPrematchWorkbenchMatch,
  getPublicPrematchWorkbenchMatch,
  listPrematchWorkbenchMatches,
  listPublicPrematchWorkbenchMatches,
} from '@/api/prematchWorkbench';
import { useAuthStore } from '@/stores/auth';

const publicMatches = [
  {
    matchId: 11,
    matchKey: 'france-brazil',
    matchName: 'France vs Brazil',
    matchday: '2026-06-22',
    jcCode: '001',
    competition: 'World Cup',
    stage: 'Group',
    venue: '上海体育场',
    kickoffTime: '2026-06-22T20:00:00',
    status: 'SCHEDULED',
    resultStatus: 'PENDING',
    homeTeamName: 'France',
    awayTeamName: 'Brazil',
    homeTeam: { teamId: 1, teamName: 'France', fifaCode: 'FRA', countryIso2: 'fr' },
    awayTeam: { teamId: 2, teamName: 'Brazil', fifaCode: 'BRA', countryIso2: 'br' },
    scoreboard: { scoreDisplay: '待开球', winnerSide: 'UNKNOWN', resultText: '赛果待同步' },
    integrityScore: 88,
    missingCount: 1,
    staleCount: 2,
    conflictCount: 3,
    teamProfileCount: 2,
    playerProfileCount: 23,
    lineupCount: 2,
    oddsMarketCount: 4,
    sentimentFactorCount: 5,
    analysisReportCount: 6,
  },
];

const publicDetail = {
  summary: publicMatches[0],
  teams: [
    {
      teamId: 1,
      teamKey: 'france',
      teamName: 'France',
      fifaCode: 'FRA',
      styleTags: '高位压迫 / 快速转换',
      attackProfile: '边路推进强',
      defenseProfile: '中路保护稳定',
      publicSentiment: '阵容完整',
      facts: [{ factId: 101, factType: 'STYLE', title: '边路速度优势', summary: '左路推进效率高' }],
    },
  ],
  lineups: [
    { id: 201, matchId: 11, teamId: 1, teamName: 'France', playerId: 9, playerName: 'Mbappe', position: 'FW', role: '核心', starter: true },
  ],
  players: [
    {
      playerId: 9,
      playerKey: 'mbappe',
      teamId: 1,
      teamName: 'France',
      playerName: 'Mbappe',
      position: 'FW',
      status: 'FIT',
      injuryStatus: '健康',
      cardStatus: '无停赛',
      lockerRoomStatus: '稳定',
      facts: [{ factId: 301, factType: 'FORM', title: '冲刺状态良好' }],
    },
  ],
  oddsMarkets: [
    {
      marketId: 401,
      bookmaker: 'Pinnacle',
      marketCode: 'HAD',
      marketName: '胜平负',
      snapshotType: 'LIVE',
      lineValue: '0',
      capturedAt: '2026-06-22T12:00:00',
      selections: [
        { selectionId: 1, selectionCode: 'H', selectionName: '主胜', oddsValue: 1.8, impliedProbability: 0.55, selectionStatus: 'OPEN' },
      ],
    },
  ],
  sentimentFactors: [
    {
      factorId: 501,
      factorCategory: 'WEATHER',
      factorType: 'RAIN',
      title: '降雨影响传控',
      summary: '雨战会降低地面推进速度',
      impactDirection: 'NEGATIVE',
      evidenceLevel: 'MEDIUM',
      risks: [{ riskId: 1, riskType: 'PACE', riskLevel: 'MEDIUM', title: '节奏变慢', rationale: '场地湿滑' }],
    },
  ],
  evidence: [
    { evidenceId: 601, sourceType: 'MEDIA', sourceName: 'Official', summary: '官方训练公开信息', reliabilityScore: 0.9 },
  ],
  conflicts: [
    { conflictId: 701, conflictType: 'LINEUP', fieldName: 'starter', resolutionStatus: 'PENDING' },
  ],
  analysisReports: [
    {
      reportId: 801,
      analysisId: 'analysis-11',
      conclusionType: '谨慎观察',
      confidence: 'MEDIUM',
      riskSummary: '低比分路径需保留',
      recommendedMarkets: 'HAD / TTG',
      dimensions: '阵容,赔率,舆情',
      createdAt: '2026-06-22T13:00:00',
    },
  ],
  integrityChecks: [
    { code: 'LINEUP', label: '阵容核对', status: 'PASS', severity: 'LOW', message: '首发样本完整', evidenceCount: 2 },
    { code: 'ODDS', label: '赔率鲜度', status: 'STALE', severity: 'MEDIUM', message: '需要临场复拉', evidenceCount: 4 },
  ],
};

function makePublicMatch(index: number, overrides: Partial<(typeof publicMatches)[number]> = {}) {
  const teams = [
    ['France', 'FRA', 'Brazil', 'BRA'],
    ['Argentina', 'ARG', 'Mexico', 'MEX'],
    ['Spain', 'ESP', 'Germany', 'GER'],
    ['Japan', 'JPN', 'Canada', 'CAN'],
    ['Portugal', 'POR', 'Korea', 'KOR'],
    ['England', 'ENG', 'USA', 'USA'],
    ['Netherlands', 'NED', 'Senegal', 'SEN'],
    ['Morocco', 'MAR', 'Croatia', 'CRO'],
    ['Australia', 'AUS', 'Denmark', 'DEN'],
    ['Brazil', 'BRA', 'Serbia', 'SRB'],
    ['France', 'FRA', 'Tunisia', 'TUN'],
    ['Poland', 'POL', 'Wales', 'WAL'],
    ['Uruguay', 'URU', 'Ghana', 'GHA'],
  ][index % 13];
  const [home, homeCode, away, awayCode] = teams;
  return {
    ...publicMatches[0],
    matchId: 100 + index,
    matchKey: `${home.toLowerCase()}-${away.toLowerCase()}-${index}`,
    matchName: `${home} vs ${away}`,
    jcCode: String(index + 1).padStart(3, '0'),
    venue: `${home} 主场`,
    homeTeamName: home,
    awayTeamName: away,
    homeTeam: { teamId: 1000 + index * 2, teamName: home, fifaCode: homeCode, countryIso2: homeCode.toLowerCase() },
    awayTeam: { teamId: 1001 + index * 2, teamName: away, fifaCode: awayCode, countryIso2: awayCode.toLowerCase() },
    integrityScore: index === 11 ? 58 : index % 4 === 0 ? 72 : 90,
    missingCount: index % 3 === 0 ? 1 : 0,
    staleCount: index % 5 === 0 ? 1 : 0,
    conflictCount: index === 0 || index === 5 ? 1 : 0,
    oddsMarketCount: index === 4 ? 0 : 2,
    sentimentFactorCount: index === 7 ? 0 : 2,
    analysisReportCount: index === 9 ? 0 : 1,
    ...overrides,
  };
}

vi.mock('@/api/prematchWorkbench', () => ({
  listPublicPrematchWorkbenchMatches: vi.fn(),
  getPublicPrematchWorkbenchMatch: vi.fn(),
  getPublicPrematchWorkbenchIntegrity: vi.fn(),
  listPrematchWorkbenchMatches: vi.fn(),
  getPrematchWorkbenchMatch: vi.fn(),
  getPrematchWorkbenchIntegrity: vi.fn(),
}));

const routerLinkStub = {
  props: ['to'],
  template: '<a class="router-link-stub" :href="typeof to === `string` ? to : to.path"><slot /></a>',
};

async function mountWorkbench() {
  const wrapper = mount(PrematchWorkbenchView, {
    global: {
      plugins: [ElementPlus],
      stubs: { RouterLink: routerLinkStub },
    },
  });
  await flushPromises();
  await flushPromises();
  return wrapper;
}

describe('PrematchWorkbenchView', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.mocked(listPublicPrematchWorkbenchMatches).mockReset();
    vi.mocked(getPublicPrematchWorkbenchMatch).mockReset();
    vi.mocked(listPrematchWorkbenchMatches).mockReset();
    vi.mocked(getPrematchWorkbenchMatch).mockReset();
    vi.mocked(listPublicPrematchWorkbenchMatches).mockResolvedValue({
      success: true,
      data: publicMatches,
      message: '',
      timestamp: '',
    });
    vi.mocked(getPublicPrematchWorkbenchMatch).mockResolvedValue({
      success: true,
      data: publicDetail,
      message: '',
      timestamp: '',
    });
  });

  it('loads the workbench through public prematch APIs by default', async () => {
    const wrapper = await mountWorkbench();

    expect(listPublicPrematchWorkbenchMatches).toHaveBeenCalledTimes(1);
    expect(getPublicPrematchWorkbenchMatch).toHaveBeenCalledWith(11);
    expect(listPrematchWorkbenchMatches).not.toHaveBeenCalled();
    expect(getPrematchWorkbenchMatch).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain('赛前分析作战室');
    expect(wrapper.text()).toContain('France vs Brazil');
    expect(wrapper.find('[data-test="workbench-term-board"]').exists()).toBe(false);
    expect(wrapper.text()).not.toMatch(/赛前关键指标|术语对照|xG、PPDA|盘口（专业叫法）/);
    expect(wrapper.text()).not.toContain('AI 分析不只看市场价格');
    expect(wrapper.text()).toContain('市场价格');
    expect(wrapper.text()).toContain('阵容核对');
    expect(wrapper.text()).toContain('赔率鲜度');
  });

  it('renders a public decision-flow card layout without sensitive betting fields', async () => {
    const wrapper = await mountWorkbench();

    expect(wrapper.find('[data-test="decision-flow"]').exists()).toBe(true);
    expect(wrapper.find('[data-test="team-card"]').text()).toContain('边路速度优势');
    expect(wrapper.find('[data-test="player-card"]').text()).toContain('Mbappe');
    expect(wrapper.find('[data-test="odds-card"]').text()).toContain('Pinnacle');
    expect(wrapper.find('[data-test="odds-card"]').text()).toContain('赔率与市场');
    expect(wrapper.find('[data-test="sentiment-card"]').text()).toContain('降雨影响传控');
    expect(wrapper.find('[data-test="analysis-card"]').text()).toContain('谨慎观察');
    expect(wrapper.find('[data-test="analysis-card"]').text()).toContain('赛前判断：谨慎观察');
    expect(wrapper.find('[data-test="analysis-card"]').text()).toContain('把握程度：中等');
    expect(wrapper.find('[data-test="analysis-card"]').text()).not.toContain('MEDIUM');
    expect(wrapper.find('[data-test="analysis-card"]').text()).not.toContain('置信度');
    expect(wrapper.text()).toContain('阵容线索');
    expect(wrapper.text()).toContain('官方训练公开信息');
    expect(wrapper.text()).toContain('阵容');
    expect(wrapper.text()).toContain('待处理');
    expect(wrapper.text()).not.toContain('AI 下注方案');
    expect(wrapper.text()).not.toContain('实际出票');
    expect(wrapper.text()).not.toContain('票号');
    expect(wrapper.text()).not.toContain('建议金额');
    expect(wrapper.text()).not.toContain('投入');
    expect(wrapper.text()).not.toContain('预算');
    expect(wrapper.text()).not.toContain('资金');
    expect(wrapper.text()).toContain('个人执行明细不在公开作战室展示');
    expect(wrapper.text()).not.toMatch(/profit|loss|stake|budget/i);
  });

  it('renders a compact prematch data console without tutorial slogans', async () => {
    const wrapper = await mountWorkbench();
    const consolePanel = wrapper.find('[data-test="workbench-decision-console"]');

    expect(consolePanel.exists()).toBe(true);
    expect(consolePanel.text()).toContain('赛前数据面板');
    expect(consolePanel.text()).toContain('88%');
    expect(consolePanel.text()).toContain('准备充分');
    expect(consolePanel.text()).toContain('资料准备度');
    expect(consolePanel.text()).toContain('风险压力');
    expect(consolePanel.text()).toContain('证据覆盖');
    expect(consolePanel.findAll('.coverage-donut')).toHaveLength(2);
    expect(consolePanel.text()).not.toMatch(/比分\/状态|数据准备度|风险项|确认是待开球|可以阅读结论|可以查看结论/);
    expect(consolePanel.text()).not.toMatch(/新手|三步|读法|先看|提示|引导|不用懂|无需理解|不懂足球|先确认|不要只看|按.*步|怎么读|路线|地图|今天先看|风险先看|先核|再看|最后|下一步|先读|先别/);
    const readinessBoard = wrapper.find('[data-test="workbench-readiness-board"]');
    expect(readinessBoard.exists()).toBe(true);
    expect(readinessBoard.text()).toContain('准备度图形');
    expect(readinessBoard.findAll('.coverage-donut')).toHaveLength(5);
    expect(readinessBoard.text()).toContain('球队画像');
    expect(readinessBoard.text()).toContain('球员画像');
    expect(readinessBoard.text()).toContain('赔率市场');
    expect(readinessBoard.text()).toContain('舆情外因');
    expect(wrapper.find('.match-rail').attributes('tabindex')).toBe('0');
    expect(wrapper.find('.decision-board').attributes('tabindex')).toBe('0');
    expect(prematchWorkbenchSource).toContain('准备充分（≥85%）');
    expect(prematchWorkbenchSource).not.toContain('max-height: min(86dvh, 720px)');
    expect(prematchWorkbenchSource).toMatch(/\.match-rail\s*\{\s*max-height: none;\s*overflow: visible;/s);
    expect(prematchWorkbenchSource).toMatch(/\.decision-board\s*\{\s*max-height: none;\s*overflow: visible;/s);
    expect(prematchWorkbenchSource).toContain('grid-template-columns: repeat(2, minmax(0, 1fr));');
    expect(prematchWorkbenchSource).not.toContain('这组条形图把进球、失球、首球时间');
    expect(prematchWorkbenchSource).not.toMatch(/AI 分析不只看盘口|盘口术语|赔率与盘口|无盘口线|较完整（≥85%）/);
  });

  it('supports match directory search, readiness filters, issue filters and pagination', async () => {
    const manyMatches = Array.from({ length: 13 }, (_, index) => makePublicMatch(index));
    vi.mocked(listPublicPrematchWorkbenchMatches).mockResolvedValue({
      success: true,
      data: manyMatches,
      message: '',
      timestamp: '',
    });
    vi.mocked(getPublicPrematchWorkbenchMatch).mockResolvedValue({
      success: true,
      data: { ...publicDetail, summary: manyMatches[0] },
      message: '',
      timestamp: '',
    });

    const wrapper = await mountWorkbench();

    expect(wrapper.find('[data-test="workbench-filter-summary"]').text()).toContain('已筛出 13 / 13 场赛前比赛');
    expect(wrapper.findAll('[data-test="workbench-match-card"]')).toHaveLength(10);

    const pagerButtons = wrapper.findAll('.directory-pager button');
    await pagerButtons[1].trigger('click');
    await flushPromises();
    expect(wrapper.findAll('[data-test="workbench-match-card"]')).toHaveLength(3);

    await wrapper.find('[data-test="workbench-match-search"]').setValue('Japan');
    await flushPromises();
    expect(wrapper.find('[data-test="workbench-filter-summary"]').text()).toContain('已筛出 1 / 13 场赛前比赛');
    expect(wrapper.findAll('[data-test="workbench-match-card"]')).toHaveLength(1);
    expect(wrapper.find('[data-test="workbench-match-list"]').text()).toContain('Japan vs Canada');

    await wrapper.find('[data-test="workbench-clear-filters"]').trigger('click');
    await flushPromises();
    await wrapper.find('[data-test="workbench-readiness-filter"]').setValue('WEAK');
    await flushPromises();
    expect(wrapper.find('[data-test="workbench-filter-summary"]').text()).toContain('已筛出 1 / 13 场赛前比赛');
    expect(wrapper.find('[data-test="workbench-match-list"]').text()).toContain('准备度 58%');

    await wrapper.find('[data-test="workbench-clear-filters"]').trigger('click');
    await flushPromises();
    await wrapper.find('[data-test="workbench-issue-filter"]').setValue('CONFLICT');
    await flushPromises();
    expect(wrapper.find('[data-test="workbench-filter-summary"]').text()).toContain('已筛出 2 / 13 场赛前比赛');
    expect(wrapper.findAll('[data-test="workbench-match-card"]')).toHaveLength(2);
  });

  it('scrubs ROI, CLV, and closing odds text from public analysis cards', async () => {
    vi.mocked(getPublicPrematchWorkbenchMatch).mockResolvedValue({
      success: true,
      data: {
        ...publicDetail,
        analysisReports: [
          {
            ...publicDetail.analysisReports[0],
            riskSummary: 'ROI 12% / CLV 为正 / 资金 100 / 收益 20',
            recommendedMarkets: 'closing_odds=2.01',
            dimensions: 'closing odds: 1.97',
          },
        ],
      },
      message: '',
      timestamp: '',
    });

    const wrapper = await mountWorkbench();
    const text = wrapper.find('[data-test="analysis-card"]').text();

    expect(text).toContain('已脱敏指标');
    expect(text).not.toMatch(/ROI|CLV|closing[_\s-]?odds|12%|2\.01|1\.97|资金|收益|100|20/i);
  });

  it('shows admin-only actions only after Basic admin login', async () => {
    const anonymousWrapper = await mountWorkbench();
    expect(anonymousWrapper.find('a[href="/admin/import-review"]').exists()).toBe(false);

    const auth = useAuthStore();
    auth.setAdmin({ username: 'operator', displayName: 'Operator', authType: 'BASIC' }, 'secret');
    const adminWrapper = await mountWorkbench();

    expect(adminWrapper.find('a[href="/admin/import-review"]').exists()).toBe(true);
    expect(adminWrapper.text()).toContain('管理员审核入口');
  });

  it('declares H5 card layout instead of desktop-only tables', () => {
    expect(prematchWorkbenchSource).not.toContain('<el-table');
    expect(prematchWorkbenchSource).toContain('@media (max-width: 640px)');
    expect(prematchWorkbenchSource).toContain('grid-template-columns: 1fr');
    expect(prematchWorkbenchSource).toContain('min-width: 0');
    expect(prematchWorkbenchSource).not.toMatch(/min-width:\s*(?:[4-9]\d{2,}|\d{4,})px/);
  });
});
