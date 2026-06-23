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
    kickoffTime: '2026-06-22T20:00:00',
    status: 'SCHEDULED',
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
    expect(wrapper.text()).toContain('阵容核对');
    expect(wrapper.text()).toContain('赔率鲜度');
  });

  it('renders a public decision-flow card layout without sensitive betting fields', async () => {
    const wrapper = await mountWorkbench();

    expect(wrapper.find('[data-test="decision-flow"]').exists()).toBe(true);
    expect(wrapper.find('[data-test="team-card"]').text()).toContain('边路速度优势');
    expect(wrapper.find('[data-test="player-card"]').text()).toContain('Mbappe');
    expect(wrapper.find('[data-test="odds-card"]').text()).toContain('Pinnacle');
    expect(wrapper.find('[data-test="sentiment-card"]').text()).toContain('降雨影响传控');
    expect(wrapper.find('[data-test="analysis-card"]').text()).toContain('谨慎观察');
    expect(wrapper.text()).not.toContain('AI 下注方案');
    expect(wrapper.text()).not.toContain('实际出票');
    expect(wrapper.text()).not.toContain('票号');
    expect(wrapper.text()).not.toContain('建议金额');
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
