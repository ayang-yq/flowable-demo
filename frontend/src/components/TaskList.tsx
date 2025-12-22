import React, { useState, useEffect } from 'react';
import { 
  Card, 
  Tabs, 
  Table, 
  Button, 
  Space, 
  Tag, 
  Modal, 
  message,
  Popconfirm,
  Input,
  Select
} from 'antd';
import { 
  CheckOutlined, 
  ExclamationCircleOutlined,
  UserOutlined,
  ClockCircleOutlined
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { taskApi, userApi } from '../services/api';
import { FlowableTask, User } from '../types';

const { TabPane } = Tabs;
const { Search } = Input;
const { Option } = Select;

const TaskList: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [myTasks, setMyTasks] = useState<FlowableTask[]>([]);
  const [claimableTasks, setClaimableTasks] = useState<FlowableTask[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [activeTab, setActiveTab] = useState('my-tasks');
  const [totalMyTasks, setTotalMyTasks] = useState(0);
  const [totalClaimableTasks, setTotalClaimableTasks] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [assignModalVisible, setAssignModalVisible] = useState(false);
  const [selectedTask, setSelectedTask] = useState<FlowableTask | null>(null);

  const navigate = useNavigate();

  useEffect(() => {
    loadMyTasks();
    loadClaimableTasks();
    loadUsers();
  }, [currentPage, pageSize, activeTab]);

  const loadMyTasks = async () => {
    try {
      setLoading(true);
      const response = await taskApi.getMyTasks('admin', {
        page: currentPage - 1,
        size: pageSize
      });
      setMyTasks(response.data.content);
      setTotalMyTasks(response.data.totalElements);
    } catch (error) {
      console.error('Failed to load my tasks:', error);
      message.error('加载我的任务失败');
    } finally {
      setLoading(false);
    }
  };

  const loadClaimableTasks = async () => {
    try {
      setLoading(true);
      const response = await taskApi.getClaimableTasks('admin', {
        page: currentPage - 1,
        size: pageSize
      });
      setClaimableTasks(response.data.content);
      setTotalClaimableTasks(response.data.totalElements);
    } catch (error) {
      console.error('Failed to load claimable tasks:', error);
      message.error('加载可认领任务失败');
    } finally {
      setLoading(false);
    }
  };

  const loadUsers = async () => {
    try {
      const response = await userApi.getUsers({ page: 0, size: 100 });
      setUsers(response.data.content);
    } catch (error) {
      console.error('Failed to load users:', error);
    }
  };

  const handleClaimTask = async (taskId: string) => {
    try {
      await taskApi.claimTask(taskId, 'admin');
      message.success('认领成功');
      loadMyTasks();
      loadClaimableTasks();
    } catch (error) {
      console.error('Failed to claim task:', error);
      message.error('认领失败');
    }
  };

  const handleUnclaimTask = async (taskId: string) => {
    try {
      await taskApi.unclaimTask(taskId);
      message.success('取消认领成功');
      loadMyTasks();
      loadClaimableTasks();
    } catch (error) {
      console.error('Failed to unclaim task:', error);
      message.error('取消认领失败');
    }
  };

  const handleCompleteTask = async (taskId: string) => {
    try {
      await taskApi.completeTask(taskId);
      message.success('完成任务成功');
      loadMyTasks();
      loadClaimableTasks();
    } catch (error) {
      console.error('Failed to complete task:', error);
      message.error('完成任务失败');
    }
  };

  const handleAssignTask = async (values: any) => {
    try {
      if (selectedTask) {
        await taskApi.assignTask(selectedTask.id, values.userId);
        message.success('分配任务成功');
        setAssignModalVisible(false);
        setSelectedTask(null);
        loadMyTasks();
        loadClaimableTasks();
      }
    } catch (error) {
      console.error('Failed to assign task:', error);
      message.error('分配任务失败');
    }
  };

  const getPriorityColor = (priority: number) => {
    if (priority >= 80) return 'red';
    if (priority >= 50) return 'orange';
    return 'green';
  };

  const getPriorityText = (priority: number) => {
    if (priority >= 80) return '高';
    if (priority >= 50) return '中';
    return '低';
  };

  const columns = [
    {
      title: '任务名称',
      dataIndex: 'name',
      key: 'name',
      render: (text: string, record: FlowableTask) => (
        <Button 
          type="link" 
          onClick={() => {
            if (record.caseInstanceId) {
              navigate(`/claims/${record.caseInstanceId}`);
            }
          }}
        >
          {text}
        </Button>
      )
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true
    },
    {
      title: '优先级',
      dataIndex: 'priority',
      key: 'priority',
      render: (priority: number) => (
        <Tag color={getPriorityColor(priority)}>
          {getPriorityText(priority)}
        </Tag>
      )
    },
    {
      title: '分配给',
      dataIndex: 'assignee',
      key: 'assignee',
      render: (assignee: string) => assignee || '未分配'
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      render: (date: string) => new Date(date).toLocaleString()
    },
    {
      title: '截止时间',
      dataIndex: 'dueDate',
      key: 'dueDate',
      render: (date: string) => date ? new Date(date).toLocaleString() : '-'
    },
    {
      title: '操作',
      key: 'actions',
      render: (text: any, record: FlowableTask) => (
        <Space size="middle">
          {activeTab === 'claimable' ? (
            <Button
              type="primary"
              size="small"
              icon={<UserOutlined />}
              onClick={() => handleClaimTask(record.id)}
            >
              认领
            </Button>
          ) : (
            <>
              {record.assignee && (
                <Popconfirm
                  title="确定要取消认领这个任务吗？"
                  onConfirm={() => handleUnclaimTask(record.id)}
                  okText="确定"
                  cancelText="取消"
                >
                  <Button size="small" danger>
                    取消认领
                  </Button>
                </Popconfirm>
              )}
              <Button
                type="primary"
                size="small"
                icon={<CheckOutlined />}
                onClick={() => handleCompleteTask(record.id)}
              >
                完成
              </Button>
              <Button
                size="small"
                onClick={() => {
                  setSelectedTask(record);
                  setAssignModalVisible(true);
                }}
              >
                分配
              </Button>
            </>
          )}
        </Space>
      )
    }
  ];

  return (
    <Card>
      <Tabs activeKey={activeTab} onChange={setActiveTab}>
        <TabPane 
          tab={
            <span>
              <UserOutlined />
              我的待办 ({totalMyTasks})
            </span>
          } 
          key="my-tasks"
        >
          <Table
            columns={columns}
            dataSource={myTasks}
            rowKey="id"
            loading={loading}
            pagination={{
              current: currentPage,
              pageSize: pageSize,
              total: totalMyTasks,
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
        </TabPane>
        
        <TabPane 
          tab={
            <span>
              <ClockCircleOutlined />
              可认领任务 ({totalClaimableTasks})
            </span>
          } 
          key="claimable"
        >
          <Table
            columns={columns}
            dataSource={claimableTasks}
            rowKey="id"
            loading={loading}
            pagination={{
              current: currentPage,
              pageSize: pageSize,
              total: totalClaimableTasks,
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
        </TabPane>
      </Tabs>

      {/* 分配任务模态框 */}
      <Modal
        title="分配任务"
        visible={assignModalVisible}
        onCancel={() => {
          setAssignModalVisible(false);
          setSelectedTask(null);
        }}
        onOk={() => {
          const form = document.querySelector('#assign-task-form') as HTMLFormElement;
          if (form) {
            form.requestSubmit();
          }
        }}
      >
        <form 
          id="assign-task-form"
          onSubmit={(e) => {
            e.preventDefault();
            const formData = new FormData(e.currentTarget);
            handleAssignTask({ userId: formData.get('userId') });
          }}
        >
          <div style={{ marginBottom: 16 }}>
            <label>任务名称：</label>
            <span>{selectedTask?.name}</span>
          </div>
          <div style={{ marginBottom: 16 }}>
            <label>分配给：</label>
            <select name="userId" style={{ width: '100%', padding: '8px' }} required>
              <option value="">请选择用户</option>
              {users.map(user => (
                <option key={user.id} value={user.id}>
                  {user.fullName} ({user.department})
                </option>
              ))}
            </select>
          </div>
        </form>
      </Modal>
    </Card>
  );
};

export default TaskList;
