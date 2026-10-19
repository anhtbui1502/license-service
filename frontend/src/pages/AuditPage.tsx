import { useEffect, useState } from 'react';
import { App, Card, Input, Select, Space, Table } from 'antd';
import { api, AuditLog, errorMessage, Page } from '../api';
import { fmtTime } from '../format';

const ACTIONS = ['LOGIN', 'LOGIN_FAILED', 'REQUEST_CREATED', 'OTP_SENT', 'OTP_INVALID', 'OTP_LOCKED',
  'REQUEST_CANCELLED', 'LICENSE_GENERATED', 'LICENSE_DOWNLOADED', 'MARKET_CREATED', 'MARKET_UPDATED',
  'USER_CREATED', 'USER_UPDATED', 'PASSWORD_CHANGED'];

export default function AuditPage() {
  const { message } = App.useApp();
  const [data, setData] = useState<Page<AuditLog>>();
  const [action, setAction] = useState<string>();
  const [username, setUsername] = useState<string>();
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setLoading(true);
    api.get<Page<AuditLog>>('/audit-logs', { params: { action, username, page, size: 50 } })
      .then((r) => setData(r.data)).catch((e) => message.error(errorMessage(e)))
      .finally(() => setLoading(false));
  }, [action, username, page, message]);

  return (
    <Card title="Nhật ký hệ thống" extra={
      <Space wrap>
        <Select allowClear placeholder="Hành động" style={{ width: 210 }} value={action}
                options={ACTIONS.map((a) => ({ value: a, label: a }))} onChange={(v) => { setAction(v); setPage(0); }} />
        <Input.Search allowClear placeholder="Tên đăng nhập" style={{ width: 180 }}
                      onSearch={(v) => { setUsername(v || undefined); setPage(0); }} />
      </Space>}>
      <Table<AuditLog> rowKey="id" size="small" loading={loading} dataSource={data?.content} scroll={{ x: true }}
        pagination={{ current: page + 1, pageSize: 50, total: data?.total ?? 0, showSizeChanger: false,
          onChange: (p) => setPage(p - 1) }}
        columns={[
          { title: 'Thời điểm', render: (_, a) => fmtTime(a.createdAt) },
          { title: 'Người dùng', dataIndex: 'username' },
          { title: 'Hành động', dataIndex: 'action' },
          { title: 'Đối tượng', render: (_, a) => a.entityType ? `${a.entityType} #${a.entityId}` : '' },
          { title: 'Chi tiết', dataIndex: 'detail' },
          { title: 'IP', dataIndex: 'ipAddress' },
        ]} />
    </Card>
  );
}
