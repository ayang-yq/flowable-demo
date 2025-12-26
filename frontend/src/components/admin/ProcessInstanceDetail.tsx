import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
    Card,
    Descriptions,
    Button,
    Space,
    Spin,
    Alert,
    Tag,
    Modal,
    message,
    Popconfirm,
    Divider,
    Table,
} from 'antd';
import {
    ArrowLeftOutlined,
    ReloadOutlined,
    StopOutlined,
    PauseCircleOutlined,
    PlayCircleOutlined,
    RocketOutlined,
    CheckCircleOutlined,
    CloseCircleOutlined,
} from '@ant-design/icons';
import { processApi, ProcessInstanceDTO, ProcessDiagramDTO, ActivityInfo } from '../../services/adminApi';

const ProcessInstanceDetail: React.FC = () => {
    const { processInstanceId } = useParams<{ processInstanceId: string }>();
    const navigate = useNavigate();
    const [process, setProcess] = useState<ProcessInstanceDTO | null>(null);
    const [diagram, setDiagram] = useState<ProcessDiagramDTO | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [diagramModalVisible, setDiagramModalVisible] = useState(false);

    useEffect(() => {
        if (processInstanceId) {
            loadProcessDetail();
            loadProcessDiagram();
        }
    }, [processInstanceId]);

    const loadProcessDetail = async () => {
        if (!processInstanceId) return;
        try {
            setLoading(true);
            setError(null);
            const response = await processApi.getProcessInstanceDetail(processInstanceId);
            setProcess(response.data);
        } catch (err: any) {
            setError(err.message || 'Failed to load process instance detail');
        } finally {
            setLoading(false);
        }
    };

    const loadProcessDiagram = async () => {
        if (!processInstanceId) return;
        try {
            const response = await processApi.getProcessDiagram(processInstanceId);
            setDiagram(response.data);
        } catch (err: any) {
            console.error('Failed to load process diagram:', err);
        }
    };

    const handleTerminate = async (reason?: string) => {
        if (!processInstanceId) return;
        try {
            await processApi.terminateProcess(processInstanceId, reason);
            message.success('Process terminated successfully');
            loadProcessDetail();
        } catch (err: any) {
            message.error(`Failed to terminate process: ${err.message}`);
        }
    };

    const handleSuspend = async () => {
        if (!processInstanceId) return;
        try {
            await processApi.suspendProcess(processInstanceId);
            message.success('Process suspended successfully');
            loadProcessDetail();
        } catch (err: any) {
            message.error(`Failed to suspend process: ${err.message}`);
        }
    };

    const handleResume = async () => {
        if (!processInstanceId) return;
        try {
            await processApi.resumeProcess(processInstanceId);
            message.success('Process resumed successfully');
            loadProcessDetail();
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
            <Tag color={config.color} icon={config.icon} style={{ fontSize: 16 }}>
                {state}
            </Tag>
        );
    };

    const activityColumns = [
        {
            title: 'Activity ID',
            dataIndex: 'activityId',
            key: 'activityId',
            render: (text: string) => <code>{text}</code>,
        },
        {
            title: 'Activity 名称',
            dataIndex: 'activityName',
            key: 'activityName',
        },
        {
            title: '类型',
            dataIndex: 'activityType',
            key: 'activityType',
            render: (type: string) => <Tag>{type}</Tag>,
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
            title: '处理人',
            dataIndex: 'assignee',
            key: 'assignee',
            render: (text: string) => text || '-',
        },
        {
            title: '耗时',
            dataIndex: 'durationInMillis',
            key: 'durationInMillis',
            render: (ms: number) => {
                if (!ms) return '-';
                const seconds = Math.floor(ms / 1000);
                if (seconds < 60) return `${seconds}s`;
                const minutes = Math.floor(seconds / 60);
                if (minutes < 60) return `${minutes}m ${seconds % 60}s`;
                const hours = Math.floor(minutes / 60);
                return `${hours}h ${minutes % 60}m`;
            },
        },
    ];

    if (loading) {
        return (
            <div style={{ textAlign: 'center', padding: '50px' }}>
                <Spin size="large" />
            </div>
        );
    }

    if (error) {
        return (
            <Alert
                message="Error"
                description={error}
                type="error"
                showIcon
                action={
                    <Button size="small" onClick={() => navigate(-1)}>
                        返回
                    </Button>
                }
                style={{ margin: '20px' }}
            />
        );
    }

    if (!process) {
        return null;
    }

    return (
        <div style={{ padding: '24px' }}>
            <Space style={{ marginBottom: 16 }}>
                <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/admin/processes')}>
                    返回列表
                </Button>
                <Button icon={<ReloadOutlined />} onClick={() => { loadProcessDetail(); loadProcessDiagram(); }}>
                    刷新
                </Button>
                {process.state === 'ACTIVE' && (
                    <Popconfirm
                        title="确认挂起?"
                        description="确定要挂起这个流程实例吗?"
                        onConfirm={handleSuspend}
                        okText="确认"
                        cancelText="取消"
                    >
                        <Button danger icon={<PauseCircleOutlined />}>
                            挂起
                        </Button>
                    </Popconfirm>
                )}
                {process.state === 'SUSPENDED' && (
                    <Button icon={<PlayCircleOutlined />} onClick={handleResume}>
                        恢复
                    </Button>
                )}
                {(process.state === 'ACTIVE' || process.state === 'SUSPENDED') && (
                    <Popconfirm
                        title="确认终止?"
                        description="确定要终止这个流程实例吗?此操作不可逆!"
                        onConfirm={() => handleTerminate('Terminated by admin')}
                        okText="确认"
                        cancelText="取消"
                    >
                        <Button danger icon={<StopOutlined />}>
                            终止
                        </Button>
                    </Popconfirm>
                )}
                {diagram && (
                    <Button type="primary" onClick={() => setDiagramModalVisible(true)}>
                        查看流程图
                    </Button>
                )}
            </Space>

            <Card title="Process 实例详情" style={{ marginBottom: 24 }}>
                <Descriptions bordered column={2}>
                    <Descriptions.Item label="Process Instance ID" span={2}>
                        <code>{process.id}</code>
                    </Descriptions.Item>
                    <Descriptions.Item label="流程定义 Key">
                        <code>{process.processDefinitionKey}</code>
                    </Descriptions.Item>
                    <Descriptions.Item label="流程名称">
                        {process.processDefinitionName}
                    </Descriptions.Item>
                    <Descriptions.Item label="版本">
                        <Tag>v{process.processDefinitionVersion}</Tag>
                    </Descriptions.Item>
                    <Descriptions.Item label="Business Key">
                        {process.businessKey || '-'}
                    </Descriptions.Item>
                    <Descriptions.Item label="状态" span={2}>
                        {getStateTag(process.state)}
                    </Descriptions.Item>
                    <Descriptions.Item label="开始时间">
                        {new Date(process.startTime).toLocaleString()}
                    </Descriptions.Item>
                    <Descriptions.Item label="结束时间">
                        {process.endTime ? new Date(process.endTime).toLocaleString() : '-'}
                    </Descriptions.Item>
                    <Descriptions.Item label="发起人" span={2}>
                        {process.startUserId}
                    </Descriptions.Item>
                    <Descriptions.Item label="当前活动" span={2}>
                        {process.currentActivityIds?.length > 0 ? (
                            <Space>
                                {process.currentActivityIds.map((id, index) => (
                                    <Tag key={index} color="blue">
                                        <code>{id}</code>
                                    </Tag>
                                ))}
                            </Space>
                        ) : '-'}
                    </Descriptions.Item>
                </Descriptions>
            </Card>

            {/* Current Activities */}
            {process.currentActivities && process.currentActivities.length > 0 && (
                <Card title="当前活动" style={{ marginBottom: 24 }}>
                    <Table
                        columns={activityColumns}
                        dataSource={process.currentActivities}
                        rowKey="activityId"
                        pagination={false}
                        size="small"
                    />
                </Card>
            )}

            {/* Completed Activities */}
            {process.completedActivities && process.completedActivities.length > 0 && (
                <Card title="已完成活动" style={{ marginBottom: 24 }}>
                    <Table
                        columns={activityColumns}
                        dataSource={process.completedActivities}
                        rowKey="activityId"
                        pagination={{
                            pageSize: 10,
                            showTotal: (total) => `共 ${total} 个活动`,
                        }}
                        size="small"
                    />
                </Card>
            )}

            {/* Process Diagram Modal */}
            <Modal
                title="BPMN 流程图"
                open={diagramModalVisible}
                onCancel={() => setDiagramModalVisible(false)}
                footer={[
                    <Button key="close" onClick={() => setDiagramModalVisible(false)}>
                        关闭
                    </Button>,
                ]}
                width="90%"
                style={{ top: 20 }}
            >
                {diagram ? (
                    <div style={{ textAlign: 'center' }}>
                        <Alert
                            message="高亮说明"
                            description={
                                <div>
                                    <p>• 当前活动: <Tag color="blue">蓝色</Tag></p>
                                    <p>• 已完成活动: <Tag color="green">绿色</Tag></p>
                                    <p>• 高亮连线: 表示流程执行路径</p>
                                </div>
                            }
                            type="info"
                            showIcon
                            style={{ marginBottom: 16, textAlign: 'left' }}
                        />
                        <div
                            dangerouslySetInnerHTML={{ __html: diagram.diagramXml }}
                            style={{
                                border: '1px solid #d9d9d9',
                                borderRadius: '4px',
                                padding: '16px',
                                overflow: 'auto',
                                maxHeight: '70vh',
                            }}
                        />
                        <Divider />
                        <Space style={{ marginBottom: 16 }}>
                            <div>高亮活动: {diagram.highlightedActivities.length}</div>
                            <div>已完成活动: {diagram.completedActivities.length}</div>
                            <div>高亮连线: {diagram.highlightedFlows.length}</div>
                        </Space>
                    </div>
                ) : (
                    <Alert message="暂无流程图数据" type="warning" />
                )}
            </Modal>
        </div>
    );
};

export default ProcessInstanceDetail;
