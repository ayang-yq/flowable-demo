import React, { useState, useEffect } from 'react';
import { 
  Card, 
  Row, 
  Col, 
  Statistic, 
  Table, 
  List, 
  Tag, 
  Button, 
  Space,
  Spin,
  message
} from 'antd';
import { 
  FileTextOutlined, 
  ClockCircleOutlined, 
  CheckCircleOutlined, 
  ExclamationCircleOutlined,
  TrophyOutlined,
  UserOutlined
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { claimApi, taskApi } from '../services/api';
import { ClaimCase, FlowableTask, DashboardStatistics, TaskStatistics } from '../types';

const Dashboard: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [statistics, setStatistics] = useState<DashboardStatistics | null>(null);
  const [taskStatistics, setTaskStatistics] = useState<TaskStatistics | null>(null);
  const [recentClaims, setRecentClaims] = useState<ClaimCase[]>([]);
  const [myTasks, setMyTasks] = useState<FlowableTask[]>([]);
  const navigate = useNavigate();

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      
      // 并行加载数据
      const [statsRes, taskStatsRes, claimsRes, tasksRes] = await Promise.all([
        claimApi.getStatistics(),
        taskApi.getStatistics('admin'), // 假设当前用户是 admin
        claimApi.getClaims({ page: 0, size: 5, sort: 'createdAt', direction: 'DESC' }),
        taskApi.getMyTasks('admin', { page: 0, size: 5 })
      ]);

      setStatistics(statsRes.data);
      setTaskStatistics(taskStatsRes.data);
      setRecentClaims(claimsRes.data.content);
      setMyTasks(tasksRes.data.content);
    } catch (error) {
      console.error('Failed to load dashboard data:', error);
      message.error('加载数据失败');
    } finally {
      setLoading(false);
    }
  };

  const getStatusColor = (status: string) => {
    const colors: { [key: string]: string } = {
      'DRAFT': 'default',
      'SUBMITTED': 'processing',
      'PENDING': 'warning',
      'UNDER_REVIEW': 'processing',
      'APPROVED': 'success',
      'REJECTED': 'error',
      'PAID': 'success',
      'CLOSED': 'default'
    };
    return colors[status] || 'default';
  };

  const getStatusText = (status: string) => {
    const texts: { [key: string]: string } = {
      'DRAFT': '草稿',
      'SUBMITTED': '已提交',
      'PENDING': '待处理',
      'UNDER_REVIEW': '审核中',
      'APPROVED': '已批准',
      'REJECTED': '已拒绝',
      'PAID': '已支付',
      'CLOSED': '已关闭'
    };
    return texts[status] || status;
  };

  const getSeverityColor = (severity: string) => {
    const colors: { [key: string]: string } = {
      'HIGH': 'red',
      'MEDIUM': 'orange',
      'LOW': 'green'
    };
    return colors[severity] || 'default';
  };

  const getSeverityText = (severity: string) => {
    const texts: { [key: string]: string } = {
      'HIGH': '高',
      'MEDIUM': '中',
      'LOW': '低'
    };
    return texts[severity] || severity;
  };

  const claimColumns = [
    {
      title: '理赔编号',
      dataIndex: 'claimNumber',
      key: 'claimNumber',
      render: (text: string) => <span className="claim-number">{text}</span>
    },
    {
      title: '投保人',
      dataIndex: 'policy',
      key: 'policyholder',
      render: (policy: any) => policy?.policyholderName || '-'
    },
    {
      title: '理赔类型',
      dataIndex: 'claimType',
      key: 'claimType'
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
      title: '申请金额',
      dataIndex: 'claimedAmount',
      key: 'claimedAmount',
      render: (amount: number) => (
        <span className="amount-display">¥{amount.toLocaleString()}</span>
      )
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (date: string) => new Date(date).toLocaleDateString()
    }
  ];

  if (loading) {
    return (
      <div className="loading-container">
        <Spin size="large" />
      </div>
    );
  }

  return (
    <div>
      {/* 统计卡片 */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="总理赔数"
              value={statistics?.totalClaims || 0}
              prefix={<FileTextOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="待处理理赔"
              value={statistics?.pendingClaims || 0}
              prefix={<ClockCircleOutlined />}
              valueStyle={{ color: '#faad14' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="已批准理赔"
              value={statistics?.approvedClaims || 0}
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="总金额"
              value={statistics?.totalAmount || 0}
              prefix="¥"
              precision={2}
              valueStyle={{ color: '#722ed1' }}
            />
          </Card>
        </Col>
      </Row>

      {/* 任务统计 */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="我的待办"
              value={taskStatistics?.myTasksCount || 0}
              prefix={<UserOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="可认领任务"
              value={taskStatistics?.claimableTasksCount || 0}
              prefix={<TrophyOutlined />}
              valueStyle={{ color: '#faad14' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="今日完成"
              value={taskStatistics?.todayCompletedCount || 0}
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="平均处理时间"
              value={statistics?.averageProcessingTime || 0}
              suffix="小时"
              precision={1}
              valueStyle={{ color: '#722ed1' }}
            />
          </Card>
        </Col>
      </Row>

      {/* 最近理赔和我的任务 */}
      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card 
            title="最近理赔案件" 
            extra={
              <Button type="link" onClick={() => navigate('/claims')}>
                查看全部
              </Button>
            }
          >
            <Table
              dataSource={recentClaims}
              columns={claimColumns}
              pagination={false}
              size="small"
              rowKey="id"
            />
          </Card>
        </Col>
        
        <Col xs={24} lg={12}>
          <Card 
            title="我的待办任务" 
            extra={
              <Button type="link" onClick={() => navigate('/tasks')}>
                查看全部
              </Button>
            }
          >
            <List
              dataSource={myTasks}
              renderItem={(task) => (
                <List.Item
                  actions={[
                    <Button 
                      type="link" 
                      onClick={() => navigate(`/claims/${task.caseInstanceId}`)}
                    >
                      处理
                    </Button>
                  ]}
                >
                  <List.Item.Meta
                    avatar={<ClockCircleOutlined style={{ color: '#1890ff' }} />}
                    title={task.name}
                    description={
                      <Space>
                        <Tag color="blue">{task.assignee || '未分配'}</Tag>
                        {task.dueDate && (
                          <Tag color="orange">
                            截止: {new Date(task.dueDate).toLocaleDateString()}
                          </Tag>
                        )}
                      </Space>
                    }
                  />
                </List.Item>
              )}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default Dashboard;
