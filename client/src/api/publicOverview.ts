import { publicHttp } from './http';
import type { ApiResponse } from './system';

export interface PublicOverviewResponse {
  generatedAt: string;
  todayMatches: unknown[];
  riskCounters: Record<string, number>;
  integrityCounters: Record<string, number>;
  oddsFreshness: Record<string, number>;
  decisionSummary: Record<string, unknown>;
  adminTodoCounters: Record<string, number>;
}

export async function fetchPublicOverview(): Promise<ApiResponse<PublicOverviewResponse>> {
  const response = await publicHttp.get<ApiResponse<PublicOverviewResponse>>('/overview');
  return response.data;
}
