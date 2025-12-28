# 支付子流程UI交互实现总结

## 一、实现概述

已完成支付子流程的UI交互实现，使支付流程能够顺利从"支付校验"任务继续执行。

## 二、实现内容

### 2.1 后端修改

#### TaskResource.java

添加了获取任务变量的API端点：

```java
@GetMapping("/{taskId}/variables")
@Operation(summary = "获取任务变量", description = "获取指定任务的变量信息")
public ResponseEntity<Map<String, Object>> getTaskVariables(
        @PathVariable String taskId) {
    // 实现细节...
}
```

这个API允许前端获取任务的变量信息，用于在对话框中显示支付详情。

### 2.2 前端修改

#### api.ts

在taskApi中添加了获取任务变量的方法：

```typescript
getTaskVariables: (taskId: string): Promise<AxiosResponse<Record<string, any>>> => {
  return api.get(`/tasks/${taskId}/variables`);
}
```

#### TaskList.tsx

增强了TaskList组件，添加了支付任务的处理功能：

1. **新增状态管理**：
   - `validationModalVisible` - 支付校验对话框
   - `confirmModalVisible` - 支付确认对话框
   - `rejectedModalVisible` - 支付被拒绝对话框
   - `disputeModalVisible` - 支付争议处理对话框
   - `taskVariables` - 任务变量
   - `loadingVariables` - 变量加载状态
   - `selectedTaskForProcessing` - 当前处理的任务

2. **新增函数**：
   - `loadTaskVariables()` - 加载任务变量
   - `handleValidatePayment()` - 处理支付校验
   - `handleConfirmPayment()` - 处理支付确认
   - `handleRejectedPayment()` - 处理支付拒绝
   - `handleDisputePayment()` - 处理支付争议

3. **修改handleCompleteTask()**：
   - 根据任务的`taskDefinitionKey`识别支付任务类型
   - 显示对应的处理对话框
   - 对于普通任务，直接完成

4. **新增对话框**：
   - 支付校验对话框 - 显示支付信息，选择批准/拒绝
   - 支付确认对话框 - 显示交易详情，选择确认/争议
   - 支付被拒绝对话框 - 输入拒绝原因
   - 支付争议处理对话框 - 选择解决方案

## 三、使用流程

### 3.1 支付校验任务

1. 用户在任务列表中看到"支付校验"任务
2. 点击"处理"按钮（支付任务显示"处理"而非"完成"）
3. 弹出支付校验对话框，显示：
   - 支付金额（只读）
   - 支付参考号（只读）
   - 收款人姓名（只读）
4. 用户选择校验结果：
   - **批准** - 流程继续到"执行支付"
   - **拒绝** - 流程继续到"支付被拒绝"
5. 可选填写校验备注
6. 点击"提交"
7. 系统传递`validationResult`和`validationComment`变量
8. 流程根据校验结果继续

### 3.2 支付确认任务

1. 用户在任务列表中看到"支付确认"任务
2. 点击"处理"按钮
3. 弹出支付确认对话框，显示：
   - 交易ID
   - 支付金额
   - 收款人姓名
   - 支付日期
4. 用户选择确认结果：
   - **确认** - 流程继续到更新Case状态和发送通知
   - **争议** - 流程继续到"处理支付争议"
5. 可选填写确认备注
6. 点击"提交"
7. 系统传递`confirmationResult`和`confirmationComment`变量

### 3.3 支付被拒绝任务

1. 用户在任务列表中看到"支付被拒绝"任务
2. 点击"处理"按钮
3. 弹出支付被拒绝对话框
4. 用户输入拒绝原因（必填）
5. 可选填写拒绝备注
6. 点击"提交"
7. 系统传递`rejectionReason`和`rejectionComment`变量
8. 流程继续到更新Case状态和发送通知，最终以"支付失败"结束

### 3.4 支付争议处理任务

1. 用户在任务列表中看到"处理支付争议"任务
2. 点击"处理"按钮
3. 弹出支付争议处理对话框
4. 用户选择争议解决方案：
   - **重新支付** - 流程回退到执行支付
   - **取消支付** - 流程继续到更新Case状态
   - **调查** - 流程继续到更新Case状态
5. 可选填写争议处理备注
6. 点击"提交"
7. 系统传递`disputeResolution`和`disputeComment`变量

## 四、任务类型识别

系统通过`taskDefinitionKey`识别支付任务类型：

| taskDefinitionKey | 任务名称 | 对应对话框 |
|-----------------|---------|-----------|
| `userTask_validatePayment` | 支付校验 | 支付校验对话框 |
| `userTask_confirmPayment` | 支付确认 | 支付确认对话框 |
| `userTask_paymentRejected` | 支付被拒绝 | 支付被拒绝对话框 |
| `userTask_handleDispute` | 处理支付争议 | 支付争议处理对话框 |

## 五、变量映射

### 5.1 支付校验任务

| 变量名 | 类型 | 说明 |
|-------|------|------|
| `amount` | long | 支付金额（只读） |
| `reference` | string | 支付参考号（只读） |
| `payeeName` | string | 收款人姓名（只读） |
| `validationResult` | enum | 校验结果（approved/rejected）|
| `validationComment` | string | 校验备注 |

### 5.2 支付确认任务

| 变量名 | 类型 | 说明 |
|-------|------|------|
| `transactionId` | string | 交易ID（只读） |
| `amount` | long | 支付金额（只读） |
| `payeeName` | string | 收款人姓名（只读） |
| `paymentDate` | date | 支付日期（只读） |
| `confirmationResult` | enum | 确认结果（confirmed/disputed）|
| `confirmationComment` | string | 确认备注 |

### 5.3 支付被拒绝任务

| 变量名 | 类型 | 说明 |
|-------|------|------|
| `rejectionReason` | string | 拒绝原因（必填） |
| `rejectionComment` | string | 拒绝备注 |

### 5.4 支付争议处理任务

| 变量名 | 类型 | 说明 |
|-------|------|------|
| `disputeResolution` | enum | 争议解决方案（retry/cancel/investigate）|
| `disputeComment` | string | 争议处理备注 |

## 六、注意事项

### 6.1 任务分配

支付任务的`assignee`在BPMN中设置为`${paymentOfficer}`变量。需要确保：

1. 在启动支付子流程时，设置`paymentOfficer`变量
2. 或者通过任务分配功能将任务分配给具体用户

### 6.2 变量传递

所有传递给`completeTask`接口的变量必须：

- 与BPMN中定义的表单属性一致
- 枚举类型变量的value必须是定义的id（如"approved"、"rejected"）

### 6.3 错误处理

- API调用失败时会显示错误提示
- 变量加载失败时对话框会显示错误信息

### 6.4 数据刷新

- 任务完成后会自动刷新任务列表
- 任务列表会同时刷新"我的待办"和"可认领任务"

## 七、测试建议

### 7.1 测试场景

1. **正常流程**：
   - 支付校验（批准）
   - 支付确认（确认）
   - 验证流程正常结束

2. **拒绝流程**：
   - 支付校验（拒绝）
   - 填写拒绝原因
   - 验证流程以"支付失败"结束

3. **争议流程**：
   - 支付校验（批准）
   - 支付确认（争议）
   - 选择争议解决方案
   - 验证流程继续

4. **边界情况**：
   - 不填写必填字段
   - 变量加载失败
   - API调用失败

### 7.2 测试步骤

1. 创建一个已批准的理赔案例
2. 触发支付子流程
3. 在任务列表中找到"支付校验"任务
4. 测试批准和拒绝两种情况
5. 测试支付确认任务
6. 测试支付被拒绝任务
7. 测试支付争议处理任务
8. 验证流程变量正确传递
9. 验证流程状态正确更新

## 八、UI设计

### 8.1 支付校验对话框

```
┌─────────────────────────────────────┐
│  💰 支付校验                         │
├─────────────────────────────────────┤
│  支付金额: ¥10,000.00              │
│  支付参考号: PAY-20241226-001      │
│  收款人姓名: 张三                    │
│                                      │
│  校验结果: ○ 批准  ○ 拒绝 *         │
│                                      │
│  校验备注:                            │
│  ┌────────────────────────────────┐  │
│  │                                │  │
│  └────────────────────────────────┘  │
│                                      │
│              [取消]  [提交]            │
└─────────────────────────────────────┘
```

### 8.2 支付确认对话框

```
┌─────────────────────────────────────┐
│  💰 支付确认                         │
├─────────────────────────────────────┤
│  交易ID: TXN-20241226-001         │
│  支付金额: ¥10,000.00              │
│  收款人姓名: 张三                    │
│  支付日期: 2024-12-26              │
│                                      │
│  确认结果: ○ 确认  ○ 争议 *         │
│                                      │
│  确认备注:                            │
│  ┌────────────────────────────────┐  │
│  │                                │  │
│  └────────────────────────────────┘  │
│                                      │
│              [取消]  [提交]            │
└─────────────────────────────────────┘
```

## 九、后续优化建议

1. **任务预览**：在任务列表中直接显示任务关键信息
2. **批量处理**：支持批量处理相同类型的任务
3. **任务过滤**：添加按任务类型筛选功能
4. **流程可视化**：显示当前任务在流程图中的位置
5. **自动填充**：根据任务类型自动填充一些默认值
6. **历史记录**：显示支付任务的处理历史
7. **通知提醒**：支付任务创建或变更时发送通知

## 十、相关文件

- `docs/payment-subprocess-ui-interaction-design.md` - UI交互设计方案
- `backend/src/main/java/com/flowable/demo/web/rest/TaskResource.java` - 后端任务API
- `frontend/src/services/api.ts` - 前端API服务
- `frontend/src/components/TaskList.tsx` - 任务列表组件
- `backend/src/main/resources/processes/ClaimPaymentProcess.bpmn` - 支付流程定义
