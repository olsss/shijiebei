import { createAuthHeaders, http, publicHttp } from './http';
import type { ApiResponse } from './system';

export interface AnalysisReviewOverview {
  reportCount: number;
  betPlanCount: number;
  betCount: number;
  reviewCount: number;
  totalStake: number;
  totalReturn: number;
  netProfit: number;
  roi: number;
  averageClv: number;
}

export interface AnalysisReportSummary {
  id: number;
  matchId?: number;
  matchName?: string;
  matchday?: string;
  jcCode?: string;
  analysisId: string;
  conclusionType?: string;
  confidence?: string;
  riskSummary?: string;
  narrativeMd?: string;
  betPlanCount: number;
  reviewCount: number;
  rawPayload?: string;
}

export interface BetPlanSummary {
  id: number;
  analysisReportId?: number;
  matchId?: number;
  matchName?: string;
  matchday?: string;
  jcCode?: string;
  planKey: string;
  planTitle: string;
  conclusionType?: string;
  confidence?: string;
  budgetAmount?: number;
  riskSummary?: string;
  bettingMethod?: string;
  strategyType?: string;
  status: string;
  generatedBy?: string;
  generatedAt?: string;
  itemCount: number;
  rawPayload?: string;
}

export interface BetPlanItem {
  id: number;
  betPlanId: number;
  matchId?: number;
  marketType?: string;
  selectionText: string;
  stakeSuggestion?: number;
  odds?: number;
  lineValue?: string;
  logicType?: string;
  riskLevel?: string;
  playType?: string;
  passType?: string;
  itemOrder: number;
  rawPayload?: string;
}

export interface BetPlanDetail {
  plan: BetPlanSummary;
  items: BetPlanItem[];
}

export interface ReviewLesson {
  id: number;
  reviewId: number;
  lessonType: string;
  lessonText: string;
  severity: string;
  rawPayload?: string;
}

export interface PostMatchReview {
  id: number;
  importItemId: number;
  matchId?: number;
  matchName?: string;
  matchday?: string;
  analysisReportId?: number;
  reviewKey: string;
  reviewTitle: string;
  mathReview?: string;
  footballReview?: string;
  handicapReview?: string;
  tournamentTemperamentReview?: string;
  oddsValueReview?: string;
  overallSummary?: string;
  rawPayload?: string;
  lessons: ReviewLesson[];
}

export interface AnalysisReportDetail {
  report: AnalysisReportSummary;
  betPlans: BetPlanSummary[];
  reviews: PostMatchReview[];
}

export interface BetRecord {
  id: number;
  importItemId: number;
  matchId?: number;
  matchName?: string;
  matchday?: string;
  betId: string;
  ticketNo?: string;
  betDate?: string;
  marketType?: string;
  selectionText?: string;
  stake?: number;
  odds?: number;
  closingOdds?: number;
  clv?: number;
  returnAmount?: number;
  hitStatus: string;
  profitLoss?: number;
  settledAt?: string;
  reviewStatus: string;
  rawPayload?: string;
}

export interface PublicDecisionReport {
  id: number;
  matchId?: number;
  matchName?: string;
  matchday?: string;
  jcCode?: string;
  conclusionType?: string;
  confidence?: string;
  riskSummary?: string;
  reviewSummary?: string;
  lessonSummary?: string;
}

export interface PublicDecisionLesson {
  id: number;
  lessonType: string;
  lessonText: string;
  severity: string;
}

export interface PublicDecisionReview {
  id: number;
  matchId?: number;
  matchName?: string;
  matchday?: string;
  analysisReportId?: number;
  reviewKey: string;
  title: string;
  mathSummary?: string;
  footballSummary?: string;
  handicapSummary?: string;
  tournamentTemperamentSummary?: string;
  oddsValueSummary?: string;
  overallSummary?: string;
  lessons: PublicDecisionLesson[];
}

export function buildAnalysisReviewOverviewPath(): string {
  return '/analysis-review/overview';
}

export function buildAnalysisReportPath(reportId: number): string {
  return `/analysis-review/reports/${reportId}`;
}

export function buildBetPlanPath(planId: number): string {
  return `/analysis-review/bet-plans/${planId}`;
}

export async function fetchAnalysisReviewOverview(authHeader: string): Promise<ApiResponse<AnalysisReviewOverview>> {
  const response = await http.get<ApiResponse<AnalysisReviewOverview>>(buildAnalysisReviewOverviewPath(), {
    headers: createAuthHeaders(authHeader),
  });
  return response.data;
}

export async function listAnalysisReports(authHeader: string): Promise<ApiResponse<AnalysisReportSummary[]>> {
  const response = await http.get<ApiResponse<AnalysisReportSummary[]>>('/analysis-review/reports', {
    headers: createAuthHeaders(authHeader),
  });
  return response.data;
}

export async function getAnalysisReport(authHeader: string, reportId: number): Promise<ApiResponse<AnalysisReportDetail>> {
  const response = await http.get<ApiResponse<AnalysisReportDetail>>(buildAnalysisReportPath(reportId), {
    headers: createAuthHeaders(authHeader),
  });
  return response.data;
}

export async function listBetPlans(authHeader: string): Promise<ApiResponse<BetPlanSummary[]>> {
  const response = await http.get<ApiResponse<BetPlanSummary[]>>('/analysis-review/bet-plans', {
    headers: createAuthHeaders(authHeader),
  });
  return response.data;
}

export async function getBetPlan(authHeader: string, planId: number): Promise<ApiResponse<BetPlanDetail>> {
  const response = await http.get<ApiResponse<BetPlanDetail>>(buildBetPlanPath(planId), {
    headers: createAuthHeaders(authHeader),
  });
  return response.data;
}

export async function listBetRecords(authHeader: string): Promise<ApiResponse<BetRecord[]>> {
  const response = await http.get<ApiResponse<BetRecord[]>>('/analysis-review/bets', {
    headers: createAuthHeaders(authHeader),
  });
  return response.data;
}

export async function listPostMatchReviews(authHeader: string): Promise<ApiResponse<PostMatchReview[]>> {
  const response = await http.get<ApiResponse<PostMatchReview[]>>('/analysis-review/reviews', {
    headers: createAuthHeaders(authHeader),
  });
  return response.data;
}

export async function listPublicDecisionReports(): Promise<ApiResponse<PublicDecisionReport[]>> {
  const response = await publicHttp.get<ApiResponse<PublicDecisionReport[]>>('/decisions/reports');
  return response.data;
}

export async function listPublicDecisionReviews(): Promise<ApiResponse<PublicDecisionReview[]>> {
  const response = await publicHttp.get<ApiResponse<PublicDecisionReview[]>>('/decisions/reviews');
  return response.data;
}
