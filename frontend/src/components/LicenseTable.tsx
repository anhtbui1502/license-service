import { Button, Table, Tag, Tooltip, Typography } from 'antd';
import { DownloadOutlined } from '@ant-design/icons';
import { Link } from 'react-router-dom';
import { downloadUrl, License, Page } from '../api';
import { useAuth } from '../auth';
import { fmtNum, fmtTime } from '../format';

interface Props {
  data?: Page<License>;
  loading: boolean;
  showMarket?: boolean;
  onPage: (page: number) => void;
}

export default function LicenseTable({ data, loading, showMarket = true, onPage }: Props) {
  const { hasRole } = useAuth();
  return (
    <Table<License>
      rowKey="id"
      size="middle"
      loading={loading}
      dataSource={data?.content}
      pagination={{ current: (data?.page ?? 0) + 1, pageSize: data?.size ?? 20, total: data?.total ?? 0,
        showSizeChanger: false, onChange: (p) => onPage(p - 1) }}
      scroll={{ x: true }}
      columns={[
        ...(showMarket
          ? [{ title: 'Thị trường', render: (_: unknown, l: License) =>
              <Link to={`/markets/${l.marketId}`}>{l.marketName} ({l.marketCode})</Link> }]
          : []),
        { title: 'Phiên bản', render: (_, l) => <>v{l.version} {l.current && <Tag color="green">Hiện hành</Tag>}</> },
        { title: 'Mã license', dataIndex: 'licenseCode' },
        { title: 'Max Sub', align: 'right', render: (_, l) => fmtNum(l.maxSub) },
        { title: 'Max Active Sub', align: 'right', render: (_, l) => fmtNum(l.maxActiveSub) },
        { title: 'Yêu cầu', render: (_, l) => l.requestId ? <Link to={`/requests/${l.requestId}`}>{l.requestCode}</Link> : '—' },
        { title: 'Người sinh', dataIndex: 'issuedBy' },
        { title: 'Thời điểm', render: (_, l) => fmtTime(l.issuedAt) },
        { title: 'SHA-256', render: (_, l) => (
            <Tooltip title={l.fileSha256}>
              <Typography.Text copyable={{ text: l.fileSha256 }} code>{l.fileSha256.slice(0, 12)}…</Typography.Text>
            </Tooltip>) },
        ...(hasRole('ADMIN', 'OPERATOR')
          ? [{ title: '', render: (_: unknown, l: License) => (
              <Button size="small" icon={<DownloadOutlined />} href={downloadUrl(l.id)}>Tải</Button>) }]
          : []),
      ]}
    />
  );
}
