import React, { useState, useEffect } from 'react';
import { 
  Form, 
  Input, 
  Select, 
  DatePicker, 
  InputNumber, 
  Button, 
  Card, 
  message,
  Row,
  Col,
  Space,
  Spin
} from 'antd';
import { useNavigate, useParams } from 'react-router-dom';
import { claimApi, policyApi } from '../services/api';
import { InsurancePolicy, ClaimCase } from '../types';
import dayjs from 'dayjs';

const { Option } = Select;
const { TextArea } = Input;

const ClaimForm: React.FC = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [initialLoading, setInitialLoading] = useState(false);
  const [policies, setPolicies] = useState<InsurancePolicy[]>([]);
  const [selectedPolicy, setSelectedPolicy] = useState<InsurancePolicy | null>(null);
  const [isEditMode, setIsEditMode] = useState(false);
  const [claimId, setClaimId] = useState<string | null>(null);
  const navigate = useNavigate();
  const { id } = useParams<{ id?: string }>();

  useEffect(() => {
    loadPolicies();
    
    // 检查是否为编辑模式
    if (id) {
      setIsEditMode(true);
      setClaimId(id);
      loadClaimData(id);
    }
  }, [id]);

  const loadPolicies = async () => {
    try {
      const response = await policyApi.getPolicies({ page: 0, size: 1000 });
      setPolicies(response.data.content);
    } catch (error) {
      console.error('Failed to load policies:', error);
      message.error('加载保单列表失败');
    }
  };

  const loadClaimData = async (id: string) => {
    try {
      setInitialLoading(true);
      const response = await claimApi.getClaim(id);
      const claim = response.data;
      
      // 设置表单数据
      form.setFieldsValue({
        policyId: claim.policy?.id,
        claimantName: claim.claimantName,
        claimantPhone: claim.claimantPhone,
        claimantEmail: claim.claimantEmail,
        incidentDate: claim.incidentDate ? dayjs(claim.incidentDate) : undefined,
        incidentLocation: claim.incidentLocation,
        incidentDescription: claim.incidentDescription,
        claimedAmount: claim.claimedAmount,
        claimType: claim.claimType,
        severity: claim.severity
      });

      // 设置选中的保单
      if (claim.policy) {
        setSelectedPolicy(claim.policy);
      }
    } catch (error) {
      console.error('Failed to load claim data:', error);
      message.error('加载理赔案件数据失败');
      navigate('/claims');
    } finally {
      setInitialLoading(false);
    }
  };

  const handlePolicyChange = (policyId: string) => {
    const policy = policies.find(p => p.id === policyId);
    setSelectedPolicy(policy || null);
    
    // 预填充投保人信息 (只填充name，因为InsurancePolicy类型中没有phone和email)
    if (policy) {
      form.setFieldsValue({
        claimantName: policy.policyholderName
      });
    }
  };

  const handleSubmit = async (values: any) => {
    try {
      setLoading(true);
      
      // 验证是否选择了保单
      if (!values.policyId) {
        message.error('请选择保单');
        return;
      }

      // 准备提交数据 - 使用正确的前端类型
      const claimData = {
        policyId: values.policyId,
        claimantName: values.claimantName,
        claimantPhone: values.claimantPhone,
        claimantEmail: values.claimantEmail,
        incidentDate: values.incidentDate ? values.incidentDate.format('YYYY-MM-DD') : undefined,
        incidentLocation: values.incidentLocation,
        incidentDescription: values.incidentDescription,
        claimedAmount: values.claimedAmount,
        claimType: values.claimType,
        severity: values.severity || 'MEDIUM'
      };

      let response;
      if (isEditMode && claimId) {
        // 编辑模式：更新现有理赔案件
        response = await claimApi.updateClaim(claimId, claimData);
        message.success('理赔案件更新成功');
      } else {
        // 新建模式：创建新理赔案件
        response = await claimApi.createClaim(claimData);
        message.success('理赔案件创建成功');
      }
      
      navigate(`/claims/${response.data.id}`);
    } catch (error: any) {
      console.error(`Failed to ${isEditMode ? 'update' : 'create'} claim:`, error);
      const errorMessage = error.response?.data?.message || error.message || `${isEditMode ? '更新' : '创建'}理赔案件失败`;
      message.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    navigate('/claims');
  };

  return (
    <Card title={isEditMode ? "编辑理赔案件" : "新建理赔案件"} className="claim-form">
      <Spin spinning={initialLoading}>
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          initialValues={{
            severity: 'MEDIUM'
          }}
        >
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              label="保单"
              name="policyId"
              rules={[{ required: true, message: '请选择保单' }]}
            >
              <Select
                placeholder="请选择保单"
                showSearch
                filterOption={(input, option) =>
                  String(option?.children || '').toLowerCase().indexOf(input.toLowerCase()) >= 0
                }
                onChange={handlePolicyChange}
              >
                {policies.map(policy => (
                  <Option key={policy.id} value={policy.id}>
                    {policy.policyNumber} - {policy.policyholderName}
                  </Option>
                ))}
              </Select>
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              label="理赔类型"
              name="claimType"
              rules={[{ required: true, message: '请选择理赔类型' }]}
            >
              <Select placeholder="请选择理赔类型">
                <Option value="ACCIDENT">意外事故</Option>
                <Option value="ILLNESS">疾病</Option>
                <Option value="PROPERTY">财产损失</Option>
                <Option value="LIABILITY">责任险</Option>
                <Option value="OTHER">其他</Option>
              </Select>
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={16}>
          <Col span={8}>
            <Form.Item
              label="索赔人姓名"
              name="claimantName"
              rules={[{ required: true, message: '请输入索赔人姓名' }]}
            >
              <Input placeholder="请输入索赔人姓名" />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item
              label="索赔人电话"
              name="claimantPhone"
              rules={[{ required: true, message: '请输入索赔人电话' }]}
            >
              <Input placeholder="请输入索赔人电话" />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item
              label="索赔人邮箱"
              name="claimantEmail"
              rules={[
                { required: true, message: '请输入索赔人邮箱' },
                { type: 'email', message: '请输入有效的邮箱地址' }
              ]}
            >
              <Input placeholder="请输入索赔人邮箱" />
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={16}>
          <Col span={8}>
            <Form.Item
              label="出险时间"
              name="incidentDate"
              rules={[{ required: true, message: '请选择出险时间' }]}
            >
              <DatePicker 
                style={{ width: '100%' }}
                placeholder="请选择出险时间"
              />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item
              label="出险地点"
              name="incidentLocation"
              rules={[{ required: true, message: '请输入出险地点' }]}
            >
              <Input placeholder="请输入出险地点" />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item
              label="严重程度"
              name="severity"
              rules={[{ required: true, message: '请选择严重程度' }]}
            >
              <Select placeholder="请选择严重程度">
                <Option value="LOW">低</Option>
                <Option value="MEDIUM">中</Option>
                <Option value="HIGH">高</Option>
                <Option value="CRITICAL">紧急</Option>
              </Select>
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              label="索赔金额"
              name="claimedAmount"
              rules={[
                { required: true, message: '请输入索赔金额' },
                { type: 'number', min: 0, message: '索赔金额必须大于0' }
              ]}
            >
              <InputNumber
                style={{ width: '100%' }}
                placeholder="请输入索赔金额"
                formatter={value => `¥ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
                parser={(value: string | undefined) => parseFloat(String(value || '').replace(/¥\s?|(,*)/g, '')) || 0}
                min={0}
                precision={2}
              />
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={16}>
          <Col span={24}>
            <Form.Item
              label="事故描述"
              name="incidentDescription"
              rules={[{ required: true, message: '请输入事故描述' }]}
            >
              <TextArea
                rows={4}
                placeholder="请详细描述事故经过、损失情况等"
              />
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={16}>
          <Col span={24}>
            <Form.Item>
              <Space>
                <Button type="primary" htmlType="submit" loading={loading}>
                  {isEditMode ? "更新理赔案件" : "提交理赔申请"}
                </Button>
                <Button onClick={handleCancel}>
                  取消
                </Button>
              </Space>
            </Form.Item>
          </Col>
        </Row>
        </Form>
      </Spin>
    </Card>
  );
};

export default ClaimForm;
