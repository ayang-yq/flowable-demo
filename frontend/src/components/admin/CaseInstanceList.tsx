import React, { useEffect, useState } from 'react';
import { Table, Button, Input, Select, Space, Tag, message, Modal } from 'antd';
import { SearchOutlined, EyeOutlined, StopOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { caseApi, CaseInstanceDTO, PageResponse } from '../../services/adminApi';
import type { ColumnsType } from 'antd/es/table';

const { Option } = Select;

const CaseInstanceList: React.FC = () => {
    const navigate = useNavigate();
    const [data, setData] = useState<CaseInstanceDTO[]>([]);
    const [loading, setLoading] = useState(false);
    const [pagination, setPagination] = useState({
        current: 1,
        pageSize: 20,
        total: 0,
    });

    // 筛选条件
    const [filters, setFilters] = useState({
        caseDefinitionKey: '',
        businessKey: '',
        state: '',
    });

    useEffect(() => {
        loadData();
    }, [pagination.current, pagination.pageSize]);

    const loadData = async () => {
        try {
            setLoading(true);
            const response = await caseApi.queryCaseInstances({
                ...filters,
                page: pagination.current - 1,
                size: pagination.pageSize,
            });

            const pageData: PageResponse<CaseInstanceDTO> = response.data;
            setData(pageData.content);
            setPagination({
                ...pagination,
                total: pageData.totalElements,
            });
        } catch (error: any) {
            message.error('Failed to load case instances: ' + error.message);
        } finally {
            setLoading(false);
        }
    };

    const handleSearch = () => {
        setPagination({ ...pagination, current: 1 });
        loadData();
    };

    const handleTerminate = (record: CaseInstanceDTO) => {
        Modal.confirm({
            title: '确认终止 Case',
            content: `确定要终止 Case "${record.businessKey}" 吗?此操作不可撤销。`,
            okText: '确认',
            okType: 'danger',
            cancelText: '取消',
            onOk: async () => {
                try {
                    await caseApi.terminateCase(record.id, '管理员手动终止');
                    message.success('Case 已终止');
                    loadData();
                } catch (error: any) {
                    message.error('Failed to terminate case: ' + error.message);
                }
            },
        });
    };

    const getStateTag = (state: string) => {
        const stateConfig: Record<string, { color: string; text: string }> = {
            ACTIVE: { color: 'blue', text: '活动中' },
            COMPLETED: { color: 'green', text: '已完成' },
            TERMINATED: { color: 'red', text: '已终止' },
            SUSPENDED: { color: 'orange', text: '已挂起' },
        };

        const config = stateConfig[state] || { color: 'default', text: state };
        return <Tag color={config.color}>{config.text}</Tag>;
    };

    const columns: ColumnsType<CaseInstanceDTO> = [
        {
            title: 'Business Key',
            dataIndex: 'businessKey',
            key: 'businessKey',
            width: 150,
        },
        {
            title: 'Case Definition',
            dataIndex: 'caseDefinitionName',
            key: 'caseDefinitionName',
            width: 200,
        },
        {
            title: 'Version',
            dataIndex: 'caseDefinitionVersion',
            key: 'caseDefinitionVersion',
            width: 80,
            align: 'center',
        },
        {
            title: '状态',
            dataIndex: 'state',
            key: 'state',
            width: 100,
            render: (state: string) => getStateTag(state),
        },
        {
            title: '活动 Plan Items',
            dataIndex: 'activePlanItems',
            key: 'activePlanItems',
            width: 120,
            align: 'center',
            render: (count: number) => <Tag color="blue">{count}</Tag>,
        },
        {
            title: '已完成 Plan Items',
            dataIndex: 'completedPlanItems',
            key: 'completedPlanItems',
            width: 140,
            align: 'center',
            render: (count: number) => <Tag color="green">{count}</Tag>,
        },
        {
            title: '开始时间',
            dataIndex: 'startTime',
            key: 'startTime',
            width: 180,
            render: (time: string) => new Date(time).toLocaleString('zh-CN'),
        },
        {
            title: '启动人',
            dataIndex: 'startUserId',
            key: 'startUserId',
            width: 120,
        },
        {
            title: '操作',
            key: 'actions',
            width: 150,
            fixed: 'right',
            render: (_, record) => (
                <Space size="small">
                    <Button
                        type="link"
                        icon={<EyeOutlined />}
                        onClick={() => navigate(`/admin/cases/${record.id}`)}
                    >
                        详情
                    </Button>
                    {record.state === 'ACTIVE' && (
                        <Button
                            type="link"
                            danger
                            icon={<StopOutlined />}
                            onClick={() => handleTerminate(record)}
                        >
                            终止
                        </Button>
                    )}
                </Space>
            ),
        },
    ];

    return (
        <div style={{ padding: '24px' }}>
            <h1>Case 实例管理</h1>

            {/* 筛选条件 */}
            <Space style={{ marginBottom: 16 }} wrap>
                <Input
                    placeholder="Case Definition Key"
                    value={filters.caseDefinitionKey}
                    onChange={(e) =>
                        setFilters({ ...filters, caseDefinitionKey: e.target.value })
                    }
                    style={{ width: 200 }}
                />
                <Input
                    placeholder="Business Key"
                    value={filters.businessKey}
                    onChange={(e) =>
                        setFilters({ ...filters, businessKey: e.target.value })
                    }
                    style={{ width: 200 }}
                />
                <Select
                    placeholder="状态"
                    value={filters.state || undefined}
                    onChange={(value) => setFilters({ ...filters, state: value || '' })}
                    style={{ width: 150 }}
                    allowClear
                >
                    <Option value="ACTIVE">活动中</Option>
                    <Option value="COMPLETED">已完成</Option>
                    <Option value="TERMINATED">已终止</Option>
                    <Option value="SUSPENDED">已挂起</Option>
                </Select>
                <Button
                    type="primary"
                    icon={<SearchOutlined />}
                    onClick={handleSearch}
                >
                    搜索
                </Button>
            </Space>

            {/* 数据表格 */}
            <Table
                columns={columns}
                dataSource={data}
                rowKey="id"
                loading={loading}
                pagination={{
                    ...pagination,
                    showSizeChanger: true,
                    showTotal: (total) => `共 ${total} 条`,
                    onChange: (page, pageSize) => {
                        setPagination({ ...pagination, current: page, pageSize });
                    },
                }}
                scroll={{ x: 1400 }}
            />
        </div>
    );
};

export default CaseInstanceList;
