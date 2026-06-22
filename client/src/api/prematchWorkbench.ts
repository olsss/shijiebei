import { createAuthHeaders, http, publicHttp } from './http';
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

export interface PublicWorkbenchMatchSummary {
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
}

export interface PublicPrematchFact {
  factId: number;
  factType: string;
  periodKey?: string;
  title: string;
  summary?: string;
  sentimentLabel?: string;
  confidenceScore?: number;
  reliabilityScore?: number;
  sourceName?: string;
  sourceUrl?: string;
  sourceRef?: string;
  capturedAt?: string;
}

export interface PublicWorkbenchTeam {
  teamId: number;
  teamKey: string;
  teamName: string;
  fifaCode?: string;
  countryRegion?: string;
  styleTags?: string;
  attackProfile?: string;
  defenseProfile?: string;
  publicSentiment?: string;
  facts: PublicPrematchFact[];
}

export interface PublicWorkbenchLineup {
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

export interface PublicWorkbenchPlayer {
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
  facts: PublicPrematchFact[];
}

export interface PublicWorkbenchOddsSelection {
  selectionId: number;
  selectionCode: string;
  selectionName: string;
  oddsValue?: number;
  impliedProbability?: number;
  selectionStatus?: string;
}

export interface PublicWorkbenchOddsMarket {
  marketId: number;
  bookmaker: string;
  marketCode: string;
  marketName?: string;
  snapshotType?: string;
  handicapLine?: number;
  lineValue?: string;
  capturedAt?: string;
  sourceRef?: string;
  selections: PublicWorkbenchOddsSelection[];
}

export interface PublicWorkbenchSentimentRisk {
  riskId: number;
  riskType: string;
  riskLevel?: string;
  riskScore?: number;
  title: string;
  rationale?: string;
  suggestedAction?: string;
  sourceName?: string;
  sourceRef?: string;
}

export interface PublicWorkbenchSentimentFactor {
  factorId: number;
  matchId?: number;
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
  risks: PublicWorkbenchSentimentRisk[];
}

export interface PublicWorkbenchEvidence {
  evidenceId: number;
  sourceType: string;
  sourceName: string;
  sourceRef?: string;
  sourceUrl?: string;
  evidenceTime?: string;
  summary?: string;
  reliabilityScore?: number;
}

export interface PublicWorkbenchConflict {
  conflictId: number;
  conflictType: string;
  entityKey?: string;
  fieldName?: string;
  resolutionStatus: string;
}

export interface PublicWorkbenchAnalysisReport {
  reportId: number;
  analysisId: string;
  conclusionType?: string;
  confidence?: string;
  riskSummary?: string;
  recommendedMarkets?: string;
  dimensions?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface PublicIntegrityCheck {
  code: string;
  label: string;
  status: 'PASS' | 'MISSING' | 'STALE' | 'CONFLICT' | string;
  severity: string;
  message: string;
  evidenceCount: number;
  lastUpdatedAt?: string;
}

export interface PublicPrematchWorkbenchDetail {
  summary: PublicWorkbenchMatchSummary;
  teams: PublicWorkbenchTeam[];
  lineups: PublicWorkbenchLineup[];
  players: PublicWorkbenchPlayer[];
  oddsMarkets: PublicWorkbenchOddsMarket[];
  sentimentFactors: PublicWorkbenchSentimentFactor[];
  evidence: PublicWorkbenchEvidence[];
  conflicts: PublicWorkbenchConflict[];
  analysisReports: PublicWorkbenchAnalysisReport[];
  integrityChecks: PublicIntegrityCheck[];
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
    headers: createAuthHeaders(authHeader),
  });
  return response.data;
}

export async function getPrematchWorkbenchMatch(authHeader: string, matchId: number): Promise<ApiResponse<PrematchWorkbenchDetail>> {
  const response = await http.get<ApiResponse<PrematchWorkbenchDetail>>(buildPrematchWorkbenchMatchPath(matchId), {
    headers: createAuthHeaders(authHeader),
  });
  return response.data;
}

export async function getPrematchWorkbenchIntegrity(authHeader: string, matchId: number): Promise<ApiResponse<IntegrityCheck[]>> {
  const response = await http.get<ApiResponse<IntegrityCheck[]>>(buildPrematchWorkbenchIntegrityPath(matchId), {
    headers: createAuthHeaders(authHeader),
  });
  return response.data;
}

export async function listPublicPrematchWorkbenchMatches(): Promise<ApiResponse<PublicWorkbenchMatchSummary[]>> {
  const response = await publicHttp.get<ApiResponse<PublicWorkbenchMatchSummary[]>>(
    buildPrematchWorkbenchMatchesPath(),
  );
  return response.data;
}

export async function getPublicPrematchWorkbenchMatch(
  matchId: number,
): Promise<ApiResponse<PublicPrematchWorkbenchDetail>> {
  const response = await publicHttp.get<ApiResponse<PublicPrematchWorkbenchDetail>>(
    buildPrematchWorkbenchMatchPath(matchId),
  );
  return response.data;
}

export async function getPublicPrematchWorkbenchIntegrity(
  matchId: number,
): Promise<ApiResponse<PublicIntegrityCheck[]>> {
  const response = await publicHttp.get<ApiResponse<PublicIntegrityCheck[]>>(
    buildPrematchWorkbenchIntegrityPath(matchId),
  );
  return response.data;
}
