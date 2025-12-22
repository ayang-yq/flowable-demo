import axios, { AxiosResponse } from 'axios';
import { 
  User, 
  InsurancePolicy, 
  ClaimCase, 
  FlowableTask, 
  TaskStatistics,
  DashboardStatistics,
  PaginationParams,
  PageResult 
} from '../types';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器
api.interceptors.request.use(
  (config) => {
    // 使用 Basic Auth
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Basic ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
api.interceptors.response.use(
  (response: AxiosResponse) => {
    return response;
  },
  (error) => {
    console.error('API Error:', error);

    // 处理 401 未授权错误
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      // 重定向到登录页面
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }

    return Promise.reject(error);
  }
);

// 理赔案件 API
export const claimApi = {
  // 获取理赔案件列表
  getClaims: (params?: PaginationParams): Promise<AxiosResponse<PageResult<ClaimCase>>> => {
    return api.get('/cases', { params });
  },

  // 根据ID获取理赔案件
  getClaim: (id: string): Promise<AxiosResponse<ClaimCase>> => {
    return api.get(`/cases/${id}`);
  },

  // 创建理赔案件
  createClaim: (claim: Partial<ClaimCase>): Promise<AxiosResponse<ClaimCase>> => {
    return api.post('/cases', claim);
  },

  // 更新理赔案件
  updateClaim: (id: string, claim: Partial<ClaimCase>): Promise<AxiosResponse<ClaimCase>> => {
    return api.put(`/cases/${id}`, claim);
  },

  // 删除理赔案件
  deleteClaim: (id: string): Promise<AxiosResponse<void>> => {
    return api.delete(`/cases/${id}`);
  },

  // 分配理赔案件
  assignClaim: (id: string, userId: string): Promise<AxiosResponse<void>> => {
    return api.post(`/cases/${id}/assign`, null, { params: { userId } });
  },

  // 更新理赔案件状态
  updateClaimStatus: (id: string, status: string, description?: string, userId?: string): Promise<AxiosResponse<ClaimCase>> => {
    return api.post(`/cases/${id}/status`, null, { params: { status, description, userId } });
  },

  // 搜索理赔案件
  searchClaims: (keyword: string, params?: PaginationParams): Promise<AxiosResponse<PageResult<ClaimCase>>> => {
    return api.get('/cases/search', { params: { keyword, ...params } });
  },

  // 获取我的理赔案件
  getMyClaims: (userId: string, params?: PaginationParams): Promise<AxiosResponse<PageResult<ClaimCase>>> => {
    return api.get('/cases/my-cases', { params: { userId, ...params } });
  },

  // 获取统计数据
  getStatistics: (): Promise<AxiosResponse<DashboardStatistics>> => {
    return api.get('/cases/statistics');
  },

  // 根据状态查询理赔案件
  getClaimsByStatus: (status: string, params?: PaginationParams): Promise<AxiosResponse<PageResult<ClaimCase>>> => {
    return api.get('/cases/by-status', { params: { status, ...params } });
  },

  // 根据分配用户查询理赔案件
  getClaimsByAssignee: (userId: string, params?: PaginationParams): Promise<AxiosResponse<PageResult<ClaimCase>>> => {
    return api.get('/cases/by-assignee', { params: { userId, ...params } });
  },

  // 审批理赔案件
  approveClaim: (id: string, variables?: any): Promise<AxiosResponse<void>> => {
    return api.post(`/cases/${id}/approve`, variables);
  },

  // 拒绝理赔案件
  rejectClaim: (id: string, reason: string): Promise<AxiosResponse<void>> => {
    return api.post(`/cases/${id}/reject`, { reason });
  },

  // 支付理赔案件
  payClaim: (id: string, paymentData: any): Promise<AxiosResponse<void>> => {
    return api.post(`/cases/${id}/pay`, paymentData);
  },
};

// 任务 API
export const taskApi = {
  // 获取我的待办任务
  getMyTasks: (userId: string, params?: PaginationParams): Promise<AxiosResponse<PageResult<FlowableTask>>> => {
    return api.get('/tasks/my-tasks', { params: { userId, ...params } });
  },

  // 获取可认领的任务
  getClaimableTasks: (userId: string, params?: PaginationParams): Promise<AxiosResponse<PageResult<FlowableTask>>> => {
    return api.get('/tasks/claimable', { params: { userId, ...params } });
  },

  // 认领任务
  claimTask: (taskId: string, userId: string): Promise<AxiosResponse<void>> => {
    return api.post(`/tasks/${taskId}/claim`, null, { params: { userId } });
  },

  // 取消认领任务
  unclaimTask: (taskId: string): Promise<AxiosResponse<void>> => {
    return api.post(`/tasks/${taskId}/unclaim`);
  },

  // 分配任务
  assignTask: (taskId: string, userId: string): Promise<AxiosResponse<void>> => {
    return api.post(`/tasks/${taskId}/assign`, null, { params: { userId } });
  },

  // 完成任务
  completeTask: (taskId: string, variables?: any): Promise<AxiosResponse<void>> => {
    return api.post(`/tasks/${taskId}/complete`, variables);
  },

  // 获取任务详情
  getTask: (taskId: string): Promise<AxiosResponse<FlowableTask>> => {
    return api.get(`/tasks/${taskId}`);
  },

  // 获取历史任务
  getHistoricTasks: (userId: string, params?: PaginationParams): Promise<AxiosResponse<PageResult<any>>> => {
    return api.get('/tasks/history', { params: { userId, ...params } });
  },

  // 获取任务统计
  getStatistics: (userId?: string): Promise<AxiosResponse<TaskStatistics>> => {
    return api.get('/tasks/statistics', { params: { userId } });
  },
};

// 用户 API
export const userApi = {
  // 获取用户列表
  getUsers: (params?: PaginationParams): Promise<AxiosResponse<PageResult<User>>> => {
    return api.get('/users', { params });
  },

  // 根据ID获取用户
  getUser: (id: string): Promise<AxiosResponse<User>> => {
    return api.get(`/users/${id}`);
  },

  // 创建用户
  createUser: (user: Partial<User>): Promise<AxiosResponse<User>> => {
    return api.post('/users', user);
  },

  // 更新用户
  updateUser: (id: string, user: Partial<User>): Promise<AxiosResponse<User>> => {
    return api.put(`/users/${id}`, user);
  },

  // 删除用户
  deleteUser: (id: string): Promise<AxiosResponse<void>> => {
    return api.delete(`/users/${id}`);
  },

  // 搜索用户
  searchUsers: (keyword: string): Promise<AxiosResponse<User[]>> => {
    return api.get('/users/search', { params: { keyword } });
  },
};

// 保单 API
export const policyApi = {
  // 获取保单列表
  getPolicies: (params?: PaginationParams): Promise<AxiosResponse<PageResult<InsurancePolicy>>> => {
    return api.get('/policies', { params });
  },

  // 根据保单号获取保单
  getPolicyByNumber: (policyNumber: string): Promise<AxiosResponse<InsurancePolicy>> => {
    return api.get(`/policies/by-number/${policyNumber}`);
  },

  // 根据ID获取保单
  getPolicy: (id: string): Promise<AxiosResponse<InsurancePolicy>> => {
    return api.get(`/policies/${id}`);
  },

  // 创建保单
  createPolicy: (policy: Partial<InsurancePolicy>): Promise<AxiosResponse<InsurancePolicy>> => {
    return api.post('/policies', policy);
  },

  // 更新保单
  updatePolicy: (id: string, policy: Partial<InsurancePolicy>): Promise<AxiosResponse<InsurancePolicy>> => {
    return api.put(`/policies/${id}`, policy);
  },
};

export default api;
