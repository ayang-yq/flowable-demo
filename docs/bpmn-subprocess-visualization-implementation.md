# BPMN 子流程可视化实现文档

## 概述

本文档描述了如何在 CMMN 案例模型可视化中展开和显示 BPMN 子流程的功能。当用户在 CMMN 可视化中点击 `processTask` 节点时，系统会弹出子流程可视化窗口，展示对应的 BPMN 流程图及其活动节点状态。

## 功能说明

### 1. 业务背景

在保险理赔系统中，CMMN 案例模型通过 `processTask` 调用 BPMN 流程来执行具体的业务流程，例如：

- **ClaimPaymentProcess**: 理赔支付流程，包含以下环节：
  - Payment Processing (支付处理)
  - Payment Approval (支付审批)
  - Payment Execution (支付执行)

### 2. 可视化实现

#### 2.1 前端组件

**文件**: `frontend/src/components/admin/BpmnSubprocessVisualizer.tsx`

组件负责展示 BPMN 子流程的可视化，包括：

- **流程信息展示**: 流程定义名称、实例ID、状态、开始/结束时间
- **SVG 流程图**: 使用简化版 SVG 渲染器绘制流程图，包含：
  - 背景网格
  - 流程标题
  - 活动节点（带状态颜色）
  - 节点间的连接线
  - 序号圆圈
  - 状态标签
- **活动节点状态列表**: 以卡片形式展示所有活动节点的详细信息

**状态颜色方案**:
- 🟢 **绿色** (`#4CAF50`): Active - 当前执行中的活动
- 🔵 **蓝色** (`#2196F3`): Completed - 已完成的活动
- ⚪ **灰色** (`#9E9E9E`): Available - 可用但未开始的活动

#### 2.2 后端 API

**文件**: `backend/src/main/java/com/flowable/demo/admin/service/CaseRuntimeService.java`

##### API 方法

```java
public BpmnSubprocessVisualizationDTO getSubprocessVisualizationData(String planItemInstanceId)
```

##### 处理流程

1. **获取 PlanItem 实例**
   - 通过 `planItemInstanceId` 查询对应的 PlanItem
   - 验证是否为 `processTask` 类型

2. **获取关联的 Process 实例**
   - 首先查询运行态流程实例
   - 如果不存在，查询历史态流程实例

3. **获取流程定义**
   - 根据流程定义 ID 获取流程定义信息

4. **获取 BPMN XML**
   - 从部署资源中读取 BPMN 模型 XML 内容
   - 用于前端渲染流程图

5. **获取活动节点状态**
   - 查询运行态活动（状态为 active）
   - 查询历史态活动（状态为 completed）
   - 合并所有活动节点信息

##### DTO 定义

**文件**: `backend/src/main/java/com/flowable/demo/admin/web/dto/BpmnSubprocessVisualizationDTO.java`

```java
public class BpmnSubprocessVisualizationDTO {
    private String processInstanceId;          // 流程实例 ID
    private String processDefinitionId;        // 流程定义 ID
    private String processDefinitionKey;       // 流程定义 Key
    private String processDefinitionName;      // 流程定义名称
    private String bpmnXml;                   // BPMN XML 内容
    private List<ActivityStateDTO> activityStates;  // 活动节点状态列表
    private String processInstanceState;       // 流程实例状态
    private String startTime;                 // 开始时间
    private String endTime;                   // 结束时间
}
```

**ActivityStateDTO** (`backend/src/main/java/com/flowable/demo/admin/web/dto/ActivityStateDTO.java`):

```java
public class ActivityStateDTO {
    private String activityId;         // 活动 ID
    private String activityName;       // 活动名称
    private String activityType;       // 活动类型
    private String state;              // 状态: active | completed | available
    private String processInstanceId;  // 流程实例 ID
    private String startTime;         // 开始时间
    private String endTime;           // 结束时间
}
```

#### 2.3 REST API 端点

**文件**: `backend/src/main/java/com/flowable/demo/admin/web/AdminCaseResource.java`

```java
@GetMapping("/cases/{caseInstanceId}/plan-items/{planItemInstanceId}/subprocess-visualization")
public ResponseEntity<BpmnSubprocessVisualizationDTO> getSubprocessVisualization(
        @PathVariable String caseInstanceId,
        @PathVariable String planItemInstanceId) {
    BpmnSubprocessVisualizationDTO visualization = 
        caseRuntimeService.getSubprocessVisualizationData(planItemInstanceId);
    return ResponseEntity.ok(visualization);
}
```

#### 2.4 前端 API 调用

**文件**: `frontend/src/services/adminApi.ts`

```typescript
export const caseApi = {
  // ... 其他方法
  
  getSubprocessVisualization: (planItemInstanceId: string) => 
    axios.get<ApiResponse<SubprocessVisualization>>(
      `/api/admin/cases/plan-items/${planItemInstanceId}/subprocess-visualization`
    )
};
```

#### 2.5 CMMN 可视化集成

**文件**: `frontend/src/components/admin/CmmnCaseVisualizer.tsx`

在 CMMN 可视化中，为 `processTask` 类型的节点添加点击事件处理：

```typescript
const handleNodeClick = (planItem: PlanItemStateDTO) => {
  if (planItem.type === 'processTask') {
    // 打开 BPMN 子流程可视化
    setSelectedPlanItemForSubprocess(planItem.id);
    setShowSubprocessVisualizer(true);
  }
};

// 渲染子流程可视化弹窗
{showSubprocessVisualizer && selectedPlanItemForSubprocess && (
  <BpmnSubprocessVisualizer
    planItemInstanceId={selectedPlanItemForSubprocess}
    onClose={() => {
      setShowSubprocessVisualizer(false);
      setSelectedPlanItemForSubprocess(null);
    }}
  />
)}
```

## 使用示例

### 1. 查看案例详情

用户导航到 Admin Dashboard → Case Instances → 点击某个案例进入详情页

### 2. 打开案例可视化

在案例详情页中，点击 "CMMN Visualization" 标签页查看案例模型

### 3. 展开子流程

在 CMMN 可视化中：
- 找到 `processTask` 类型的节点（例如 "Payment Processing"）
- 点击该节点
- 系统弹出 BPMN 子流程可视化窗口

### 4. 查看子流程详情

在弹出的窗口中：
- 查看流程定义名称和实例 ID
- 查看流程状态（Active/Completed/Suspended）
- 查看流程开始和结束时间
- 查看流程图，活动节点根据状态显示不同颜色
- 查看底部活动节点状态列表

## 技术实现细节

### 1. 活动节点状态查询

系统通过以下方式获取活动节点状态：

```java
private List<ActivityStateDTO> getActivityStates(String processInstanceId) {
    // 获取运行态活动（状态为 active）
    List<ActivityInstance> runtimeActivities = 
        runtimeService.createActivityInstanceQuery()
            .processInstanceId(processInstanceId)
            .list();

    // 获取历史态活动（状态为 completed）
    List<HistoricActivityInstance> historicActivities = 
        historyService.createHistoricActivityInstanceQuery()
            .processInstanceId(processInstanceId)
            .orderByHistoricActivityInstanceStartTime()
            .asc()
            .list();

    // 合并去重
    Map<String, ActivityStateDTO> activityStateMap = new HashMap<>();
    
    // ... 合并逻辑
}
```

### 2. BPMN XML 获取

通过 Flowable Repository Service 获取部署的 BPMN 资源：

```java
String bpmnXml = repositoryAdapter.getProcessDefinitionResourceContent(
        processDefinition.getDeploymentId(),
        processDefinition.getResourceName()
);
```

### 3. 简化版 SVG 渲染

由于 bpmn-js 的复杂配置和类型定义问题，当前实现使用简化版 SVG 渲染器：

- 直接使用 JavaScript DOM API 创建 SVG 元素
- 根据活动节点状态动态绘制节点
- 添加连接线、箭头、阴影等视觉效果

### 4. 流程实例与 PlanItem 的关联

流程实例与 CMMN PlanItem 的关联通过 `superProcessInstanceId` 字段建立：

```java
ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
        .superProcessInstanceId(planItem.getCaseInstanceId())
        .singleResult();
```

## 后续改进方向

### 1. 使用 bpmn-js 完整渲染

当前使用简化版 SVG 渲染，未来可以：

- 安装 bpmn-js 类型定义
- 配置 bpmn-js Viewer
- 导入并渲染完整的 BPMN 模型
- 使用 bpmn-js 的样式和布局

### 2. 添加更多交互功能

- 支持缩放和平移
- 支持点击节点查看详情
- 支持流程图导出为图片
- 支持流程变量查看

### 3. 支持更多流程类型

- 支持嵌套子流程展开
- 支持调用活动可视化
- 支持事件子流程可视化

### 4. 性能优化

- 添加缓存机制
- 优化 SVG 渲染性能
- 懒加载子流程数据

## 测试建议

### 1. 单元测试

```java
@Test
void testGetSubprocessVisualizationData() {
    String planItemInstanceId = "test-plan-item-id";
    BpmnSubprocessVisualizationDTO result = 
        caseRuntimeService.getSubprocessVisualizationData(planItemInstanceId);
    
    assertNotNull(result);
    assertEquals("ClaimPaymentProcess", result.getProcessDefinitionName());
    assertFalse(result.getActivityStates().isEmpty());
}
```

### 2. 集成测试

- 创建包含 processTask 的 CMMN 案例
- 启动案例实例
- 验证子流程可视化 API 返回正确数据
- 验证前端组件正确渲染流程图

### 3. 端到端测试

- 通过用户界面打开案例详情
- 点击 processTask 节点
- 验证子流程可视化窗口正确显示
- 验证活动节点状态正确显示

## 总结

BPMN 子流程可视化功能为管理员提供了查看 CMMN 案例中 BPMN 子流程执行状态的便捷方式。通过点击 processTask 节点，用户可以直观地了解子流程的执行进度、活动节点状态以及流程的整体状态，从而更好地进行案例管理和监控。

该功能的实现遵循了以下原则：
- **简洁性**: 使用简化版 SVG 渲染器，降低复杂度
- **完整性**: 获取并展示所有活动节点状态
- **可扩展性**: 预留了使用 bpmn-js 的接口
- **用户体验**: 提供直观的可视化界面和详细的节点信息
