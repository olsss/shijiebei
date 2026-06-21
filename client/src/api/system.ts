import { http } from './http';

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string;
  timestamp: string;
}

export interface HealthStatus {
  status: 'UP';
}

export interface SystemSettings {
  archivePath: string;
  analysisSystemProtected: boolean;
  boundaryDescription: string;
}

export interface LoginResponse {
  username: string;
  displayName: string;
  authType: string;
}

export async function fetchHealth(): Promise<ApiResponse<HealthStatus>> {
  const response = await http.get<ApiResponse<HealthStatus>>('/health');
  return response.data;
}

export async function fetchSystemSettings(authHeader: string): Promise<ApiResponse<SystemSettings>> {
  const response = await http.get<ApiResponse<SystemSettings>>('/system/settings', {
    headers: { Authorization: authHeader },
  });
  return response.data;
}

export async function login(username: string, password: string): Promise<ApiResponse<LoginResponse>> {
  const response = await http.post<ApiResponse<LoginResponse>>('/auth/login', { username, password });
  return response.data;
}
