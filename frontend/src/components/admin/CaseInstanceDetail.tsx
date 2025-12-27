import React, { useCallback, useEffect, useState } from 'react';
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
    Tree,
    Tabs,
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
    ThunderboltOutlined,
    FileOutlined,
    ApartmentOutlined,
} from '@ant-design/icons';
import { caseApi, CaseInstanceDTO, PlanItemTreeNode } from '../../services/adminApi';
import { CmmnCaseVisualizer } from './CmmnCaseVisualizer';
import { BpmnSubprocessVisualizer } from './BpmnSubprocessVisualizer';
import { PlanItemState } from '../../types';

interface DataNode {
    title: React.ReactNode;
    key: string;
    children?: DataNode[];
    icon?: React.ReactNode;
}

const CaseInstanceDetail: React.FC = () => {
    const { caseInstanceId } = useParams<{ caseInstanceId: string }>();
    const navigate = useNavigate();
    const [caseInstance, setCaseInstance] = useState<CaseInstanceDTO | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [selectedPlanItemId, setSelectedPlanItemId] = useState<string | null>(null);

    const loadCaseDetail = useCallback(async () => {
        if (!caseInstanceId) return;
        try {
            setLoading(true);
            setError(null);
            const response = await caseApi.getCaseInstanceDetail(caseInstanceId);
            setCaseInstance(response.data);
        } catch (err: any) {
            setError(err.message || 'Failed to load case instance detail');
        } finally {
            setLoading(false);
        }
    }, [caseInstanceId]);

    useEffect(() => {
        if (caseInstanceId) {
            loadCaseDetail();
        }
    }, [caseInstanceId, loadCaseDetail]);

    const handleTerminate = async (reason?: string) => {
        if (!caseInstanceId) return;
        try {
            await caseApi.terminateCase(caseInstanceId, reason);
            message.success('Case terminated successfully');
            loadCaseDetail();
        } catch (err: any) {
            message.error(`Failed to terminate case: ${err.message}`);
        }
    };

    const handleSuspend = async () => {
        if (!caseInstanceId) return;
        try {
            await caseApi.suspendCase(caseInstanceId);
            message.success('Case suspended successfully');
            loadCaseDetail();
        } catch (err: any) {
            message.error(`Failed to suspend case: ${err.message}`);
        }
    };

    const handleResume = async () => {
        if (!caseInstanceId) return;
        try {
            await caseApi.resumeCase(caseInstanceId);
            message.success('Case resumed successfully');
            loadCaseDetail();
        } catch (err: any) {
            message.error(`Failed to resume case: ${err.message}`);
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

    const getPlanItemStateIcon = (state: string) => {
        const iconMap: Record<string, React.ReactNode> = {
            ACTIVE: <RocketOutlined style={{ color: '#1890ff' }} />,
            SUSPENDED: <PauseCircleOutlined style={{ color: '#faad14' }} />,
            COMPLETED: <CheckCircleOutlined style={{ color: '#52c41a' }} />,
            TERMINATED: <CloseCircleOutlined style={{ color: '#ff4d4f' }} />,
            AVAILABLE: <FileOutlined style={{ color: '#722ed1' }} />,
            ENABLED: <ThunderboltOutlined style={{ color: '#13c2c2' }} />,
        };
        return iconMap[state] || <FileOutlined />;
    };

    const getPlanItemStateColor = (state: string) => {
        const colorMap: Record<string, string> = {
            ACTIVE: '#1890ff',
            SUSPENDED: '#faad14',
            COMPLETED: '#52c41a',
            TERMINATED: '#ff4d4f',
            AVAILABLE: '#722ed1',
            ENABLED: '#13c2c2',
            FAILED: '#ff4d4f',
        };
        return colorMap[state] || '#d9d9d9';
    };

    const buildTreeData = (node: PlanItemTreeNode): DataNode => {
        const stateColor = getPlanItemStateColor(node.state);
        const title = (
            <span style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <strong>{node.name}</strong>
                <Tag color={stateColor}>{node.state}</Tag>
                {node.assignee && (
                    <Tag>👤 {node.assignee}</Tag>
                )}
            </span>
        );

        const dataNode: DataNode = {
            title,
            key: node.id,
            icon: getPlanItemStateIcon(node.state),
        };

        if (node.children && node.children.length > 0) {
            dataNode.children = node.children.map((child) => buildTreeData(child));
        }

        return dataNode;
    };

    const formatDuration = (startTime: string, endTime?: string) => {
        const start = new Date(startTime).getTime();
        const end = endTime ? new Date(endTime).getTime() : Date.now();
        const duration = Math.floor((end - start) / 1000);
        if (duration < 60) return `${duration}s`;
        const minutes = Math.floor(duration / 60);
        if (minutes < 60) return `${minutes}m ${duration % 60}s`;
        const hours = Math.floor(minutes / 60);
        return `${hours}h ${minutes % 60}m`;
    };

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

    if (!caseInstance) {
        return null;
    }

    const treeData = caseInstance.planItemTree ? [buildTreeData(caseInstance.planItemTree)] : [];

    const handlePlanItemClick = (planItem: PlanItemState) => {
        // For process tasks, show subprocess visualization modal
        if (planItem.type === 'processtask') {
            console.log('Process task clicked, opening subprocess visualizer:', planItem.id);
            setSelectedPlanItemId(planItem.id);
            return;
        }

        // For other task types, show plan item details modal
        Modal.info({
            title: `Plan Item: ${planItem.name}`,
            width: 600,
            content: (
                <Descriptions bordered column={1} size="small">
                    <Descriptions.Item label="ID">{planItem.id}</Descriptions.Item>
                    <Descriptions.Item label="Plan Item Definition ID">
                        <code>{planItem.planItemDefinitionId}</code>
                    </Descriptions.Item>
                    <Descriptions.Item label="Name">{planItem.name}</Descriptions.Item>
                    <Descriptions.Item label="Type">{planItem.type}</Descriptions.Item>
                    <Descriptions.Item label="State">
                        <Tag color={getPlanItemStateColor(planItem.state)}>
                            {planItem.state}
                        </Tag>
                    </Descriptions.Item>
                    <Descriptions.Item label="Create Time">
                        {new Date(planItem.createTime).toLocaleString()}
                    </Descriptions.Item>
                    <Descriptions.Item label="Completed Time">
                        {planItem.completedTime ? new Date(planItem.completedTime).toLocaleString() : '-'}
                    </Descriptions.Item>
                    <Descriptions.Item label="Terminated Time">
                        {planItem.terminatedTime ? new Date(planItem.terminatedTime).toLocaleString() : '-'}
                    </Descriptions.Item>
                    {planItem.stageInstanceId && (
                        <Descriptions.Item label="Stage Instance ID">
                            <code>{planItem.stageInstanceId}</code>
                        </Descriptions.Item>
                    )}
                </Descriptions>
            ),
        });
    };

    return (
        <div style={{ padding: '24px' }}>
            <Space style={{ marginBottom: 16 }}>
                <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/admin/cases')}>
                    返回列表
                </Button>
                <Button icon={<ReloadOutlined />} onClick={loadCaseDetail}>
                    刷新
                </Button>
                {caseInstance.state === 'ACTIVE' && (
                    <Popconfirm
                        title="确认挂起?"
                        description="确定要挂起这个 Case 实例吗?"
                        onConfirm={handleSuspend}
                        okText="确认"
                        cancelText="取消"
                    >
                        <Button danger icon={<PauseCircleOutlined />}>
                            挂起
                        </Button>
                    </Popconfirm>
                )}
                {caseInstance.state === 'SUSPENDED' && (
                    <Button icon={<PlayCircleOutlined />} onClick={handleResume}>
                        恢复
                    </Button>
                )}
                {(caseInstance.state === 'ACTIVE' || caseInstance.state === 'SUSPENDED') && (
                    <Popconfirm
                        title="确认终止?"
                        description="确定要终止这个 Case 实例吗?此操作不可逆!"
                        onConfirm={() => handleTerminate('Terminated by admin')}
                        okText="确认"
                        cancelText="取消"
                    >
                        <Button danger icon={<StopOutlined />}>
                            终止
                        </Button>
                    </Popconfirm>
                )}
            </Space>

            <Card title="Case 实例详情" style={{ marginBottom: 24 }}>
                <Descriptions bordered column={2}>
                    <Descriptions.Item label="Case Instance ID" span={2}>
                        <code>{caseInstance.id}</code>
                    </Descriptions.Item>
                    <Descriptions.Item label="Case 定义 Key">
                        <code>{caseInstance.caseDefinitionKey}</code>
                    </Descriptions.Item>
                    <Descriptions.Item label="Case 名称">
                        {caseInstance.caseDefinitionName}
                    </Descriptions.Item>
                    <Descriptions.Item label="版本">
                        <Tag>v{caseInstance.caseDefinitionVersion}</Tag>
                    </Descriptions.Item>
                    <Descriptions.Item label="Business Key">
                        {caseInstance.businessKey || '-'}
                    </Descriptions.Item>
                    <Descriptions.Item label="状态" span={2}>
                        {getStateTag(caseInstance.state)}
                    </Descriptions.Item>
                    <Descriptions.Item label="开始时间">
                        {new Date(caseInstance.startTime).toLocaleString()}
                    </Descriptions.Item>
                    <Descriptions.Item label="结束时间">
                        {caseInstance.endTime ? new Date(caseInstance.endTime).toLocaleString() : '-'}
                    </Descriptions.Item>
                    <Descriptions.Item label="发起人" span={2}>
                        {caseInstance.startUserId}
                    </Descriptions.Item>
                    <Descriptions.Item label="运行时长">
                        {formatDuration(caseInstance.startTime, caseInstance.endTime)}
                    </Descriptions.Item>
                    <Descriptions.Item label="活动 Plan Items">
                        <Tag color="blue">{caseInstance.activePlanItems}</Tag>
                    </Descriptions.Item>
                    <Descriptions.Item label="已完成 Plan Items">
                        <Tag color="green">{caseInstance.completedPlanItems}</Tag>
                    </Descriptions.Item>
                </Descriptions>
            </Card>

            {/* Plan Item Tree & Visualization */}
            <Card title="Case 执行视图">
                <Tabs
                    defaultActiveKey="visualization"
                    items={[
                        {
                            key: 'visualization',
                            label: (
                                <span>
                                    <ApartmentOutlined />
                                    CMMN 模型可视化
                                </span>
                            ),
                            children: (
                                <div style={{ marginTop: 16 }}>
                                    <Alert
                                        message="可视化说明"
                                        description="点击模型节点可查看 Plan Item 详细信息"
                                        type="info"
                                        showIcon
                                        style={{ marginBottom: 16 }}
                                    />
                                    <CmmnCaseVisualizer
                                        caseInstanceId={caseInstanceId || ''}
                                        height="800px"
                                        onPlanItemClick={handlePlanItemClick}
                                    />
                                </div>
                            ),
                        },
                        {
                            key: 'tree',
                            label: (
                                <span>
                                    <FileOutlined />
                                    Plan Item Tree
                                </span>
                            ),
                            children: (
                                treeData.length > 0 ? (
                                    <>
                                        <Alert
                                            message="图例说明"
                                            description={
                                                <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap' }}>
                                                    <span>🚀 ACTIVE: 活动</span>
                                                    <span>⏸️ SUSPENDED: 挂起</span>
                                                    <span>✅ COMPLETED: 已完成</span>
                                                    <span>❌ TERMINATED: 已终止</span>
                                                    <span>📄 AVAILABLE: 可用</span>
                                                    <span>⚡ ENABLED: 已启用</span>
                                                </div>
                                            }
                                            type="info"
                                            showIcon
                                            style={{ marginBottom: 16 }}
                                        />
                                        <Tree
                                            showIcon
                                            treeData={treeData}
                                            expandedKeys={[]}
                                            defaultExpandAll
                                            style={{ background: '#fafafa', padding: '16px', borderRadius: '4px' }}
                                        />
                                    </>
                                ) : (
                                    <Alert message="无 Plan Item Tree 数据" type="warning" />
                                )
                            ),
                        },
                    ]}
                />
            </Card>

            {/* Variables */}
            {caseInstance.variables && Object.keys(caseInstance.variables).length > 0 && (
                <Card title="流程变量" style={{ marginTop: 24 }}>
                    <div style={{ display: 'grid', gap: '8px' }}>
                        {Object.entries(caseInstance.variables).map(([key, value]) => (
                            <div key={key} style={{ display: 'flex', gap: '8px' }}>
                                <Tag color="blue"><code>{key}</code></Tag>
                                <span style={{ fontFamily: 'monospace', background: '#f5f5f5', padding: '2px 8px', borderRadius: '4px' }}>
                                    {typeof value === 'object' ? JSON.stringify(value, null, 2) : String(value)}
                                </span>
                            </div>
                        ))}
                    </div>
                </Card>
            )}

            {/* BPMN Subprocess Visualizer Modal */}
            {selectedPlanItemId && (
                <BpmnSubprocessVisualizer
                    planItemInstanceId={selectedPlanItemId}
                    onClose={() => setSelectedPlanItemId(null)}
                />
            )}
        </div>
    );
};

export default CaseInstanceDetail;
