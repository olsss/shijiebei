import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  buildAnalysisReportPath,
  buildAnalysisReviewOverviewPath,
  buildBetPlanPath,
  listPublicDecisionReports,
  listPublicDecisionReviews,
} from '@/api/analysisReview';
import { publicHttp } from '@/api/http';

const okResponse = { data: { success: true, data: [], message: '', timestamp: '' } };

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
});
