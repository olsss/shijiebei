import { createAuthHeaders, http, publicHttp } from './http';
import type { ApiResponse } from './system';
import type { Scoreboard, TeamVisual } from '@/utils/football-visuals';

export interface SentimentFactorSummary {
  id: number;
  matchId?: number;
  matchName?: string;
  matchday?: string;
  jcCode?: string;
  homeTeam?: TeamVisual;
  awayTeam?: TeamVisual;
  scoreboard?: Scoreboard;
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
  homeTeam?: TeamVisual;
  awayTeam?: TeamVisual;
  scoreboard?: Scoreboard;
  factors: SentimentFactorDetail[];
  risks: SentimentRisk[];
}

export type PublicSentimentFactorSummary = SentimentFactorSummary;
export type PublicSentimentFactorDetail = Omit<SentimentFactorDetail, 'rawPayload'>;
export type PublicSentimentRisk = Omit<SentimentRisk, 'rawPayload'>;
export interface PublicSentimentMatchDetail extends Omit<SentimentMatchDetail, 'factors' | 'risks'> {
  factors: PublicSentimentFactorDetail[];
  risks: PublicSentimentRisk[];
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
    headers: createAuthHeaders(authHeader),
  });
  return response.data;
}

export async function getMatchSentiment(
  authHeader: string,
  matchId: number,
): Promise<ApiResponse<SentimentMatchDetail>> {
  const response = await http.get<ApiResponse<SentimentMatchDetail>>(buildMatchSentimentPath(matchId), {
    headers: createAuthHeaders(authHeader),
  });
  return response.data;
}

export async function listSentimentCategories(authHeader: string): Promise<ApiResponse<string[]>> {
  const response = await http.get<ApiResponse<string[]>>(buildSentimentCategoriesPath(), {
    headers: createAuthHeaders(authHeader),
  });
  return response.data;
}

export async function listSentimentRiskTypes(authHeader: string): Promise<ApiResponse<string[]>> {
  const response = await http.get<ApiResponse<string[]>>(buildSentimentRiskTypesPath(), {
    headers: createAuthHeaders(authHeader),
  });
  return response.data;
}

export async function listPublicSentimentOverview(): Promise<ApiResponse<PublicSentimentFactorSummary[]>> {
  const response = await publicHttp.get<ApiResponse<PublicSentimentFactorSummary[]>>('/sentiment');
  return response.data;
}

export async function getPublicMatchSentiment(
  matchId: number,
): Promise<ApiResponse<PublicSentimentMatchDetail>> {
  const response = await publicHttp.get<ApiResponse<PublicSentimentMatchDetail>>(buildMatchSentimentPath(matchId));
  return response.data;
}

export async function listPublicSentimentCategories(): Promise<ApiResponse<string[]>> {
  const response = await publicHttp.get<ApiResponse<string[]>>(buildSentimentCategoriesPath());
  return response.data;
}

export async function listPublicSentimentRiskTypes(): Promise<ApiResponse<string[]>> {
  const response = await publicHttp.get<ApiResponse<string[]>>(buildSentimentRiskTypesPath());
  return response.data;
}
