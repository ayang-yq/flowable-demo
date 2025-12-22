# Flowable Admin 管理模块设计文档

## 📐 一、整体架构设计

### 1.1 模块定位

**Flowable Admin** 是一个技术管理员控制台,用于:
- 管理 Flowable 模型(CMMN/BPMN/DMN)的部署和版本
- 监控和管理运行态的 Case 和 Process 实例
- 可视化流程状态和执行路径
- 提供系统运行正确性与可控性保障

**与业务系统的关系**:
```
┌─────────────────────────────────────────────────────────┐
│                   Frontend (React)                       │
├──────────────────────┬──────────────────────────────────┤
│   Business UI        │      Admin Console UI            │
│  (理赔业务界面)       │   (技术管理控制台)                │
└──────────┬───────────┴──────────────┬───────────────────┘
           │                          │
           ▼                          ▼
┌──────────────────────┐   ┌──────────────────────────────┐
│  Business API        │   │      Admin API               │
│  /api/cases          │   │  /api/admin/models           │
│  /api/tasks          │   │  /api/admin/cases            │
│  /api/policies       │   │  /api/admin/processes        │
└──────────┬───────────┘   └──────────┬───────────────────┘
           │                          │
           │         ┌────────────────┘
           │         │
           ▼         ▼
    ┌──────────────────────────────┐
    │    Flowable Engine Layer     │
    │  (RepositoryService,         │
    │   RuntimeService,            │
    │   CmmnRuntimeService,        │
    │   HistoryService)            │
    └──────────────────────────────┘
```

### 1.2 模块划分

```
backend/src/main/java/com/flowable/demo/
├── admin/                          # Admin 模块根目录
│   ├── model/                      # Admin 领域模型
│   │   ├── ModelInfo.java         # 模型信息
│   │   ├── DeploymentInfo.java    # 部署信息
│   │   ├── CaseInstanceInfo.java  # Case 实例信息
│   │   └── ProcessInstanceInfo.java # Process 实例信息
│   ├── service/                    # Admin 业务服务
│   │   ├── ModelManagementService.java      # 模型管理
│   │   ├── CaseRuntimeService.java          # Case 运行态管理
│   │   ├── ProcessRuntimeService.java       # Process 运行态管理
│   │   └── DiagramVisualizationService.java # 可视化服务
│   ├── adapter/                    # Flowable 适配器
│   │   ├── FlowableRepositoryAdapter.java   # Repository 适配
│   │   ├── FlowableCmmnAdapter.java         # CMMN 适配
│   │   └── FlowableBpmnAdapter.java         # BPMN 适配
│   └── web/                        # Admin REST API
│       ├── AdminModelResource.java          # 模型管理 API
│       ├── AdminCaseResource.java           # Case 管理 API
│       ├── AdminProcessResource.java        # Process 管理 API
│       └── dto/                             # DTO 对象
│           ├── ModelDTO.java
│           ├── DeploymentDTO.java
│           ├── CaseInstanceDTO.java
│           └── ProcessInstanceDTO.java
```

### 1.3 分层职责

#### 1.3.1 Web Layer (REST API)
- 接收 HTTP 请求
- 参数验证
- 调用 Application Service
- 返回标准化响应

#### 1.3.2 Service Layer (Application Service)
- 业务逻辑编排
- 事务管理
- 调用 Flowable Adapter
- DTO 转换

#### 1.3.3 Adapter Layer (Flowable Adapter)
- 封装 Flowable API 调用
- 统一异常处理
- 数据转换
- 与 Flowable Engine 解耦

#### 1.3.4 Model Layer
- 领域模型定义
- 业务规则封装
- 不依赖 Flowable 具体实现

---

## 🔌 二、关键 REST API 设计

### 2.1 模型管理 API

#### 2.1.1 查询模型列表
```http
GET /api/admin/models?type={CMMN|BPMN|DMN}&page=0&size=20

Response:
{
  "content": [
    {
      "id": "model-uuid",
      "key": "ClaimCase",
      "name": "理赔案件流程",
      "type": "CMMN",
      "version": 3,
      "deployed": true,
      "latestDeploymentId": "deployment-uuid",
      "latestDeploymentTime": "2025-12-21T10:30:00",
      "createdBy": "admin",
      "lastModified": "2025-12-20T15:20:00"
    }
  ],
  "totalElements": 15,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

#### 2.1.2 获取模型详情
```http
GET /api/admin/models/{modelId}

Response:
{
  "id": "model-uuid",
  "key": "ClaimCase",
  "name": "理赔案件流程",
  "type": "CMMN",
  "version": 3,
  "deployed": true,
  "deployments": [
    {
      "id": "deployment-3",
      "version": 3,
      "deploymentTime": "2025-12-21T10:30:00",
      "deployedBy": "admin",
      "active": true
    },
    {
      "id": "deployment-2",
      "version": 2,
      "deploymentTime": "2025-12-15T09:00:00",
      "deployedBy": "admin",
      "active": false
    }
  ],
  "xmlContent": "<?xml version=\"1.0\" encoding=\"UTF-8\"?>..."
}
```

#### 2.1.3 部署模型
```http
POST /api/admin/models/{modelId}/deploy

Request:
{
  "tenantId": "insurance-dept",  // 可选
  "deploymentName": "ClaimCase-v3"  // 可选,默认自动生成
}

Response:
{
  "deploymentId": "deployment-uuid",
  "deploymentName": "ClaimCase-v3",
  "deploymentTime": "2025-12-21T11:00:00",
  "definitionId": "ClaimCase:3:def-uuid",
  "definitionKey": "ClaimCase",
  "version": 3
}
```

#### 2.1.4 禁用/启用模型
```http
PUT /api/admin/models/{modelId}/status

Request:
{
  "enabled": false,
  "reason": "流程升级维护中"
}

Response:
{
  "success": true,
  "message": "模型已禁用"
}
```

### 2.2 Case 运行态管理 API

#### 2.2.1 查询 Case 实例列表
```http
GET /api/admin/cases?
  caseDefinitionKey=ClaimCase&
  businessKey=CLM2024001&
  state=ACTIVE&
  startedAfter=2025-12-01&
  page=0&size=20

Response:
{
  "content": [
    {
      "id": "case-instance-uuid",
      "caseDefinitionKey": "ClaimCase",
      "caseDefinitionName": "理赔案件流程",
      "caseDefinitionVersion": 3,
      "businessKey": "CLM2024001",
      "state": "ACTIVE",
      "startTime": "2025-12-21T09:00:00",
      "startUserId": "handler1",
      "tenantId": null,
      "activePlanItems": 3,
      "completedPlanItems": 5
    }
  ],
  "totalElements": 42,
  "totalPages": 3,
  "size": 20,
  "number": 0
}
```

#### 2.2.2 获取 Case 实例详情
```http
GET /api/admin/cases/{caseInstanceId}

Response:
{
  "id": "case-instance-uuid",
  "caseDefinitionKey": "ClaimCase",
  "caseDefinitionName": "理赔案件流程",
  "caseDefinitionVersion": 3,
  "businessKey": "CLM2024001",
  "state": "ACTIVE",
  "startTime": "2025-12-21T09:00:00",
  "startUserId": "handler1",
  "variables": {
    "claimAmount": 50000,
    "policyType": "CAR_INSURANCE",
    "severity": "MEDIUM"
  },
  "planItemTree": {
    "id": "case-plan-model",
    "name": "理赔案件处理",
    "type": "STAGE",
    "state": "ACTIVE",
    "children": [
      {
        "id": "collect-documents",
        "name": "材料收集",
        "type": "HUMAN_TASK",
        "state": "COMPLETED",
        "completedTime": "2025-12-21T10:00:00"
      },
      {
        "id": "assess-loss",
        "name": "损失评估",
        "type": "HUMAN_TASK",
        "state": "ACTIVE",
        "assignee": "auditor1"
      },
      {
        "id": "payment-milestone",
        "name": "支付完成",
        "type": "MILESTONE",
        "state": "AVAILABLE"
      }
    ]
  }
}
```

#### 2.2.3 Case 操作 - 终止
```http
POST /api/admin/cases/{caseInstanceId}/terminate

Request:
{
  "reason": "客户撤销理赔申请"
}

Response:
{
  "success": true,
  "message": "Case 已终止",
  "terminatedTime": "2025-12-21T12:00:00"
}
```

#### 2.2.4 Case 操作 - 挂起/恢复
```http
POST /api/admin/cases/{caseInstanceId}/suspend
POST /api/admin/cases/{caseInstanceId}/resume

Response:
{
  "success": true,
  "state": "SUSPENDED"
}
```

### 2.3 Process 运行态管理 API

#### 2.3.1 查询 Process 实例列表
```http
GET /api/admin/processes?
  processDefinitionKey=ClaimPaymentProcess&
  businessKey=CLM2024001&
  page=0&size=20

Response:
{
  "content": [
    {
      "id": "process-instance-uuid",
      "processDefinitionKey": "ClaimPaymentProcess",
      "processDefinitionName": "理赔支付流程",
      "processDefinitionVersion": 2,
      "businessKey": "CLM2024001",
      "state": "ACTIVE",
      "startTime": "2025-12-21T11:00:00",
      "startUserId": "system",
      "currentActivityIds": ["validate-payment", "execute-payment"]
    }
  ],
  "totalElements": 28,
  "totalPages": 2
}
```

#### 2.3.2 获取 Process 实例详情
```http
GET /api/admin/processes/{processInstanceId}

Response:
{
  "id": "process-instance-uuid",
  "processDefinitionKey": "ClaimPaymentProcess",
  "processDefinitionName": "理赔支付流程",
  "processDefinitionVersion": 2,
  "businessKey": "CLM2024001",
  "state": "ACTIVE",
  "startTime": "2025-12-21T11:00:00",
  "variables": {
    "paymentAmount": 50000,
    "paymentMethod": "BANK_TRANSFER"
  },
  "currentActivities": [
    {
      "activityId": "validate-payment",
      "activityName": "支付校验",
      "activityType": "serviceTask",
      "startTime": "2025-12-21T11:00:00"
    }
  ],
  "completedActivities": [
    {
      "activityId": "start-event",
      "activityName": "开始",
      "activityType": "startEvent",
      "startTime": "2025-12-21T11:00:00",
      "endTime": "2025-12-21T11:00:01"
    }
  ]
}
```

#### 2.3.3 获取 BPMN 高亮数据
```http
GET /api/admin/processes/{processInstanceId}/diagram

Response:
{
  "processDefinitionId": "ClaimPaymentProcess:2:def-uuid",
  "diagramXml": "<?xml version=\"1.0\" encoding=\"UTF-8\"?>...",
  "highlightedActivities": ["validate-payment"],
  "highlightedFlows": ["flow-1", "flow-2"],
  "completedActivities": ["start-event"],
  "completedFlows": ["flow-start"]
}
```

### 2.4 统计分析 API

#### 2.4.1 获取系统统计
```http
GET /api/admin/statistics

Response:
{
  "models": {
    "total": 15,
    "cmmn": 5,
    "bpmn": 8,
    "dmn": 2
  },
  "deployments": {
    "total": 42,
    "lastDeploymentTime": "2025-12-21T10:30:00"
  },
  "cases": {
    "active": 28,
    "completed": 156,
    "suspended": 3,
    "terminated": 5
  },
  "processes": {
    "active": 15,
    "completed": 203,
    "suspended": 1
  }
}
```

---

## 🔧 三、后端核心实现

### 3.1 Flowable Adapter 层

#### 3.1.1 Repository Adapter
```java
@Component
public class FlowableRepositoryAdapter {
    
    private final RepositoryService repositoryService;
    private final CmmnRepositoryService cmmnRepositoryService;
    private final DmnRepositoryService dmnRepositoryService;
    
    /**
     * 获取所有 CMMN 模型
     */
    public List<Model> getCmmnModels() {
        return cmmnRepositoryService.createModelQuery()
            .modelType(CmmnModel.MODEL_TYPE_CMMN)
            .orderByLastUpdateTime().desc()
            .list();
    }
    
    /**
     * 部署 CMMN 模型
     */
    public Deployment deployCmmnModel(String modelId, String deploymentName, String tenantId) {
        Model model = cmmnRepositoryService.getModel(modelId);
        byte[] modelXml = cmmnRepositoryService.getModelEditorSource(modelId);
        
        CmmnDeploymentBuilder builder = cmmnRepositoryService.createDeployment()
            .name(deploymentName != null ? deploymentName : generateDeploymentName(model))
            .addBytes(model.getKey() + ".cmmn", modelXml);
            
        if (tenantId != null) {
            builder.tenantId(tenantId);
        }
        
        return builder.deploy();
    }
    
    /**
     * 获取模型的所有部署版本
     */
    public List<CaseDefinition> getCaseDefinitionVersions(String caseDefinitionKey) {
        return cmmnRepositoryService.createCaseDefinitionQuery()
            .caseDefinitionKey(caseDefinitionKey)
            .orderByVersion().desc()
            .list();
    }
}
```

#### 3.1.2 CMMN Runtime Adapter
```java
@Component
public class FlowableCmmnAdapter {
    
    private final CmmnRuntimeService cmmnRuntimeService;
    private final CmmnHistoryService cmmnHistoryService;
    
    /**
     * 查询 Case 实例(支持多条件)
     */
    public Page<CaseInstance> queryCaseInstances(
        String caseDefinitionKey,
        String businessKey,
        String state,
        LocalDateTime startedAfter,
        Pageable pageable
    ) {
        CaseInstanceQuery query = cmmnRuntimeService.createCaseInstanceQuery();
        
        if (caseDefinitionKey != null) {
            query.caseDefinitionKey(caseDefinitionKey);
        }
        if (businessKey != null) {
            query.caseInstanceBusinessKey(businessKey);
        }
        if (state != null) {
            query.caseInstanceState(state);
        }
        if (startedAfter != null) {
            query.caseInstanceStartedAfter(Date.from(startedAfter.atZone(ZoneId.systemDefault()).toInstant()));
        }
        
        long total = query.count();
        List<CaseInstance> instances = query
            .orderByStartTime().desc()
            .listPage((int) pageable.getOffset(), pageable.getPageSize());
            
        return new PageImpl<>(instances, pageable, total);
    }
    
    /**
     * 获取 Case Plan Item Tree
     */
    public PlanItemTreeNode getCasePlanItemTree(String caseInstanceId) {
        List<PlanItemInstance> planItems = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstanceId)
            .list();
            
        // 构建树结构
        return buildPlanItemTree(planItems);
    }
    
    /**
     * 终止 Case
     */
    public void terminateCase(String caseInstanceId, String reason) {
        cmmnRuntimeService.terminateCaseInstance(caseInstanceId);
        // 记录终止原因到历史
        cmmnRuntimeService.setVariable(caseInstanceId, "terminationReason", reason);
    }
    
    /**
     * 挂起 Case
     */
    public void suspendCase(String caseInstanceId) {
        cmmnRuntimeService.suspendCaseInstance(caseInstanceId);
    }
    
    /**
     * 恢复 Case
     */
    public void resumeCase(String caseInstanceId) {
        cmmnRuntimeService.resumeCaseInstance(caseInstanceId);
    }
}
```

#### 3.1.3 BPMN Runtime Adapter
```java
@Component
public class FlowableBpmnAdapter {
    
    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final RepositoryService repositoryService;
    
    /**
     * 获取流程实例的高亮数据
     */
    public ProcessDiagramHighlightData getProcessDiagramHighlight(String processInstanceId) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
            .processInstanceId(processInstanceId)
            .singleResult();
            
        if (processInstance == null) {
            // 查询历史
            HistoricProcessInstance historicInstance = historyService
                .createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
            return getHistoricProcessHighlight(historicInstance);
        }
        
        // 当前活动节点
        List<String> activeActivityIds = runtimeService.getActiveActivityIds(processInstanceId);
        
        // 已完成的活动节点
        List<HistoricActivityInstance> completedActivities = historyService
            .createHistoricActivityInstanceQuery()
            .processInstanceId(processInstanceId)
            .finished()
            .list();
            
        List<String> completedActivityIds = completedActivities.stream()
            .map(HistoricActivityInstance::getActivityId)
            .distinct()
            .collect(Collectors.toList());
        
        // 高亮的流程线(Sequence Flow)
        List<String> highlightedFlows = getHighlightedFlows(processInstanceId, completedActivities);
        
        // 获取流程图 XML
        ProcessDefinition processDefinition = repositoryService.getProcessDefinition(
            processInstance.getProcessDefinitionId()
        );
        InputStream diagramStream = repositoryService.getProcessModel(processDefinition.getId());
        String diagramXml = new String(diagramStream.readAllBytes(), StandardCharsets.UTF_8);
        
        return ProcessDiagramHighlightData.builder()
            .processDefinitionId(processInstance.getProcessDefinitionId())
            .diagramXml(diagramXml)
            .highlightedActivities(activeActivityIds)
            .completedActivities(completedActivityIds)
            .highlightedFlows(highlightedFlows)
            .build();
    }
    
    /**
     * 计算高亮的 Sequence Flow
     */
    private List<String> getHighlightedFlows(
        String processInstanceId,
        List<HistoricActivityInstance> completedActivities
    ) {
        // 基于已完成活动的顺序,推断已执行的 Sequence Flow
        List<String> flows = new ArrayList<>();
        
        for (int i = 0; i < completedActivities.size() - 1; i++) {
            HistoricActivityInstance current = completedActivities.get(i);
            HistoricActivityInstance next = completedActivities.get(i + 1);
            
            // 查找连接这两个活动的 Sequence Flow
            // 这需要解析 BPMN XML 或使用 Flowable BpmnModel API
            String flowId = findSequenceFlowBetween(current.getActivityId(), next.getActivityId());
            if (flowId != null) {
                flows.add(flowId);
            }
        }
        
        return flows;
    }
}
```

### 3.2 Application Service 层

#### 3.2.1 Model Management Service
```java
@Service
@Transactional
public class ModelManagementService {
    
    private final FlowableRepositoryAdapter repositoryAdapter;
    
    /**
     * 查询模型列表
     */
    public Page<ModelDTO> queryModels(String type, Pageable pageable) {
        List<Model> models;
        
        switch (type) {
            case "CMMN":
                models = repositoryAdapter.getCmmnModels();
                break;
            case "BPMN":
                models = repositoryAdapter.getBpmnModels();
                break;
            case "DMN":
                models = repositoryAdapter.getDmnModels();
                break;
            default:
                models = repositoryAdapter.getAllModels();
        }
        
        // 转换为 DTO 并分页
        List<ModelDTO> dtos = models.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
            
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), dtos.size());
        
        return new PageImpl<>(
            dtos.subList(start, end),
            pageable,
            dtos.size()
        );
    }
    
    /**
     * 部署模型
     */
    public DeploymentDTO deployModel(String modelId, DeploymentRequest request) {
        Model model = repositoryAdapter.getModel(modelId);
        
        Deployment deployment;
        switch (model.getModelType()) {
            case "CMMN":
                deployment = repositoryAdapter.deployCmmnModel(
                    modelId,
                    request.getDeploymentName(),
                    request.getTenantId()
                );
                break;
            case "BPMN":
                deployment = repositoryAdapter.deployBpmnModel(
                    modelId,
                    request.getDeploymentName(),
                    request.getTenantId()
                );
                break;
            case "DMN":
                deployment = repositoryAdapter.deployDmnModel(
                    modelId,
                    request.getDeploymentName(),
                    request.getTenantId()
                );
                break;
            default:
                throw new IllegalArgumentException("Unsupported model type: " + model.getModelType());
        }
        
        return convertToDeploymentDTO(deployment);
    }
    
    /**
     * 获取模型的所有版本
     */
    public List<DefinitionVersionDTO> getModelVersions(String modelKey, String modelType) {
        switch (modelType) {
            case "CMMN":
                return repositoryAdapter.getCaseDefinitionVersions(modelKey).stream()
                    .map(this::convertToCaseVersionDTO)
                    .collect(Collectors.toList());
            case "BPMN":
                return repositoryAdapter.getProcessDefinitionVersions(modelKey).stream()
                    .map(this::convertToProcessVersionDTO)
                    .collect(Collectors.toList());
            default:
                throw new IllegalArgumentException("Unsupported model type: " + modelType);
        }
    }
}
```

#### 3.2.2 Case Runtime Service
```java
@Service
@Transactional(readOnly = true)
public class CaseRuntimeService {
    
    private final FlowableCmmnAdapter cmmnAdapter;
    
    /**
     * 查询 Case 实例
     */
    public Page<CaseInstanceDTO> queryCaseInstances(CaseQueryRequest request, Pageable pageable) {
        Page<CaseInstance> instances = cmmnAdapter.queryCaseInstances(
            request.getCaseDefinitionKey(),
            request.getBusinessKey(),
            request.getState(),
            request.getStartedAfter(),
            pageable
        );
        
        return instances.map(this::convertToDTO);
    }
    
    /**
     * 获取 Case 实例详情
     */
    public CaseInstanceDetailDTO getCaseInstanceDetail(String caseInstanceId) {
        CaseInstance caseInstance = cmmnAdapter.getCaseInstance(caseInstanceId);
        PlanItemTreeNode planItemTree = cmmnAdapter.getCasePlanItemTree(caseInstanceId);
        Map<String, Object> variables = cmmnAdapter.getCaseVariables(caseInstanceId);
        
        return CaseInstanceDetailDTO.builder()
            .id(caseInstance.getId())
            .caseDefinitionKey(caseInstance.getCaseDefinitionKey())
            .caseDefinitionName(caseInstance.getCaseDefinitionName())
            .caseDefinitionVersion(caseInstance.getCaseDefinitionVersion())
            .businessKey(caseInstance.getBusinessKey())
            .state(caseInstance.getState())
            .startTime(caseInstance.getStartTime())
            .startUserId(caseInstance.getStartUserId())
            .variables(variables)
            .planItemTree(planItemTree)
            .build();
    }
    
    /**
     * 终止 Case
     */
    @Transactional
    public void terminateCase(String caseInstanceId, String reason) {
        cmmnAdapter.terminateCase(caseInstanceId, reason);
    }
    
    /**
     * 挂起/恢复 Case
     */
    @Transactional
    public void suspendCase(String caseInstanceId) {
        cmmnAdapter.suspendCase(caseInstanceId);
    }
    
    @Transactional
    public void resumeCase(String caseInstanceId) {
        cmmnAdapter.resumeCase(caseInstanceId);
    }
}
```

---

## 🎨 四、前端页面结构

### 4.1 菜单结构

```
Admin 控制台
├── 📊 Dashboard (仪表盘)
│   └── 系统统计概览
├── 📦 模型管理
│   ├── CMMN 模型
│   ├── BPMN 模型
│   └── DMN 模型
├── 🔄 运行态管理
│   ├── Case 实例
│   │   ├── 实例列表
│   │   └── 实例详情
│   └── Process 实例
│       ├── 实例列表
│       ├── 实例详情
│       └── 流程图可视化
└── 📈 统计分析
    ├── 部署历史
    └── 执行统计
```

### 4.2 主要页面

#### 4.2.1 模型列表页
- **功能**: 展示所有模型,支持筛选(类型/状态)
- **操作**: 部署、查看详情、查看版本历史
- **表格列**: Model Key | Name | Type | Version | Deployed | Last Modified

#### 4.2.2 模型详情页
- **Tab 1 - 基本信息**: Key, Name, Type, Version
- **Tab 2 - 部署历史**: 所有部署版本,标记当前激活版本
- **Tab 3 - XML 内容**: 只读展示

#### 4.2.3 Case 实例列表页
- **功能**: 查询 Case 实例,支持多条件筛选
- **操作**: 查看详情、终止、挂起/恢复
- **表格列**: Business Key | Definition | State | Start Time | Active Items

#### 4.2.4 Case 实例详情页
- **Tab 1 - 基本信息**: Definition, Business Key, State, Variables
- **Tab 2 - Plan Item Tree**: 树形结构展示,状态标识
- **Tab 3 - 历史记录**: 操作日志

#### 4.2.5 Process 实例详情页
- **Tab 1 - 基本信息**: Definition, Business Key, State, Variables
- **Tab 2 - 流程图**: BPMN 可视化,高亮当前节点
- **Tab 3 - 活动历史**: 已完成/进行中的活动

---

## 🚀 五、可扩展点

### 5.1 多租户支持
- 在部署时指定 `tenantId`
- 查询时过滤 `tenantId`
- 前端增加租户选择器

### 5.2 权限控制
```java
@PreAuthorize("hasRole('ADMIN')")  // 完全控制
public void deployModel(...) { }

@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")  // 查看权限
public Page<ModelDTO> queryModels(...) { }

@PreAuthorize("hasRole('ADMIN')")  // 危险操作
public void terminateCase(...) { }
```

### 5.3 审计日志
- 记录所有管理操作(部署、终止、挂起等)
- 存储操作人、操作时间、操作原因

### 5.4 通知机制
- Case 异常终止时发送告警
- 长时间运行的 Case 预警
- 部署成功/失败通知

---

## ⚠️ 技术约束说明

### 不适合直接用 Flowable API 实现的功能

#### 1. CMMN 可视化图形生成
**问题**: Flowable 7.x 没有提供 CMMN 的图形渲染 API(只有 BPMN)

**替代方案**:
- 使用 `CmmnModel` API 解析 CMMN XML
- 提取 Stage/Task/Milestone 结构
- 前端使用树形组件(Ant Design Tree)渲染
- 用颜色/图标标识状态(Active/Completed/Available)

#### 2. 模型在线编辑
**问题**: 需要 Flowable Modeler UI,与"不使用 UI 模块"冲突

**替代方案**:
- Admin 模块只管理已存在的模型
- 模型设计由外部工具完成(Flowable Modeler / Camunda Modeler)
- 通过 REST API 导入模型 XML

#### 3. 实时流程监控
**问题**: Flowable 不提供 WebSocket 推送

**替代方案**:
- 前端定时轮询(每 5-10 秒)
- 或集成 Spring WebSocket 自行实现

---

## 📋 实施步骤

1. **Phase 1 - 基础架构** (1-2 天)
   - 创建 admin 模块目录结构
   - 实现 Adapter 层
   - 实现基础 DTO

2. **Phase 2 - 模型管理** (2-3 天)
   - 实现模型查询 API
   - 实现模型部署 API
   - 前端模型列表/详情页

3. **Phase 3 - Case 管理** (3-4 天)
   - 实现 Case 查询 API
   - 实现 Case 操作 API
   - 前端 Case 列表/详情页
   - Plan Item Tree 可视化

4. **Phase 4 - Process 管理** (2-3 天)
   - 实现 Process 查询 API
   - 实现 BPMN 高亮数据 API
   - 前端流程图可视化

5. **Phase 5 - 统计与优化** (1-2 天)
   - 实现统计 API
   - Dashboard 页面
   - 性能优化

**总计**: 约 9-14 天
