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
    expect(fetchAnalysisReviewOverview).not.toHaveBeenCalled();
    expect(listAnalysisReports).not.toHaveBeenCalled();
    expect(getAnalysisReport).not.toHaveBeenCalled();
    expect(listBetPlans).not.toHaveBeenCalled();
    expect(getBetPlan).not.toHaveBeenCalled();
    expect(listBetRecords).not.toHaveBeenCalled();
    expect(listPostMatchReviews).not.toHaveBeenCalled();

    expect(wrapper.text()).toContain('决策复盘中心');
    expect(wrapper.text()).toContain('France vs Brazil');
    expect(wrapper.text()).toContain('复盘指向稳健');
    expect(wrapper.text()).toContain('高热度比赛降低权重');
    expect(wrapper.text()).not.toMatch(/ticket|ticketNo|stake|stakeSuggestion|betPlan|rawPayload|profit|loss|profitLoss|budgetAmount|returnAmount|ROI|CLV|票号|投入|返还|盈亏|预算|下注|金额建议|原始 JSON/i);
  });

  it('declares an H5 card layout for public decisions', () => {
    expect(analysisReviewSource).not.toContain('<el-table');
    expect(analysisReviewSource).toContain('@media (max-width: 640px)');
    expect(analysisReviewSource).toContain('grid-template-columns: 1fr');
    expect(analysisReviewSource).toContain('min-width: 0');
  });
});
