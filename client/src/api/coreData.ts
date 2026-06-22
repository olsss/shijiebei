import { http } from './http';
import type { ApiResponse } from './system';

export interface CoreDataOverview {
  teams: number;
  players: number;
  matches: number;
  analysisReports: number;
  bets: number;
  oddsSnapshots: number;
  evidence: number;
  mappings: number;
}

export interface CoreDataMapping {
  id: number;
  importItemId: number;
  targetType: string;
  targetId: number;
  mappingStatus: string;
  message: string;
}

export interface CoreDataImportResult {
  importItemId: number;
  status: string;
  message: string;
  mappings: CoreDataMapping[];
}

function authHeaders(authHeader: string) {
  return authHeader ? { Authorization: authHeader } : undefined;
}

export function buildCoreDataImportPath(id: number): string {
  return `/core-data/import-items/${id}/import`;
}

export function buildCoreDataMappingsPath(id: number): string {
  return `/core-data/import-items/${id}/mappings`;
}

export async function fetchCoreDataOverview(authHeader: string): Promise<ApiResponse<CoreDataOverview>> {
  const response = await http.get<ApiResponse<CoreDataOverview>>('/core-data/overview', {
    headers: authHeaders(authHeader),
  });
  return response.data;
}

export async function importCoreDataItem(
  authHeader: string,
  id: number,
): Promise<ApiResponse<CoreDataImportResult>> {
  const response = await http.post<ApiResponse<CoreDataImportResult>>(
    buildCoreDataImportPath(id),
    undefined,
    { headers: authHeaders(authHeader) },
  );
  return response.data;
}

export async function listCoreDataMappings(
  authHeader: string,
  id: number,
): Promise<ApiResponse<CoreDataMapping[]>> {
  const response = await http.get<ApiResponse<CoreDataMapping[]>>(buildCoreDataMappingsPath(id), {
    headers: authHeaders(authHeader),
  });
  return response.data;
}
