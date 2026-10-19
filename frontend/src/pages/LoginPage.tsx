import { useState } from 'react';
import { Alert, Button, Card, Form, Input, Typography } from 'antd';
import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { useAuth } from '../auth';
import { errorMessage } from '../api';

export default function LoginPage() {
  const { login } = useAuth();
  const [error, setError] = useState<string>();
  const [submitting, setSubmitting] = useState(false);

  const onFinish = async (v: { username: string; password: string }) => {
    setSubmitting(true);
    setError(undefined);
    try {
      await login(v.username, v.password);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center', background: '#f0f2f5', padding: 16 }}>
      <Card style={{ width: '100%', maxWidth: 380 }}>
        <Typography.Title level={4} style={{ textAlign: 'center' }}>vOCS License Manager</Typography.Title>
        {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 16 }} />}
        <Form layout="vertical" onFinish={onFinish}>
          <Form.Item name="username" rules={[{ required: true, message: 'Nhập tên đăng nhập' }]}>
            <Input prefix={<UserOutlined />} placeholder="Tên đăng nhập" autoFocus />
          </Form.Item>
          <Form.Item name="password" rules={[{ required: true, message: 'Nhập mật khẩu' }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="Mật khẩu" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={submitting}>Đăng nhập</Button>
        </Form>
      </Card>
    </div>
  );
}
