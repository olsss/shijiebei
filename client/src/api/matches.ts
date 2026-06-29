import { createAuthHeaders, http, publicHttp } from './http';
import type { ApiResponse } from './system';
import type { Scoreboard, TeamVisual } from '@/utils/football-visuals';

export interface MatchSummary {
  id: number;
  matchKey: string;
  matchName: string;
  matchday?: string;
  jcCode?: string;
  competition?: string;
  stage?: string;
  venue?: string;
  kickoffTime?: string;
  status: string;
  resultStatus: string;
  homeTeamId?: number;
  homeTeamName?: string;
  awayTeamId?: number;
  awayTeamName?: string;
  homeTeam?: TeamVisual;
  awayTeam?: TeamVisual;
  scoreboard?: Scoreboard;
  eventCount: number;
  lineupCount: number;
  evidenceCount: number;
  conflictCount: number;
}

export interface MatchLineup {
  id: number;
  matchId: number;
  teamId?: number;
  teamName?: string;
  playerId?: number;
  playerName?: string;
  role?: string;
  position?: string;
  starter: boolean;
}

export interface MatchEvent {
  id: number;
  matchId: number;
  eventMinute?: number;
  eventType: string;
  teamId?: number;
  teamName?: string;
  playerId?: number;
  playerName?: string;
  payload?: string;
}

export interface MatchTeamStats {
  id: number;
  matchId: number;
  teamId?: number;
  teamName?: string;
  statsType: string;
  goalsFor?: number;
  goalsAgainst?: number;
  firstGoalMinute?: number;
  scoringMinutes?: string;
  payload?: string;
}

export interface MatchPlayerStats {
  id: number;
  matchId: number;
  playerId?: number;
  playerName?: string;
  teamId?: number;
  teamName?: string;
  minutesPlayed?: number;
  goals?: number;
  assists?: number;
  yellowCards?: number;
  redCards?: number;
  payload?: string;
}

export interface MatchEvidence {
  id: number;
  sourceType: string;
  sourceName: string;
  sourceRef?: string;
  sourceUrl?: string;
  evidenceTime?: string;
  summary?: string;
  reliabilityScore?: number;
  qualityLevel?: string;
  freshnessStatus?: string;
  supportsConclusion?: string;
  suggestedAction?: string;
}

export interface MatchConflict {
  id: number;
  conflictType: string;
  entityKey?: string;
  fieldName?: string;
  currentValue?: string;
  incomingValue?: string;
  resolutionStatus: string;
  rawPayload?: string;
}

export interface MatchDetail {
  summary: MatchSummary;
  externalFactors?: string;
  lineups: MatchLineup[];
  events: MatchEvent[];
  teamStats: MatchTeamStats[];
  playerStats: MatchPlayerStats[];
  evidence: MatchEvidence[];
  conflicts: MatchConflict[];
}

export type PublicMatchSummary = MatchSummary;
export type PublicMatchLineup = MatchLineup;
export type PublicMatchEvent = Omit<MatchEvent, 'payload'>;
export type PublicMatchTeamStats = Omit<MatchTeamStats, 'payload'>;
export type PublicMatchPlayerStats = Omit<MatchPlayerStats, 'payload'>;
export type PublicMatchEvidence = MatchEvidence;
export type PublicMatchConflict = Omit<MatchConflict, 'currentValue' | 'incomingValue' | 'rawPayload'>;

export interface PublicMatchDetail {
  summary: PublicMatchSummary;
  externalFactors?: string;
  lineups: PublicMatchLineup[];
  events: PublicMatchEvent[];
  teamStats: PublicMatchTeamStats[];
  playerStats: PublicMatchPlayerStats[];
  evidence: PublicMatchEvidence[];
  conflicts: PublicMatchConflict[];
}

export function buildMatchDetailPath(id: number): string {
  return `/matches/${id}`;
}

export function buildMatchLineupsPath(id: number): string {
  return `/matches/${id}/lineups`;
}

export function buildMatchEventsPath(id: number): string {
  return `/matches/${id}/events`;
}

export function buildMatchTeamStatsPath(id: number): string {
  return `/matches/${id}/team-stats`;
}

export function buildMatchPlayerStatsPath(id: number): string {
  return `/matches/${id}/player-stats`;
}

export async function listMatches(authHeader: string): Promise<ApiResponse<MatchSummary[]>> {
  const response = await http.get<ApiResponse<MatchSummary[]>>('/matches', {
    headers: createAuthHeaders(authHeader),
  });
  return response.data;
}

export async function getMatchDetail(authHeader: string, id: number): Promise<ApiResponse<MatchDetail>> {
  const response = await http.get<ApiResponse<MatchDetail>>(buildMatchDetailPath(id), {
    headers: createAuthHeaders(authHeader),
  });
  return response.data;
}

export async function listMatchLineups(authHeader: string, id: number): Promise<ApiResponse<MatchLineup[]>> {
  const response = await http.get<ApiResponse<MatchLineup[]>>(buildMatchLineupsPath(id), {
    headers: createAuthHeaders(authHeader),
  });
  return response.data;
}

export async function listMatchEvents(authHeader: string, id: number): Promise<ApiResponse<MatchEvent[]>> {
  const response = await http.get<ApiResponse<MatchEvent[]>>(buildMatchEventsPath(id), {
    headers: createAuthHeaders(authHeader),
  });
  return response.data;
}

export async function listMatchTeamStats(authHeader: string, id: number): Promise<ApiResponse<MatchTeamStats[]>> {
  const response = await http.get<ApiResponse<MatchTeamStats[]>>(buildMatchTeamStatsPath(id), {
    headers: createAuthHeaders(authHeader),
  });
  return response.data;
}

export async function listMatchPlayerStats(authHeader: string, id: number): Promise<ApiResponse<MatchPlayerStats[]>> {
  const response = await http.get<ApiResponse<MatchPlayerStats[]>>(buildMatchPlayerStatsPath(id), {
    headers: createAuthHeaders(authHeader),
  });
  return response.data;
}

export async function listPublicMatches(): Promise<ApiResponse<PublicMatchSummary[]>> {
  const response = await publicHttp.get<ApiResponse<PublicMatchSummary[]>>('/matches');
  return response.data;
}

export async function getPublicMatchDetail(id: number): Promise<ApiResponse<PublicMatchDetail>> {
  const response = await publicHttp.get<ApiResponse<PublicMatchDetail>>(buildMatchDetailPath(id));
  return response.data;
}

export async function listPublicMatchLineups(id: number): Promise<ApiResponse<PublicMatchLineup[]>> {
  const response = await publicHttp.get<ApiResponse<PublicMatchLineup[]>>(buildMatchLineupsPath(id));
  return response.data;
}

export async function listPublicMatchEvents(id: number): Promise<ApiResponse<PublicMatchEvent[]>> {
  const response = await publicHttp.get<ApiResponse<PublicMatchEvent[]>>(buildMatchEventsPath(id));
  return response.data;
}

export async function listPublicMatchTeamStats(id: number): Promise<ApiResponse<PublicMatchTeamStats[]>> {
  const response = await publicHttp.get<ApiResponse<PublicMatchTeamStats[]>>(buildMatchTeamStatsPath(id));
  return response.data;
}

export async function listPublicMatchPlayerStats(id: number): Promise<ApiResponse<PublicMatchPlayerStats[]>> {
  const response = await publicHttp.get<ApiResponse<PublicMatchPlayerStats[]>>(buildMatchPlayerStatsPath(id));
  return response.data;
}
