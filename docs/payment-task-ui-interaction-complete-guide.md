# 支付子流程UI交互完整指南

## 概述

本指南详细说明了支付子流程的UI交互设计，以及如何让流程能够顺利完成。

## 流程架构

### CMMN案例 (ClaimCase.cmmn)

主案例流程包含以下任务：
1. **taskSubmitClaim** - 提交索赔
2. **taskReviewClaim** - 审核索赔
3. **taskApproveClaim** - 批准索赔
4. **taskRejectClaim** - 拒绝索赔
5. **Process_PaymentSubprocess** - 支付子流程 (BPMN流程)

### BPMN支付子流程 (ClaimPaymentProcess.bpmn)

支付子流程包含以下用户任务：
1. **userTask_validatePayment** - 支付校验
2. **userTask_confirmPayment** - 支付确认
3. **userTask_handleDispute** - 争议处理

流程流转逻辑：
```
支付校验 (userTask_validatePayment)
  ↓
支付确认 (userTask_confirmPayment)
  ↓
结束 (事件)
```

如果支付被拒绝，流程将不会进入支付确认环节。

## 后端实现

### TaskResource.java 关键修复

#### 1. 任务类型识别

```java
// Get task info to check task type
Task task = cmmnTaskService.createTaskQuery().taskId(taskId).singleResult();
if (task != null) {
    String taskKey = task.getTaskDefinitionKey();
    log.debug("Task definition key: {}, processInstanceId: {}, scopeId: {}", 
        taskKey, task.getProcessInstanceId(), task.getScopeId());
}
```

#### 2. BPMN/CMMN任务完成逻辑

**问题：** Flowable区分CMMN任务和BPMN任务，它们需要使用不同的服务：
- CMMN任务 → `cmmnTaskService.complete()`
- BPMN任务 → `taskService.complete()`

**解决方案：** 根据任务的`processInstanceId`判断任务来源

```java
// Determine if task is from BPMN or CMMN engine
// BPMN tasks have processInstanceId, CMMN tasks have scopeId
boolean isBpmnTask = task != null && task.getProcessInstanceId() != null;

if (isBpmnTask) {
    log.debug("Task is from BPMN process, using taskService.complete()");
    taskService.complete(taskId, variables);
} else {
    log.debug("Task is from CMMN case, using cmmnTaskService.complete()");
    cmmnTaskService.complete(taskId, variables);
}
```

#### 3. 支付任务特殊处理

```java
// 支付校验任务
if ("userTask_validatePayment".equals(taskKey)) {
    log.debug("Completing payment validation task with variables: {}", variables);
    Object validationResult = variables.get("validationResult");
    if (validationResult != null) {
        variables.put("validationResult", validationResult.toString());
        log.debug("Set validationResult to: {}", variables.get("validationResult"));
    }
}

// 支付确认任务
if ("userTask_confirmPayment".equals(taskKey)) {
    log.debug("Completing payment confirmation task with variables: {}", variables);
    Object confirmationResult = variables.get("confirmationResult");
    if (confirmationResult != null) {
        variables.put("confirmationResult", confirmationResult.toString());
        log.debug("Set confirmationResult to: {}", variables.get("confirmationResult"));
    }
}

// 争议处理任务
if ("userTask_handleDispute".equals(taskKey)) {
    log.debug("Completing dispute handling task with variables: {}", variables);
    Object disputeResolution = variables.get("disputeResolution");
    if (disputeResolution != null) {
        variables.put("disputeResolution", disputeResolution.toString());
        log.debug("Set disputeResolution to: {}", variables.get("disputeResolution"));
    }
}
```

## 前端实现

### TaskList.tsx 组件

#### 1. 任务变量获取

```typescript
// 获取任务变量
const fetchTaskVariables = async (taskId: string) => {
  try {
    const response = await fetch(`http://localhost:8080/api/tasks/${taskId}/variables`);
    if (response.ok) {
      const variables = await response.json();
      setTaskVariables(variables);
      console.log('Task variables:', variables);
    }
  } catch (error) {
    console.error('Failed to fetch task variables:', error);
  }
};
```

#### 2. 支付任务UI渲染

```typescript
const renderPaymentTaskActions = (task: TaskDTO) => {
  const taskKey = task.taskDefinitionKey;

  if (taskKey === 'userTask_validatePayment') {
    return (
      <div className="payment-validation-actions">
        <h4>支付校验</h4>
        <p>请审核支付信息的准确性</p>
        <button 
          className="btn btn-success" 
          onClick={() => completePaymentTask(task.id, { validationResult: 'approved' })}
        >
          批准支付
        </button>
        <button 
          className="btn btn-danger" 
          onClick={() => completePaymentTask(task.id, { validationResult: 'rejected' })}
        >
          拒绝支付
        </button>
      </div>
    );
  }

  if (taskKey === 'userTask_confirmPayment') {
    return (
      <div className="payment-confirmation-actions">
        <h4>支付确认</h4>
        <p>请确认支付执行结果</p>
        <button 
          className="btn btn-success" 
          onClick={() => completePaymentTask(task.id, { confirmationResult: 'confirmed' })}
        >
          支付成功
        </button>
        <button 
          className="btn btn-warning" 
          onClick={() => completePaymentTask(task.id, { confirmationResult: 'failed' })}
        >
          支付失败
        </button>
      </div>
    );
  }

  if (taskKey === 'userTask_handleDispute') {
    return (
      <div className="dispute-handling-actions">
        <h4>争议处理</h4>
        <p>请处理用户的支付争议</p>
        <button 
          className="btn btn-success" 
          onClick={() => completePaymentTask(task.id, { disputeResolution: 'resolved' })}
        >
          已解决
        </button>
        <button 
          className="btn btn-danger" 
          onClick={() => completePaymentTask(task.id, { disputeResolution: 'escalated' })}
        >
          升级处理
        </button>
      </div>
    );
  }

  return null;
};
```

#### 3. 任务完成函数

```typescript
const completePaymentTask = async (taskId: string, variables: Record<string, string>) => {
  try {
    const token = localStorage.getItem('token');
    const response = await fetch(`http://localhost:8080/api/tasks/${taskId}/complete`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
      },
      body: JSON.stringify(variables),
    });

    if (response.ok) {
      console.log(`Task ${taskId} completed with variables:`, variables);
      fetchTasks(); // 刷新任务列表
    } else {
      console.error('Failed to complete task:', await response.text());
    }
  } catch (error) {
    console.error('Error completing task:', error);
  }
};
```

## 用户操作流程

### 场景：完成支付子流程

1. **财务人员登录**
   - 使用财务人员账户登录系统
   - 进入"我的任务"页面

2. **支付校验任务**
   - 看到"支付校验"任务
   - 点击任务查看详情
   - 选择操作：
     - "批准支付" → 流程继续到支付确认
     - "拒绝支付" → 流程终止（不进入支付确认）

3. **支付确认任务**
   - 支付校验批准后，自动出现"支付确认"任务
   - 执行实际支付操作
   - 选择操作：
     - "支付成功" → 流程完成，子流程结束
     - "支付失败" → 可能触发争议处理（根据流程设计）

4. **争议处理任务**（可选）
   - 如果支付失败或用户提出争议
   - 处理争议
   - 选择操作：
     - "已解决" → 流程完成
     - "升级处理" → 转交给上级处理

## API端点

### 获取任务变量

```
GET /api/tasks/{taskId}/variables
```

**响应示例：**
```json
{
  "validationResult": null,
  "confirmationResult": null,
  "paymentAmount": 5000.00,
  "accountNumber": "****1234"
}
```

### 完成任务

```
POST /api/tasks/{taskId}/complete
Content-Type: application/json

{
  "validationResult": "approved"
}
```

**支持的变量：**

| 任务类型 | 变量名 | 可选值 |
|---------|--------|--------|
| 支付校验 | validationResult | approved, rejected |
| 支付确认 | confirmationResult | confirmed, failed |
| 争议处理 | disputeResolution | resolved, escalated |

## 调试技巧

### 1. 查看任务详情

在浏览器控制台中查看任务详情：
```javascript
console.log('Task:', task);
console.log('Task variables:', taskVariables);
```

### 2. 查看后端日志

后端日志会显示：
- 任务完成时的变量值
- 任务类型判断结果
- 使用的服务（taskService 或 cmmnTaskService）

### 3. 常见问题

**问题：** "Task is created by the process engine and should be completed via the process engine API"

**原因：** 使用了错误的任务服务完成BPMN任务

**解决：** 后端已修复，会自动判断任务类型并使用正确的服务

**问题：** 任务完成后流程没有继续

**原因：** 变量值格式不正确

**解决：** 后端已修复，会自动将枚举值转换为字符串

## 测试步骤

1. 启动后端服务（端口8080）
2. 启动前端服务（端口3000）
3. 登录系统（使用管理员或财务人员账户）
4. 创建一个测试理赔案例
5. 批准理赔（触发支付子流程）
6. 完成支付校验任务
7. 完成支付确认任务
8. 验证流程完成

## 总结

通过以下改进，支付子流程现在可以顺利完成：

1. ✅ 后端添加了任务变量获取API
2. ✅ 前端实现了支付任务的操作界面
3. ✅ 后端修复了BPMN/CMMN任务完成逻辑
4. ✅ 后端添加了支付任务的特殊处理
5. ✅ 前端提供了清晰的用户操作流程

现在支付子流程的每个节点都有对应的UI操作，用户可以顺利完成整个支付流程。
