import { http } from './http';
import type { ApiResponse } from './system';

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

function authHeaders(authHeader: string) {
  return authHeader ? { Authorization: authHeader } : undefined;
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
    headers: authHeaders(authHeader),
  });
  return response.data;
}

export async function getMatchDetail(authHeader: string, id: number): Promise<ApiResponse<MatchDetail>> {
  const response = await http.get<ApiResponse<MatchDetail>>(buildMatchDetailPath(id), {
    headers: authHeaders(authHeader),
  });
  return response.data;
}

export async function listMatchLineups(authHeader: string, id: number): Promise<ApiResponse<MatchLineup[]>> {
  const response = await http.get<ApiResponse<MatchLineup[]>>(buildMatchLineupsPath(id), {
    headers: authHeaders(authHeader),
  });
  return response.data;
}

export async function listMatchEvents(authHeader: string, id: number): Promise<ApiResponse<MatchEvent[]>> {
  const response = await http.get<ApiResponse<MatchEvent[]>>(buildMatchEventsPath(id), {
    headers: authHeaders(authHeader),
  });
  return response.data;
}

export async function listMatchTeamStats(authHeader: string, id: number): Promise<ApiResponse<MatchTeamStats[]>> {
  const response = await http.get<ApiResponse<MatchTeamStats[]>>(buildMatchTeamStatsPath(id), {
    headers: authHeaders(authHeader),
  });
  return response.data;
}

export async function listMatchPlayerStats(authHeader: string, id: number): Promise<ApiResponse<MatchPlayerStats[]>> {
  const response = await http.get<ApiResponse<MatchPlayerStats[]>>(buildMatchPlayerStatsPath(id), {
    headers: authHeaders(authHeader),
  });
  return response.data;
}
