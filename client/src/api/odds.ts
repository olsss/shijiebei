import { http } from './http';
import type { ApiResponse } from './system';

export interface OddsMarketSummary {
  id: number;
  matchId?: number;
  matchName?: string;
  matchday?: string;
  jcCode?: string;
  bookmaker: string;
  marketCode: string;
  marketName?: string;
  snapshotType: string;
  handicapLine?: number;
  lineValue?: string;
  capturedAt?: string;
  selectionCount: number;
}

export interface OddsSelection {
  id: number;
  marketSnapshotId: number;
  selectionCode: string;
  selectionName: string;
  oddsValue?: number;
  impliedProbability?: number;
  selectionStatus: string;
  rawPayload?: string;
}

export interface OddsMarketDetail extends OddsMarketSummary {
  sourceRef?: string;
  rawPayload?: string;
  selections: OddsSelection[];
}

export interface OddsMatchDetail {
  matchId: number;
  matchName: string;
  matchday?: string;
  jcCode?: string;
  markets: OddsMarketDetail[];
}

export interface OddsMarketDictionaryItem {
  marketCode: string;
  marketName?: string;
}

function authHeaders(authHeader: string) {
  return authHeader ? { Authorization: authHeader } : undefined;
}

export function buildMatchOddsPath(matchId: number): string {
  return `/odds/matches/${matchId}`;
}

export function buildBookmakersPath(): string {
  return '/odds/bookmakers';
}

export function buildMarketsPath(): string {
  return '/odds/markets';
}

export async function listOddsOverview(authHeader: string): Promise<ApiResponse<OddsMarketSummary[]>> {
  const response = await http.get<ApiResponse<OddsMarketSummary[]>>('/odds', {
    headers: authHeaders(authHeader),
  });
  return response.data;
}

export async function getMatchOdds(authHeader: string, matchId: number): Promise<ApiResponse<OddsMatchDetail>> {
  const response = await http.get<ApiResponse<OddsMatchDetail>>(buildMatchOddsPath(matchId), {
    headers: authHeaders(authHeader),
  });
  return response.data;
}

export async function listBookmakers(authHeader: string): Promise<ApiResponse<string[]>> {
  const response = await http.get<ApiResponse<string[]>>(buildBookmakersPath(), {
    headers: authHeaders(authHeader),
  });
  return response.data;
}

export async function listOddsMarkets(authHeader: string): Promise<ApiResponse<OddsMarketDictionaryItem[]>> {
  const response = await http.get<ApiResponse<OddsMarketDictionaryItem[]>>(buildMarketsPath(), {
    headers: authHeaders(authHeader),
  });
  return response.data;
}
