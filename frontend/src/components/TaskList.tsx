import React, { useCallback, useEffect, useState } from 'react';
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
  Form,
  Radio,
  Input,
  Descriptions,
  Spin
} from 'antd';
import { 
  CheckOutlined, 
  UserOutlined,
  ClockCircleOutlined,
  DollarOutlined
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { taskApi, userApi } from '../services/api';
import { FlowableTask, User } from '../types';

const { TabPane } = Tabs;
const { TextArea } = Input;

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
  
  // 支付任务相关状态
  const [validationModalVisible, setValidationModalVisible] = useState(false);
  const [confirmModalVisible, setConfirmModalVisible] = useState(false);
  const [rejectedModalVisible, setRejectedModalVisible] = useState(false);
  const [disputeModalVisible, setDisputeModalVisible] = useState(false);
  const [taskVariables, setTaskVariables] = useState<Record<string, any>>({});
  const [loadingVariables, setLoadingVariables] = useState(false);
  const [selectedTaskForProcessing, setSelectedTaskForProcessing] = useState<FlowableTask | null>(null);
  
  const [form] = Form.useForm();
  const navigate = useNavigate();

  const loadMyTasks = useCallback(async () => {
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
  }, [currentPage, pageSize]);

  const loadClaimableTasks = useCallback(async () => {
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
  }, [currentPage, pageSize]);

  const loadUsers = async () => {
    try {
      const response = await userApi.getUsers({ page: 0, size: 100 });
      setUsers(response.data.content);
    } catch (error) {
      console.error('Failed to load users:', error);
    }
  };

  useEffect(() => {
    loadMyTasks();
    loadClaimableTasks();
    loadUsers();
  }, [currentPage, pageSize, activeTab, loadMyTasks, loadClaimableTasks]);

  const loadTaskVariables = async (taskId: string) => {
    try {
      setLoadingVariables(true);
      const response = await taskApi.getTaskVariables(taskId);
      setTaskVariables(response.data);
      console.log('Task variables:', response.data);
    } catch (error) {
      console.error('Failed to load task variables:', error);
      message.error('加载任务变量失败');
    } finally {
      setLoadingVariables(false);
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

  const handleCompleteTask = async (taskId: string, task?: FlowableTask) => {
    const taskToProcess = task || myTasks.find(t => t.id === taskId);
    
    if (!taskToProcess) {
      message.error('任务不存在');
      return;
    }

    // 根据任务类型处理
    switch (taskToProcess.taskDefinitionKey) {
      case 'userTask_validatePayment':
        // 支付校验任务
        setSelectedTaskForProcessing(taskToProcess);
        await loadTaskVariables(taskId);
        setValidationModalVisible(true);
        break;
      
      case 'userTask_confirmPayment':
        // 支付确认任务
        setSelectedTaskForProcessing(taskToProcess);
        await loadTaskVariables(taskId);
        setConfirmModalVisible(true);
        break;
      
      case 'userTask_paymentRejected':
        // 支付被拒绝任务
        setSelectedTaskForProcessing(taskToProcess);
        setRejectedModalVisible(true);
        break;
      
      case 'userTask_handleDispute':
        // 处理支付争议任务
        setSelectedTaskForProcessing(taskToProcess);
        setDisputeModalVisible(true);
        break;
      
      default:
        // 普通任务，直接完成
        try {
          await taskApi.completeTask(taskId);
          message.success('完成任务成功');
          loadMyTasks();
          loadClaimableTasks();
        } catch (error) {
          console.error('Failed to complete task:', error);
          message.error('完成任务失败');
        }
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

  const handleValidatePayment = async (values: any) => {
    try {
      if (selectedTaskForProcessing) {
        const variables = {
          validationResult: values.validationResult,
          validationComment: values.validationComment
        };
        
        await taskApi.completeTask(selectedTaskForProcessing.id, variables);
        message.success('支付校验成功');
        setValidationModalVisible(false);
        form.resetFields();
        loadMyTasks();
        loadClaimableTasks();
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
        setConfirmModalVisible(false);
        form.resetFields();
        loadMyTasks();
        loadClaimableTasks();
      }
    } catch (error) {
      console.error('Failed to confirm payment:', error);
      message.error('支付确认失败');
    }
  };

  const handleRejectedPayment = async (values: any) => {
    try {
      if (selectedTaskForProcessing) {
        const variables = {
          rejectionReason: values.rejectionReason,
          rejectionComment: values.rejectionComment
        };
        
        await taskApi.completeTask(selectedTaskForProcessing.id, variables);
        message.success('处理支付拒绝成功');
        setRejectedModalVisible(false);
        form.resetFields();
        loadMyTasks();
        loadClaimableTasks();
      }
    } catch (error) {
      console.error('Failed to handle rejected payment:', error);
      message.error('处理支付拒绝失败');
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
        message.success('处理支付争议成功');
        setDisputeModalVisible(false);
        form.resetFields();
        loadMyTasks();
        loadClaimableTasks();
      }
    } catch (error) {
      console.error('Failed to handle dispute:', error);
      message.error('处理支付争议失败');
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
                onClick={() => handleCompleteTask(record.id, record)}
              >
                {record.taskDefinitionKey?.includes('Payment') ? '处理' : '完成'}
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
          const formEl = document.querySelector('#assign-task-form') as HTMLFormElement;
          if (formEl) {
            formEl.requestSubmit();
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

      {/* 支付校验模态框 */}
      <Modal
        title={
          <span>
            <DollarOutlined /> 支付校验
          </span>
        }
        visible={validationModalVisible}
        onCancel={() => {
          setValidationModalVisible(false);
          form.resetFields();
          setTaskVariables({});
          setSelectedTaskForProcessing(null);
        }}
        onOk={() => {
          form.validateFields()
            .then((values) => {
              handleValidatePayment(values);
            })
            .catch((error) => {
              console.error('表单验证失败:', error);
            });
        }}
        okText="提交"
        cancelText="取消"
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
              {taskVariables.payeeName || '-'}
            </Descriptions.Item>
          </Descriptions>

          <Form form={form} onFinish={handleValidatePayment} layout="vertical">
            <Form.Item 
              name="validationResult" 
              label="校验结果" 
              rules={[{ required: true, message: '请选择校验结果' }]}
            >
              <Radio.Group>
                <Radio value="approved">批准</Radio>
                <Radio value="rejected">拒绝</Radio>
              </Radio.Group>
            </Form.Item>
            <Form.Item name="validationComment" label="校验备注">
              <TextArea rows={4} placeholder="请输入校验备注（可选）" />
            </Form.Item>
          </Form>
        </Spin>
      </Modal>

      {/* 支付确认模态框 */}
      <Modal
        title={
          <span>
            <DollarOutlined /> 支付确认
          </span>
        }
        visible={confirmModalVisible}
        onCancel={() => {
          setConfirmModalVisible(false);
          form.resetFields();
          setTaskVariables({});
          setSelectedTaskForProcessing(null);
        }}
        onOk={() => {
          form.validateFields()
            .then((values) => {
              handleConfirmPayment(values);
            })
            .catch((error) => {
              console.error('表单验证失败:', error);
            });
        }}
        okText="提交"
        cancelText="取消"
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
              {taskVariables.payeeName || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="支付日期">
              {taskVariables.paymentDate ? new Date(taskVariables.paymentDate).toLocaleDateString() : '-'}
            </Descriptions.Item>
          </Descriptions>

          <Form form={form} onFinish={handleConfirmPayment} layout="vertical">
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

      {/* 支付被拒绝对话框 */}
      <Modal
        title={
          <span>
            <DollarOutlined /> 支付被拒绝
          </span>
        }
        visible={rejectedModalVisible}
        onCancel={() => {
          setRejectedModalVisible(false);
          form.resetFields();
          setSelectedTaskForProcessing(null);
        }}
        onOk={() => {
          form.validateFields()
            .then((values) => {
              handleRejectedPayment(values);
            })
            .catch((error) => {
              console.error('表单验证失败:', error);
            });
        }}
        okText="提交"
        cancelText="取消"
      >
        <Form form={form} onFinish={handleRejectedPayment} layout="vertical">
          <Form.Item 
            name="rejectionReason" 
            label="拒绝原因" 
            rules={[{ required: true, message: '请输入拒绝原因' }]}
          >
            <TextArea rows={4} placeholder="请输入拒绝原因" />
          </Form.Item>
          <Form.Item name="rejectionComment" label="拒绝备注">
            <TextArea rows={3} placeholder="请输入拒绝备注（可选）" />
          </Form.Item>
        </Form>
      </Modal>

      {/* 支付争议处理对话框 */}
      <Modal
        title={
          <span>
            <DollarOutlined /> 处理支付争议
          </span>
        }
        visible={disputeModalVisible}
        onCancel={() => {
          setDisputeModalVisible(false);
          form.resetFields();
          setSelectedTaskForProcessing(null);
        }}
        onOk={() => {
          form.validateFields()
            .then((values) => {
              handleDisputePayment(values);
            })
            .catch((error) => {
              console.error('表单验证失败:', error);
            });
        }}
        okText="提交"
        cancelText="取消"
      >
        <Form form={form} onFinish={handleDisputePayment} layout="vertical">
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
    </Card>
  );
};

export default TaskList;
