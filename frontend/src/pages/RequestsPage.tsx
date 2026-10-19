import { useEffect, useState } from 'react';
import { App, Card, Select, Space, Table } from 'antd';
import { Link } from 'react-router-dom';
import { api, errorMessage, LicenseRequest, Market, Page, RequestStatus } from '../api';
import { fmtNum, fmtTime } from '../format';
import StatusTag, { STATUS_OPTIONS } from '../components/StatusTag';

export default function RequestsPage() {
  const { message } = App.useApp();
  const [data, setData] = useState<Page<LicenseRequest>>();
  const [markets, setMarkets] = useState<Market[]>([]);
  const [status, setStatus] = useState<RequestStatus>();
  const [marketId, setMarketId] = useState<number>();
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    api.get<Market[]>('/markets').then((r) => setMarkets(r.data)).catch(() => undefined);
  }, []);

  useEffect(() => {
    setLoading(true);
    api.get<Page<LicenseRequest>>('/requests', { params: { status, marketId, page, size: 20 } })
      .then((r) => setData(r.data)).catch((e) => message.error(errorMessage(e)))
      .finally(() => setLoading(false));
  }, [status, marketId, page, message]);

  return (
    <Card title="Yêu cầu cấp license" extra={
      <Space wrap>
        <Select allowClear placeholder="Trạng thái" style={{ width: 170 }} options={STATUS_OPTIONS}
                value={status} onChange={(v) => { setStatus(v); setPage(0); }} />
        <Select allowClear placeholder="Thị trường" style={{ width: 200 }} showSearch optionFilterProp="label"
                options={markets.map((m) => ({ value: m.id, label: `${m.name} (${m.code})` }))}
                value={marketId} onChange={(v) => { setMarketId(v); setPage(0); }} />
      </Space>}>
      <Table<LicenseRequest>
        rowKey="id" loading={loading} dataSource={data?.content} scroll={{ x: true }}
        pagination={{ current: page + 1, pageSize: 20, total: data?.total ?? 0, showSizeChanger: false,
          onChange: (p) => setPage(p - 1) }}
        columns={[
          { title: 'Mã yêu cầu', render: (_, r) => <Link to={`/requests/${r.id}`}>{r.requestCode}</Link> },
          { title: 'Thị trường', render: (_, r) => `${r.marketName} (${r.marketCode})` },
          { title: 'Max Sub', align: 'right', render: (_, r) => fmtNum(r.maxSub) },
          { title: 'Max Active Sub', align: 'right', render: (_, r) => fmtNum(r.maxActiveSub) },
          { title: 'Trạng thái', render: (_, r) => <StatusTag status={r.status} /> },
          { title: 'Người tạo', dataIndex: 'requestedBy' },
          { title: 'Tạo lúc', render: (_, r) => fmtTime(r.createdAt) },
          { title: 'Đóng lúc', render: (_, r) => fmtTime(r.closedAt) },
        ]}
      />
    </Card>
  );
}
