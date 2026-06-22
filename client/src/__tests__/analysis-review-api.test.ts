import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  buildAnalysisReportPath,
  buildAnalysisReviewOverviewPath,
  buildBetPlanPath,
  fetchAnalysisReviewOverview,
  listAnalysisReports,
  listBetRecords,
  listPublicDecisionReports,
  listPublicDecisionReviews,
} from '@/api/analysisReview';
import { http, publicHttp } from '@/api/http';

const okResponse = { data: { success: true, data: [], message: '', timestamp: '' } };
const authHeaders = { Authorization: 'Basic abc' };

describe('analysis review api helpers', () => {
  afterEach(() => vi.restoreAllMocks());

  it('builds analysis review center paths', () => {
    expect(buildAnalysisReviewOverviewPath()).toBe('/analysis-review/overview');
    expect(buildAnalysisReportPath(6)).toBe('/analysis-review/reports/6');
    expect(buildBetPlanPath(7)).toBe('/analysis-review/bet-plans/7');
  });

  it('calls public decision endpoints without Authorization', async () => {
    const getSpy = vi.spyOn(publicHttp, 'get').mockResolvedValue(okResponse);

    await listPublicDecisionReports();
    await listPublicDecisionReviews();

    expect(getSpy).toHaveBeenNthCalledWith(1, '/decisions/reports');
    expect(getSpy).toHaveBeenNthCalledWith(2, '/decisions/reviews');
  });

  it('calls admin analysis review endpoints with Authorization headers', async () => {
    const getSpy = vi.spyOn(http, 'get').mockResolvedValue(okResponse);

    await fetchAnalysisReviewOverview('Basic abc');
    await listAnalysisReports('Basic abc');
    await listBetRecords('Basic abc');

    expect(getSpy).toHaveBeenNthCalledWith(1, '/analysis-review/overview', { headers: authHeaders });
    expect(getSpy).toHaveBeenNthCalledWith(2, '/analysis-review/reports', { headers: authHeaders });
    expect(getSpy).toHaveBeenNthCalledWith(3, '/analysis-review/bets', { headers: authHeaders });
  });
});
