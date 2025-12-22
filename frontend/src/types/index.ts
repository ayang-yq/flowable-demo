export interface User {
  id: string;
  username: string;
  email: string;
  fullName: string;
  department: string;
  roles: string[];
  status: 'ACTIVE' | 'INACTIVE';
  createdAt: string;
  updatedAt: string;
}

export interface InsurancePolicy {
  id: string;
  policyNumber: string;
  policyholderName: string;
  policyholderId: string;
  policyType: string;
  coverageAmount: number;
  premium: number;
  startDate: string;
  endDate: string;
  status: 'ACTIVE' | 'EXPIRED' | 'CANCELLED';
  createdAt: string;
  updatedAt: string;
}

export interface ClaimCase {
  id: string;
  claimNumber: string;
  policy?: InsurancePolicy;
  claimType: string;
  severity: 'HIGH' | 'MEDIUM' | 'LOW';
  status: 'DRAFT' | 'SUBMITTED' | 'PENDING' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED' | 'PAID' | 'CLOSED';
  claimantName: string;
  claimantPhone: string;
  claimantEmail: string;
  incidentDate: string;
  incidentLocation: string;
  incidentDescription: string;
  claimedAmount: number;
  approvedAmount?: number;
  paidAmount?: number;
  assignedToId?: string;
  assignedToName?: string;
  createdById: string;
  createdByName: string;
  createdAt: string;
  updatedAt: string;
  dueDate?: string;
  caseInstanceId?: string;
  documents: ClaimDocument[];
  histories: ClaimHistory[];
}

export interface ClaimDocument {
  id: string;
  claimCase: ClaimCase;
  fileName: string;
  fileType: string;
  fileSize: number;
  filePath: string;
  description?: string;
  uploadedBy: User;
  uploadedAt: string;
  version: number;
}

export interface ClaimHistory {
  id: string;
  claimCase: ClaimCase;
  action: string;
  description: string;
  oldStatus?: string;
  newStatus?: string;
  operator: User;
  operatedAt: string;
}

export interface FlowableTask {
  id: string;
  name: string;
  description?: string;
  assignee?: string;
  owner?: string;
  processInstanceId?: string;
  caseInstanceId?: string;
  taskDefinitionKey: string;
  formKey?: string;
  priority: number;
  createTime: string;
  dueDate?: string;
  category?: string;
  tenantId?: string;
  suspended: boolean;
  candidateUsers: string[];
  candidateGroups: string[];
}

export interface TaskStatistics {
  myTasksCount: number;
  claimableTasksCount: number;
  todayCompletedCount: number;
  totalActiveTasks: number;
}

export interface DashboardStatistics {
  totalClaims: number;
  pendingClaims: number;
  approvedClaims: number;
  rejectedClaims: number;
  totalAmount: number;
  averageProcessingTime: number;
}

export interface PaginationParams {
  page: number;
  size: number;
  sort?: string;
  direction?: 'ASC' | 'DESC';
}

export interface PageResult<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface ApiResponse<T> {
  data: T;
  success: boolean;
  message?: string;
  code?: number;
}

export interface ErrorResponse {
  code: number;
  message: string;
  details?: string;
  timestamp: string;
}
