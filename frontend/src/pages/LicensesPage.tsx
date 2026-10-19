import { useEffect, useState } from 'react';
import { App, Card, Select } from 'antd';
import { api, errorMessage, License, Market, Page } from '../api';
import LicenseTable from '../components/LicenseTable';

export default function LicensesPage() {
  const { message } = App.useApp();
  const [data, setData] = useState<Page<License>>();
  const [markets, setMarkets] = useState<Market[]>([]);
  const [marketId, setMarketId] = useState<number>();
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    api.get<Market[]>('/markets').then((r) => setMarkets(r.data)).catch(() => undefined);
  }, []);

  useEffect(() => {
    setLoading(true);
    api.get<Page<License>>('/licenses', { params: { marketId, page, size: 20 } })
      .then((r) => setData(r.data)).catch((e) => message.error(errorMessage(e)))
      .finally(() => setLoading(false));
  }, [marketId, page, message]);

  return (
    <Card title="Lịch sử file license" extra={
      <Select allowClear placeholder="Lọc theo thị trường" style={{ width: 220 }} showSearch optionFilterProp="label"
              options={markets.map((m) => ({ value: m.id, label: `${m.name} (${m.code})` }))}
              value={marketId} onChange={(v) => { setMarketId(v); setPage(0); }} />}>
      <LicenseTable data={data} loading={loading} onPage={setPage} />
    </Card>
  );
}
