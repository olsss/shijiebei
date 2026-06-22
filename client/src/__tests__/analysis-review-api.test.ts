import { describe, expect, it } from 'vitest';
import {
  buildAnalysisReportPath,
  buildAnalysisReviewOverviewPath,
  buildBetPlanPath,
} from '@/api/analysisReview';

describe('analysis review api helpers', () => {
  it('builds analysis review center paths', () => {
    expect(buildAnalysisReviewOverviewPath()).toBe('/analysis-review/overview');
    expect(buildAnalysisReportPath(6)).toBe('/analysis-review/reports/6');
    expect(buildBetPlanPath(7)).toBe('/analysis-review/bet-plans/7');
  });
});
