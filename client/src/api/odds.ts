import { createAuthHeaders, http, publicHttp } from './http';
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

export type PublicOddsMarketSummary = OddsMarketSummary;
export type PublicOddsSelection = Omit<OddsSelection, 'rawPayload'>;
export type PublicOddsMarketDetail = Omit<OddsMarketDetail, 'rawPayload' | 'selections'> & {
  selections: PublicOddsSelection[];
};
export interface PublicOddsMatchDetail extends Omit<OddsMatchDetail, 'markets'> {
  markets: PublicOddsMarketDetail[];
}
export type PublicOddsMarketDictionaryItem = OddsMarketDictionaryItem;

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
    headers: createAuthHeaders(authHeader),
  });
  return response.data;
}

export async function getMatchOdds(authHeader: string, matchId: number): Promise<ApiResponse<OddsMatchDetail>> {
  const response = await http.get<ApiResponse<OddsMatchDetail>>(buildMatchOddsPath(matchId), {
    headers: createAuthHeaders(authHeader),
  });
  return response.data;
}

export async function listBookmakers(authHeader: string): Promise<ApiResponse<string[]>> {
  const response = await http.get<ApiResponse<string[]>>(buildBookmakersPath(), {
    headers: createAuthHeaders(authHeader),
  });
  return response.data;
}

export async function listOddsMarkets(authHeader: string): Promise<ApiResponse<OddsMarketDictionaryItem[]>> {
  const response = await http.get<ApiResponse<OddsMarketDictionaryItem[]>>(buildMarketsPath(), {
    headers: createAuthHeaders(authHeader),
  });
  return response.data;
}

export async function listPublicOddsOverview(): Promise<ApiResponse<PublicOddsMarketSummary[]>> {
  const response = await publicHttp.get<ApiResponse<PublicOddsMarketSummary[]>>('/odds');
  return response.data;
}

export async function getPublicMatchOdds(matchId: number): Promise<ApiResponse<PublicOddsMatchDetail>> {
  const response = await publicHttp.get<ApiResponse<PublicOddsMatchDetail>>(buildMatchOddsPath(matchId));
  return response.data;
}

export async function listPublicBookmakers(): Promise<ApiResponse<string[]>> {
  const response = await publicHttp.get<ApiResponse<string[]>>(buildBookmakersPath());
  return response.data;
}

export async function listPublicOddsMarkets(): Promise<ApiResponse<PublicOddsMarketDictionaryItem[]>> {
  const response = await publicHttp.get<ApiResponse<PublicOddsMarketDictionaryItem[]>>(buildMarketsPath());
  return response.data;
}
