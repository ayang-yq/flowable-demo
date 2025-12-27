# 支付子流程BPMN在模型可视化中的体现方式

## 问题概述

用户询问：**支付子流程bpmn 在模型可视化里应如何体现**

## 当前实现方案

### 1. 模型层面

#### CMMN模型中的processTask节点

在CMMN案例模型中，支付子流程通过`processTask`节点体现：

```xml
<cmmn:planItem id="planItemStagePayment" name="Payment" definitionRef="stagePayment">
    <cmmn:entryCriterion id="entrySentryPayment" sentryRef="sentryPaymentEntry" />
</cmmn:planItem>

<cmmn:stage id="stagePayment" name="Payment">
    <cmmn:planItem id="planItemTaskProcessPayment" name="Process Claim Payment" 
                   definitionRef="taskProcessPayment" />
</cmmn:stage>

<cmmn:processTask id="taskProcessPayment" name="Process Claim Payment">
    <cmmn:processRefExpression><![CDATA[ClaimPaymentProcess]]></cmmn:processRefExpression>
    <cmmn:extensionElements>
        <flowable:in source="amount" target="amount" />
        <flowable:in source="reference" target="reference" />
        <flowable:in source="payeeName" target="payeeName" />
        <flowable:in source="claimId" target="claimId" />
        <flowable:in source="caseInstanceId" target="caseInstanceId" />
        <flowable:out source="paymentStatus" target="paymentStatus" />
    </cmmn:extensionElements>
</cmmn:processTask>
```

**关键点：**
- CMMN的`processTask`是CMMN和BPMN之间的桥梁
- 它启动名为`ClaimPaymentProcess`的BPMN流程
- 变量通过`<flowable:in>`和`<flowable:out>`在CMMN和BPMN之间传递

#### BPMN模型

`ClaimPaymentProcess.bpmn`包含支付流程的详细步骤：

```
StartEvent
  ↓
UserTask: preparePayment (准备支付)
  ↓
UserTask: validatePayment (验证支付)
  ↓
UserTask: executePayment (执行支付)
  ↓
UserTask: confirmPayment (确认支付)
  ↓
EndEvent
```

### 2. 可视化层面

#### 主界面：CMMN案例可视化

**体现方式：**
- 在CMMN流程图中，`Payment` Stage显示为一个独立的方框
- `Process Claim Payment`显示为Stage内的一个节点
- 节点状态通过颜色/图标表示：
  - ✅ 绿色：已完成
  - 🔵 蓝色：进行中
  - ⚪ 灰色：未激活

**状态显示：**
```typescript
// PlanItem状态映射
{
  "active": {
    color: "#3b82f6",      // 蓝色 - 进行中
    label: "进行中",
    icon: "🔄"
  },
  "completed": {
    color: "#10b981",      // 绿色 - 已完成
    label: "已完成",
    icon: "✅"
  },
  "available": {
    color: "#9ca3af",      // 灰色 - 待激活
    label: "待激活",
    icon: "⏳"
  }
}
```

#### 子流程展开：BPMN可视化

**交互方式：**
1. 用户在CMMN可视化界面中点击`Process Claim Payment`节点
2. 系统检测到这是`processTask`类型节点
3. 弹出子流程可视化窗口，显示对应的BPMN流程图
4. BPMN流程图中显示各个任务节点的执行状态

**实现代码：**
```typescript
// 在CmmnCaseVisualizer中
const handlePlanItemClick = (planItem: PlanItemStateDTO) => {
  if (planItem.type === 'processtask') {
    // 展开BPMN子流程
    setSelectedPlanItem(planItem);
    setShowSubprocessModal(true);
  } else {
    // 普通PlanItem的处理
    // ...
  }
};

// 弹出子流程可视化组件
<Modal isOpen={showSubprocessModal}>
  <BpmnSubprocessVisualizer planItemId={selectedPlanItem.id} />
</Modal>
```

**BPMN节点状态显示：**
```typescript
// ActivityStateDTO包含每个节点的状态信息
{
  "activityId": "preparePayment",
  "activityName": "Prepare Payment",
  "activityType": "userTask",
  "state": "active",        // active | completed
  "startTime": "2025-12-27T18:53:35",
  "endTime": null
}
```

### 3. 数据流转

#### CMMN → BPMN 数据传递

当`processTask`被激活时：

```
CMMN变量                              BPMN变量
---------                              ---------
caseInstanceId         →             caseInstanceId
amount                 →             amount
reference              →             reference
payeeName              →             payeeName
claimId                →             claimId
```

#### BPMN → CMMN 数据返回

当BPMN流程完成时：

```
BPMN变量                              CMMN变量
---------                              ---------
paymentStatus           →             paymentStatus
```

### 4. API设计

#### 后端API

```java
/**
 * 获取BPMN子流程可视化数据
 * GET /api/admin/cases/plan-items/{planItemId}/subprocess-visualization
 */
@GetMapping("/plan-items/{planItemId}/subprocess-visualization")
public BpmnSubprocessVisualizationDTO getSubprocessVisualization(
    @PathVariable String planItemId) {
    
    return caseRuntimeService.getSubprocessVisualizationData(planItemId);
}
```

#### 响应数据结构

```typescript
BpmnSubprocessVisualizationDTO {
  processInstanceId: string;
  processDefinitionId: string;
  processDefinitionKey: string;
  processDefinitionName: string;
  bpmnXml: string;                    // BPMN XML源码
  activityStates: ActivityStateDTO[]; // 各节点状态
  processInstanceState: string;        // active | suspended | completed
  startTime: string;
  endTime: string;
}

ActivityStateDTO {
  activityId: string;
  activityName: string;
  activityType: string;
  state: string;
  processInstanceId: string;
  startTime: string;
  endTime: string;
}
```

### 5. 可视化效果

#### CMMN主视图

```
┌─────────────────────────────────────────────────────────────┐
│                   Insurance Claim Case                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐               │
│  │ Triage   │→ │Investigat│→ │ Approval │ ✅             │
│  │   ✅     │  │   ⏳     │  │          │               │
│  └──────────┘  └──────────┘  └──────────┘               │
│       ↓                                 ↓                  │
│  ┌──────────────────────────────────────────┐              │
│  │         Payment Stage                    │              │
│  │  ┌─────────────────────────────┐        │              │
│  │  │ Process Claim Payment 🔄     │ ← 点击展开│          │
│  │  │    (BPMN Subprocess)        │        │              │
│  │  └─────────────────────────────┘        │              │
│  └──────────────────────────────────────────┘              │
│                      ↓                                     │
│  ┌──────────┐                                              │
│  │ Closure  │  ⏳                                         │
│  └──────────┘                                              │
└─────────────────────────────────────────────────────────────┘
```

#### 点击展开后的BPMN子流程视图

```
┌─────────────────────────────────────────────────────────────┐
│              ClaimPaymentProcess (BPMN)                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────┐                                            │
│  │   Start     │                                            │
│  └──────┬──────┘                                            │
│         ↓                                                     │
│  ┌─────────────────────┐  ✅ 已完成                         │
│  │  Prepare Payment    │                                    │
│  │    (paymentOfficer) │                                    │
│  └──────────┬──────────┘                                    │
│             ↓                                               │
│  ┌─────────────────────┐  🔄 进行中                         │
│  │  Validate Payment   │                                    │
│  │   (paymentManager)  │  ← 当前活动节点                   │
│  └──────────┬──────────┘                                    │
│             ↓                                               │
│  ┌─────────────────────┐  ⏳ 待激活                         │
│  │  Execute Payment    │                                    │
│  └──────────┬──────────┘                                    │
│             ↓                                               │
│  ┌─────────────────────┐  ⏳ 待激活                         │
│  │  Confirm Payment    │                                    │
│  └──────────┬──────────┘                                    │
│             ↓                                               │
│  ┌─────────────┐                                            │
│  │    End      │                                            │
│  └─────────────┘                                            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 优势与特点

### 1. 清晰的层次结构
- **CMMN层面**：显示案例的整体流程和阶段
- **BPMN层面**：显示子流程的详细执行步骤
- 两层视图通过点击交互连接，保持主界面的简洁

### 2. 状态可视化
- 使用颜色和图标直观展示执行状态
- 支持多节点并行执行的可视化
- 实时更新当前活动节点

### 3. 数据追溯
- 从CMMN Case到BPMN Process的完整数据链
- 支持查看变量的传递和流转
- 历史数据可追溯

### 4. 用户友好
- 点击交互，操作直观
- 弹出式子流程视图，不离开主界面
- 支持返回和切换

## 技术实现要点

### 1. PlanItem到ProcessInstance的关联

**挑战：** 如何从CMMN的PlanItem关联到BPMN的ProcessInstance？

**解决方案尝试：**
1. 通过`superProcessInstanceId`查询
2. 通过`caseInstanceId`变量查询
3. 通过时间匹配（PlanItem创建时间 ≈ Process创建时间）

**当前状态：** 正在调试中，详见`bpmn-subprocess-visualization-troubleshooting.md`

### 2. SVG渲染

- 解析BPMN XML中的DI（Diagram Interchange）信息
- 使用SVG动态绘制流程图
- 根据节点状态添加颜色高亮

### 3. 前端组件集成

- `CmmnCaseVisualizer`: CMMN主视图
- `BpmnSubprocessVisualizer`: BPMN子流程视图
- 通过Modal/Dialog组件集成

## 总结

支付子流程BPMN在模型可视化中的体现方式：

1. **在CMMN主视图中**：作为`processTask`节点显示，表示一个子流程的调用点
2. **点击交互**：用户可以点击processTask节点展开查看BPMN子流程
3. **子流程视图**：弹出独立的BPMN流程图，显示详细的支付步骤和执行状态
4. **状态同步**：CMMN和BPMN的执行状态实时同步显示

这种设计既保持了CMMN案例视图的简洁性，又提供了BPMN子流程的详细可视化能力，实现了层次化的流程监控和管理。
