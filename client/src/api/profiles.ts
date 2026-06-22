import { http } from './http';
import type { ApiResponse } from './system';

export type CollectionItemStatus = 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED';
export type ProfileEntityType = 'TEAM' | 'PLAYER';

export interface CollectionItem {
  id: number;
  jobId: number;
  entityType: ProfileEntityType;
  entityKey: string;
  factType: string;
  periodKey?: string;
  title: string;
  summary: string;
  sentimentLabel?: string;
  confidenceScore?: number;
  reliabilityScore?: number;
  sourceName: string;
  sourceUrl?: string;
  sourceRef?: string;
  capturedAt?: string;
  status: CollectionItemStatus;
  reviewNote?: string;
  reviewedBy?: string;
  reviewedAt?: string;
  targetType?: string;
  targetId?: number;
}

export interface CollectionItemReviewResult {
  id: number;
  status: CollectionItemStatus;
  targetType?: string;
  targetId?: number;
  message: string;
}

export interface ProfileFact {
  id: number;
  factType: string;
  periodKey?: string;
  title: string;
  summary: string;
  sentimentLabel?: string;
  confidenceScore?: number;
  reliabilityScore?: number;
  sourceName: string;
  sourceUrl?: string;
  sourceRef?: string;
  capturedAt?: string;
  approvedBy?: string;
}

export interface TeamProfileSummary {
  id: number;
  teamKey: string;
  displayName: string;
  fifaCode?: string;
  countryRegion?: string;
  styleTags?: string;
  attackProfile?: string;
  defenseProfile?: string;
  publicSentiment?: string;
  playerCount: number;
  factCount: number;
  latestProfileUpdate?: string;
}

export interface TeamPlayer {
  id: number;
  playerKey: string;
  displayName: string;
  shirtNumber?: number;
  position?: string;
  status?: string;
  injuryStatus?: string;
  cardStatus?: string;
  lockerRoomStatus?: string;
}

export interface TeamLineup {
  matchId: number;
  matchName: string;
  matchday?: string;
  playerId?: number;
  playerName: string;
  role?: string;
  position?: string;
  starter: boolean;
}

export interface TeamScoringPattern {
  matchId: number;
  matchName: string;
  matchday?: string;
  goalsFor?: number;
  goalsAgainst?: number;
  firstGoalMinute?: number;
  scoringMinutes?: string;
}

export interface TeamExternalFactor {
  matchId: number;
  matchName: string;
  matchday?: string;
  externalFactors: string;
}

export interface TeamMatchHistory {
  matchId: number;
  matchName: string;
  matchday?: string;
  competition?: string;
  stage?: string;
  venue?: string;
  resultStatus?: string;
  goalsFor?: number;
  goalsAgainst?: number;
  scoringMinutes?: string;
}

export interface TeamProfileDetail {
  team: TeamProfileSummary;
  facts: ProfileFact[];
  players: TeamPlayer[];
  lineups: TeamLineup[];
  scoringPatterns: TeamScoringPattern[];
  externalFactors: TeamExternalFactor[];
  matchHistory: TeamMatchHistory[];
  evidenceCount: number;
  conflictCount: number;
}

export interface PlayerProfileSummary {
  id: number;
  playerKey: string;
  teamId?: number;
  teamName?: string;
  displayName: string;
  shirtNumber?: number;
  position?: string;
  status?: string;
  injuryStatus?: string;
  cardStatus?: string;
  lockerRoomStatus?: string;
  factCount: number;
  latestProfileUpdate?: string;
}

export interface PlayerProfileDetail {
  player: PlayerProfileSummary;
  facts: ProfileFact[];
}

function authHeaders(authHeader: string) {
  return authHeader ? { Authorization: authHeader } : undefined;
}

export function buildCollectionItemsPath(status?: CollectionItemStatus): string {
  return status ? `/profiles/collections/items?status=${status}` : '/profiles/collections/items';
}

export function buildApproveCollectionItemPath(id: number): string {
  return `/profiles/collections/items/${id}/approve`;
}

export function buildRejectCollectionItemPath(id: number): string {
  return `/profiles/collections/items/${id}/reject`;
}

export function buildTeamDetailPath(id: number): string {
  return `/profiles/teams/${id}`;
}

export function buildTeamPlayersPath(id: number): string {
  return `/profiles/teams/${id}/players`;
}

export function buildPlayerDetailPath(id: number): string {
  return `/profiles/players/${id}`;
}

export async function listCollectionItems(
  authHeader: string,
  status?: CollectionItemStatus,
): Promise<ApiResponse<CollectionItem[]>> {
  const response = await http.get<ApiResponse<CollectionItem[]>>('/profiles/collections/items', {
    headers: authHeaders(authHeader),
    params: status ? { status } : undefined,
  });
  return response.data;
}

export async function approveCollectionItem(
  authHeader: string,
  id: number,
): Promise<ApiResponse<CollectionItemReviewResult>> {
  const response = await http.post<ApiResponse<CollectionItemReviewResult>>(
    buildApproveCollectionItemPath(id),
    undefined,
    { headers: authHeaders(authHeader) },
  );
  return response.data;
}

export async function rejectCollectionItem(
  authHeader: string,
  id: number,
  reason: string,
): Promise<ApiResponse<CollectionItemReviewResult>> {
  const response = await http.post<ApiResponse<CollectionItemReviewResult>>(
    buildRejectCollectionItemPath(id),
    { reason },
    { headers: authHeaders(authHeader) },
  );
  return response.data;
}

export async function listTeamProfiles(authHeader: string): Promise<ApiResponse<TeamProfileSummary[]>> {
  const response = await http.get<ApiResponse<TeamProfileSummary[]>>('/profiles/teams', {
    headers: authHeaders(authHeader),
  });
  return response.data;
}

export async function getTeamProfile(authHeader: string, id: number): Promise<ApiResponse<TeamProfileDetail>> {
  const response = await http.get<ApiResponse<TeamProfileDetail>>(buildTeamDetailPath(id), {
    headers: authHeaders(authHeader),
  });
  return response.data;
}

export async function listTeamPlayers(authHeader: string, id: number): Promise<ApiResponse<TeamPlayer[]>> {
  const response = await http.get<ApiResponse<TeamPlayer[]>>(buildTeamPlayersPath(id), {
    headers: authHeaders(authHeader),
  });
  return response.data;
}

export async function listPlayerProfiles(authHeader: string): Promise<ApiResponse<PlayerProfileSummary[]>> {
  const response = await http.get<ApiResponse<PlayerProfileSummary[]>>('/profiles/players', {
    headers: authHeaders(authHeader),
  });
  return response.data;
}

export async function getPlayerProfile(authHeader: string, id: number): Promise<ApiResponse<PlayerProfileDetail>> {
  const response = await http.get<ApiResponse<PlayerProfileDetail>>(buildPlayerDetailPath(id), {
    headers: authHeaders(authHeader),
  });
  return response.data;
}
