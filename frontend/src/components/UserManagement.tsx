import React, { useCallback, useEffect, useState } from 'react';
import { 
  Card, 
  Table, 
  Button, 
  Space, 
  Input, 
  Select, 
  Modal, 
  Form,
  message,
  Popconfirm,
  Tag
} from 'antd';
import { 
  PlusOutlined, 
  EditOutlined, 
  DeleteOutlined,
  SearchOutlined
} from '@ant-design/icons';
import { userApi } from '../services/api';
import { User } from '../types';

const { Search } = Input;
const { Option } = Select;

const UserManagement: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [users, setUsers] = useState<User[]>([]);
  const [total, setTotal] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [modalVisible, setModalVisible] = useState(false);
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [form] = Form.useForm();

  const loadUsers = useCallback(async () => {
    try {
      setLoading(true);
      const response = await userApi.getUsers({
        page: currentPage - 1,
        size: pageSize
      });
      
      let filteredUsers = response.data.content;
      
      // 应用状态过滤
      if (statusFilter) {
        filteredUsers = filteredUsers.filter(user => user.status === statusFilter);
      }
      
      // 应用搜索过滤
      if (searchKeyword) {
        filteredUsers = filteredUsers.filter(user => 
          user.fullName.toLowerCase().includes(searchKeyword.toLowerCase()) ||
          user.email.toLowerCase().includes(searchKeyword.toLowerCase()) ||
          user.username.toLowerCase().includes(searchKeyword.toLowerCase())
        );
      }

      setUsers(filteredUsers);
      setTotal(response.data.totalElements);
    } catch (error) {
      console.error('Failed to load users:', error);
      message.error('加载用户列表失败');
    } finally {
      setLoading(false);
    }
  }, [currentPage, pageSize, searchKeyword, statusFilter]);

  useEffect(() => {
    loadUsers();
  }, [currentPage, pageSize, searchKeyword, statusFilter, loadUsers]);

  const handleCreate = () => {
    setEditingUser(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (user: User) => {
    setEditingUser(user);
    const userData = {
      ...user,
      role: user.roles[0],
    }
    form.setFieldsValue(userData);
    setModalVisible(true);
  };

  const handleDelete = async (id: string) => {
    try {
      await userApi.deleteUser(id);
      message.success('删除成功');
      loadUsers();
    } catch (error) {
      console.error('Failed to delete user:', error);
      message.error('删除失败');
    }
  };

  const handleSubmit = async (values: any) => {
    try {
      const payload = { ...values, roles: [values.role] };
      delete payload.role;
      if (editingUser) {
        await userApi.updateUser(editingUser.id, payload);
        message.success('更新成功');
      } else {
        await userApi.createUser(payload);
        message.success('创建成功');
      }
      setModalVisible(false);
      loadUsers();
    } catch (error) {
      console.error('Failed to save user:', error);
      message.error('保存失败');
    }
  };

  const handleSearch = (value: string) => {
    setSearchKeyword(value);
    setCurrentPage(1);
  };

  const handleStatusChange = (value: string) => {
    setStatusFilter(value);
    setCurrentPage(1);
  };

  const getStatusColor = (status: string) => {
    return status === 'ACTIVE' ? 'green' : 'red';
  };

  const getStatusText = (status: string) => {
    return status === 'ACTIVE' ? '活跃' : '非活跃';
  };

  const columns = [
    {
      title: '用户名',
      dataIndex: 'username',
      key: 'username'
    },
    {
      title: '姓名',
      dataIndex: 'fullName',
      key: 'fullName'
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      key: 'email'
    },
    {
      title: '部门',
      dataIndex: 'department',
      key: 'department'
    },
    {
      title: '角色',
      dataIndex: 'roles',
      key: 'roles',
      render: (roles: string[]) => (
        <>
          {roles.map(role => (
            <Tag color="blue" key={role}>{role}</Tag>
          ))}
        </>
      )
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={getStatusColor(status)}>
          {getStatusText(status)}
        </Tag>
      )
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (date: string) => new Date(date).toLocaleString()
    },
    {
      title: '操作',
      key: 'actions',
      render: (text: any, record: User) => (
        <Space size="middle">
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确定要删除这个用户吗？"
            onConfirm={() => handleDelete(record.id)}
            okText="确定"
            cancelText="取消"
          >
            <Button type="link" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      )
    }
  ];

  return (
    <Card>
      {/* 工具栏 */}
      <div className="table-toolbar">
        <div className="table-toolbar-left">
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={handleCreate}
          >
            新建用户
          </Button>
        </div>
        <div className="table-toolbar-right">
          <Space>
            <Search
              placeholder="搜索用户名、姓名、邮箱"
              allowClear
              enterButton={<SearchOutlined />}
              style={{ width: 250 }}
              onSearch={handleSearch}
            />
            <Select
              placeholder="状态筛选"
              allowClear
              style={{ width: 120 }}
              onChange={handleStatusChange}
            >
              <Option value="ACTIVE">活跃</Option>
              <Option value="INACTIVE">非活跃</Option>
            </Select>
          </Space>
        </div>
      </div>

      {/* 表格 */}
      <Table
        columns={columns}
        dataSource={users}
        rowKey="id"
        loading={loading}
        pagination={{
          current: currentPage,
          pageSize: pageSize,
          total: total,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total, range) => 
            `第 ${range[0]}-${range[1]} 条，共 ${total} 条`,
          onChange: (page, size) => {
            setCurrentPage(page);
            setPageSize(size);
          }
        }}
      />

      {/* 用户编辑模态框 */}
      <Modal
        title={editingUser ? '编辑用户' : '新建用户'}
        visible={modalVisible}
        onCancel={() => setModalVisible(false)}
        onOk={() => form.submit()}
        width={600}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
        >
          <Form.Item
            name="username"
            label="用户名"
            rules={[
              { required: true, message: '请输入用户名' },
              { min: 3, message: '用户名至少3个字符' }
            ]}
          >
            <Input placeholder="请输入用户名" />
          </Form.Item>

          <Form.Item
            name="fullName"
            label="姓名"
            rules={[{ required: true, message: '请输入姓名' }]}
          >
            <Input placeholder="请输入姓名" />
          </Form.Item>

          <Form.Item
            name="email"
            label="邮箱"
            rules={[
              { required: true, message: '请输入邮箱' },
              { type: 'email', message: '请输入有效的邮箱地址' }
            ]}
          >
            <Input placeholder="请输入邮箱" />
          </Form.Item>

          <Form.Item
            name="department"
            label="部门"
            rules={[{ required: true, message: '请选择部门' }]}
          >
            <Select placeholder="请选择部门">
              <Option value="理赔部">理赔部</Option>
              <Option value="审核部">审核部</Option>
              <Option value="财务部">财务部</Option>
              <Option value="管理部">管理部</Option>
              <Option value="客服部">客服部</Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="role"
            label="角色"
            rules={[{ required: true, message: '请选择角色' }]}
          >
            <Select placeholder="请选择角色">
              <Option value="ROLE_ADMIN">管理员</Option>
              <Option value="ROLE_MANAGER">经理</Option>
              <Option value="ROLE_CLAIM_HANDLER">理赔员</Option>
              <Option value="ROLE_APPROVER">审批员</Option>
              <Option value="ROLE_FINANCE">财务</Option>
              <Option value="ROLE_USER">普通用户</Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="status"
            label="状态"
            rules={[{ required: true, message: '请选择状态' }]}
          >
            <Select placeholder="请选择状态">
              <Option value="ACTIVE">活跃</Option>
              <Option value="INACTIVE">非活跃</Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
};

export default UserManagement;
