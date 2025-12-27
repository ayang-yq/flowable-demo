import React, { useState } from 'react';
import {
    Card,
    Upload,
    Button,
    Select,
    Input,
    Form,
    Alert,
    message,
    Space,
    Typography,
    Divider
} from 'antd';
import { 
    UploadOutlined, 
    DeploymentUnitOutlined, 
    FileTextOutlined,
    CheckCircleOutlined,
    ExclamationCircleOutlined
} from '@ant-design/icons';
import { modelApi, DeploymentDTO } from '../../services/adminApi';

const { Title, Text } = Typography;
const { Option } = Select;
const { TextArea } = Input;

interface DeployResult {
    success: boolean;
    deployment?: DeploymentDTO;
    error?: string;
}

const ModelDeployment: React.FC = () => {
    const [form] = Form.useForm();
    const [loading, setLoading] = useState(false);
    const [deployResult, setDeployResult] = useState<DeployResult | null>(null);
    const [fileList, setFileList] = useState<any[]>([]);
    const [modelType, setModelType] = useState<string>('');

    const handleModelTypeChange = (value: string) => {
        setModelType(value);
        // 清空文件列表，因为不同类型需要不同的文件
        setFileList([]);
        setDeployResult(null);
    };

    const beforeUpload = (file: File) => {
        if (!modelType) {
            message.error('请先选择模型类型');
            return false;
        }

        const allowedExtensions = {
            CMMN: ['.cmmn'],
            BPMN: ['.bpmn', '.bpmn20.xml'],
            DMN: ['.dmn']
        };

        const allowedExts = allowedExtensions[modelType as keyof typeof allowedExtensions];
        const fileExtension = '.' + file.name.split('.').pop()?.toLowerCase();
        
        if (!allowedExts.includes(fileExtension)) {
            message.error(`模型类型 ${modelType} 只支持 ${allowedExts.join(', ')} 文件格式`);
            return false;
        }

        // 检查文件大小 (限制为10MB)
        const isLt10M = file.size / 1024 / 1024 < 10;
        if (!isLt10M) {
            message.error('文件大小不能超过 10MB');
            return false;
        }

        return false; // 阻止自动上传
    };

    const handleFileChange = (info: any) => {
        setFileList(info.fileList.slice(-1)); // 只保留最新选择的文件
        setDeployResult(null);
    };

    const handleDeploy = async (values: any) => {
        if (!fileList.length) {
            message.error('请选择要部署的模型文件');
            return;
        }

        const file = fileList[0].originFileObj;
        if (!file) {
            message.error('文件无效，请重新选择');
            return;
        }

        setLoading(true);
        setDeployResult(null);

        try {
            const response = await modelApi.deployModel(
                file, 
                modelType, 
                values.deploymentName
            );
            
            setDeployResult({
                success: true,
                deployment: response.data
            });
            
            message.success('模型部署成功！');
            
            // 清空表单和文件
            form.resetFields();
            setFileList([]);
            setModelType('');
            
        } catch (error: any) {
            console.error('部署失败:', error);
            
            let errorMessage = '部署失败';
            
            if (error.response?.data) {
                // 尝试从响应中提取详细错误信息
                const errorData = error.response.data;
                if (typeof errorData === 'string') {
                    errorMessage = errorData;
                } else if (errorData.message) {
                    errorMessage = errorData.message;
                } else if (errorData.error) {
                    errorMessage = errorData.error;
                }
            } else if (error.message) {
                errorMessage = error.message;
            }
            
            setDeployResult({
                success: false,
                error: errorMessage
            });
            
            message.error('模型部署失败');
        } finally {
            setLoading(false);
        }
    };

    const uploadProps = {
        fileList,
        beforeUpload,
        onChange: handleFileChange,
        onRemove: () => {
            setFileList([]);
            setDeployResult(null);
        },
        accept: modelType ? {
            'CMMN': '.cmmn',
            'BPMN': '.bpmn,.bpmn20.xml',
            'DMN': '.dmn'
        }[modelType] : ''
    };

    return (
        <div style={{ padding: '24px' }}>
            <Card>
                <div style={{ marginBottom: '24px' }}>
                    <Title level={3}>
                        <DeploymentUnitOutlined /> 模型部署
                    </Title>
                    <Text type="secondary">
                        上传并部署 Flowable 模型文件（支持 CMMN、BPMN、DMN 格式）
                    </Text>
                </div>

                <Form
                    form={form}
                    layout="vertical"
                    onFinish={handleDeploy}
                >
                    <Form.Item
                        label="模型类型"
                        name="modelType"
                        rules={[{ required: true, message: '请选择模型类型' }]}
                    >
                        <Select
                            placeholder="请选择模型类型"
                            onChange={handleModelTypeChange}
                            value={modelType}
                        >
                            <Option value="CMMN">
                                <Space>
                                    <FileTextOutlined />
                                    CMMN (Case Management Model)
                                </Space>
                            </Option>
                            <Option value="BPMN">
                                <Space>
                                    <FileTextOutlined />
                                    BPMN (Business Process Model)
                                </Space>
                            </Option>
                            <Option value="DMN">
                                <Space>
                                    <FileTextOutlined />
                                    DMN (Decision Model)
                                </Space>
                            </Option>
                        </Select>
                    </Form.Item>

                    {modelType && (
                        <Form.Item
                            label="模型文件"
                            required
                            tooltip={
                                modelType === 'CMMN' ? '支持 .cmmn 文件' :
                                modelType === 'BPMN' ? '支持 .bpmn, .bpmn20.xml 文件' :
                                '支持 .dmn 文件'
                            }
                        >
                            <Upload.Dragger {...uploadProps}>
                                <p className="ant-upload-drag-icon">
                                    <UploadOutlined style={{ fontSize: '48px', color: '#1890ff' }} />
                                </p>
                                <p className="ant-upload-text">
                                    点击或拖拽文件到此区域上传
                                </p>
                                <p className="ant-upload-hint">
                                    {modelType === 'CMMN' && '支持单个 .cmmn 文件上传'}
                                    {modelType === 'BPMN' && '支持单个 .bpmn 或 .bpmn20.xml 文件上传'}
                                    {modelType === 'DMN' && '支持单个 .dmn 文件上传'}
                                </p>
                            </Upload.Dragger>
                        </Form.Item>
                    )}

                    <Form.Item
                        label="部署名称"
                        name="deploymentName"
                        tooltip="可选，如果不填写将使用文件名作为部署名称"
                    >
                        <Input 
                            placeholder="可选，不填写将使用文件名" 
                        />
                    </Form.Item>

                    <Form.Item>
                        <Button 
                            type="primary" 
                            htmlType="submit" 
                            loading={loading}
                            icon={<DeploymentUnitOutlined />}
                            disabled={!fileList.length || !modelType}
                        >
                            部署模型
                        </Button>
                    </Form.Item>
                </Form>

                {deployResult && (
                    <>
                        <Divider />
                        <div>
                            <Title level={4}>
                                部署结果
                            </Title>
                            
                            {deployResult.success ? (
                                <Alert
                                    type="success"
                                    icon={<CheckCircleOutlined />}
                                    message="模型部署成功"
                                    description={
                                        <div>
                                            <p><strong>部署ID:</strong> {deployResult.deployment?.id}</p>
                                            <p><strong>部署名称:</strong> {deployResult.deployment?.name}</p>
                                            <p><strong>定义ID:</strong> {deployResult.deployment?.definitionId || 'N/A'}</p>
                                            <p><strong>定义Key:</strong> {deployResult.deployment?.definitionKey || 'N/A'}</p>
                                            <p><strong>版本:</strong> {deployResult.deployment?.version || 'N/A'}</p>
                                            <p><strong>部署时间:</strong> {deployResult.deployment?.deploymentTime || 'N/A'}</p>
                                        </div>
                                    }
                                    showIcon
                                />
                            ) : (
                                <Alert
                                    type="error"
                                    icon={<ExclamationCircleOutlined />}
                                    message="模型部署失败"
                                    description={
                                        <div>
                                            <p><strong>错误信息:</strong></p>
                                            <TextArea
                                                value={deployResult.error}
                                                readOnly
                                                rows={6}
                                                style={{ marginTop: '8px', fontFamily: 'monospace' }}
                                            />
                                        </div>
                                    }
                                    showIcon
                                />
                            )}
                        </div>
                    </>
                )}

                {modelType && (
                    <>
                        <Divider />
                        <Card size="small" title="模型类型说明">
                            {modelType === 'CMMN' && (
                                <div>
                                    <p><strong>CMMN (Case Management Model and Notation)</strong></p>
                                    <ul>
                                        <li>用于描述案例管理流程</li>
                                        <li>支持动态、非结构化的业务流程</li>
                                        <li>文件扩展名: .cmmn</li>
                                        <li>适用于需要灵活处理的复杂业务场景</li>
                                    </ul>
                                </div>
                            )}
                            {modelType === 'BPMN' && (
                                <div>
                                    <p><strong>BPMN (Business Process Model and Notation)</strong></p>
                                    <ul>
                                        <li>用于描述结构化的业务流程</li>
                                        <li>支持顺序、并行、条件等流程控制</li>
                                        <li>文件扩展名: .bpmn, .bpmn20.xml</li>
                                        <li>适用于标准化的业务流程管理</li>
                                    </ul>
                                </div>
                            )}
                            {modelType === 'DMN' && (
                                <div>
                                    <p><strong>DMN (Decision Model and Notation)</strong></p>
                                    <ul>
                                        <li>用于描述业务决策规则</li>
                                        <li>支持决策表和决策树</li>
                                        <li>文件扩展名: .dmn</li>
                                        <li>适用于复杂的业务规则管理</li>
                                    </ul>
                                </div>
                            )}
                        </Card>
                    </>
                )}
            </Card>
        </div>
    );
};

export default ModelDeployment;
