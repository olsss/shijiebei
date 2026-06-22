import { describe, expect, it } from 'vitest';
import type { PublicOverviewResponse } from '@/api/publicOverview';
import type { PublicProfileFact } from '@/api/profiles';
import type { PublicPrematchWorkbenchDetail, PublicWorkbenchAnalysisReport } from '@/api/prematchWorkbench';

const publicFact: PublicProfileFact = {
  id: 1,
  factType: 'STYLE',
  title: 'Public style',
  summary: 'Public summary',
  sourceName: 'Source',
};

const publicFactWithApprovedBy: PublicProfileFact = {
  ...publicFact,
  // @ts-expect-error public profile facts must not expose approval metadata
  approvedBy: 'admin',
};

const publicReport: PublicWorkbenchAnalysisReport = {
  reportId: 1,
  analysisId: 'analysis-1',
  conclusionType: 'VALUE',
  confidence: 'MEDIUM',
  riskSummary: 'Public risk',
  recommendedMarkets: 'HAD',
  dimensions: 'odds,integrity',
  createdAt: '2026-06-22T00:00:00',
  updatedAt: '2026-06-22T01:00:00',
};

const publicReportWithNarrative: PublicWorkbenchAnalysisReport = {
  ...publicReport,
  // @ts-expect-error public prematch reports must not expose rich narrative markdown
  narrativeMd: 'internal markdown',
};

const publicPrematchDetail: PublicPrematchWorkbenchDetail = {
  summary: {
    matchId: 1,
    matchKey: 'match-1',
    matchName: 'France vs Brazil',
    integrityScore: 90,
    missingCount: 0,
    staleCount: 0,
    conflictCount: 0,
    teamProfileCount: 2,
    playerProfileCount: 22,
    lineupCount: 2,
    oddsMarketCount: 8,
    sentimentFactorCount: 3,
    analysisReportCount: 1,
  },
  teams: [],
  lineups: [],
  players: [],
  oddsMarkets: [],
  sentimentFactors: [],
  evidence: [],
  conflicts: [],
  analysisReports: [],
  integrityChecks: [],
};

const publicPrematchWithBetPlans: PublicPrematchWorkbenchDetail = {
  ...publicPrematchDetail,
  // @ts-expect-error public prematch detail must not expose bet plans
  betPlans: [],
};

const publicPrematchWithBets: PublicPrematchWorkbenchDetail = {
  ...publicPrematchDetail,
  // @ts-expect-error public prematch detail must not expose bet records
  bets: [],
};

const publicOverview: PublicOverviewResponse = {
  generatedAt: '2026-06-22T00:00:00',
  upcomingMatches: [],
  riskCounters: {
    highRiskCount: 0,
    mediumRiskCount: 0,
    staleFactorCount: 0,
    unresolvedConflictCount: 0,
  },
  integrityCounters: {
    completeCount: 0,
    partialCount: 0,
    blockedCount: 0,
  },
  oddsFreshness: {
    marketCount: 0,
    liveMarketCount: 0,
    staleLiveMarketCount: 0,
  },
  decisionSummary: {
    reportCount: 0,
    reviewCount: 0,
  },
};

const publicOverviewWithAdminTodos: PublicOverviewResponse = {
  ...publicOverview,
  // @ts-expect-error public overview must not expose admin review queue counters
  adminTodoCounters: {
    pendingImportReviews: 1,
    pendingCollectionReviews: 2,
  },
};

describe('public api types', () => {
  it('keeps public fixtures usable at runtime', () => {
    expect(publicFact.sourceName).toBe('Source');
    expect(publicReport.analysisId).toBe('analysis-1');
    expect(publicPrematchDetail.analysisReports).toEqual([]);
    expect(publicFactWithApprovedBy.id).toBe(1);
    expect(publicReportWithNarrative.reportId).toBe(1);
    expect(publicPrematchWithBetPlans.summary.matchId).toBe(1);
    expect(publicPrematchWithBets.summary.matchId).toBe(1);
    expect(publicOverview.upcomingMatches).toEqual([]);
    expect(publicOverviewWithAdminTodos.generatedAt).toBe('2026-06-22T00:00:00');
  });
});
