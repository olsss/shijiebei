import { publicHttp } from './http';
import type { ApiResponse } from './system';

export interface PublicOverviewMatch {
  matchId: number;
  matchName: string;
  matchday?: string;
  jcCode?: string;
  competition?: string;
  stage?: string;
  kickoffTime?: string;
  status?: string;
  integrityScore: number;
  riskCount: number;
}

export interface PublicRiskCounters {
  highRiskCount: number;
  mediumRiskCount: number;
  staleFactorCount: number;
  unresolvedConflictCount: number;
}

export interface PublicIntegrityCounters {
  completeCount: number;
  partialCount: number;
  blockedCount: number;
}

export interface PublicOddsFreshness {
  marketCount: number;
  liveMarketCount: number;
  staleLiveMarketCount: number;
}

export interface PublicDecisionSummary {
  reportCount: number;
  reviewCount: number;
  latestDecisionAt?: string;
}

export interface PublicOverviewResponse {
  generatedAt: string;
  upcomingMatches: PublicOverviewMatch[];
  riskCounters: PublicRiskCounters;
  integrityCounters: PublicIntegrityCounters;
  oddsFreshness: PublicOddsFreshness;
  decisionSummary: PublicDecisionSummary;
}

export async function fetchPublicOverview(): Promise<ApiResponse<PublicOverviewResponse>> {
  const response = await publicHttp.get<ApiResponse<PublicOverviewResponse>>('/overview');
  return response.data;
}
