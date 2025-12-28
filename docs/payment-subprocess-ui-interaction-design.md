# 支付子流程UI交互设计方案

## 一、当前状态分析

### 1.1 BPMN支付流程结构

根据`ClaimPaymentProcess.bpmn`文件分析，支付流程包含以下节点：

```
开始事件(支付开始)
  ↓
用户任务[支付校验] ← 当前卡在这里
  ↓
网关[校验结果]
  ├─ 批准 → 服务任务[执行支付] → 用户任务[支付确认]
  │                                         ↓
  └─ 拒绝 → 用户任务[支付被拒绝]     网关[确认结果]
                                               ├─ 确认 → 服务任务[更新Case状态] → 服务任务[发送通知] → 结束事件[支付成功]
                                               │
                                               └─ 争议 → 用户任务[处理支付争议]
```

### 1.2 支付校验任务的表单属性

根据BPMN定义，"支付校验"任务需要以下变量：

```xml
<flowable:formProperty id="amount" name="支付金额" type="long" readonly="true" />
<flowable:formProperty id="reference" name="支付参考号" type="string" readonly="true" />
<flowable:formProperty id="payeeName" name="收款人姓名" type="string" readonly="true" />
<flowable:formProperty id="validationResult" name="校验结果" type="enum" required="true">
  <flowable:value id="approved" name="批准" />
  <flowable:value id="rejected" name="拒绝" />
</flowable:formProperty>
<flowable:formProperty id="validationComment" name="校验备注" type="string" />
```

### 1.3 当前问题

1. **前端TaskList组件**：
   - `handleCompleteTask`函数直接调用`taskApi.completeTask(taskId)`
   - 没有传递任何variables参数
   - 缺少对支付校验任务的特殊处理

2. **后端API**：
   - `TaskResource.completeTask`方法支持传递variables参数
   - 但没有专门处理支付校验任务的逻辑

## 二、UI交互设计方案

### 2.1 支付校验任务交互流程

```
用户在任务列表看到"支付校验"任务
  ↓
点击"处理"按钮
  ↓
弹出"支付校验"对话框，显示：
  - 支付金额（只读）
  - 支付参考号（只读）
  - 收款人姓名（只读）
  - 校验结果选择（批准/拒绝）- 必填
  - 校验备注（可选）
  ↓
用户填写并提交
  ↓
系统调用completeTask接口，传递validationResult和validationComment变量
  ↓
流程根据校验结果继续：
  - 批准 → 执行支付
  - 拒绝 → 进入支付被拒绝任务
```

### 2.2 支付确认任务交互流程

```
用户在任务列表看到"支付确认"任务
  ↓
点击"处理"按钮
  ↓
弹出"支付确认"对话框，显示：
  - 交易ID（只读）
  - 支付金额（只读）
  - 收款人姓名（只读）
  - 支付日期（只读）
  - 确认结果选择（确认/争议）- 必填
  - 确认备注（可选）
  ↓
用户填写并提交
  ↓
系统调用completeTask接口，传递confirmationResult和confirmationComment变量
  ↓
流程根据确认结果继续：
  - 确认 → 更新Case状态 → 发送通知 → 支付成功
  - 争议 → 进入处理支付争议任务
```

### 2.3 支付被拒绝任务交互流程

```
用户在任务列表看到"支付被拒绝"任务
  ↓
点击"处理"按钮
  ↓
弹出"支付被拒绝"对话框，显示：
  - 拒绝原因（必填）
  - 拒绝备注（可选）
  ↓
用户填写并提交
  ↓
系统调用completeTask接口，传递rejectionReason和rejectionComment变量
  ↓
流程继续 → 更新Case状态 → 发送通知 → 支付失败
```

## 三、实现方案

### 3.1 前端实现

#### 3.1.1 在TaskList组件中添加支付任务处理对话框

需要添加以下功能：

1. **识别支付相关任务**：
   - 通过`taskDefinitionKey`识别支付任务
   - `userTask_validatePayment` - 支付校验
   - `userTask_confirmPayment` - 支付确认
   - `userTask_paymentRejected` - 支付被拒绝
   - `userTask_handleDispute` - 处理支付争议

2. **添加任务处理对话框**：
   - 支付校验对话框
   - 支付确认对话框
   - 支付被拒绝对话框
   - 支付争议处理对话框

3. **修改handleCompleteTask函数**：
   - 检测任务类型
   - 显示对应的对话框
   - 收集用户输入
   - 调用completeTask API并传递正确的variables

#### 3.1.2 获取任务变量

需要在API中添加获取任务变量的接口：

```typescript
// api.ts
getTaskVariables: (taskId: string): Promise<AxiosResponse<Record<string, any>>> => {
  return api.get(`/tasks/${taskId}/variables`);
}
```

#### 3.1.3 完成任务时传递变量

```typescript
completeTask: (taskId: string, variables: any): Promise<AxiosResponse<void>> => {
  return api.post(`/tasks/${taskId}/complete`, variables);
}
```

### 3.2 后端实现（已有支持）

后端`TaskResource.completeTask`方法已经支持传递variables参数：

```java
@PostMapping("/{taskId}/complete")
public ResponseEntity<Void> completeTask(
    @PathVariable String taskId,
    @RequestBody(required = false) Map<String, Object> variables) {
    // ...
}
```

### 3.3 需要添加的后端API

获取任务变量的API：

```java
@GetMapping("/{taskId}/variables")
@Operation(summary = "获取任务变量", description = "获取指定任务的变量信息")
public ResponseEntity<Map<String, Object>> getTaskVariables(
    @Parameter(description = "任务ID") @PathVariable String taskId) {
    log.debug("REST request to get task variables: {}", taskId);
    
    Map<String, Object> variables = taskService.getVariables(taskId);
    return ResponseEntity.ok(variables);
}
```

## 四、具体实现步骤

### 步骤1：添加后端获取任务变量API

在`TaskResource.java`中添加`getTaskVariables`方法。

### 步骤2：扩展前端API服务

在`frontend/src/services/api.ts`中添加`getTaskVariables`方法。

### 步骤3：修改TaskList组件

1. 添加状态管理：
   - `validationModalVisible` - 支付校验对话框可见性
   - `confirmModalVisible` - 支付确认对话框可见性
   - `rejectedModalVisible` - 支付被拒绝对话框可见性
   - `disputeModalVisible` - 支付争议对话框可见性
   - `taskVariables` - 任务变量
   - `selectedTaskForProcessing` - 当前处理的任务

2. 添加加载任务变量的函数

3. 添加支付任务处理对话框组件

4. 修改handleCompleteTask函数，根据任务类型显示不同对话框

### 步骤4：测试流程

1. 创建一个已批准的理赔案例
2. 触发支付子流程
3. 在任务列表中找到"支付校验"任务
4. 测试批准和拒绝两种情况
5. 测试支付确认任务
6. 测试支付被拒绝任务
7. 测试支付争议处理任务

## 五、界面设计

### 5.1 支付校验对话框

```
┌─────────────────────────────────────┐
│  支付校验                              │
├─────────────────────────────────────┤
│  支付金额: ¥10,000.00 (只读)          │
│  支付参考号: PAY-20241226-001 (只读)  │
│  收款人姓名: 张三 (只读)                │
│                                      │
│  校验结果: [○ 批准  ○ 拒绝] *必填      │
│                                      │
│  校验备注:                             │
│  ┌────────────────────────────────┐  │
│  │                                │  │
│  └────────────────────────────────┘  │
│                                      │
│              [取消]  [确定]            │
└─────────────────────────────────────┘
```

### 5.2 支付确认对话框

```
┌─────────────────────────────────────┐
│  支付确认                              │
├─────────────────────────────────────┤
│  交易ID: TXN-20241226-001 (只读)      │
│  支付金额: ¥10,000.00 (只读)          │
│  收款人姓名: 张三 (只读)                │
│  支付日期: 2024-12-26 (只读)          │
│                                      │
│  确认结果: [○ 确认  ○ 争议] *必填      │
│                                      │
│  确认备注:                             │
│  ┌────────────────────────────────┐  │
│  │                                │  │
│  └────────────────────────────────┘  │
│                                      │
│              [取消]  [确定]            │
└─────────────────────────────────────┘
```

### 5.3 支付被拒绝对话框

```
┌─────────────────────────────────────┐
│  支付被拒绝                            │
├─────────────────────────────────────┤
│  拒绝原因: *必填                       │
│  ┌────────────────────────────────┐  │
│  │                                │  │
│  └────────────────────────────────┘  │
│                                      │
│  拒绝备注:                             │
│  ┌────────────────────────────────┐  │
│  │                                │  │
│  └────────────────────────────────┘  │
│                                      │
│              [取消]  [确定]            │
└─────────────────────────────────────┘
```

## 六、注意事项

1. **任务分配问题**：
   - 支付任务的`assignee`设置为`${paymentOfficer}`变量
   - 需要确保在启动支付子流程时设置该变量

2. **变量传递**：
   - 所有variables必须是BPMN定义的表单属性
   - 变量名必须与BPMN中的formProperty id一致
   - 枚举类型变量的value必须是定义的id（如"approved"、"rejected"）

3. **错误处理**：
   - 需要处理API调用失败的情况
   - 提供清晰的错误提示

4. **数据刷新**：
   - 任务完成后需要刷新任务列表
   - 如果任务与Case相关，需要刷新Case详情

## 七、后续优化

1. **任务预览**：
   - 在任务列表中直接显示任务关键信息
   - 避免用户需要点击才能查看

2. **批量处理**：
   - 支持批量处理相同类型的任务

3. **任务过滤**：
   - 添加按任务类型筛选功能
   - 添加按优先级、日期等筛选功能

4. **流程可视化**：
   - 显示当前任务在流程图中的位置
   - 高亮显示已完成和待完成的节点
