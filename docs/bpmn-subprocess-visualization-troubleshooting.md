# BPMN子流程可视化 - 问题排查和解决方案

## 问题描述

在CMMN案例模型中，`processTask`节点用于启动BPMN子流程。在可视化时，需要能够：

1. 在CMMN流程图中识别processTask节点
2. 点击processTask时展开显示其对应的BPMN子流程图
3. 在BPMN子流程图中显示当前活动节点和已完成节点的状态

## 当前实现状态

### 1. 后端实现

#### 已完成：
- ✅ 创建了`BpmnSubprocessVisualizationDTO`数据传输对象
- ✅ 创建了`ActivityStateDTO`用于表示活动节点状态
- ✅ 在`CaseRuntimeService`中实现了`getSubprocessVisualizationData()`方法
- ✅ 在`FlowableRepositoryAdapter`中实现了`getProcessDefinitionResourceContent()`方法
- ✅ 在`AdminCaseResource`中添加了REST API端点

#### API端点：
```
GET /api/admin/cases/plan-items/{planItemId}/subprocess-visualization
```

#### 核心问题：Process实例关联

当前遇到的主要问题是如何从`PlanItemInstance`关联到对应的`ProcessInstance`。

**尝试的方法：**

1. **方法1：通过superProcessInstanceId**
   - 查询条件：`superProcessInstanceId = planItem.getCaseInstanceId()`
   - 问题：Flowable在processTask启动BPMN流程时，可能不会设置superProcessInstanceId

2. **方法2：通过变量关联**
   - 查询条件：`caseInstanceId = planItem.getCaseInstanceId()`
   - 问题：processTask启动的BPMN流程实例中可能没有设置caseInstanceId变量

3. **方法3：通过时间匹配**
   - 查询条件：比较processTask创建时间和Process实例启动时间
   - 问题：如果时间差较大，可能匹配不到正确的实例

### 2. 前端实现

#### 已完成：
- ✅ 创建了`BpmnSubprocessVisualizer`组件
- ✅ 实现了简单的SVG渲染（基于XML解析）
- ✅ 在`adminApi.ts`中添加了API调用方法
- ✅ 在`CmmnCaseVisualizer`中集成了子流程展开功能

### 3. 模型设计

#### CMMN模型 (ClaimCase.cmmn)
```xml
<cmmn:processTask id="taskProcessPayment" name="Process Claim Payment">
    <cmmn:processRefExpression><![CDATA[ClaimPaymentProcess]]></cmmn:processRefExpression>
    <cmmn:extensionElements>
        <flowable:in source="amount" target="amount" />
        <flowable:in source="reference" target="reference" />
        <flowable:in source="payeeName" target="payeeName" />
        <flowable:in source="claimId" target="claimId" />
        <flowable:in source="paymentOfficer" target="paymentOfficer" />
        <flowable:in source="paymentManager" target="paymentManager" />
        <flowable:in source="caseInstanceId" target="caseInstanceId" />
        <flowable:out source="paymentStatus" target="paymentStatus" />
    </cmmn:extensionElements>
</cmmn:processTask>
```

#### BPMN模型 (ClaimPaymentProcess.bpmn)
标准的BPMN流程，包含：
- StartEvent
- UserTask: Prepare Payment
- UserTask: Validate Payment  
- UserTask: Execute Payment
- UserTask: Confirm Payment
- EndEvent

## 根本原因分析

### Flowable processTask的工作原理

当CMMN中的processTask被激活时：

1. **启动BPMN流程实例**
   - Flowable创建一个新的Process实例
   - 传入processTask定义的参数（通过`<flowable:in>`元素）

2. **关联机制**
   - **理论上**：Process实例的`superProcessInstanceId`应该指向CMMN Case实例ID
   - **实际上**：这可能取决于Flowable版本和配置

3. **变量传递**
   - processTask定义中通过`<flowable:in>`传入的变量会在Process实例中可用
   - 包括：`caseInstanceId`, `amount`, `reference`, `payeeName`等

### 当前数据状态

根据测试数据：

**CMMN PlanItem (processTask):**
- ID: `495acfdd-e312-11f0-ade3-005056c00001`
- Type: `processtask`
- Case Instance ID: `42044d32-e312-11f0-ade3-005056c00001`
- Create Time: `2025-12-27T18:53:32.809`
- State: `active`

**Process实例:**
- ID: `495b1dfe-e312-11f0-ade3-005056c00001`
- Process Definition Key: `ClaimPaymentProcess`
- Create Time: `2025-12-27T18:53:32.911`
- State: `active`
- Current Activity: `userTask_validatePayment`

**时间差分析：**
```
PlanItem创建时间: 2025-12-27T18:53:32.809
Process创建时间:  2025-12-27T18:53:32.911
时间差: 约 102毫秒
```

这个时间差表明它们是同时创建的，PlanItem先创建，Process紧随其后。

### 问题诊断

添加了详细调试日志后，应该能够看到：

1. PlanItem的基本信息
2. 方法1-3查询的结果
3. 所有Process实例的列表和时间差
4. 历史Process实例的查询结果

这些信息将帮助我们确定：
- superProcessInstanceId是否被正确设置
- caseInstanceId变量是否被正确传递
- 时间匹配是否可行

## 可能的解决方案

### 方案1：检查Flowable配置

确保Flowable引擎正确配置了processTask和BPMN流程的关联。

### 方案2：直接查询PlanItem变量

Flowable CMMN引擎可能提供了直接获取processTask关联Process实例的API。

### 方案3：使用HistoricProcessInstance查询

即使当前Process实例无法通过上述方法查询到，历史实例中可能保存了完整的关联信息。

### 方案4：优化时间匹配算法

- 扩大时间匹配窗口（如从30秒扩大到2分钟）
- 结合Process Definition Key进行匹配
- 考虑使用Case的业务键（Business Key）进行匹配

### 方案5：自定义变量传递

确保processTask定义中的`caseInstanceId`变量被正确传递到Process实例。

## 下一步调试步骤

1. **等待后端重新编译**后，使用新的调试日志重新测试API
2. **检查日志输出**，查看：
   - PlanItem创建时间和Process实例创建时间的对比
   - 所有Process实例的列表
   - 各种查询方法的结果
3. **根据日志结果**选择最合适的关联方法
4. **更新查询逻辑**以使用确定有效的关联方法

## 可视化组件使用指南

一旦后端API正常工作，前端集成如下：

```typescript
// 1. 在CmmnCaseVisualizer中检测processTask节点
if (planItem.type === 'processtask') {
  // 显示可展开的按钮
  return (
    <button onClick={() => handleExpandSubprocess(planItem.id)}>
      展开子流程
    </button>
  );
}

// 2. 点击后调用API获取子流程可视化数据
const subprocessData = await adminApi.getSubprocessVisualization(planItemId);

// 3. 在BpmnSubprocessVisualizer中渲染
<BpmnSubprocessVisualizer data={subprocessData} />
```

## 总结

当前实现已经接近完成，主要的障碍在于：

1. **Process实例关联** - 需要找到可靠的方法从PlanItem关联到Process实例
2. **时间匹配精度** - 需要优化时间匹配算法以提高准确性

一旦解决了Process实例关联问题，整个功能就能正常工作，用户将能够在CMMN可视化界面中点击processTask节点，展开查看其对应的BPMN子流程的详细执行状态。
