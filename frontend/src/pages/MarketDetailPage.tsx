import { useEffect, useState } from 'react';
import { Alert, App, Button, Card, Descriptions, Space, Tag } from 'antd';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api, errorMessage, License, Market, Page } from '../api';
import { useAuth } from '../auth';
import { fmtNum, fmtTime } from '../format';
import LicenseTable from '../components/LicenseTable';

export default function MarketDetailPage() {
  const { id } = useParams();
  const { hasRole } = useAuth();
  const { message } = App.useApp();
  const navigate = useNavigate();
  const [market, setMarket] = useState<Market>();
  const [licenses, setLicenses] = useState<Page<License>>();
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    api.get<Market>(`/markets/${id}`).then((r) => setMarket(r.data)).catch((e) => message.error(errorMessage(e)));
  }, [id, message]);

  useEffect(() => {
    setLoading(true);
    api.get<Page<License>>('/licenses', { params: { marketId: id, page, size: 20 } })
      .then((r) => setLicenses(r.data)).catch((e) => message.error(errorMessage(e)))
      .finally(() => setLoading(false));
  }, [id, page, message]);

  if (!market) return null;
  const cur = market.currentLicense;

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Card title={`${market.name} (${market.code})`}
            extra={hasRole('ADMIN', 'OPERATOR') && market.licenseRequired && market.active && !market.pendingRequest && (
              <Button type="primary" onClick={() => navigate(`/requests/new?marketId=${market.id}`)}>Tạo license mới</Button>)}>
        {!market.licenseRequired && <Alert type="info" showIcon message="Thị trường này không quản lý license." />}
        {market.pendingRequest && (
          <Alert type="warning" showIcon style={{ marginBottom: 16 }}
                 message={<>Đang có yêu cầu <Link to={`/requests/${market.pendingRequest.id}`}>
                   {market.pendingRequest.requestCode}</Link> của {market.pendingRequest.requestedBy} chờ OTP
                   (hết hạn {fmtTime(market.pendingRequest.otpExpiresAt)}).</>} />)}
        <Descriptions column={{ xs: 1, md: 3 }} bordered size="small">
          <Descriptions.Item label="Quốc gia">{market.country || '—'}</Descriptions.Item>
          <Descriptions.Item label="Trạng thái">{market.active ? <Tag color="green">Hoạt động</Tag> : <Tag>Ngừng</Tag>}</Descriptions.Item>
          <Descriptions.Item label="Quản lý license">{market.licenseRequired ? 'Có' : 'Không'}</Descriptions.Item>
          <Descriptions.Item label="License hiện hành">{cur ? `v${cur.version} (${cur.licenseCode})` : '—'}</Descriptions.Item>
          <Descriptions.Item label="Max Sub">{fmtNum(cur?.maxSub)}</Descriptions.Item>
          <Descriptions.Item label="Max Active Sub">{fmtNum(cur?.maxActiveSub)}</Descriptions.Item>
          {market.note && <Descriptions.Item label="Ghi chú" span={3}>{market.note}</Descriptions.Item>}
        </Descriptions>
      </Card>
      <Card title="Lịch sử file license">
        <LicenseTable data={licenses} loading={loading} showMarket={false} onPage={setPage} />
      </Card>
    </Space>
  );
}
