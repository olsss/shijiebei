import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AnalysisReviewCenterView from '@/views/AnalysisReviewCenterView.vue';
import analysisReviewSource from '@/views/AnalysisReviewCenterView.vue?raw';
import {
  fetchAnalysisReviewOverview,
  getAnalysisReport,
  getBetPlan,
  listAnalysisReports,
  listBetPlans,
  listBetRecords,
  listPostMatchReviews,
  listPublicDecisionReports,
  listPublicDecisionReviews,
} from '@/api/analysisReview';
import { listPublicMatches } from '@/api/matches';

vi.mock('@/api/analysisReview', () => ({
  listPublicDecisionReports: vi.fn(),
  listPublicDecisionReviews: vi.fn(),
  fetchAnalysisReviewOverview: vi.fn(),
  listAnalysisReports: vi.fn(),
  getAnalysisReport: vi.fn(),
  listBetPlans: vi.fn(),
  getBetPlan: vi.fn(),
  listBetRecords: vi.fn(),
  listPostMatchReviews: vi.fn(),
}));

vi.mock('@/api/matches', () => ({
  listPublicMatches: vi.fn(),
}));

const apiOk = <T,>(data: T) => ({ success: true, data, message: '', timestamp: '' });

const publicReports = [{
  id: 1,
  matchId: 7,
  matchName: 'France vs Brazil',
  matchday: '2026-06-22',
  jcCode: '001',
  conclusionType: 'LEAN_HOME',
  confidence: 'MEDIUM',
  riskSummary: '天气影响节奏，需要降低过热判断权重，ROI 不应公开',
  reviewSummary: '复盘指向稳健，赛前风险识别有效，profitLoss 不应公开',
  lessonSummary: '不要在高热度比赛追高，budgetAmount 不应公开',
}];

const publicMatchSummary = {
  id: 7,
  matchKey: 'france-brazil',
  matchName: 'France vs Brazil',
  matchday: '2026-06-22',
  jcCode: '001',
  venue: 'New York',
  status: 'FINISHED',
  resultStatus: 'FINAL',
  homeTeam: { teamName: '法国', fifaCode: 'FRA', countryIso2: 'FR' },
  awayTeam: { teamName: '巴西', fifaCode: 'BRA', countryIso2: 'BR' },
  scoreboard: { homeScore: 2, awayScore: 0, scoreDisplay: '2 - 0', winnerSide: 'HOME', resultText: '主队胜', scoreSource: 'TEAM_STATS' },
  eventCount: 1,
  lineupCount: 1,
  evidenceCount: 3,
  conflictCount: 0,
};

const publicReviews = [{
  id: 2,
  matchId: 7,
  matchName: 'France vs Brazil',
  matchday: '2026-06-22',
  analysisReportId: 1,
  reviewKey: 'france-brazil-review',
  title: '法国巴西赛后复盘',
  mathSummary: '模型置信度与赛果方向一致，ROI 12%',
  footballSummary: '边路速度优势兑现',
  handicapSummary: '盘口升温后风险增大',
  tournamentTemperamentSummary: '大赛淘汰倾向控制节奏',
  oddsValueSummary: '临场价值不宜追高，CLV 为正',
  overallSummary: '高热度比赛降低权重，returnAmount 不公开',
  lessons: [
    { id: 3, lessonType: 'RISK_CONTROL', lessonText: '高热度比赛降低权重', severity: 'MEDIUM' },
  ],
}];

const mountDecisionView = async () => {
  const wrapper = mount(AnalysisReviewCenterView, {
    global: {
      stubs: {
        RouterLink: {
          props: ['to'],
          template: '<a :href="typeof to === `string` ? to : to.path"><slot /></a>',
        },
      },
    },
  });
  await flushPromises();
  await flushPromises();
  return wrapper;
};

describe('AnalysisReviewCenterView public decisions', () => {
  beforeEach(() => {
    vi.mocked(listPublicDecisionReports).mockReset().mockResolvedValue(apiOk(publicReports));
    vi.mocked(listPublicDecisionReviews).mockReset().mockResolvedValue(apiOk(publicReviews));
    vi.mocked(listPublicMatches).mockReset().mockResolvedValue(apiOk([publicMatchSummary]));
    vi.mocked(fetchAnalysisReviewOverview).mockReset();
    vi.mocked(listAnalysisReports).mockReset();
    vi.mocked(getAnalysisReport).mockReset();
    vi.mocked(listBetPlans).mockReset();
    vi.mocked(getBetPlan).mockReset();
    vi.mocked(listBetRecords).mockReset();
    vi.mocked(listPostMatchReviews).mockReset();
  });

  it('renders public decision reports and review lessons without admin analysis data', async () => {
    const wrapper = await mountDecisionView();

    expect(listPublicDecisionReports).toHaveBeenCalledTimes(1);
    expect(listPublicDecisionReviews).toHaveBeenCalledTimes(1);
    expect(listPublicMatches).toHaveBeenCalledTimes(1);
    expect(fetchAnalysisReviewOverview).not.toHaveBeenCalled();
    expect(listAnalysisReports).not.toHaveBeenCalled();
    expect(getAnalysisReport).not.toHaveBeenCalled();
    expect(listBetPlans).not.toHaveBeenCalled();
    expect(getBetPlan).not.toHaveBeenCalled();
    expect(listBetRecords).not.toHaveBeenCalled();
    expect(listPostMatchReviews).not.toHaveBeenCalled();

    expect(wrapper.text()).toContain('决策复盘中心');
    expect(wrapper.find('[data-test="decision-console"]').text()).toContain('比赛结果与复盘结论');
    expect(wrapper.find('[data-test="decision-console"]').attributes('tabindex')).toBe('0');
    expect(wrapper.find('[data-test="decision-console"]').text()).toContain('2 - 0');
    expect(wrapper.find('[data-test="decision-console"]').text()).toContain('复盘可读性');
    expect(wrapper.find('[data-test="decision-console"]').text()).toContain('上下文资料较齐');
    expect(wrapper.findAll('[data-test="decision-console"] .decision-ring-grid .coverage-donut')).toHaveLength(4);
    expect(wrapper.find('[data-test="decision-console"] .decision-ring-grid').attributes('tabindex')).toBe('0');
    expect(wrapper.find('[data-test="decision-console"]').text()).toContain('摘要覆盖');
    expect(wrapper.find('[data-test="decision-console"]').text()).toContain('规则记录');
    expect(wrapper.findAll('[data-test="decision-overview-rings"] .coverage-donut')).toHaveLength(5);
    expect(wrapper.find('[data-test="decision-overview-rings"]').attributes('tabindex')).toBe('0');
    expect(wrapper.find('[data-test="decision-overview-rings"]').text()).toContain('公开报告');
    expect(wrapper.find('[data-test="decision-overview-rings"]').text()).toContain('赛后复盘');
    expect(wrapper.find('[data-test="decision-overview-rings"]').text()).toContain('覆盖比赛');
    expect(wrapper.find('[data-test="decision-overview-rings"]').text()).toContain('规则沉淀');
    expect(wrapper.find('[data-test="decision-overview-rings"]').text()).toContain('材料合计');
    expect(wrapper.findAll('[data-test="review-structure-rings"] .coverage-donut')).toHaveLength(5);
    expect(wrapper.find('[data-test="review-structure-rings"]').text()).toContain('复盘记录');
    expect(wrapper.find('[data-test="review-structure-rings"]').text()).toContain('规则条目');
    expect(wrapper.find('[data-test="review-structure-rings"]').text()).toContain('总评覆盖');
    expect(wrapper.find('[data-test="review-structure-rings"]').text()).toContain('模型校验');
    expect(wrapper.find('[data-test="review-structure-rings"]').text()).toContain('市场摘要');
    expect(wrapper.findAll('[data-test="lesson-structure-rings"] .coverage-donut')).toHaveLength(5);
    expect(wrapper.find('[data-test="lesson-structure-rings"]').text()).toContain('规则总数');
    expect(wrapper.find('[data-test="lesson-structure-rings"]').text()).toContain('高等级规则');
    expect(wrapper.find('[data-test="lesson-structure-rings"]').text()).toContain('中等级规则');
    expect(wrapper.find('[data-test="lesson-structure-rings"]').text()).toContain('低等级规则');
    expect(wrapper.find('[data-test="lesson-structure-rings"]').text()).toContain('类型覆盖');
    expect(wrapper.find('[data-test="decision-term-board"]').exists()).toBe(false);
    expect(wrapper.text()).not.toMatch(/复盘页核心术语|术语对照|术语解释|复盘结构|赛前判断.+把握程度.+结果回看/);
    expect(wrapper.text()).toContain('France vs Brazil');
    expect(wrapper.text()).toContain('复盘指向稳健');
    expect(wrapper.text()).toContain('高热度比赛降低权重');
    expect(wrapper.text()).toContain('已筛出 1 / 1 份报告');
    expect(wrapper.text()).not.toContain('复盘上下文较完整');
    expect(analysisReviewSource).toContain('复盘上下文资料较齐');
    expect(analysisReviewSource).not.toContain('复盘上下文较完整');
    expect(wrapper.text()).not.toContain('置信度');
    expect(wrapper.text()).not.toContain('数学层');
    expect(wrapper.text()).not.toContain('盘口层');
    expect(wrapper.text()).not.toMatch(/ticket|ticketNo|stake|stakeSuggestion|betPlan|rawPayload|profit|loss|profitLoss|budgetAmount|returnAmount|ROI|CLV|票号|投入|返还|盈亏|预算|下注|金额建议|原始 JSON/i);
  });

  it('filters, recovers and paginates public decision reports', async () => {
    const generatedReports = Array.from({ length: 12 }, (_, index) => ({
      ...publicReports[0],
      id: 100 + index,
      matchId: 700 + index,
      matchName: index === 11 ? 'Brazil vs Germany' : `测试复盘${index + 1}`,
      jcCode: `${index + 1}`.padStart(3, '0'),
      conclusionType: index % 2 === 0 ? 'LEAN_HOME' : 'LEAN_AWAY',
      riskSummary: index === 11 ? '德国高位压迫风险' : `公开风险摘要${index + 1}`,
    }));
    vi.mocked(listPublicDecisionReports).mockResolvedValue(apiOk(generatedReports));
    vi.mocked(listPublicMatches).mockResolvedValue(apiOk([{
      ...publicMatchSummary,
      id: 711,
      matchName: 'Brazil vs Germany',
      homeTeam: { teamName: '巴西', fifaCode: 'BRA', countryIso2: 'BR' },
      awayTeam: { teamName: '德国', fifaCode: 'GER', countryIso2: 'DE' },
      scoreboard: { homeScore: 1, awayScore: 3, scoreDisplay: '1 - 3', winnerSide: 'AWAY', resultText: '客队胜', scoreSource: 'TEAM_STATS' },
    }]));

    const wrapper = await mountDecisionView();

    expect(wrapper.text()).toContain('已筛出 12 / 12 份报告');
    expect(wrapper.text()).toContain('第 1 / 2 页');
    expect(wrapper.findAll('.side-panel .list-card')).toHaveLength(10);

    await wrapper.find('input[aria-label="搜索比赛、竞彩编号、判断或风险摘要"]').setValue('Brazil');
    expect(wrapper.text()).toContain('已筛出 1 / 12 份报告');
    expect(wrapper.findAll('.side-panel .list-card')).toHaveLength(1);
    expect(wrapper.find('.side-panel .list-card').text()).toContain('Brazil vs Germany');

    await wrapper.find('select[aria-label="按赛前判断筛选报告"]').setValue('LEAN_HOME');
    expect(wrapper.text()).toContain('没有找到匹配报告');

    await wrapper.find('.empty-filter-state .ghost-button').trigger('click');
    expect(wrapper.text()).toContain('已筛出 12 / 12 份报告');
  });

  it('renders an empty public decision state with score context', async () => {
    vi.mocked(listPublicDecisionReports).mockResolvedValue(apiOk([]));
    vi.mocked(listPublicDecisionReviews).mockResolvedValue(apiOk([]));
    vi.mocked(listPublicMatches).mockResolvedValue(apiOk([publicMatchSummary]));

    const wrapper = await mountDecisionView();

    expect(wrapper.find('[data-test="decision-empty-state"]').exists()).toBe(true);
    expect(wrapper.find('[data-test="decision-empty-state"]').attributes('tabindex')).toBe('0');
    expect(wrapper.find('[data-test="decision-empty-guide"]').exists()).toBe(false);
    expect(wrapper.find('[data-test="decision-empty-readiness"]').exists()).toBe(true);
    expect(wrapper.find('[data-test="decision-empty-state"] .decision-scoreboard').exists()).toBe(true);
    expect(wrapper.find('[data-test="decision-empty-readiness"] .empty-readiness-bars').exists()).toBe(true);
    expect(wrapper.find('[data-test="decision-empty-readiness"] .decision-ring-grid').attributes('tabindex')).toBe('0');
    expect(wrapper.find('[data-test="decision-empty-readiness"] .empty-readiness-bars').attributes('tabindex')).toBe('0');
    expect(analysisReviewSource).toContain('align-items: start');
    expect(analysisReviewSource).toContain('.decision-scoreboard');
    expect(analysisReviewSource).toContain('minmax(0, .9fr) minmax(0, 1.35fr) minmax(0, .55fr)');
    expect(analysisReviewSource).not.toContain('max-height: min(70dvh, 560px)');
    expect(analysisReviewSource).not.toContain('max-height: min(66dvh, 540px)');
    expect(analysisReviewSource).toContain('max-height: min(36dvh, 280px)');
    expect(analysisReviewSource).toMatch(/\.decisions-grid\s+\{\s+max-height: none;\s+overflow: visible;\s+padding-right: 0;\s+scrollbar-gutter: auto;\s+\}/);
    expect(analysisReviewSource).toMatch(/\.side-panel,\s+\.detail-panel\s+\{\s+max-height: none;\s+overflow: visible;\s+scrollbar-gutter: auto;\s+\}/);
    expect(wrapper.text()).toContain('暂无公开复盘');
    expect(wrapper.text()).toContain('公开复盘数据待入库');
    expect(wrapper.text()).toContain('France vs Brazil');
    expect(wrapper.text()).toContain('2 - 0');
    expect(wrapper.find('[data-test="decision-empty-readiness"]').text()).toContain('复盘资料度');
    expect(wrapper.find('[data-test="decision-empty-readiness"]').text()).toContain('赛果记录');
    expect(wrapper.find('[data-test="decision-empty-readiness"]').text()).toContain('复盘入库');
    expect(wrapper.findAll('[data-test="decision-empty-readiness"] .decision-ring-grid .coverage-donut')).toHaveLength(5);
    expect(wrapper.findAll('[data-test="decision-overview-rings"] .coverage-donut')).toHaveLength(5);
    expect(wrapper.find('[data-test="decision-overview-rings"]').text()).toContain('材料合计');
    expect(wrapper.text()).not.toMatch(/第一步|第二步|第三步|确认比分\/状态|比分\/状态锚点|复盘结构/);
    expect(wrapper.find('a[href="/evidence/matches"]').exists()).toBe(true);
    expect(wrapper.find('a[href="/evidence"]').exists()).toBe(true);
    expect(wrapper.text()).toContain('已筛出 0 / 0 份报告');
  });

  it('declares an H5 card layout for public decisions', () => {
    expect(analysisReviewSource).not.toContain('<el-table');
    expect(analysisReviewSource).toContain('@media (max-width: 640px)');
    expect(analysisReviewSource).toContain('grid-template-columns: 1fr');
    expect(analysisReviewSource).toContain('grid-template-columns: repeat(2, minmax(0, 1fr));');
    expect(analysisReviewSource).toContain('min-width: 0');
  });
});
