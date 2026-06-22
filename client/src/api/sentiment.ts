import { http } from './http';
import type { ApiResponse } from './system';

export interface SentimentFactorSummary {
  id: number;
  matchId?: number;
  matchName?: string;
  matchday?: string;
  jcCode?: string;
  factorCategory: string;
  factorType: string;
  title: string;
  summary?: string;
  impactDirection?: string;
  entityType?: string;
  entityKey?: string;
  evidenceLevel?: string;
  sourceName?: string;
  sourceUrl?: string;
  sourceRef?: string;
  observedAt?: string;
  expiresAt?: string;
  confidenceScore?: number;
  reliabilityScore?: number;
  stale: boolean;
  riskCount: number;
  highestRiskLevel: string;
}

export interface SentimentFactorDetail extends Omit<SentimentFactorSummary, 'riskCount' | 'highestRiskLevel'> {
  rawPayload?: string;
}

export interface SentimentRisk {
  id: number;
  matchId?: number;
  factorId?: number;
  riskType: string;
  riskLevel: string;
  riskScore?: number;
  title: string;
  rationale?: string;
  suggestedAction?: string;
  sourceName?: string;
  sourceRef?: string;
  rawPayload?: string;
}

export interface SentimentMatchDetail {
  matchId: number;
  matchName: string;
  matchday?: string;
  jcCode?: string;
  factors: SentimentFactorDetail[];
  risks: SentimentRisk[];
}

function authHeaders(authHeader: string) {
  return authHeader ? { Authorization: authHeader } : undefined;
}

export function buildMatchSentimentPath(matchId: number): string {
  return `/sentiment/matches/${matchId}`;
}

export function buildSentimentCategoriesPath(): string {
  return '/sentiment/categories';
}

export function buildSentimentRiskTypesPath(): string {
  return '/sentiment/risk-types';
}

export async function listSentimentOverview(authHeader: string): Promise<ApiResponse<SentimentFactorSummary[]>> {
  const response = await http.get<ApiResponse<SentimentFactorSummary[]>>('/sentiment', {
    headers: authHeaders(authHeader),
  });
  return response.data;
}

export async function getMatchSentiment(
  authHeader: string,
  matchId: number,
): Promise<ApiResponse<SentimentMatchDetail>> {
  const response = await http.get<ApiResponse<SentimentMatchDetail>>(buildMatchSentimentPath(matchId), {
    headers: authHeaders(authHeader),
  });
  return response.data;
}

export async function listSentimentCategories(authHeader: string): Promise<ApiResponse<string[]>> {
  const response = await http.get<ApiResponse<string[]>>(buildSentimentCategoriesPath(), {
    headers: authHeaders(authHeader),
  });
  return response.data;
}

export async function listSentimentRiskTypes(authHeader: string): Promise<ApiResponse<string[]>> {
  const response = await http.get<ApiResponse<string[]>>(buildSentimentRiskTypesPath(), {
    headers: authHeaders(authHeader),
  });
  return response.data;
}
