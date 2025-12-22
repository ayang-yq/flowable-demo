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
  InputNumber
} from 'antd';
import { 
  ArrowLeftOutlined, 
  EditOutlined, 
  CheckCircleOutlined,
  CloseCircleOutlined,
  DollarOutlined
} from '@ant-design/icons';
import { claimApi, taskApi, userApi } from '../services/api';
import { ClaimCase, User } from '../types';

const { TabPane } = Tabs;
const { TextArea } = Input;

const ClaimDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  
  const [loading, setLoading] = useState(true);
  const [claim, setClaim] = useState<ClaimCase | null>(null);
  const [users, setUsers] = useState<User[]>([]);
  const [approveModalVisible, setApproveModalVisible] = useState(false);
  const [rejectModalVisible, setRejectModalVisible] = useState(false);
  const [paymentModalVisible, setPaymentModalVisible] = useState(false);
  const [assignModalVisible, setAssignModalVisible] = useState(false);
  
  const [form] = Form.useForm();

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
    try {
      await taskApi.getMyTasks('admin', { page: 0, size: 100 });
    } catch (error) {
      console.error('Failed to load tasks:', error);
    }
  }, []);

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
      loadTasks();
      loadUsers();
    }
  }, [id, loadClaimDetail, loadTasks, loadUsers]);

  const handleApprove = async (values: any) => {
    try {
      await claimApi.approveClaim(id!, values);
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
      await claimApi.payClaim(id!, values);
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

      {/* 详情内容 */}
      <Tabs defaultActiveKey="basic">
        <TabPane tab="基本信息" key="basic">
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
        </TabPane>

        <TabPane tab="相关文档" key="documents">
          <Card title="文档列表">
            <Table
              columns={documentColumns}
              dataSource={claim.documents || []}
              rowKey="id"
              pagination={false}
            />
          </Card>
        </TabPane>

        <TabPane tab="处理历史" key="history">
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
        </TabPane>
      </Tabs>

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
              parser={value => Number(value!.replace(/¥\s?|(,*)/g, ''))}
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
              parser={value => Number(value!.replace(/¥\s?|(,*)/g, ''))}
            />
          </Form.Item>
          <Form.Item name="paymentMethod" label="支付方式" rules={[{ required: true }]}>
            <Select>
              <Select.Option value="BANK_TRANSFER">银行转账</Select.Option>
              <Select.Option value="CASH">现金</Select.Option>
              <Select.Option value="CHECK">支票</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="comments" label="备注">
            <TextArea rows={4} />
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
    </div>
  );
};

export default ClaimDetail;
