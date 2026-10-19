import { useCallback, useEffect, useState } from 'react';
import { App, Button, Card, Form, Input, Modal, Select, Switch, Table, Tag } from 'antd';
import { EditOutlined, PlusOutlined } from '@ant-design/icons';
import { api, errorMessage, User } from '../api';
import { fmtTime } from '../format';

const ROLE_OPTIONS = [
  { value: 'ADMIN', label: 'ADMIN – toàn quyền' },
  { value: 'OPERATOR', label: 'OPERATOR – tạo yêu cầu, sinh license' },
  { value: 'VIEWER', label: 'VIEWER – chỉ xem' },
];

export default function UsersPage() {
  const { message } = App.useApp();
  const [data, setData] = useState<User[]>([]);
  const [editing, setEditing] = useState<User | 'new' | null>(null);
  const [form] = Form.useForm();

  const load = useCallback(() => {
    api.get<User[]>('/users').then((r) => setData(r.data)).catch((e) => message.error(errorMessage(e)));
  }, [message]);
  useEffect(load, [load]);

  const open = (u: User | 'new') => {
    setEditing(u);
    form.resetFields();
    form.setFieldsValue(u === 'new' ? { role: 'OPERATOR', enabled: true } : u);
  };

  const save = async () => {
    const v = await form.validateFields();
    if (!v.password) delete v.password;
    try {
      if (editing === 'new') await api.post('/users', v);
      else if (editing) await api.put(`/users/${editing.id}`, v);
      message.success('Đã lưu');
      setEditing(null);
      load();
    } catch (e) {
      message.error(errorMessage(e));
    }
  };

  return (
    <Card title="Người dùng" extra={<Button icon={<PlusOutlined />} onClick={() => open('new')}>Thêm người dùng</Button>}>
      <Table<User> rowKey="id" dataSource={data} pagination={false} scroll={{ x: true }} columns={[
        { title: 'Tên đăng nhập', dataIndex: 'username' },
        { title: 'Họ tên', dataIndex: 'fullName' },
        { title: 'Email', dataIndex: 'email' },
        { title: 'Vai trò', render: (_, u) => <Tag color="blue">{u.role}</Tag> },
        { title: 'Trạng thái', render: (_, u) => u.enabled ? <Tag color="green">Hoạt động</Tag> : <Tag color="red">Khóa</Tag> },
        { title: 'Tạo lúc', render: (_, u) => fmtTime(u.createdAt) },
        { title: '', render: (_, u) => <Button size="small" icon={<EditOutlined />} onClick={() => open(u)} /> },
      ]} />
      <Modal open={editing !== null} title={editing === 'new' ? 'Thêm người dùng' : 'Sửa người dùng'}
             onOk={save} onCancel={() => setEditing(null)} okText="Lưu" destroyOnClose>
        <Form form={form} layout="vertical">
          <Form.Item name="username" label="Tên đăng nhập" rules={[{ required: true }]}>
            <Input disabled={editing !== 'new'} maxLength={100} />
          </Form.Item>
          <Form.Item name="fullName" label="Họ tên"><Input maxLength={200} /></Form.Item>
          <Form.Item name="email" label="Email" rules={[{ required: true, type: 'email' }]}><Input maxLength={200} /></Form.Item>
          <Form.Item name="role" label="Vai trò" rules={[{ required: true }]}><Select options={ROLE_OPTIONS} /></Form.Item>
          {editing !== 'new' && <Form.Item name="enabled" label="Hoạt động" valuePropName="checked"><Switch /></Form.Item>}
          <Form.Item name="password" label={editing === 'new' ? 'Mật khẩu' : 'Đặt lại mật khẩu (để trống nếu không đổi)'}
                     rules={[{ required: editing === 'new' }, { min: 8, message: 'Tối thiểu 8 ký tự' }]}>
            <Input.Password autoComplete="new-password" />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
}
