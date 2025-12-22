import React, { useState, useEffect } from 'react';
import { 
  Table, 
  Button, 
  Space, 
  Input, 
  Select, 
  Card, 
  Tag, 
  Modal, 
  message,
  Popconfirm,
  Row,
  Col
} from 'antd';
import { 
  PlusOutlined, 
  SearchOutlined, 
  EditOutlined, 
  DeleteOutlined,
  EyeOutlined,
  ExportOutlined
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { claimApi, policyApi } from '../services/api';
import { ClaimCase, InsurancePolicy, PaginationParams } from '../types';

const { Search } = Input;
const { Option } = Select;

const ClaimList: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [claims, setClaims] = useState<ClaimCase[]>([]);
  const [policies, setPolicies] = useState<InsurancePolicy[]>([]);
  const [total, setTotal] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [severityFilter, setSeverityFilter] = useState<string>('');

  const navigate = useNavigate();

  useEffect(() => {
    loadClaims();
    loadPolicies();
  }, [currentPage, pageSize, searchKeyword, statusFilter, severityFilter]);

  const loadClaims = async () => {
    try {
      setLoading(true);
      const params: PaginationParams = {
        page: currentPage - 1,
        size: pageSize,
        sort: 'createdAt',
        direction: 'DESC'
      };

      let response;
      if (searchKeyword) {
        response = await claimApi.searchClaims(searchKeyword, params);
      } else {
        response = await claimApi.getClaims(params);
      }

      let filteredClaims = response.data.content;
      
      // 应用状态过滤
      if (statusFilter) {
        filteredClaims = filteredClaims.filter(claim => claim.status === statusFilter);
      }
      
      // 应用严重程度过滤
      if (severityFilter) {
        filteredClaims = filteredClaims.filter(claim => claim.severity === severityFilter);
      }

      setClaims(filteredClaims);
      setTotal(response.data.totalElements);
    } catch (error) {
      console.error('Failed to load claims:', error);
      message.error('加载理赔案件失败');
    } finally {
      setLoading(false);
    }
  };

  const loadPolicies = async () => {
    try {
      const response = await policyApi.getPolicies({ page: 0, size: 1000 });
      setPolicies(response.data.content);
    } catch (error) {
      console.error('Failed to load policies:', error);
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await claimApi.deleteClaim(id);
      message.success('删除成功');
      loadClaims();
    } catch (error) {
      console.error('Failed to delete claim:', error);
      message.error('删除失败');
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

  const handleSeverityChange = (value: string) => {
    setSeverityFilter(value);
    setCurrentPage(1);
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

  const columns = [
    {
      title: '理赔编号',
      dataIndex: 'claimNumber',
      key: 'claimNumber',
      render: (text: string) => <span className="claim-number">{text}</span>
    },
    {
      title: '保单号',
      dataIndex: 'policy',
      key: 'policyNumber',
      render: (policy: any) => policy?.policyNumber || '-'
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
      title: '严重程度',
      dataIndex: 'severity',
      key: 'severity',
      render: (severity: string) => (
        <Tag color={getSeverityColor(severity)}>
          {getSeverityText(severity)}
        </Tag>
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
      title: '申请金额',
      dataIndex: 'claimedAmount',
      key: 'claimedAmount',
      render: (amount: number) => (
        <span className="amount-display">¥{amount.toLocaleString()}</span>
      )
    },
    {
      title: '已批准金额',
      dataIndex: 'approvedAmount',
      key: 'approvedAmount',
      render: (amount?: number) => amount ? (
        <span className="amount-display">¥{amount.toLocaleString()}</span>
      ) : '-'
    },
    {
      title: '分配给',
      dataIndex: 'assignedToName',
      key: 'assignedTo',
      render: (assignedToName: string) => assignedToName || '-'
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
      render: (text: any, record: ClaimCase) => (
        <Space size="middle">
          <Button
            type="link"
            icon={<EyeOutlined />}
            onClick={() => navigate(`/claims/${record.id}`)}
          >
            查看
          </Button>
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => navigate(`/claims/${record.id}/edit`)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确定要删除这个理赔案件吗？"
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
            onClick={() => navigate('/claims/new')}
          >
            新建理赔
          </Button>
          <Button icon={<ExportOutlined />}>
            导出
          </Button>
        </div>
        <div className="table-toolbar-right">
          <Space>
            <Search
              placeholder="搜索理赔编号、投保人"
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
              <Option value="DRAFT">草稿</Option>
              <Option value="SUBMITTED">已提交</Option>
              <Option value="PENDING">待处理</Option>
              <Option value="UNDER_REVIEW">审核中</Option>
              <Option value="APPROVED">已批准</Option>
              <Option value="REJECTED">已拒绝</Option>
              <Option value="PAID">已支付</Option>
              <Option value="CLOSED">已关闭</Option>
            </Select>
            <Select
              placeholder="严重程度"
              allowClear
              style={{ width: 120 }}
              onChange={handleSeverityChange}
            >
              <Option value="HIGH">高</Option>
              <Option value="MEDIUM">中</Option>
              <Option value="LOW">低</Option>
            </Select>
          </Space>
        </div>
      </div>

      {/* 表格 */}
      <Table
        columns={columns}
        dataSource={claims}
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
        scroll={{ x: 1200 }}
      />
    </Card>
  );
};

export default ClaimList;
