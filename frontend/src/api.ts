import axios, { AxiosError } from 'axios';

export const api = axios.create({ baseURL: '/api' });
// axios tự đọc cookie XSRF-TOKEN và gửi header X-XSRF-TOKEN (cùng origin)

export type Role = 'ADMIN' | 'OPERATOR' | 'VIEWER';
export type RequestStatus = 'PENDING_OTP' | 'GENERATED' | 'EXPIRED' | 'CANCELLED' | 'FAILED';

export interface Page<T> {
  content: T[];
  total: number;
  page: number;
  size: number;
}

export interface User {
  id: number;
  username: string;
  fullName?: string;
  email: string;
  role: Role;
  enabled: boolean;
  createdAt: string;
}

export interface Market {
  id: number;
  code: string;
  name: string;
  country?: string;
  licenseRequired: boolean;
  active: boolean;
  note?: string;
  createdAt: string;
  currentLicense?: {
    id: number;
    licenseCode: string;
    version: number;
    maxSub: number;
    maxActiveSub: number;
    issuedAt: string;
  };
  pendingRequest?: {
    id: number;
    requestCode: string;
    maxSub: number;
    maxActiveSub: number;
    requestedBy: string;
    otpExpiresAt: string;
  };
}

export interface LicenseRequest {
  id: number;
  requestCode: string;
  marketId: number;
  marketCode: string;
  marketName: string;
  prevLicense?: { id: number; version: number; maxSub: number; maxActiveSub: number };
  maxSub: number;
  maxActiveSub: number;
  reason: string;
  status: RequestStatus;
  requestedBy: string;
  requestedByUsername: string;
  approverEmail: string;
  otpExpiresAt?: string;
  otpLastSentAt?: string;
  nextResendAt?: string;
  resendsLeft: number;
  attemptsLeft: number;
  createdAt: string;
  closedAt?: string;
  closedBy?: string;
  licenseId?: number;
  canAct: boolean;
}

export interface License {
  id: number;
  licenseCode: string;
  marketId: number;
  marketCode: string;
  marketName: string;
  version: number;
  maxSub: number;
  maxActiveSub: number;
  fileName: string;
  fileSha256: string;
  cryptoName: string;
  current: boolean;
  requestId?: number;
  requestCode?: string;
  issuedBy?: string;
  issuedAt: string;
}

export interface AuditLog {
  id: number;
  username?: string;
  action: string;
  entityType?: string;
  entityId?: number;
  detail?: string;
  ipAddress?: string;
  createdAt: string;
}

interface ApiErrorBody {
  code?: string;
  message?: string;
  retryAfterSeconds?: number;
}

export function errorCode(err: unknown): string | undefined {
  return (err as AxiosError<ApiErrorBody>)?.response?.data?.code;
}

export function errorMessage(err: unknown): string {
  const e = err as AxiosError<ApiErrorBody>;
  return e?.response?.data?.message ?? e?.message ?? 'Lỗi không xác định';
}

export function downloadUrl(licenseId: number): string {
  return `/api/licenses/${licenseId}/download`;
}
