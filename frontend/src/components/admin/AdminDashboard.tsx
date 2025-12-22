import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Statistic, Spin, Alert } from 'antd';
import {
    DatabaseOutlined,
    DeploymentUnitOutlined,
    FolderOpenOutlined,
    RocketOutlined,
} from '@ant-design/icons';
import { statisticsApi, AdminStatisticsDTO } from '../../services/adminApi';

const AdminDashboard: React.FC = () => {
    const [statistics, setStatistics] = useState<AdminStatisticsDTO | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        loadStatistics();
    }, []);

    const loadStatistics = async () => {
        try {
            setLoading(true);
            setError(null);
            const response = await statisticsApi.getStatistics();
            setStatistics(response.data);
        } catch (err: any) {
            setError(err.message || 'Failed to load statistics');
        } finally {
            setLoading(false);
        }
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
                style={{ margin: '20px' }}
            />
        );
    }

    if (!statistics) {
        return null;
    }

    return (
        <div style={{ padding: '24px' }}>
            <h1 style={{ marginBottom: '24px' }}>Flowable Admin Dashboard</h1>

            {/* 模型统计 */}
            <Card title="模型统计" style={{ marginBottom: '24px' }}>
                <Row gutter={16}>
                    <Col span={6}>
                        <Statistic
                            title="总模型数"
                            value={statistics.models.total}
                            prefix={<DatabaseOutlined />}
                        />
                    </Col>
                    <Col span={6}>
                        <Statistic
                            title="CMMN 模型"
                            value={statistics.models.cmmn}
                            valueStyle={{ color: '#3f8600' }}
                        />
                    </Col>
                    <Col span={6}>
                        <Statistic
                            title="BPMN 模型"
                            value={statistics.models.bpmn}
                            valueStyle={{ color: '#1890ff' }}
                        />
                    </Col>
                    <Col span={6}>
                        <Statistic
                            title="DMN 模型"
                            value={statistics.models.dmn}
                            valueStyle={{ color: '#cf1322' }}
                        />
                    </Col>
                </Row>
            </Card>

            {/* 部署统计 */}
            <Card title="部署统计" style={{ marginBottom: '24px' }}>
                <Row gutter={16}>
                    <Col span={12}>
                        <Statistic
                            title="总部署数"
                            value={statistics.deployments.total}
                            prefix={<DeploymentUnitOutlined />}
                        />
                    </Col>
                    <Col span={12}>
                        <Statistic
                            title="最后部署时间"
                            value={statistics.deployments.lastDeploymentTime || 'N/A'}
                            valueStyle={{ fontSize: '16px' }}
                        />
                    </Col>
                </Row>
            </Card>

            {/* Case 实例统计 */}
            <Card title="Case 实例统计" style={{ marginBottom: '24px' }}>
                <Row gutter={16}>
                    <Col span={6}>
                        <Statistic
                            title="活动中"
                            value={statistics.cases.ACTIVE || 0}
                            valueStyle={{ color: '#1890ff' }}
                            prefix={<RocketOutlined />}
                        />
                    </Col>
                    <Col span={6}>
                        <Statistic
                            title="已完成"
                            value={statistics.cases.COMPLETED || 0}
                            valueStyle={{ color: '#3f8600' }}
                        />
                    </Col>
                    <Col span={6}>
                        <Statistic
                            title="已挂起"
                            value={statistics.cases.SUSPENDED || 0}
                            valueStyle={{ color: '#faad14' }}
                        />
                    </Col>
                    <Col span={6}>
                        <Statistic
                            title="已终止"
                            value={statistics.cases.TERMINATED || 0}
                            valueStyle={{ color: '#cf1322' }}
                        />
                    </Col>
                </Row>
            </Card>

            {/* Process 实例统计 */}
            <Card title="Process 实例统计">
                <Row gutter={16}>
                    <Col span={8}>
                        <Statistic
                            title="活动中"
                            value={statistics.processes.ACTIVE || 0}
                            valueStyle={{ color: '#1890ff' }}
                            prefix={<FolderOpenOutlined />}
                        />
                    </Col>
                    <Col span={8}>
                        <Statistic
                            title="已完成"
                            value={statistics.processes.COMPLETED || 0}
                            valueStyle={{ color: '#3f8600' }}
                        />
                    </Col>
                    <Col span={8}>
                        <Statistic
                            title="已挂起"
                            value={statistics.processes.SUSPENDED || 0}
                            valueStyle={{ color: '#faad14' }}
                        />
                    </Col>
                </Row>
            </Card>
        </div>
    );
};

export default AdminDashboard;
