import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

// Admin API Client
const adminApi = axios.create({
    baseURL: `${API_BASE_URL}/admin`,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Request interceptor for adding auth token
adminApi.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers.Authorization = `Basic ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

// Response interceptor for error handling
adminApi.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            // Redirect to login
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

export interface ModelDTO {
    id: string;
    key: string;
    name: string;
    type: 'CMMN' | 'BPMN' | 'DMN';
    version: number;
    deployed: boolean;
    latestDeploymentId?: string;
    latestDeploymentTime?: string;
    createdBy?: string;
    lastModified?: string;
    tenantId?: string;
    description?: string;
    deployments?: DeploymentDTO[];
    xmlContent?: string;
}

export interface DeploymentDTO {
    id: string;
    name: string;
    definitionId?: string;
    definitionKey?: string;
    version?: number;
    deploymentTime?: string;
    deployedBy?: string;
    active?: boolean;
    tenantId?: string;
    category?: string;
}

export interface CaseInstanceDTO {
    id: string;
    caseDefinitionId: string;
    caseDefinitionKey: string;
    caseDefinitionName: string;
    caseDefinitionVersion: number;
    businessKey: string;
    state: string;
    startTime: string;
    endTime?: string;
    startUserId: string;
    tenantId?: string;
    activePlanItems: number;
    completedPlanItems: number;
    variables?: Record<string, any>;
    planItemTree?: PlanItemTreeNode;
}

export interface PlanItemTreeNode {
    id: string;
    name: string;
    elementId: string;
    type: string;
    state: string;
    createTime?: string;
    completedTime?: string;
    terminatedTime?: string;
    assignee?: string;
    children: PlanItemTreeNode[];
    repeatable?: boolean;
    required?: boolean;
}

export interface ProcessInstanceDTO {
    id: string;
    processDefinitionId: string;
    processDefinitionKey: string;
    processDefinitionName: string;
    processDefinitionVersion: number;
    businessKey: string;
    state: string;
    startTime: string;
    endTime?: string;
    startUserId: string;
    tenantId?: string;
    currentActivityIds: string[];
    variables?: Record<string, any>;
    currentActivities?: ActivityInfo[];
    completedActivities?: ActivityInfo[];
}

export interface ActivityInfo {
    activityId: string;
    activityName: string;
    activityType: string;
    startTime: string;
    endTime?: string;
    assignee?: string;
    durationInMillis?: number;
}

export interface ProcessDiagramDTO {
    processDefinitionId: string;
    diagramXml: string;
    highlightedActivities: string[];
    completedActivities: string[];
    highlightedFlows: string[];
}

export interface AdminStatisticsDTO {
    models: {
        total: number;
        cmmn: number;
        bpmn: number;
        dmn: number;
    };
    deployments: {
        total: number;
        lastDeploymentTime?: string;
    };
    cases: Record<string, number>;
    processes: Record<string, number>;
}

export interface PageResponse<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
}

// Model Management API
export const modelApi = {
    /**
     * 查询模型列表
     */
    queryModels: (type?: string, page = 0, size = 20) =>
        adminApi.get<PageResponse<ModelDTO>>('/models', {
            params: { type, page, size },
        }),

    /**
     * 获取模型详情
     */
    getModelDetail: (modelKey: string, modelType: string) =>
        adminApi.get<ModelDTO>(`/models/${modelKey}`, {
            params: { modelType },
        }),

    /**
     * 部署模型
     */
    deployModel: (file: File, modelType: string, deploymentName?: string) => {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('modelType', modelType);
        if (deploymentName) {
            formData.append('deploymentName', deploymentName);
        }

        return adminApi.post<DeploymentDTO>('/models/deploy', formData, {
            headers: {
                'Content-Type': 'multipart/form-data',
            },
        });
    },
};

// Case Management API
export const caseApi = {
    /**
     * 查询 Case 实例列表
     */
    queryCaseInstances: (params: {
        caseDefinitionKey?: string;
        businessKey?: string;
        state?: string;
        startedAfter?: string;
        page?: number;
        size?: number;
    }) =>
        adminApi.get<PageResponse<CaseInstanceDTO>>('/cases', { params }),

    /**
     * 获取 Case 实例详情
     */
    getCaseInstanceDetail: (caseInstanceId: string) =>
        adminApi.get<CaseInstanceDTO>(`/cases/${caseInstanceId}`),

    /**
     * 终止 Case
     */
    terminateCase: (caseInstanceId: string, reason?: string) =>
        adminApi.post(`/cases/${caseInstanceId}/terminate`, { reason }),

    /**
     * 挂起 Case
     */
    suspendCase: (caseInstanceId: string) =>
        adminApi.post(`/cases/${caseInstanceId}/suspend`),

    /**
     * 恢复 Case
     */
    resumeCase: (caseInstanceId: string) =>
        adminApi.post(`/cases/${caseInstanceId}/resume`),

    /**
     * 触发 Plan Item
     */
    triggerPlanItem: (caseInstanceId: string, planItemInstanceId: string) =>
        adminApi.post(`/cases/${caseInstanceId}/plan-items/${planItemInstanceId}/trigger`),
};

// Process Management API
export const processApi = {
    /**
     * 查询 Process 实例列表
     */
    queryProcessInstances: (params: {
        processDefinitionKey?: string;
        businessKey?: string;
        startedAfter?: string;
        page?: number;
        size?: number;
    }) =>
        adminApi.get<PageResponse<ProcessInstanceDTO>>('/processes', { params }),

    /**
     * 获取 Process 实例详情
     */
    getProcessInstanceDetail: (processInstanceId: string) =>
        adminApi.get<ProcessInstanceDTO>(`/processes/${processInstanceId}`),

    /**
     * 获取流程图高亮数据
     */
    getProcessDiagram: (processInstanceId: string) =>
        adminApi.get<ProcessDiagramDTO>(`/processes/${processInstanceId}/diagram`),

    /**
     * 终止 Process
     */
    terminateProcess: (processInstanceId: string, reason?: string) =>
        adminApi.post(`/processes/${processInstanceId}/terminate`, { reason }),

    /**
     * 挂起 Process
     */
    suspendProcess: (processInstanceId: string) =>
        adminApi.post(`/processes/${processInstanceId}/suspend`),

    /**
     * 恢复 Process
     */
    resumeProcess: (processInstanceId: string) =>
        adminApi.post(`/processes/${processInstanceId}/resume`),
};

// Statistics API
export const statisticsApi = {
    /**
     * 获取系统统计信息
     */
    getStatistics: () =>
        adminApi.get<AdminStatisticsDTO>('/statistics'),
};

export default adminApi;
