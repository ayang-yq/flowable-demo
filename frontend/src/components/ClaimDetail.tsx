import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  Card, 
  Descriptions, 
  Tag, 
  Button, 
  Space, 
  Modal, 
  message,
  Tabs,
  Table,
  Timeline,
  Form,
  Input,
  Select,
  InputNumber,
  Radio,
  List,
  Spin,
  Row,
  Col,
  Divider
} from 'antd';
import { 
  ArrowLeftOutlined, 
  EditOutlined, 
  CheckCircleOutlined,
  CloseCircleOutlined,
  DollarOutlined,
  UserOutlined,
  ClockCircleOutlined,
  FileTextOutlined,
  SolutionOutlined
} from '@ant-design/icons';
import { claimApi, taskApi, userApi } from '../services/api';
import { ClaimCase, User } from '../types';

const { TextArea } = Input;

const ClaimDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  
  const [loading, setLoading] = useState(true);
  const [claim, setClaim] = useState<ClaimCase | null>(null);
  const [users, setUsers] = useState<User[]>([]);
  const [tasks, setTasks] = useState<any>(null);
  const [currentTask, setCurrentTask] = useState<any>(null);
  const [taskActionModalVisible, setTaskActionModalVisible] = useState(false);
  const [approveModalVisible, setApproveModalVisible] = useState(false);
  const [rejectModalVisible, setRejectModalVisible] = useState(false);
  const [paymentModalVisible, setPaymentModalVisible] = useState(false);
  const [assignModalVisible, setAssignModalVisible] = useState(false);
  const [reviewModalVisible, setReviewModalVisible] = useState(false);
  
  // 任务相关状态
  const [taskVariables, setTaskVariables] = useState<Record<string, any>>({});
  const [loadingVariables, setLoadingVariables] = useState(false);
  const [selectedTaskForProcessing, setSelectedTaskForProcessing] = useState<any>(null);
  
  // 任务特定模态框
  const [reviewTaskModalVisible, setReviewTaskModalVisible] = useState(false);
  const [assessComplexityModalVisible, setAssessComplexityModalVisible] = useState(false);
  const [finalApprovalModalVisible, setFinalApprovalModalVisible] = useState(false);
  const [paymentTaskModalVisible, setPaymentTaskModalVisible] = useState(false);
  const [finalizeTaskModalVisible, setFinalizeTaskModalVisible] = useState(false);
  const [validatePaymentModalVisible, setValidatePaymentModalVisible] = useState(false);
  const [confirmPaymentModalVisible, setConfirmPaymentModalVisible] = useState(false);
  const [handleDisputeModalVisible, setHandleDisputeModalVisible] = useState(false);
  
  const [form] = Form.useForm();

  // Common parser function for InputNumber with currency formatting
  const currencyParser = ((value: string | undefined) => {
    if (!value) return 0;
    const parsed = Number(value.replace(/¥\s?|(,*)/g, ''));
    return isNaN(parsed) ? 0 : parsed;
  }) as (displayValue: string | undefined) => number;

  const loadClaimDetail = useCallback(async () => {
    try {
      setLoading(true);
      const response = await claimApi.getClaim(id!);
      setClaim(response.data);
    } catch (error) {
      console.error('Failed to load claim detail:', error);
      message.error('加载理赔详情失败');
    } finally {
      setLoading(false);
    }
  }, [id]);

  const loadTasks = useCallback(async () => {
    if (!claim?.caseInstanceId) return;
    
    try {
      // Get current user from localStorage
      const userStr = localStorage.getItem('user');
      const user = userStr ? JSON.parse(userStr) : null;
      
      const response = await taskApi.getTasksByCase(claim.caseInstanceId, user?.id);
      setTasks(response.data);
    } catch (error) {
      console.error('Failed to load tasks:', error);
    }
  }, [claim?.caseInstanceId]);

  const loadUsers = useCallback(async () => {
    try {
      const response = await userApi.getUsers({ page: 0, size: 100 });
      setUsers(response.data.content);
    } catch (error) {
      console.error('Failed to load users:', error);
    }
  }, []);

  useEffect(() => {
    if (id) {
      loadClaimDetail();
      loadUsers();
    }
  }, [id, loadClaimDetail, loadUsers]);

  useEffect(() => {
    if (claim?.caseInstanceId) {
      loadTasks();
    }
  }, [claim?.caseInstanceId, loadTasks]);

  const loadTaskVariables = async (taskId: string) => {
    try {
      setLoadingVariables(true);
      const response = await taskApi.getTaskVariables(taskId);
      setTaskVariables(response.data);
    } catch (error) {
      console.error('Failed to load task variables:', error);
      message.error('加载任务变量失败');
    } finally {
      setLoadingVariables(false);
    }
  };

  const handleApprove = async (values: any) => {
    try {
      // Get current user from localStorage
      const userStr = localStorage.getItem('user');
      const user = userStr ? JSON.parse(userStr) : null;
      
      await claimApi.approveClaim(id!, user?.id || 'admin', values);
      message.success('审批成功');
      setApproveModalVisible(false);
      loadClaimDetail();
      loadTasks();
    } catch (error) {
      console.error('Failed to approve claim:', error);
      message.error('审批失败');
    }
  };

  const handleReject = async (values: any) => {
    try {
      await claimApi.rejectClaim(id!, values.reason);
      message.success('拒绝成功');
      setRejectModalVisible(false);
      loadClaimDetail();
      loadTasks();
    } catch (error) {
      console.error('Failed to reject claim:', error);
      message.error('拒绝失败');
    }
  };

  const handlePay = async (values: any) => {
    try {
      // Get current user from localStorage
      const userStr = localStorage.getItem('user');
      const user = userStr ? JSON.parse(userStr) : null;
      
      // Build payment request with all required fields
      const paymentData = {
        paymentAmount: values.paymentAmount,
        paymentDate: values.paymentDate || new Date().toISOString().split('T')[0],
        paymentMethod: values.paymentMethod,
        paymentReference: values.paymentReference || `PAY-${new Date().toISOString().split('T')[0]}-${id?.substring(0, 8)}`,
        userId: user?.id || 'admin'
      };
      
      await claimApi.payClaim(id!, paymentData);
      message.success('支付成功');
      setPaymentModalVisible(false);
      loadClaimDetail();
      loadTasks();
    } catch (error) {
      console.error('Failed to pay claim:', error);
      message.error('支付失败');
    }
  };

  const handleAssign = async (values: any) => {
    try {
      await claimApi.assignClaim(id!, values.userId);
      message.success('分配成功');
      setAssignModalVisible(false);
      loadClaimDetail();
      loadTasks();
    } catch (error) {
      console.error('Failed to assign claim:', error);
      message.error('分配失败');
    }
  };

  const handleCompleteReview = async (values: any) => {
    try {
      // Get current user from localStorage
      const userStr = localStorage.getItem('user');
      const user = userStr ? JSON.parse(userStr) : null;
      
      const reviewData = {
        reviewComments: values.comments,
        reviewNotes: values.notes
      };
      
      await claimApi.completeReview(id!, user?.id || 'admin', reviewData);
      message.success('审核完成');
      setReviewModalVisible(false);
      form.resetFields();
      loadClaimDetail();
      loadTasks();
    } catch (error) {
      console.error('Failed to complete review:', error);
      message.error('审核完成失败');
    }
  };

  const handleClaimTask = async (taskId: string) => {
    try {
      const userStr = localStorage.getItem('user');
      const user = userStr ? JSON.parse(userStr) : null;
      
      await taskApi.claimTask(taskId, user?.id || 'admin');
      message.success('任务认领成功');
      loadTasks();
    } catch (error) {
      console.error('Failed to claim task:', error);
      message.error('任务认领失败');
    }
  };

  const handleCompleteTaskAction = async (values: any) => {
    if (!currentTask) return;
    
    try {
      await taskApi.completeTask(currentTask.id, values);
      message.success('任务完成');
      setTaskActionModalVisible(false);
      setCurrentTask(null);
      form.resetFields();
      loadTasks();
      loadClaimDetail();
    } catch (error) {
      console.error('Failed to complete task:', error);
      message.error('任务完成失败');
    }
  };

  const openTaskActionModal = async (task: any) => {
    setCurrentTask(task);
    setSelectedTaskForProcessing(task);
    
    // 根据任务类型打开对应的模态框
    switch (task.taskDefinitionKey) {
      case 'taskReviewClaim':
        await loadTaskVariables(task.id);
        setReviewTaskModalVisible(true);
        break;
      
      case 'taskAssessComplexity':
        setAssessComplexityModalVisible(true);
        break;
      
      case 'taskFinalApproval':
        setFinalApprovalModalVisible(true);
        break;
      
      case 'taskProcessPayment':
        await loadTaskVariables(task.id);
        setPaymentTaskModalVisible(true);
        break;
      
      case 'taskFinalizeClosure':
        setFinalizeTaskModalVisible(true);
        break;
      
      case 'userTask_validatePayment':
        await loadTaskVariables(task.id);
        setValidatePaymentModalVisible(true);
        break;
      
      case 'userTask_confirmPayment':
        await loadTaskVariables(task.id);
        setConfirmPaymentModalVisible(true);
        break;
      
      case 'userTask_handleDispute':
        setHandleDisputeModalVisible(true);
        break;
      
      default:
        // 普通任务，使用通用模态框
        setTaskActionModalVisible(true);
    }
  };

  // 任务处理函数
  const handleReviewTask = async (values: any) => {
    try {
      if (selectedTaskForProcessing) {
        const variables = {
          reviewResult: values.reviewResult,
          reviewComments: values.reviewComments
        };
        await taskApi.completeTask(selectedTaskForProcessing.id, variables);
        message.success('审核任务完成');
        setReviewTaskModalVisible(false);
        form.resetFields();
        loadTasks();
        loadClaimDetail();
      }
    } catch (error) {
      console.error('Failed to complete review task:', error);
      message.error('审核任务完成失败');
    }
  };

  const handleAssessComplexity = async (values: any) => {
    try {
      if (selectedTaskForProcessing) {
        const variables = {
          complexityLevel: values.complexityLevel,
          complexityReason: values.complexityReason
        };
        await taskApi.completeTask(selectedTaskForProcessing.id, variables);
        message.success('复杂度评估完成');
        setAssessComplexityModalVisible(false);
        form.resetFields();
        loadTasks();
        loadClaimDetail();
      }
    } catch (error) {
      console.error('Failed to assess complexity:', error);
      message.error('复杂度评估失败');
    }
  };

  const handleFinalApproval = async (values: any) => {
    try {
      if (selectedTaskForProcessing) {
        const variables = {
          approved: values.approved,
          approvalComments: values.approvalComments,
          approvedAmount: values.approvedAmount || claim?.approvedAmount
        };
        await taskApi.completeTask(selectedTaskForProcessing.id, variables);
        message.success('最终审批完成');
        setFinalApprovalModalVisible(false);
        form.resetFields();
        loadTasks();
        loadClaimDetail();
      }
    } catch (error) {
      console.error('Failed to complete final approval:', error);
      message.error('最终审批失败');
    }
  };

  const handlePaymentTask = async (values: any) => {
    try {
      if (selectedTaskForProcessing) {
        const variables = {
          paymentAmount: values.paymentAmount,
          paymentMethod: values.paymentMethod,
          paymentReference: values.paymentReference,
          paymentComments: values.paymentComments
        };
        await taskApi.completeTask(selectedTaskForProcessing.id, variables);
        message.success('支付处理完成');
        setPaymentTaskModalVisible(false);
        form.resetFields();
        loadTasks();
        loadClaimDetail();
      }
    } catch (error) {
      console.error('Failed to process payment task:', error);
      message.error('支付处理失败');
    }
  };

  const handleFinalizeTask = async (values: any) => {
    try {
      if (selectedTaskForProcessing) {
        const variables = {
          closureReason: values.closureReason,
          closureNotes: values.closureNotes
        };
        await taskApi.completeTask(selectedTaskForProcessing.id, variables);
        message.success('结案处理完成');
        setFinalizeTaskModalVisible(false);
        form.resetFields();
        loadTasks();
        loadClaimDetail();
      }
    } catch (error) {
      console.error('Failed to finalize case:', error);
      message.error('结案处理失败');
    }
  };

  const handleValidatePayment = async (values: any) => {
    try {
      if (selectedTaskForProcessing) {
        const variables = {
          validationResult: values.validationResult,
          validationComment: values.validationComment
        };
        await taskApi.completeTask(selectedTaskForProcessing.id, variables);
        message.success('支付校验成功');
        setValidatePaymentModalVisible(false);
        form.resetFields();
        loadTasks();
        loadClaimDetail();
      }
    } catch (error) {
      console.error('Failed to validate payment:', error);
      message.error('支付校验失败');
    }
  };

  const handleConfirmPayment = async (values: any) => {
    try {
      if (selectedTaskForProcessing) {
        const variables = {
          confirmationResult: values.confirmationResult,
          confirmationComment: values.confirmationComment
        };
        await taskApi.completeTask(selectedTaskForProcessing.id, variables);
        message.success('支付确认成功');
        setConfirmPaymentModalVisible(false);
        form.resetFields();
        loadTasks();
        loadClaimDetail();
      }
    } catch (error) {
      console.error('Failed to confirm payment:', error);
      message.error('支付确认失败');
    }
  };

  const handleDisputePayment = async (values: any) => {
    try {
      if (selectedTaskForProcessing) {
        const variables = {
          disputeResolution: values.disputeResolution,
          disputeComment: values.disputeComment
        };
        await taskApi.completeTask(selectedTaskForProcessing.id, variables);
        message.success('争议处理完成');
        setHandleDisputeModalVisible(false);
        form.resetFields();
        loadTasks();
        loadClaimDetail();
      }
    } catch (error) {
      console.error('Failed to handle dispute:', error);
      message.error('争议处理失败');
    }
  };

  const resetTaskModals = () => {
    setTaskActionModalVisible(false);
    setReviewTaskModalVisible(false);
    setAssessComplexityModalVisible(false);
    setFinalApprovalModalVisible(false);
    setPaymentTaskModalVisible(false);
    setFinalizeTaskModalVisible(false);
    setValidatePaymentModalVisible(false);
    setConfirmPaymentModalVisible(false);
    setHandleDisputeModalVisible(false);
    setCurrentTask(null);
    setSelectedTaskForProcessing(null);
    setTaskVariables({});
    form.resetFields();
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

  const getTaskTypeText = (task: any) => {
    // Use the task name if available, otherwise fall back to the mapped text from taskDefinitionKey
    if (task.name) {
      return task.name;
    }
    
    const taskNames: { [key: string]: string } = {
      'taskReviewClaim': '审核理赔',
      'taskAssessComplexity': '评估复杂度',
      'taskProcessPayment': '处理支付',
      'taskFinalizeClosure': '结案处理',
      'userTask_validatePayment': '验证支付',
      'userTask_confirmPayment': '确认支付',
      'userTask_handleDispute': '处理争议'
    };
    return taskNames[task.taskDefinitionKey] || task.taskDefinitionKey;
  };

  const getTaskIcon = (taskDefinitionKey: string) => {
    const icons: { [key: string]: any } = {
      'taskReviewClaim': <FileTextOutlined />,
      'taskAssessComplexity': <SolutionOutlined />,
      'taskProcessPayment': <DollarOutlined />,
      'taskFinalizeClosure': <CheckCircleOutlined />,
      'userTask_validatePayment': <CheckCircleOutlined />,
      'userTask_confirmPayment': <CheckCircleOutlined />,
      'userTask_handleDispute': <CloseCircleOutlined />
    };
    return icons[taskDefinitionKey] || <UserOutlined />;
  };

  const getUserNameById = (userId: string) => {
    if (!userId) return '-';
    const user = users.find(u => u.id === userId);
    return user ? user.fullName : userId;
  };

  const getTaskPriorityColor = (priority: number) => {
    if (!priority) return '#999';
    if (priority >= 80) return '#ff4d4f';
    if (priority >= 50) return '#faad14';
    return '#52c41a';
  };

  const documentColumns = [
    {
      title: '文件名',
      dataIndex: 'fileName',
      key: 'fileName'
    },
    {
      title: '文件类型',
      dataIndex: 'fileType',
      key: 'fileType'
    },
    {
      title: '文件大小',
      dataIndex: 'fileSize',
      key: 'fileSize',
      render: (size: number) => `${(size / 1024).toFixed(2)} KB`
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description'
    },
    {
      title: '上传人',
      dataIndex: ['uploadedBy', 'fullName'],
      key: 'uploadedBy'
    },
    {
      title: '上传时间',
      dataIndex: 'uploadedAt',
      key: 'uploadedAt',
      render: (date: string) => new Date(date).toLocaleString()
    }
  ];

  if (loading || !claim) {
    return <div className="loading-container">加载中...</div>;
  }

  const activeTasks = tasks?.availableForMe || tasks?.activeTasks || [];

  return (
    <div>
      {/* 页面头部 */}
      <Card style={{ marginBottom: 24 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div style={{ display: 'flex', alignItems: 'center' }}>
            <Button 
              icon={<ArrowLeftOutlined />} 
              onClick={() => navigate('/claims')}
              style={{ marginRight: 16 }}
            >
              返回
            </Button>
            <div>
              <h2 style={{ margin: 0 }}>理赔详情 - {claim.claimNumber}</h2>
              <Space style={{ marginTop: 8 }}>
                <Tag color={getStatusColor(claim.status)}>
                  {getStatusText(claim.status)}
                </Tag>
                <Tag color={getSeverityColor(claim.severity)}>
                  {getSeverityText(claim.severity)}
                </Tag>
              </Space>
            </div>
          </div>
          <Space>
            <Button 
              icon={<EditOutlined />} 
              onClick={() => navigate(`/claims/${id}/edit`)}
            >
              编辑
            </Button>
            {claim.status === 'SUBMITTED' && (
              <Button 
                type="primary" 
                onClick={() => setReviewModalVisible(true)}
              >
                完成审核
              </Button>
            )}
            {claim.status === 'UNDER_REVIEW' && (
              <>
                <Button 
                  type="primary" 
                  icon={<CheckCircleOutlined />}
                  onClick={() => setApproveModalVisible(true)}
                >
                  批准
                </Button>
                <Button 
                  danger 
                  icon={<CloseCircleOutlined />}
                  onClick={() => setRejectModalVisible(true)}
                >
                  拒绝
                </Button>
              </>
            )}
            {claim.status === 'APPROVED' && (
              <Button 
                type="primary" 
                icon={<DollarOutlined />}
                onClick={() => setPaymentModalVisible(true)}
              >
                支付
              </Button>
            )}
            <Button 
              onClick={() => setAssignModalVisible(true)}
            >
              分配
            </Button>
          </Space>
        </div>
      </Card>

      {/* 分栏布局：左边80%详情，右边20%任务 */}
      <Row gutter={16}>
        <Col span={19}>
          {/* 详情内容 */}
          <Tabs 
            defaultActiveKey="basic"
            items={[
              {
                key: 'basic',
                label: '基本信息',
                children: (
              <Card>
                <Descriptions title="理赔信息" bordered column={2}>
                  <Descriptions.Item label="理赔编号">{claim.claimNumber}</Descriptions.Item>
                  <Descriptions.Item label="理赔类型">{claim.claimType}</Descriptions.Item>
                  <Descriptions.Item label="严重程度">
                    <Tag color={getSeverityColor(claim.severity)}>
                      {getSeverityText(claim.severity)}
                    </Tag>
                  </Descriptions.Item>
                  <Descriptions.Item label="状态">
                    <Tag color={getStatusColor(claim.status)}>
                      {getStatusText(claim.status)}
                    </Tag>
                  </Descriptions.Item>
                  <Descriptions.Item label="申请金额">
                    <span className="amount-display">¥{claim.claimedAmount.toLocaleString()}</span>
                  </Descriptions.Item>
                  <Descriptions.Item label="已批准金额">
                    {claim.approvedAmount ? (
                      <span className="amount-display">¥{claim.approvedAmount.toLocaleString()}</span>
                    ) : '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="已支付金额">
                    {claim.paidAmount ? (
                      <span className="amount-display">¥{claim.paidAmount.toLocaleString()}</span>
                    ) : '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="分配给">
                    {claim.assignedToName || '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="创建时间">{new Date(claim.createdAt).toLocaleString()}</Descriptions.Item>
                  <Descriptions.Item label="更新时间">{new Date(claim.updatedAt).toLocaleString()}</Descriptions.Item>
                </Descriptions>

                <Descriptions title="保单信息" bordered column={2} style={{ marginTop: 24 }}>
                  <Descriptions.Item label="保单号">{claim.policy?.policyNumber || '-'}</Descriptions.Item>
                  <Descriptions.Item label="投保人">{claim.policy?.policyholderName || '-'}</Descriptions.Item>
                  <Descriptions.Item label="保单类型">{claim.policy?.policyType || '-'}</Descriptions.Item>
                  <Descriptions.Item label="保额">
                    {claim.policy?.coverageAmount ? (
                      <span className="amount-display">¥{claim.policy.coverageAmount.toLocaleString()}</span>
                    ) : '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="保费">
                    {claim.policy?.premium ? (
                      <span className="amount-display">¥{claim.policy.premium.toLocaleString()}</span>
                    ) : '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="保单状态">{claim.policy?.status || '-'}</Descriptions.Item>
                </Descriptions>

                <Descriptions title="申请人信息" bordered column={2} style={{ marginTop: 24 }}>
                  <Descriptions.Item label="申请人">{claim.claimantName}</Descriptions.Item>
                  <Descriptions.Item label="联系电话">{claim.claimantPhone}</Descriptions.Item>
                  <Descriptions.Item label="邮箱">{claim.claimantEmail}</Descriptions.Item>
                  <Descriptions.Item label="事故时间">{new Date(claim.incidentDate).toLocaleString()}</Descriptions.Item>
                  <Descriptions.Item label="事故地点" span={2}>{claim.incidentLocation}</Descriptions.Item>
                  <Descriptions.Item label="事故描述" span={2}>{claim.incidentDescription}</Descriptions.Item>
                </Descriptions>
              </Card>
                )
              },
              {
                key: 'documents',
                label: '相关文档',
                children: (
              <Card title="文档列表">
                <Table
                  columns={documentColumns}
                  dataSource={claim.documents || []}
                  rowKey="id"
                  pagination={false}
                />
              </Card>
                )
              },
              {
                key: 'history',
                label: '处理历史',
                children: (
              <Card title="处理记录">
                <Timeline>
                  {(claim.histories || []).map((history) => (
                    <Timeline.Item key={history.id}>
                      <div className="timeline-item-content">
                        <div className="timeline-item-title">{history.action}</div>
                        <div className="timeline-item-description">{history.description}</div>
                        <div>
                          <span>{history.operator.fullName} - </span>
                          <span>{new Date(history.operatedAt).toLocaleString()}</span>
                        </div>
                      </div>
                    </Timeline.Item>
                  ))}
                </Timeline>
              </Card>
                )
              }
            ]}
          />
        </Col>

        <Col span={5}>
          {/* 待办任务侧边栏 */}
          <Card 
            title={
              <Space>
                <ClockCircleOutlined />
                <span>待办任务 ({activeTasks.length})</span>
              </Space>
            }
            style={{ height: 'calc(100vh - 200px)', overflow: 'auto' }}
          >
            {activeTasks.length === 0 ? (
              <div style={{ 
                textAlign: 'center', 
                padding: '40px 20px', 
                color: '#999',
                fontSize: '14px'
              }}>
                <ClockCircleOutlined style={{ fontSize: '48px', marginBottom: '12px', color: '#d9d9d9' }} />
                <div>暂无待办任务</div>
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                {activeTasks.map((task: any) => (
                  <Card 
                    key={task.id}
                    size="small"
                    style={{
                      borderLeft: `4px solid ${getTaskPriorityColor(task.priority)}`,
                      boxShadow: '0 1px 2px rgba(0,0,0,0.08)',
                      transition: 'all 0.2s',
                      cursor: 'pointer'
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.boxShadow = '0 2px 8px rgba(0,0,0,0.15)';
                      e.currentTarget.style.transform = 'translateY(-2px)';
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.boxShadow = '0 1px 2px rgba(0,0,0,0.08)';
                      e.currentTarget.style.transform = 'translateY(0)';
                    }}
                  >
                    <div style={{ marginBottom: '8px' }}>
                      <div style={{ 
                        display: 'flex', 
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        marginBottom: '6px'
                      }}>
                        <div style={{ 
                          display: 'flex', 
                          alignItems: 'center',
                          gap: '6px',
                          flex: 1
                        }}>
                          <span style={{ 
                            color: '#1890ff',
                            fontSize: '16px'
                          }}>
                            {getTaskIcon(task.taskDefinitionKey)}
                          </span>
                          <span style={{ 
                            fontWeight: 500,
                            fontSize: '13px',
                            color: '#262626'
                          }}>
                            {getTaskTypeText(task)}
                          </span>
                        </div>
                        <Tag 
                          color={task.priority >= 80 ? 'red' : task.priority >= 50 ? 'orange' : 'green'}
                          style={{ 
                            fontSize: '11px', 
                            margin: 0,
                            padding: '0 4px',
                            height: '18px',
                            lineHeight: '18px'
                          }}
                        >
                          {task.priority || 50}
                        </Tag>
                      </div>
                    </div>

                    <div style={{ 
                      display: 'flex', 
                      flexDirection: 'column', 
                      gap: '4px',
                      marginBottom: '10px'
                    }}>
                      <div style={{ 
                        display: 'flex',
                        alignItems: 'center',
                        gap: '4px',
                        fontSize: '12px',
                        color: '#8c8c8c'
                      }}>
                        <UserOutlined style={{ fontSize: '12px' }} />
                        <span>
                          {task.assignee ? getUserNameById(task.assignee) : '未分配'}
                        </span>
                      </div>
                      <div style={{ 
                        display: 'flex',
                        alignItems: 'center',
                        gap: '4px',
                        fontSize: '12px',
                        color: '#8c8c8c'
                      }}>
                        <ClockCircleOutlined style={{ fontSize: '12px' }} />
                        <span>
                          {new Date(task.createTime).toLocaleDateString('zh-CN', {
                            year: 'numeric',
                            month: '2-digit',
                            day: '2-digit'
                          })}
                          {' '}
                          {new Date(task.createTime).toLocaleTimeString('zh-CN', {
                            hour: '2-digit',
                            minute: '2-digit'
                          })}
                        </span>
                      </div>
                    </div>

                    <div>
                      {task.assignee ? (
                        <Button 
                          type="primary" 
                          size="small"
                          block
                          onClick={() => openTaskActionModal(task)}
                          style={{
                            height: '28px',
                            fontSize: '12px',
                            borderRadius: '4px'
                          }}
                        >
                          <Space size={4}>
                            <CheckCircleOutlined style={{ fontSize: '12px' }} />
                            处理任务
                          </Space>
                        </Button>
                      ) : (
                        <Button 
                          block
                          size="small"
                          onClick={() => handleClaimTask(task.id)}
                          style={{
                            height: '28px',
                            fontSize: '12px',
                            borderRadius: '4px'
                          }}
                        >
                          <Space size={4}>
                            <UserOutlined style={{ fontSize: '12px' }} />
                            认领任务
                          </Space>
                        </Button>
                      )}
                    </div>
                  </Card>
                ))}
              </div>
            )}

            {tasks?.historicTasks && tasks.historicTasks.length > 0 && (
              <>
                <Divider style={{ margin: '16px 0' }} />
                <div style={{ 
                  marginBottom: 12,
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                  color: '#8c8c8c',
                  fontSize: '13px',
                  fontWeight: 500
                }}>
                  <ClockCircleOutlined />
                  <span>历史任务 ({tasks.historicTasks.length})</span>
                </div>
                <List
                  dataSource={tasks.historicTasks}
                  renderItem={(task: any) => (
                    <Card 
                      size="small"
                      style={{ 
                        marginBottom: '8px',
                        backgroundColor: '#fafafa',
                        border: '1px solid #f0f0f0'
                      }}
                    >
                      <div style={{ 
                        display: 'flex', 
                        alignItems: 'center',
                        gap: '8px',
                        marginBottom: '6px'
                      }}>
                        <span style={{ color: '#8c8c8c' }}>
                          {getTaskIcon(task.taskDefinitionKey)}
                        </span>
                        <span style={{ 
                          fontSize: '13px',
                          color: '#595959',
                          fontWeight: 400
                        }}>
                          {getTaskTypeText(task)}
                        </span>
                      </div>
                      <div style={{ 
                        display: 'flex',
                        alignItems: 'center',
                        gap: '4px',
                        fontSize: '12px',
                        color: '#8c8c8c'
                      }}>
                        <UserOutlined style={{ fontSize: '12px' }} />
                        <span>
                          {task.assignee ? getUserNameById(task.assignee) : '-'}
                        </span>
                      </div>
                    </Card>
                  )}
                />
              </>
            )}
          </Card>
        </Col>
      </Row>

      {/* 审批模态框 */}
      <Modal
        title="批准理赔"
        visible={approveModalVisible}
        onCancel={() => setApproveModalVisible(false)}
        onOk={() => form.submit()}
      >
        <Form form={form} onFinish={handleApprove}>
          <Form.Item 
            name="approvedAmount" 
            label="批准金额" 
            rules={[{ required: true, message: '请输入批准金额' }]}
            initialValue={claim.claimedAmount}
          >
            <InputNumber 
              style={{ width: '100%' }} 
              min={0} 
              max={claim.claimedAmount}
              formatter={value => `¥ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
              parser={currencyParser}
            />
          </Form.Item>
          <Form.Item name="comments" label="审批意见">
            <TextArea rows={4} />
          </Form.Item>
        </Form>
      </Modal>

      {/* 拒绝模态框 */}
      <Modal
        title="拒绝理赔"
        visible={rejectModalVisible}
        onCancel={() => setRejectModalVisible(false)}
        onOk={() => form.submit()}
      >
        <Form form={form} onFinish={handleReject}>
          <Form.Item 
            name="reason" 
            label="拒绝原因" 
            rules={[{ required: true, message: '请输入拒绝原因' }]}
          >
            <TextArea rows={4} />
          </Form.Item>
        </Form>
      </Modal>

      {/* 支付模态框 */}
      <Modal
        title="支付理赔"
        visible={paymentModalVisible}
        onCancel={() => setPaymentModalVisible(false)}
        onOk={() => form.submit()}
      >
        <Form form={form} onFinish={handlePay}>
          <Form.Item 
            name="paymentAmount" 
            label="支付金额" 
            rules={[{ required: true, message: '请输入支付金额' }]}
            initialValue={claim.approvedAmount}
          >
            <InputNumber 
              style={{ width: '100%' }} 
              min={0} 
              max={claim.approvedAmount}
              formatter={value => `¥ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
              parser={currencyParser}
            />
          </Form.Item>
          <Form.Item 
            name="paymentDate" 
            label="支付日期" 
            rules={[{ required: true, message: '请选择支付日期' }]}
            initialValue={new Date().toISOString().split('T')[0]}
          >
            <input type="date" style={{ width: '100%', padding: '8px', border: '1px solid #d9d9d9', borderRadius: '2px' }} />
          </Form.Item>
          <Form.Item name="paymentMethod" label="支付方式" rules={[{ required: true, message: '请选择支付方式' }]}>
            <Select placeholder="请选择支付方式">
              <Select.Option value="TRANSFER">银行转账</Select.Option>
              <Select.Option value="CASH">现金</Select.Option>
              <Select.Option value="CHECK">支票</Select.Option>
              <Select.Option value="ONLINE">在线支付</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="paymentReference" label="支付参考号" help="可选，留空将自动生成">
            <Input placeholder="如：PAY-20241226-001" />
          </Form.Item>
        </Form>
      </Modal>

      {/* 通用任务处理模态框 */}
      <Modal
        title="处理任务"
        visible={taskActionModalVisible}
        onCancel={resetTaskModals}
        onOk={() => form.submit()}
      >
        <Form form={form} onFinish={handleCompleteTaskAction}>
          <Form.Item name="comments" label="处理意见">
            <TextArea rows={4} placeholder="请填写处理意见" />
          </Form.Item>
          <Form.Item name="result" label="处理结果">
            <Select placeholder="请选择处理结果">
              <Select.Option value="APPROVED">通过</Select.Option>
              <Select.Option value="REJECTED">拒绝</Select.Option>
              <Select.Option value="REQUIRES_INFO">需要更多信息</Select.Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>

      {/* 审核理赔任务模态框 */}
      <Modal
        title={
          <span>
            <FileTextOutlined /> 审核理赔任务
          </span>
        }
        visible={reviewTaskModalVisible}
        onCancel={resetTaskModals}
        onOk={() => form.submit()}
        width={600}
      >
        <Spin spinning={loadingVariables}>
          <Descriptions bordered column={1} style={{ marginBottom: 16 }}>
            <Descriptions.Item label="理赔编号">{claim.claimNumber}</Descriptions.Item>
            <Descriptions.Item label="申请金额">¥{claim.claimedAmount.toLocaleString()}</Descriptions.Item>
            <Descriptions.Item label="理赔类型">{claim.claimType}</Descriptions.Item>
            <Descriptions.Item label="事故描述">{claim.incidentDescription}</Descriptions.Item>
          </Descriptions>

          <Form form={form} onFinish={handleReviewTask}>
            <Form.Item 
              name="reviewResult" 
              label="审核结果" 
              rules={[{ required: true, message: '请选择审核结果' }]}
            >
              <Radio.Group>
                <Radio value="APPROVED">通过</Radio>
                <Radio value="REJECTED">拒绝</Radio>
                <Radio value="REQUIRES_INFO">需要更多信息</Radio>
              </Radio.Group>
            </Form.Item>
            <Form.Item name="reviewComments" label="审核意见" rules={[{ required: true, message: '请填写审核意见' }]}>
              <TextArea rows={4} placeholder="请填写审核意见" />
            </Form.Item>
          </Form>
        </Spin>
      </Modal>

      {/* 评估复杂度任务模态框 */}
      <Modal
        title={
          <span>
            <SolutionOutlined /> 评估复杂度
          </span>
        }
        visible={assessComplexityModalVisible}
        onCancel={resetTaskModals}
        onOk={() => form.submit()}
      >
        <Form form={form} onFinish={handleAssessComplexity}>
          <Form.Item 
            name="complexityLevel" 
            label="复杂度等级" 
            rules={[{ required: true, message: '请选择复杂度等级' }]}
          >
            <Radio.Group>
              <Radio value="LOW">低</Radio>
              <Radio value="MEDIUM">中</Radio>
              <Radio value="HIGH">高</Radio>
            </Radio.Group>
          </Form.Item>
          <Form.Item name="complexityReason" label="评估原因">
            <TextArea rows={4} placeholder="请说明评估原因（可选）" />
          </Form.Item>
        </Form>
      </Modal>

      {/* 最终审批任务模态框 */}
      <Modal
        title={
          <span>
            <CheckCircleOutlined /> 最终审批
          </span>
        }
        visible={finalApprovalModalVisible}
        onCancel={resetTaskModals}
        onOk={() => form.submit()}
        width={600}
      >
        <Descriptions bordered column={1} style={{ marginBottom: 16 }}>
          <Descriptions.Item label="理赔编号">{claim.claimNumber}</Descriptions.Item>
          <Descriptions.Item label="申请金额">¥{claim.claimedAmount.toLocaleString()}</Descriptions.Item>
          <Descriptions.Item label="理赔类型">{claim.claimType}</Descriptions.Item>
          <Descriptions.Item label="事故描述">{claim.incidentDescription}</Descriptions.Item>
        </Descriptions>

        <Form form={form} onFinish={handleFinalApproval}>
          <Form.Item 
            name="approved" 
            label="审批结果" 
            rules={[{ required: true, message: '请选择审批结果' }]}
          >
            <Radio.Group>
              <Radio value={true}>批准</Radio>
              <Radio value={false}>拒绝</Radio>
            </Radio.Group>
          </Form.Item>
          <Form.Item 
            name="approvedAmount" 
            label="批准金额" 
            rules={[{ required: true, message: '请输入批准金额' }]}
            initialValue={claim.claimedAmount}
          >
            <InputNumber 
              style={{ width: '100%' }} 
              min={0} 
              max={claim.claimedAmount}
              formatter={value => `¥ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
              parser={currencyParser}
            />
          </Form.Item>
          <Form.Item 
            name="approvalComments" 
            label="审批意见" 
            rules={[{ required: true, message: '请填写审批意见' }]}
          >
            <TextArea rows={4} placeholder="请填写审批意见" />
          </Form.Item>
        </Form>
      </Modal>

      {/* 处理支付任务模态框 */}
      <Modal
        title={
          <span>
            <DollarOutlined /> 处理支付任务
          </span>
        }
        visible={paymentTaskModalVisible}
        onCancel={resetTaskModals}
        onOk={() => form.submit()}
        width={600}
      >
        <Spin spinning={loadingVariables}>
          <Descriptions bordered column={1} style={{ marginBottom: 16 }}>
            <Descriptions.Item label="理赔编号">{claim.claimNumber}</Descriptions.Item>
            <Descriptions.Item label="批准金额">
              {claim.approvedAmount ? `¥${claim.approvedAmount.toLocaleString()}` : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="收款人">{claim.claimantName}</Descriptions.Item>
          </Descriptions>

          <Form form={form} onFinish={handlePaymentTask}>
            <Form.Item 
              name="paymentAmount" 
              label="支付金额" 
              rules={[{ required: true, message: '请输入支付金额' }]}
              initialValue={claim.approvedAmount}
            >
              <InputNumber 
                style={{ width: '100%' }} 
                min={0}
                formatter={value => `¥ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
                parser={currencyParser}
              />
            </Form.Item>
            <Form.Item name="paymentMethod" label="支付方式" rules={[{ required: true, message: '请选择支付方式' }]}>
              <Select placeholder="请选择支付方式">
                <Select.Option value="TRANSFER">银行转账</Select.Option>
                <Select.Option value="CASH">现金</Select.Option>
                <Select.Option value="CHECK">支票</Select.Option>
                <Select.Option value="ONLINE">在线支付</Select.Option>
              </Select>
            </Form.Item>
            <Form.Item name="paymentReference" label="支付参考号">
              <Input placeholder="如：PAY-20241226-001" />
            </Form.Item>
            <Form.Item name="paymentComments" label="支付备注">
              <TextArea rows={3} placeholder="请填写支付备注（可选）" />
            </Form.Item>
          </Form>
        </Spin>
      </Modal>

      {/* 结案处理任务模态框 */}
      <Modal
        title={
          <span>
            <CheckCircleOutlined /> 结案处理
          </span>
        }
        visible={finalizeTaskModalVisible}
        onCancel={resetTaskModals}
        onOk={() => form.submit()}
      >
        <Form form={form} onFinish={handleFinalizeTask}>
          <Form.Item name="closureReason" label="结案原因" rules={[{ required: true, message: '请填写结案原因' }]}>
            <TextArea rows={4} placeholder="请填写结案原因" />
          </Form.Item>
          <Form.Item name="closureNotes" label="备注">
            <TextArea rows={3} placeholder="其他需要记录的信息（可选）" />
          </Form.Item>
        </Form>
      </Modal>

      {/* 验证支付任务模态框 */}
      <Modal
        title={
          <span>
            <DollarOutlined /> 验证支付
          </span>
        }
        visible={validatePaymentModalVisible}
        onCancel={resetTaskModals}
        onOk={() => form.submit()}
        width={600}
      >
        <Spin spinning={loadingVariables}>
          <Descriptions bordered column={1} style={{ marginBottom: 16 }}>
            <Descriptions.Item label="支付金额">
              ¥{(taskVariables.amount || 0).toLocaleString()}
            </Descriptions.Item>
            <Descriptions.Item label="支付参考号">
              {taskVariables.reference || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="收款人姓名">
              {taskVariables.payeeName || claim.claimantName}
            </Descriptions.Item>
          </Descriptions>

          <Form form={form} onFinish={handleValidatePayment}>
            <Form.Item 
              name="validationResult" 
              label="验证结果" 
              rules={[{ required: true, message: '请选择验证结果' }]}
            >
              <Radio.Group>
                <Radio value="approved">批准</Radio>
                <Radio value="rejected">拒绝</Radio>
              </Radio.Group>
            </Form.Item>
            <Form.Item name="validationComment" label="验证备注">
              <TextArea rows={4} placeholder="请输入验证备注（可选）" />
            </Form.Item>
          </Form>
        </Spin>
      </Modal>

      {/* 确认支付任务模态框 */}
      <Modal
        title={
          <span>
            <DollarOutlined /> 确认支付
          </span>
        }
        visible={confirmPaymentModalVisible}
        onCancel={resetTaskModals}
        onOk={() => form.submit()}
        width={600}
      >
        <Spin spinning={loadingVariables}>
          <Descriptions bordered column={1} style={{ marginBottom: 16 }}>
            <Descriptions.Item label="交易ID">
              {taskVariables.transactionId || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="支付金额">
              ¥{(taskVariables.amount || 0).toLocaleString()}
            </Descriptions.Item>
            <Descriptions.Item label="收款人姓名">
              {taskVariables.payeeName || claim.claimantName}
            </Descriptions.Item>
            <Descriptions.Item label="支付日期">
              {taskVariables.paymentDate ? new Date(taskVariables.paymentDate).toLocaleDateString() : '-'}
            </Descriptions.Item>
          </Descriptions>

          <Form form={form} onFinish={handleConfirmPayment}>
            <Form.Item 
              name="confirmationResult" 
              label="确认结果" 
              rules={[{ required: true, message: '请选择确认结果' }]}
            >
              <Radio.Group>
                <Radio value="confirmed">确认</Radio>
                <Radio value="disputed">争议</Radio>
              </Radio.Group>
            </Form.Item>
            <Form.Item name="confirmationComment" label="确认备注">
              <TextArea rows={4} placeholder="请输入确认备注（可选）" />
            </Form.Item>
          </Form>
        </Spin>
      </Modal>

      {/* 处理争议任务模态框 */}
      <Modal
        title={
          <span>
            <CloseCircleOutlined /> 处理支付争议
          </span>
        }
        visible={handleDisputeModalVisible}
        onCancel={resetTaskModals}
        onOk={() => form.submit()}
      >
        <Form form={form} onFinish={handleDisputePayment}>
          <Form.Item 
            name="disputeResolution" 
            label="争议解决方案" 
            rules={[{ required: true, message: '请选择争议解决方案' }]}
          >
            <Radio.Group>
              <Radio value="retry">重新支付</Radio>
              <Radio value="cancel">取消支付</Radio>
              <Radio value="investigate">调查</Radio>
            </Radio.Group>
          </Form.Item>
          <Form.Item name="disputeComment" label="争议处理备注">
            <TextArea rows={4} placeholder="请输入争议处理备注（可选）" />
          </Form.Item>
        </Form>
      </Modal>

      {/* 分配模态框 */}
      <Modal
        title="分配理赔案件"
        visible={assignModalVisible}
        onCancel={() => setAssignModalVisible(false)}
        onOk={() => form.submit()}
      >
        <Form form={form} onFinish={handleAssign}>
          <Form.Item 
            name="userId" 
            label="分配给" 
            rules={[{ required: true, message: '请选择分配对象' }]}
          >
            <Select placeholder="请选择用户">
              {(users || []).map(user => (
                <Select.Option key={user.id} value={user.id}>
                  {user.fullName} ({user.department})
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
        </Form>
      </Modal>

      {/* 完成审核模态框 */}
      <Modal
        title="审核理赔申请"
        visible={reviewModalVisible}
        onCancel={() => {
          setReviewModalVisible(false);
          form.resetFields();
        }}
        onOk={() => form.submit()}
        okText="提交审核"
        cancelText="取消"
      >
        <Form 
          form={form} 
          onFinish={handleCompleteReview}
          layout="vertical"
        >
          <Form.Item 
            name="comments" 
            label="审核意见" 
            rules={[{ required: true, message: '请填写审核意见' }]}
            help="请简要说明审核结果，例如：申请材料齐全，符合理赔条件"
          >
            <TextArea 
              rows={4} 
              placeholder="请填写审核意见" 
              showCount
              maxLength={500}
            />
          </Form.Item>
          <Form.Item name="notes" label="备注">
            <TextArea 
              rows={3} 
              placeholder="其他需要记录的信息（可选）"
              showCount
              maxLength={300}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default ClaimDetail;
