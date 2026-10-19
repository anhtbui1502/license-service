import { useEffect, useMemo, useState } from 'react';
import { Alert, App, Button, Card, Descriptions, Form, Input, InputNumber, Modal, Select, Space, Typography } from 'antd';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { api, errorMessage, LicenseRequest, Market } from '../api';
import { useAuth } from '../auth';
import { fmtDiff, fmtNum, numberInputProps } from '../format';

interface FormValues {
  marketId: number;
  maxSub: number;
  maxActiveSub: number;
  reason: string;
}

export default function NewRequestPage() {
  const { hasRole } = useAuth();
  const { message } = App.useApp();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [markets, setMarkets] = useState<Market[]>([]);
  const [form] = Form.useForm<FormValues>();
  const [submitting, setSubmitting] = useState(false);
  const marketId = Form.useWatch('marketId', form);
  const maxSub = Form.useWatch('maxSub', form);
  const maxActiveSub = Form.useWatch('maxActiveSub', form);

  useEffect(() => {
    api.get<Market[]>('/markets').then((r) => {
      setMarkets(r.data.filter((m) => m.licenseRequired && m.active));
      const pre = Number(params.get('marketId'));
      if (pre) form.setFieldValue('marketId', pre);
    }).catch((e) => message.error(errorMessage(e)));
  }, [form, params, message]);

  const market = useMemo(() => markets.find((m) => m.id === marketId), [markets, marketId]);
  const cur = market?.currentLicense;

  if (!hasRole('ADMIN', 'OPERATOR')) {
    return <Alert type="warning" message="Bạn không có quyền tạo yêu cầu cấp license." />;
  }

  const submit = async (v: FormValues) => {
    const decrease = cur && (v.maxSub < cur.maxSub || v.maxActiveSub < cur.maxActiveSub);
    Modal.confirm({
      title: 'Xác nhận gửi OTP tới người duyệt',
      width: 520,
      content: (
        <Space direction="vertical">
          <Typography.Text>Thị trường: <b>{market?.name} ({market?.code})</b></Typography.Text>
          <Typography.Text>Max Sub: <b>{fmtNum(v.maxSub)}</b> {cur && `(${fmtDiff(cur.maxSub, v.maxSub)})`}</Typography.Text>
          <Typography.Text>Max Active Sub: <b>{fmtNum(v.maxActiveSub)}</b> {cur && `(${fmtDiff(cur.maxActiveSub, v.maxActiveSub)})`}</Typography.Text>
          {decrease && <Alert type="error" showIcon message="Số mới NHỎ HƠN license hiện hành. Thị trường có thể bị chặn đấu nối/kích hoạt." />}
          <Typography.Text type="secondary">Sau khi gửi OTP sẽ không sửa được các con số này.</Typography.Text>
        </Space>
      ),
      okText: 'Gửi OTP',
      onOk: async () => {
        setSubmitting(true);
        try {
          const r = await api.post<LicenseRequest>('/requests', v);
          message.success('Đã gửi OTP tới người duyệt');
          navigate(`/requests/${r.data.id}`);
        } catch (e) {
          message.error(errorMessage(e));
        } finally {
          setSubmitting(false);
        }
      },
    });
  };

  return (
    <Card title="Tạo license mới" style={{ maxWidth: 760 }}>
      <Form form={form} layout="vertical" onFinish={submit}>
        <Form.Item name="marketId" label="Thị trường" rules={[{ required: true, message: 'Chọn thị trường' }]}>
          <Select showSearch optionFilterProp="label" placeholder="Chọn thị trường"
                  options={markets.map((m) => ({ value: m.id, label: `${m.name} (${m.code})` }))} />
        </Form.Item>

        {market && (
          <Descriptions size="small" bordered column={1} style={{ marginBottom: 16 }}>
            <Descriptions.Item label="License hiện hành">{cur ? `v${cur.version}` : 'Chưa có'}</Descriptions.Item>
            <Descriptions.Item label="Max Sub hiện tại">{fmtNum(cur?.maxSub)}</Descriptions.Item>
            <Descriptions.Item label="Max Active Sub hiện tại">{fmtNum(cur?.maxActiveSub)}</Descriptions.Item>
          </Descriptions>
        )}
        {market?.pendingRequest && (
          <Alert type="warning" showIcon style={{ marginBottom: 16 }}
                 message={<>Thị trường đang có yêu cầu <Link to={`/requests/${market.pendingRequest.id}`}>
                   {market.pendingRequest.requestCode}</Link> chờ OTP. Hãy hoàn tất hoặc hủy yêu cầu đó trước.</>} />
        )}

        <Space size="large" wrap>
          <Form.Item name="maxSub" label="Max Sub mới" rules={[{ required: true, message: 'Nhập Max Sub' }]}
                     extra={cur && maxSub ? `Chênh lệch: ${fmtDiff(cur.maxSub, maxSub)}` : undefined}>
            <InputNumber min={1} max={10_000_000_000} style={{ width: 220 }} {...numberInputProps} />
          </Form.Item>
          <Form.Item name="maxActiveSub" label="Max Active Sub mới" dependencies={['maxSub']}
                     extra={cur && maxActiveSub ? `Chênh lệch: ${fmtDiff(cur.maxActiveSub, maxActiveSub)}` : undefined}
                     rules={[{ required: true, message: 'Nhập Max Active Sub' },
                       ({ getFieldValue }) => ({
                         validator: (_, v) => !v || !getFieldValue('maxSub') || v <= getFieldValue('maxSub')
                           ? Promise.resolve() : Promise.reject(new Error('Không được lớn hơn Max Sub')),
                       })]}>
            <InputNumber min={1} max={10_000_000_000} style={{ width: 220 }} {...numberInputProps} />
          </Form.Item>
        </Space>

        <Form.Item name="reason" label="Lý do / số hợp đồng" rules={[{ required: true, whitespace: true, message: 'Nhập lý do' }]}>
          <Input.TextArea rows={3} maxLength={2000} showCount />
        </Form.Item>

        <Button type="primary" htmlType="submit" loading={submitting} disabled={!!market?.pendingRequest}>
          Gửi OTP tới người duyệt
        </Button>
      </Form>
    </Card>
  );
}
