import { Button, Layout, Menu, Space, Spin, Tag, Typography } from 'antd';
import {
  AuditOutlined,
  FileProtectOutlined,
  GlobalOutlined,
  LogoutOutlined,
  PlusCircleOutlined,
  SolutionOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import { Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from './auth';
import LoginPage from './pages/LoginPage';
import MarketsPage from './pages/MarketsPage';
import MarketDetailPage from './pages/MarketDetailPage';
import NewRequestPage from './pages/NewRequestPage';
import RequestsPage from './pages/RequestsPage';
import RequestDetailPage from './pages/RequestDetailPage';
import LicensesPage from './pages/LicensesPage';
import UsersPage from './pages/UsersPage';
import AuditPage from './pages/AuditPage';

const { Header, Sider, Content } = Layout;

export default function App() {
  const { user, loading, logout, hasRole } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  if (loading) {
    return <Spin fullscreen />;
  }
  if (!user) {
    return <LoginPage />;
  }

  const items = [
    { key: '/markets', icon: <GlobalOutlined />, label: 'Thị trường' },
    ...(hasRole('ADMIN', 'OPERATOR')
      ? [{ key: '/requests/new', icon: <PlusCircleOutlined />, label: 'Tạo license mới' }]
      : []),
    { key: '/requests', icon: <SolutionOutlined />, label: 'Yêu cầu cấp license' },
    { key: '/licenses', icon: <FileProtectOutlined />, label: 'Lịch sử file license' },
    ...(hasRole('ADMIN')
      ? [
          { key: '/users', icon: <TeamOutlined />, label: 'Người dùng' },
          { key: '/audit', icon: <AuditOutlined />, label: 'Nhật ký hệ thống' },
        ]
      : []),
  ];
  const selected = items
    .map((i) => i.key)
    .filter((k) => location.pathname === k || location.pathname.startsWith(k + '/'))
    .sort((a, b) => b.length - a.length)[0];

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider breakpoint="lg" collapsedWidth={0} width={230}>
        <div style={{ color: '#fff', padding: '18px 20px', fontWeight: 600, fontSize: 16 }}>vOCS License Manager</div>
        <Menu theme="dark" mode="inline" selectedKeys={selected ? [selected] : []} items={items}
              onClick={(e) => navigate(e.key)} />
      </Sider>
      <Layout>
        <Header style={{ background: '#fff', padding: '0 24px', display: 'flex', justifyContent: 'flex-end' }}>
          <Space>
            <Typography.Text>{user.fullName || user.username}</Typography.Text>
            <Tag color="blue">{user.role}</Tag>
            <Button icon={<LogoutOutlined />} onClick={logout}>Đăng xuất</Button>
          </Space>
        </Header>
        <Content style={{ margin: 24 }}>
          <Routes>
            <Route path="/" element={<Navigate to="/markets" replace />} />
            <Route path="/markets" element={<MarketsPage />} />
            <Route path="/markets/:id" element={<MarketDetailPage />} />
            <Route path="/requests/new" element={<NewRequestPage />} />
            <Route path="/requests" element={<RequestsPage />} />
            <Route path="/requests/:id" element={<RequestDetailPage />} />
            <Route path="/licenses" element={<LicensesPage />} />
            {hasRole('ADMIN') && <Route path="/users" element={<UsersPage />} />}
            {hasRole('ADMIN') && <Route path="/audit" element={<AuditPage />} />}
            <Route path="*" element={<Navigate to="/markets" replace />} />
          </Routes>
        </Content>
      </Layout>
    </Layout>
  );
}
