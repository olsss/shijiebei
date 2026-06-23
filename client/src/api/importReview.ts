import { createAuthHeaders, http } from './http';
import type { ApiResponse } from './system';

export type ImportItemType =
  | 'TEAM'
  | 'PLAYER'
  | 'MATCH'
  | 'MATCH_LINEUP'
  | 'MATCH_EVENT'
  | 'MATCH_STATS'
  | 'BETS'
  | 'BET'
  | 'BET_PLAN'
  | 'ANALYSIS'
  | 'ODDS'
  | 'SOURCE'
  | 'POST_REVIEW'
  | 'REVIEW_LESSON';
export type ImportItemStatus = 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED';

export interface ImportJobResponse {
  id: number;
  archivePath: string;
  status: string;
  totalItems: number;
  validItems: number;
  invalidItems: number;
  message: string;
}

export interface ImportItemResponse {
  id: number;
  jobId: number;
  itemType: ImportItemType;
  status: ImportItemStatus;
  relativePath: string;
  sha256: string;
  summaryTitle: string;
  validJson: boolean;
  validationMessage: string;
}

export interface ImportItemDetailResponse {
  item: ImportItemResponse;
  rawJson: string;
  rejectionReason?: string;
}

export interface ImportItemFilters {
  status?: ImportItemStatus;
  type?: ImportItemType;
}

export function buildImportItemPath(id: number): string {
  return `/import-items/${id}`;
}

export async function scanArchive(
  authHeader: string,
  archivePath?: string,
): Promise<ApiResponse<ImportJobResponse>> {
  const response = await http.post<ApiResponse<ImportJobResponse>>(
    '/import-jobs/scan',
    { archivePath: archivePath?.trim() || undefined },
    { headers: createAuthHeaders(authHeader) },
  );
  return response.data;
}

export async function listImportItems(
  authHeader: string,
  filters: ImportItemFilters = {},
): Promise<ApiResponse<ImportItemResponse[]>> {
  const response = await http.get<ApiResponse<ImportItemResponse[]>>('/import-items', {
    headers: createAuthHeaders(authHeader),
    params: filters,
  });
  return response.data;
}

export async function getImportItem(
  authHeader: string,
  id: number,
): Promise<ApiResponse<ImportItemDetailResponse>> {
  const response = await http.get<ApiResponse<ImportItemDetailResponse>>(buildImportItemPath(id), {
    headers: createAuthHeaders(authHeader),
  });
  return response.data;
}

export async function approveImportItem(
  authHeader: string,
  id: number,
): Promise<ApiResponse<ImportItemResponse>> {
  const response = await http.post<ApiResponse<ImportItemResponse>>(
    `${buildImportItemPath(id)}/approve`,
    undefined,
    { headers: createAuthHeaders(authHeader) },
  );
  return response.data;
}

export async function batchApproveImportItems(
  authHeader: string,
  itemIds: number[],
): Promise<ApiResponse<ImportItemResponse[]>> {
  const response = await http.post<ApiResponse<ImportItemResponse[]>>(
    '/import-items/batch-approve',
    { itemIds },
    { headers: createAuthHeaders(authHeader) },
  );
  return response.data;
}

export async function rejectImportItem(
  authHeader: string,
  id: number,
  reason: string,
): Promise<ApiResponse<ImportItemResponse>> {
  const response = await http.post<ApiResponse<ImportItemResponse>>(
    `${buildImportItemPath(id)}/reject`,
    { reason },
    { headers: createAuthHeaders(authHeader) },
  );
  return response.data;
}
