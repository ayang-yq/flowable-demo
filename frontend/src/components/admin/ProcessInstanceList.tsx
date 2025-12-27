import React, { useCallback, useEffect, useState } from 'react';
import {
    Table,
    Button,
    Tag,
    Space,
    Card,
    Alert,
    Input,
    message,
    Popconfirm,
} from 'antd';
import {
    ReloadOutlined,
    EyeOutlined,
    StopOutlined,
    PauseCircleOutlined,
    PlayCircleOutlined,
    RocketOutlined,
    CheckCircleOutlined,
    CloseCircleOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { processApi, ProcessInstanceDTO } from '../../services/adminApi';

const ProcessInstanceList: React.FC = () => {
    const navigate = useNavigate();
    const [processes, setProcesses] = useState<ProcessInstanceDTO[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [filterKey, setFilterKey] = useState<string>('');
    const [filterBusinessKey, setFilterBusinessKey] = useState<string>('');

    const loadProcesses = useCallback(async () => {
        try {
            setLoading(true);
            setError(null);
            const response = await processApi.queryProcessInstances({
                processDefinitionKey: filterKey || undefined,
                businessKey: filterBusinessKey || undefined,
                page: 0,
                size: 100,
            });
            setProcesses(response.data.content);
        } catch (err: any) {
            setError(err.message || 'Failed to load process instances');
        } finally {
            setLoading(false);
        }
    }, [filterKey, filterBusinessKey]);

    useEffect(() => {
        loadProcesses();
    }, [loadProcesses]);

    const handleViewDetail = (processInstanceId: string) => {
        navigate(`/admin/processes/${processInstanceId}`);
    };

    const handleTerminate = async (processInstanceId: string, reason?: string) => {
        try {
            await processApi.terminateProcess(processInstanceId, reason);
            message.success('Process terminated successfully');
            loadProcesses();
        } catch (err: any) {
            message.error(`Failed to terminate process: ${err.message}`);
        }
    };

    const handleSuspend = async (processInstanceId: string) => {
        try {
            await processApi.suspendProcess(processInstanceId);
            message.success('Process suspended successfully');
            loadProcesses();
        } catch (err: any) {
            message.error(`Failed to suspend process: ${err.message}`);
        }
    };

    const handleResume = async (processInstanceId: string) => {
        try {
            await processApi.resumeProcess(processInstanceId);
            message.success('Process resumed successfully');
            loadProcesses();
        } catch (err: any) {
            message.error(`Failed to resume process: ${err.message}`);
        }
    };

    const getStateTag = (state: string) => {
        const stateConfig: Record<string, { color: string; icon: React.ReactNode }> = {
            ACTIVE: { color: 'blue', icon: <RocketOutlined /> },
            SUSPENDED: { color: 'orange', icon: <PauseCircleOutlined /> },
            COMPLETED: { color: 'green', icon: <CheckCircleOutlined /> },
            TERMINATED: { color: 'red', icon: <CloseCircleOutlined /> },
        };
        const config = stateConfig[state] || { color: 'default', icon: null };
        return (
            <Tag color={config.color} icon={config.icon}>
                {state}
            </Tag>
        );
    };

    const columns = [
        {
            title: 'Process Instance ID',
            dataIndex: 'id',
            key: 'id',
            render: (text: string) => <code>{text.substring(0, 12)}...</code>,
        },
        {
            title: '流程定义 Key',
            dataIndex: 'processDefinitionKey',
            key: 'processDefinitionKey',
            render: (text: string) => <code>{text}</code>,
        },
        {
            title: '流程名称',
            dataIndex: 'processDefinitionName',
            key: 'processDefinitionName',
        },
        {
            title: '版本',
            dataIndex: 'processDefinitionVersion',
            key: 'processDefinitionVersion',
            render: (version: number) => <Tag>v{version}</Tag>,
        },
        {
            title: 'Business Key',
            dataIndex: 'businessKey',
            key: 'businessKey',
            render: (text: string) => text || '-',
        },
        {
            title: '状态',
            dataIndex: 'state',
            key: 'state',
            render: (state: string) => getStateTag(state),
        },
        {
            title: '开始时间',
            dataIndex: 'startTime',
            key: 'startTime',
            render: (date: string) => new Date(date).toLocaleString(),
        },
        {
            title: '结束时间',
            dataIndex: 'endTime',
            key: 'endTime',
            render: (date: string) => (date ? new Date(date).toLocaleString() : '-'),
        },
        {
            title: '发起人',
            dataIndex: 'startUserId',
            key: 'startUserId',
        },
        {
            title: '操作',
            key: 'action',
            render: (_: any, record: ProcessInstanceDTO) => (
                <Space>
                    <Button
                        type="link"
                        icon={<EyeOutlined />}
                        onClick={() => handleViewDetail(record.id)}
                    >
                        详情
                    </Button>
                    {record.state === 'ACTIVE' && (
                        <Popconfirm
                            title="确认挂起?"
                            description="确定要挂起这个流程实例吗?"
                            onConfirm={() => handleSuspend(record.id)}
                            okText="确认"
                            cancelText="取消"
                        >
                            <Button
                                type="link"
                                danger
                                icon={<PauseCircleOutlined />}
                            >
                                挂起
                            </Button>
                        </Popconfirm>
                    )}
                    {record.state === 'SUSPENDED' && (
                        <Button
                            type="link"
                            icon={<PlayCircleOutlined />}
                            onClick={() => handleResume(record.id)}
                        >
                            恢复
                        </Button>
                    )}
                    {(record.state === 'ACTIVE' || record.state === 'SUSPENDED') && (
                        <Popconfirm
                            title="确认终止?"
                            description="确定要终止这个流程实例吗?此操作不可逆!"
                            onConfirm={() => handleTerminate(record.id, 'Terminated by admin')}
                            okText="确认"
                            cancelText="取消"
                        >
                            <Button
                                type="link"
                                danger
                                icon={<StopOutlined />}
                            >
                                终止
                            </Button>
                        </Popconfirm>
                    )}
                </Space>
            ),
        },
    ];

    return (
        <div style={{ padding: '24px' }}>
            <Card
                title={
                    <Space>
                        <RocketOutlined />
                        Process 实例管理
                    </Space>
                }
                extra={
                    <Button icon={<ReloadOutlined />} onClick={loadProcesses}>
                        刷新
                    </Button>
                }
            >
                <Space style={{ marginBottom: 16 }}>
                    <Input
                        placeholder="流程定义 Key"
                        value={filterKey}
                        onChange={(e) => setFilterKey(e.target.value)}
                        style={{ width: 200 }}
                        allowClear
                    />
                    <Input
                        placeholder="Business Key"
                        value={filterBusinessKey}
                        onChange={(e) => setFilterBusinessKey(e.target.value)}
                        style={{ width: 200 }}
                        allowClear
                    />
                    <Button type="primary" onClick={loadProcesses}>
                        查询
                    </Button>
                </Space>

                {error && (
                    <Alert
                        message="Error"
                        description={error}
                        type="error"
                        showIcon
                        closable
                        style={{ marginBottom: 16 }}
                        onClose={() => setError(null)}
                    />
                )}

                <Table
                    columns={columns}
                    dataSource={processes}
                    rowKey="id"
                    loading={loading}
                    pagination={{
                        pageSize: 20,
                        showSizeChanger: true,
                        showTotal: (total) => `共 ${total} 个流程实例`,
                    }}
                />
            </Card>
        </div>
    );
};

export default ProcessInstanceList;
