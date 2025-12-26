import React from 'react';
import { Routes, Route, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { Layout, Menu, Button, Dropdown, Space } from 'antd';
import {
  DashboardOutlined,
  FileSearchOutlined,
  UnorderedListOutlined,
  UserOutlined,
  LogoutOutlined,
  SettingOutlined,
  DeploymentUnitOutlined
} from '@ant-design/icons';
import { useState } from 'react';

import { AuthProvider, useAuth } from './contexts/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Login from './components/Login';
import Dashboard from './components/Dashboard';
import ClaimList from './components/ClaimList';
import ClaimDetail from './components/ClaimDetail';
import ClaimForm from './components/ClaimForm';
import TaskList from './components/TaskList';
import UserManagement from './components/UserManagement';
import AdminDashboard from './components/admin/AdminDashboard';
import CaseInstanceList from './components/admin/CaseInstanceList';
import CaseInstanceDetail from './components/admin/CaseInstanceDetail';
import ProcessInstanceList from './components/admin/ProcessInstanceList';
import ProcessInstanceDetail from './components/admin/ProcessInstanceDetail';
import ModelList from './components/admin/ModelList';
import ModelDeployment from './components/admin/ModelDeployment';

const { Header, Sider, Content } = Layout;

const AppContent: React.FC = () => {
  const [collapsed, setCollapsed] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  const menuItems = [
    {
      key: '/',
      icon: <DashboardOutlined />,
      label: '工作台',
    },
    {
      key: '/claims',
      icon: <FileSearchOutlined />,
      label: '理赔管理',
    },
    {
      key: '/tasks',
      icon: <UnorderedListOutlined />,
      label: '任务中心',
    },
    {
      key: '/users',
      icon: <UserOutlined />,
      label: '用户管理',
    },
    {
      key: '/admin',
      icon: <SettingOutlined />,
      label: 'Flowable Admin',
      children: [
        {
          key: '/admin/dashboard',
          label: 'Dashboard',
        },
        {
          key: '/admin/models',
          label: '模型管理',
        },
        {
          key: '/admin/models/deploy',
          label: '模型部署',
        },
        {
          key: '/admin/cases',
          label: 'Case 实例',
        },
        {
          key: '/admin/processes',
          label: 'Process 实例',
        },
      ],
    },
  ];

  const userMenuItems = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '个人资料',
      disabled: true,
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: logout,
    },
  ];

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider
        collapsible
        collapsed={collapsed}
        onCollapse={setCollapsed}
        theme="dark"
      >
        <div className="logo">
          {collapsed ? '理赔' : '保险理赔系统'}
        </div>
        <Menu
          theme="dark"
          selectedKeys={[location.pathname]}
          mode="inline"
          items={menuItems}
          onClick={({ key }) => {
            navigate(key);
          }}
        />
      </Sider>
      <Layout>
        <Header style={{
          background: '#fff',
          padding: '0 24px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between'
        }}>
          <div style={{ fontSize: '18px', fontWeight: 'bold' }}>
            保险理赔管理系统
          </div>
          <div>
            <Space>
              <span>欢迎，{user?.fullName || user?.username || '用户'}</span>
              <Dropdown
                menu={{ items: userMenuItems }}
                placement="bottomRight"
                arrow
              >
                <Button type="text" icon={<UserOutlined />}>
                  {user?.username}
                </Button>
              </Dropdown>
            </Space>
          </div>
        </Header>
        <Content style={{ margin: '24px 16px 0', overflow: 'auto' }}>
          <div className="site-layout-background" style={{ padding: 24, minHeight: 360 }}>
            <Routes>
              <Route path="/" element={<Dashboard />} />
              <Route path="/claims" element={<ClaimList />} />
              <Route path="/claims/new" element={<ClaimForm />} />
              <Route path="/claims/:id" element={<ClaimDetail />} />
              <Route path="/claims/:id/edit" element={<ClaimForm />} />
              <Route path="/tasks" element={<TaskList />} />
              <Route path="/users" element={<UserManagement />} />
              <Route path="/admin/dashboard" element={<AdminDashboard />} />
              <Route path="/admin/models" element={<ModelList />} />
              <Route path="/admin/models/deploy" element={<ModelDeployment />} />
              <Route path="/admin/cases" element={<CaseInstanceList />} />
              <Route path="/admin/cases/:caseInstanceId" element={<CaseInstanceDetail />} />
              <Route path="/admin/processes" element={<ProcessInstanceList />} />
              <Route path="/admin/processes/:processInstanceId" element={<ProcessInstanceDetail />} />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </div>
        </Content>
      </Layout>
    </Layout>
  );
};

const App: React.FC = () => {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route
          path="/*"
          element={
            <ProtectedRoute>
              <AppContent />
            </ProtectedRoute>
          }
        />
      </Routes>
    </AuthProvider>
  );
};

export default App;
