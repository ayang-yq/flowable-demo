import React, { useEffect, useState } from 'react';
import {
    Table,
    Button,
    Tag,
    Space,
    Card,
    Spin,
    Alert,
    Select,
    Input,
    Modal,
    Descriptions,
} from 'antd';
import {
    ReloadOutlined,
    EyeOutlined,
    FolderOpenOutlined,
    DeploymentUnitOutlined,
} from '@ant-design/icons';
import { modelApi, ModelDTO, DeploymentDTO } from '../../services/adminApi';

const { Option } = Select;

const ModelList: React.FC = () => {
    const [models, setModels] = useState<ModelDTO[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [selectedType, setSelectedType] = useState<string | undefined>(undefined);
    const [selectedModel, setSelectedModel] = useState<ModelDTO | null>(null);
    const [detailModalVisible, setDetailModalVisible] = useState(false);

    useEffect(() => {
        loadModels();
    }, [selectedType]);

    const loadModels = async () => {
        try {
            setLoading(true);
            setError(null);
            const response = await modelApi.queryModels(selectedType, 0, 100);
            setModels(response.data.content);
        } catch (err: any) {
            setError(err.message || 'Failed to load models');
        } finally {
            setLoading(false);
        }
    };

    const handleViewDetail = async (model: ModelDTO) => {
        try {
            const response = await modelApi.getModelDetail(model.key, model.type);
            setSelectedModel(response.data);
            setDetailModalVisible(true);
        } catch (err: any) {
            console.error('Failed to load model detail:', err);
        }
    };

    const columns = [
        {
            title: '模型 Key',
            dataIndex: 'key',
            key: 'key',
            render: (text: string) => <code>{text}</code>,
        },
        {
            title: '名称',
            dataIndex: 'name',
            key: 'name',
        },
        {
            title: '类型',
            dataIndex: 'type',
            key: 'type',
            render: (type: string) => (
                <Tag color={type === 'CMMN' ? 'blue' : type === 'BPMN' ? 'green' : 'orange'}>
                    {type}
                </Tag>
            ),
        },
        {
            title: '版本',
            dataIndex: 'version',
            key: 'version',
            render: (version: number) => <Tag>v{version}</Tag>,
        },
        {
            title: '部署状态',
            dataIndex: 'deployed',
            key: 'deployed',
            render: (deployed: boolean) => (
                <Tag color={deployed ? 'green' : 'red'}>
                    {deployed ? '已部署' : '未部署'}
                </Tag>
            ),
        },
        {
            title: '创建者',
            dataIndex: 'createdBy',
            key: 'createdBy',
            render: (text: string) => text || '-',
        },
        {
            title: '最后修改',
            dataIndex: 'lastModified',
            key: 'lastModified',
            render: (date: string) => date ? new Date(date).toLocaleString() : '-',
        },
        {
            title: '操作',
            key: 'action',
            render: (_: any, record: ModelDTO) => (
                <Space>
                    <Button
                        type="link"
                        icon={<EyeOutlined />}
                        onClick={() => handleViewDetail(record)}
                    >
                        详情
                    </Button>
                </Space>
            ),
        },
    ];

    return (
        <div style={{ padding: '24px' }}>
            <Card
                title={
                    <Space>
                        <DatabaseOutlined />
                        模型管理
                    </Space>
                }
                extra={
                    <Button icon={<ReloadOutlined />} onClick={loadModels}>
                        刷新
                    </Button>
                }
            >
                <Space style={{ marginBottom: 16 }}>
                    <Select
                        placeholder="选择模型类型"
                        style={{ width: 200 }}
                        allowClear
                        value={selectedType}
                        onChange={setSelectedType}
                    >
                        <Option value="CMMN">CMMN</Option>
                        <Option value="BPMN">BPMN</Option>
                        <Option value="DMN">DMN</Option>
                    </Select>
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
                    dataSource={models}
                    rowKey="id"
                    loading={loading}
                    pagination={{
                        pageSize: 20,
                        showSizeChanger: true,
                        showTotal: (total) => `共 ${total} 个模型`,
                    }}
                />
            </Card>

            {/* Model Detail Modal */}
            <Modal
                title="模型详情"
                open={detailModalVisible}
                onCancel={() => setDetailModalVisible(false)}
                footer={[
                    <Button key="close" onClick={() => setDetailModalVisible(false)}>
                        关闭
                    </Button>,
                ]}
                width={800}
            >
                {selectedModel && (
                    <Descriptions bordered column={2}>
                        <Descriptions.Item label="模型 Key" span={2}>
                            <code>{selectedModel.key}</code>
                        </Descriptions.Item>
                        <Descriptions.Item label="名称" span={2}>
                            {selectedModel.name}
                        </Descriptions.Item>
                        <Descriptions.Item label="类型">
                            <Tag color={selectedModel.type === 'CMMN' ? 'blue' : selectedModel.type === 'BPMN' ? 'green' : 'orange'}>
                                {selectedModel.type}
                            </Tag>
                        </Descriptions.Item>
                        <Descriptions.Item label="版本">
                            <Tag>v{selectedModel.version}</Tag>
                        </Descriptions.Item>
                        <Descriptions.Item label="部署状态">
                            <Tag color={selectedModel.deployed ? 'green' : 'red'}>
                                {selectedModel.deployed ? '已部署' : '未部署'}
                            </Tag>
                        </Descriptions.Item>
                        <Descriptions.Item label="创建者">
                            {selectedModel.createdBy || '-'}
                        </Descriptions.Item>
                        <Descriptions.Item label="最后修改">
                            {selectedModel.lastModified ? new Date(selectedModel.lastModified).toLocaleString() : '-'}
                        </Descriptions.Item>
                        <Descriptions.Item label="描述" span={2}>
                            {selectedModel.description || '-'}
                        </Descriptions.Item>
                    </Descriptions>
                )}

                {selectedModel?.deployments && selectedModel.deployments.length > 0 && (
                    <div style={{ marginTop: 24 }}>
                        <h4><DeploymentUnitOutlined /> 部署历史</h4>
                        <Table
                            columns={[
                                {
                                    title: '部署 ID',
                                    dataIndex: 'id',
                                    key: 'id',
                                    render: (text: string) => <code>{text.substring(0, 8)}...</code>,
                                },
                                {
                                    title: '部署名称',
                                    dataIndex: 'name',
                                    key: 'name',
                                },
                                {
                                    title: '版本',
                                    dataIndex: 'version',
                                    key: 'version',
                                },
                                {
                                    title: '部署时间',
                                    dataIndex: 'deploymentTime',
                                    key: 'deploymentTime',
                                    render: (date: string) => date ? new Date(date).toLocaleString() : '-',
                                },
                            ]}
                            dataSource={selectedModel.deployments}
                            rowKey="id"
                            pagination={false}
                            size="small"
                        />
                    </div>
                )}
            </Modal>
        </div>
    );
};

const DatabaseOutlined = FolderOpenOutlined;

export default ModelList;
