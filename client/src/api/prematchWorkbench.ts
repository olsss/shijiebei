import { http } from './http';
import type { ApiResponse } from './system';

export interface WorkbenchMatchSummary {
  matchId: number;
  matchKey: string;
  matchName: string;
  matchday?: string;
  jcCode?: string;
  competition?: string;
  stage?: string;
  venue?: string;
  kickoffTime?: string;
  status?: string;
  resultStatus?: string;
  homeTeamId?: number;
  homeTeamName?: string;
  awayTeamId?: number;
  awayTeamName?: string;
  integrityScore: number;
  missingCount: number;
  staleCount: number;
  conflictCount: number;
  teamProfileCount: number;
  playerProfileCount: number;
  lineupCount: number;
  oddsMarketCount: number;
  sentimentFactorCount: number;
  analysisReportCount: number;
  betPlanCount: number;
  betCount: number;
}

export interface IntegrityCheck {
  code: string;
  label: string;
  status: 'PASS' | 'MISSING' | 'STALE' | 'CONFLICT' | string;
  severity: string;
  message: string;
  evidenceCount: number;
  lastUpdatedAt?: string;
}

export interface WorkbenchTeamFact {
  factId: number;
  factType: string;
  title: string;
  summary?: string;
  sourceName?: string;
  capturedAt?: string;
}

export interface WorkbenchTeam {
  teamId: number;
  teamKey: string;
  teamName: string;
  fifaCode?: string;
  styleTags?: string;
  attackProfile?: string;
  defenseProfile?: string;
  publicSentiment?: string;
  facts: WorkbenchTeamFact[];
}

export interface WorkbenchLineup {
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

export interface WorkbenchPlayerFact {
  factId: number;
  factType: string;
  title: string;
  summary?: string;
  sourceName?: string;
  capturedAt?: string;
}

export interface WorkbenchPlayer {
  playerId: number;
  playerKey: string;
  teamId?: number;
  teamName?: string;
  playerName: string;
  shirtNumber?: number;
  position?: string;
  status?: string;
  injuryStatus?: string;
  cardStatus?: string;
  lockerRoomStatus?: string;
  facts: WorkbenchPlayerFact[];
}

export interface WorkbenchOddsSelection {
  selectionId: number;
  selectionCode: string;
  selectionName: string;
  oddsValue?: number;
  impliedProbability?: number;
  selectionStatus?: string;
}

export interface WorkbenchOddsMarket {
  marketId: number;
  bookmaker: string;
  marketCode: string;
  marketName?: string;
  snapshotType?: string;
  handicapLine?: number;
  lineValue?: string;
  capturedAt?: string;
  sourceRef?: string;
  selections: WorkbenchOddsSelection[];
}

export interface WorkbenchSentimentRisk {
  riskId: number;
  riskType: string;
  riskLevel?: string;
  riskScore?: number;
  title: string;
  rationale?: string;
  suggestedAction?: string;
  sourceName?: string;
}

export interface WorkbenchSentimentFactor {
  factorId: number;
  factorCategory: string;
  factorType: string;
  title: string;
  summary?: string;
  impactDirection?: string;
  evidenceLevel?: string;
  sourceName?: string;
  observedAt?: string;
  expiresAt?: string;
  risks: WorkbenchSentimentRisk[];
}

export interface WorkbenchEvidence {
  evidenceId: number;
  sourceType: string;
  sourceName: string;
  sourceRef?: string;
  sourceUrl?: string;
  evidenceTime?: string;
  summary?: string;
  reliabilityScore?: number;
}

export interface WorkbenchConflict {
  conflictId: number;
  conflictType: string;
  fieldName?: string;
  currentValue?: string;
  incomingValue?: string;
  resolutionStatus: string;
}

export interface WorkbenchAnalysisReport {
  reportId: number;
  analysisId: string;
  conclusionType?: string;
  confidence?: string;
  riskSummary?: string;
  narrativeMd?: string;
}

export interface WorkbenchBetPlanItem {
  itemId: number;
  marketType?: string;
  selectionText: string;
  stakeSuggestion?: number;
  odds?: number;
  logicType?: string;
  riskLevel?: string;
  playType?: string;
  passType?: string;
}

export interface WorkbenchBetPlan {
  planId: number;
  planKey: string;
  planTitle: string;
  bettingMethod?: string;
  strategyType?: string;
  confidence?: string;
  budgetAmount?: number;
  riskSummary?: string;
  status?: string;
  generatedBy?: string;
  generatedAt?: string;
  items: WorkbenchBetPlanItem[];
}

export interface WorkbenchBetRecord {
  betRecordId: number;
  betId: string;
  ticketNo?: string;
  betDate?: string;
  matchday?: string;
  matchName?: string;
  marketType?: string;
  selectionText?: string;
  stake?: number;
  odds?: number;
  closingOdds?: number;
  clv?: number;
  returnAmount?: number;
  hitStatus?: string;
  profitLoss?: number;
  reviewStatus?: string;
}

export interface PrematchWorkbenchDetail {
  summary: WorkbenchMatchSummary;
  teams: WorkbenchTeam[];
  lineups: WorkbenchLineup[];
  players: WorkbenchPlayer[];
  oddsMarkets: WorkbenchOddsMarket[];
  sentimentFactors: WorkbenchSentimentFactor[];
  evidence: WorkbenchEvidence[];
  conflicts: WorkbenchConflict[];
  analysisReports: WorkbenchAnalysisReport[];
  betPlans: WorkbenchBetPlan[];
  bets: WorkbenchBetRecord[];
  integrityChecks: IntegrityCheck[];
}

function authHeaders(authHeader: string) {
  return authHeader ? { Authorization: authHeader } : undefined;
}

export function buildPrematchWorkbenchMatchesPath(): string {
  return '/prematch-workbench/matches';
}

export function buildPrematchWorkbenchMatchPath(matchId: number): string {
  return `/prematch-workbench/matches/${matchId}`;
}

export function buildPrematchWorkbenchIntegrityPath(matchId: number): string {
  return `/prematch-workbench/matches/${matchId}/integrity`;
}

export async function listPrematchWorkbenchMatches(authHeader: string): Promise<ApiResponse<WorkbenchMatchSummary[]>> {
  const response = await http.get<ApiResponse<WorkbenchMatchSummary[]>>(buildPrematchWorkbenchMatchesPath(), {
    headers: authHeaders(authHeader),
  });
  return response.data;
}

export async function getPrematchWorkbenchMatch(authHeader: string, matchId: number): Promise<ApiResponse<PrematchWorkbenchDetail>> {
  const response = await http.get<ApiResponse<PrematchWorkbenchDetail>>(buildPrematchWorkbenchMatchPath(matchId), {
    headers: authHeaders(authHeader),
  });
  return response.data;
}

export async function getPrematchWorkbenchIntegrity(authHeader: string, matchId: number): Promise<ApiResponse<IntegrityCheck[]>> {
  const response = await http.get<ApiResponse<IntegrityCheck[]>>(buildPrematchWorkbenchIntegrityPath(matchId), {
    headers: authHeaders(authHeader),
  });
  return response.data;
}
