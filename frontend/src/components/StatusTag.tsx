import { Tag } from 'antd';
import { RequestStatus } from '../api';

const MAP: Record<RequestStatus, { color: string; label: string }> = {
  PENDING_OTP: { color: 'orange', label: 'Chờ OTP' },
  GENERATED: { color: 'green', label: 'Đã sinh file' },
  EXPIRED: { color: 'default', label: 'Hết hạn' },
  CANCELLED: { color: 'default', label: 'Đã hủy' },
  FAILED: { color: 'red', label: 'Bị khóa (sai OTP)' },
};

export const STATUS_OPTIONS = (Object.keys(MAP) as RequestStatus[]).map((s) => ({ value: s, label: MAP[s].label }));

export default function StatusTag({ status }: { status: RequestStatus }) {
  const m = MAP[status];
  return <Tag color={m.color}>{m.label}</Tag>;
}
