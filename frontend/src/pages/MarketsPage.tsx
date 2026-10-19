import { useCallback, useEffect, useState } from 'react';
import { App, Button, Card, Form, Input, Modal, Space, Switch, Table, Tag, Typography } from 'antd';
import { EditOutlined, PlusOutlined } from '@ant-design/icons';
import { Link, useNavigate } from 'react-router-dom';
import { api, errorMessage, Market } from '../api';
import { useAuth } from '../auth';
import { fmtNum, fmtTime } from '../format';

export default function MarketsPage() {
  const { hasRole } = useAuth();
  const { message } = App.useApp();
  const navigate = useNavigate();
  const [data, setData] = useState<Market[]>([]);
  const [loading, setLoading] = useState(false);
  const [editing, setEditing] = useState<Market | 'new' | null>(null);
  const [form] = Form.useForm();

  const load = useCallback(() => {
    setLoading(true);
    api.get<Market[]>('/markets').then((r) => setData(r.data))
      .catch((e) => message.error(errorMessage(e))).finally(() => setLoading(false));
  }, [message]);

  useEffect(load, [load]);

  const openEdit = (m: Market | 'new') => {
    setEditing(m);
    form.resetFields();
    form.setFieldsValue(m === 'new' ? { licenseRequired: true, active: true } : m);
  };

  const save = async () => {
    const v = await form.validateFields();
    try {
      if (editing === 'new') {
        await api.post('/markets', v);
      } else if (editing) {
        await api.put(`/markets/${editing.id}`, v);
      }
      message.success('Đã lưu');
      setEditing(null);
      load();
    } catch (e) {
      message.error(errorMessage(e));
    }
  };

  return (
    <Card title="Thị trường"
          extra={hasRole('ADMIN') && <Button icon={<PlusOutlined />} onClick={() => openEdit('new')}>Thêm thị trường</Button>}>
      <Table<Market>
        rowKey="id" loading={loading} dataSource={data} pagination={false} scroll={{ x: true }}
        columns={[
          { title: 'Mã', dataIndex: 'code', render: (c, m) => <Link to={`/markets/${m.id}`}>{c}</Link> },
          { title: 'Tên', dataIndex: 'name' },
          { title: 'Quản lý license', render: (_, m) => m.licenseRequired
              ? <Tag color="blue">Có</Tag> : <Tag>Không</Tag> },
          { title: 'License hiện hành', render: (_, m) => m.currentLicense
              ? <>v{m.currentLicense.version} · {fmtTime(m.currentLicense.issuedAt)}</> : '—' },
          { title: 'Max Sub', align: 'right', render: (_, m) => fmtNum(m.currentLicense?.maxSub) },
          { title: 'Max Active Sub', align: 'right', render: (_, m) => fmtNum(m.currentLicense?.maxActiveSub) },
          { title: 'Yêu cầu chờ OTP', render: (_, m) => m.pendingRequest
              ? <Link to={`/requests/${m.pendingRequest.id}`}><Tag color="orange">{m.pendingRequest.requestCode}</Tag></Link>
              : '—' },
          { title: 'Trạng thái', render: (_, m) => m.active ? <Tag color="green">Hoạt động</Tag> : <Tag>Ngừng</Tag> },
          { title: '', render: (_, m) => (
              <Space>
                {hasRole('ADMIN', 'OPERATOR') && m.licenseRequired && m.active && !m.pendingRequest && (
                  <Button size="small" type="primary" onClick={() => navigate(`/requests/new?marketId=${m.id}`)}>
                    Tạo license</Button>)}
                {hasRole('ADMIN') && <Button size="small" icon={<EditOutlined />} onClick={() => openEdit(m)} />}
              </Space>) },
        ]}
      />
      <Modal open={editing !== null} title={editing === 'new' ? 'Thêm thị trường' : 'Sửa thị trường'}
             onOk={save} onCancel={() => setEditing(null)} okText="Lưu" destroyOnClose>
        <Form form={form} layout="vertical">
          <Form.Item name="code" label="Mã thị trường"
                     rules={[{ required: true }, { pattern: /^[A-Z0-9_-]+$/, message: 'Chữ in hoa, số, _ -' }]}>
            <Input disabled={editing !== 'new'} maxLength={20} />
          </Form.Item>
          <Form.Item name="name" label="Tên" rules={[{ required: true }]}><Input maxLength={200} /></Form.Item>
          <Form.Item name="country" label="Quốc gia"><Input maxLength={100} /></Form.Item>
          <Form.Item name="licenseRequired" label="Quản lý license" valuePropName="checked"
                     extra="Tắt cho thị trường Việt Nam"><Switch /></Form.Item>
          {editing !== 'new' && (
            <Form.Item name="active" label="Hoạt động" valuePropName="checked"><Switch /></Form.Item>)}
          <Form.Item name="note" label="Ghi chú"><Input.TextArea rows={2} maxLength={1000} /></Form.Item>
        </Form>
        <Typography.Text type="secondary">Mã thị trường không thể sửa sau khi tạo.</Typography.Text>
      </Modal>
    </Card>
  );
}
