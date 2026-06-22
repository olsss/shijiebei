import { describe, expect, it, vi } from 'vitest';
import { fetchPublicOverview, type PublicOverviewResponse } from '@/api/publicOverview';
import { publicHttp } from '@/api/http';

const overviewFixture: PublicOverviewResponse = {
  generatedAt: '2026-06-22T00:00:00',
  upcomingMatches: [
    {
      matchId: 1,
      matchName: 'France vs Brazil',
      matchday: '2026-06-22',
      jcCode: '001',
      competition: 'World Cup',
      stage: 'Group',
      kickoffTime: '2026-06-22T20:00:00',
      status: 'SCHEDULED',
      integrityScore: 88,
      riskCount: 2,
    },
  ],
  riskCounters: {
    highRiskCount: 1,
    mediumRiskCount: 2,
    staleFactorCount: 0,
    unresolvedConflictCount: 0,
  },
  integrityCounters: {
    completeCount: 3,
    partialCount: 1,
    blockedCount: 0,
  },
  oddsFreshness: {
    marketCount: 10,
    liveMarketCount: 6,
    staleLiveMarketCount: 1,
  },
  decisionSummary: {
    reportCount: 4,
    reviewCount: 2,
    latestDecisionAt: '2026-06-22T01:00:00',
  },
  adminTodoCounters: {
    pendingImportReviews: 1,
    pendingCollectionReviews: 2,
  },
};

describe('public overview api', () => {
  it('calls public overview without Authorization', async () => {
    const getSpy = vi.spyOn(publicHttp, 'get').mockResolvedValueOnce({
      data: { success: true, data: overviewFixture, message: '', timestamp: '' },
    });

    const result = await fetchPublicOverview();

    expect(getSpy).toHaveBeenCalledWith('/overview');
    expect(result.data.upcomingMatches).toHaveLength(1);
  });
});
