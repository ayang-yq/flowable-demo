# 支付流程完成指南

## 问题概述

当前BPMN支付子流程已创建，但流程停在"支付校验"任务。本指南将说明如何通过UI交互完成整个支付流程。

## 已完成的修复

### 1. 修复的类型转换错误

**问题**：`PaymentService` 在处理 `amount` 变量时出现 `ClassCastException`
- 原因：Flowable 返回的 `amount` 是 `BigDecimal` 类型，但代码直接转换为 `Long`
- 解决方案：使用类型安全的转换方式，检查 `Number` 类型并调用 `longValue()`

**修复代码**：
```java
Object amountObj = execution.getVariable("amount");
Long amount = null;
if (amountObj instanceof Number) {
    amount = ((Number) amountObj).longValue();
}
```

### 2. 创建的Service类

| Service类 | 用途 | Bean名称 |
|-----------|------|---------|
| PaymentService | 执行支付操作 | paymentService |
| PaymentUpdateService | 更新Case状态 | paymentUpdateService |
| NotificationService | 发送支付通知 | notificationService |

### 3. 创建的监听器

| 监听器类 | 用途 | Bean名称 |
|---------|------|---------|
| PaymentCompletionListener | 支付成功后的处理 | paymentCompletionListener |
| PaymentFailureListener | 支付失败后的处理 | paymentFailureListener |

## 如何完成支付流程

### 步骤1：登录支付专员账号

```
用户名：payment_officer
密码：payment123
```

### 步骤2：找到支付校验任务

1. 登录后，导航到"我的任务"页面
2. 查找任务名称为"支付校验"的任务
3. 点击任务查看详情

### 步骤3：执行支付校验

任务将显示以下表单字段：

| 字段名称 | 类型 | 只读 | 说明 |
|---------|------|------|------|
| 支付金额 (amount) | long | ✓ | 例如：50000 |
| 支付参考号 (reference) | string | ✓ | 例如：PAY-2025-001 |
| 收款人姓名 (payeeName) | string | ✓ | 例如：张三 |
| 校验结果 (validationResult) | enum | ✗ | 必须选择：批准或拒绝 |
| 校验备注 (validationComment) | string | ✗ | 可选 |

**操作**：
1. 在"校验结果"下拉框中选择"批准"或"拒绝"
2. （可选）在"校验备注"中填写说明
3. 点击"提交"按钮

### 步骤4：等待支付确认任务

如果选择"批准"，流程会：
1. 执行 `PaymentService`（生成交易ID）
2. 创建新的用户任务："支付确认"

支付专员需要再次登录并完成"支付确认"任务。

### 步骤5：执行支付确认

"支付确认"任务表单：

| 字段名称 | 类型 | 只读 | 说明 |
|---------|------|------|------|
| 交易ID (transactionId) | string | ✓ | 由系统生成，例如：TXN-XXXXXXXX |
| 支付金额 (amount) | long | ✓ | 支付金额 |
| 收款人姓名 (payeeName) | string | ✓ | 收款人姓名 |
| 支付日期 (paymentDate) | date | ✓ | 支付日期 |
| 确认结果 (confirmationResult) | enum | ✗ | 必须选择：确认或争议 |
| 确认备注 (confirmationComment) | string | ✗ | 可选 |

**操作**：
1. 查看支付信息（只读）
2. 在"确认结果"下拉框中选择"确认"或"争议"
3. （可选）填写确认备注
4. 点击"提交"按钮

### 步骤6：流程完成

如果选择"确认"，流程会：
1. 执行 `PaymentUpdateService` 更新Case状态
2. 执行 `NotificationService` 发送通知
3. 触发 `PaymentCompletionListener`
4. 流程结束于"支付成功"

## 流程图

```
支付校验任务
    ↓
选择结果：批准/拒绝
    ↓
├─ 拒绝 → 支付被拒绝任务 → 更新Case → 通知 → 支付失败
│
└─ 批准 → 执行支付（PaymentService）
              ↓
         生成交易ID
              ↓
         支付确认任务
              ↓
         选择结果：确认/争议
              ↓
         ├─ 争议 → 处理争议任务
         │           ↓
         │      选择解决方案
         │           ↓
         │    重新支付/取消/调查
         │
         └─ 确认 → 更新Case → 通知 → 支付成功
```

## 异常情况处理

### 场景1：支付被拒绝

如果在"支付校验"任务中选择"拒绝"：
1. 流程进入"支付被拒绝"任务
2. 必须填写"拒绝原因"（必填）
3. 流程更新Case状态为"支付失败"
4. 发送通知
5. 流程结束

### 场景2：支付争议

如果在"支付确认"任务中选择"争议"：
1. 流程进入"处理支付争议"任务
2. 需要切换到支付经理账号（`payment_manager`）
3. 选择解决方案：
   - **重新支付**：流程回到执行支付步骤
   - **取消支付**：取消支付，更新Case状态
   - **调查**：启动调查流程

## API使用说明

### 获取任务列表
```http
GET /api/tasks
Authorization: Basic {base64(payment_officer:payment123)}
```

### 获取任务详情（含变量）
```http
GET /api/tasks/{taskId}/variables
Authorization: Basic {base64(payment_officer:payment123)}
```

### 完成任务
```http
POST /api/tasks/{taskId}/complete
Authorization: Basic {base64(payment_officer:payment123)}
Content-Type: application/json

{
  "validationResult": "approved",
  "validationComment": "支付信息校验通过"
}
```

## 常见问题

### Q1: 为什么任务列表中看不到"支付校验"任务？

**可能原因**：
- 任务已被其他用户认领
- 当前登录用户不是支付专员
- 流程变量设置错误

**解决方案**：
- 确保使用 `payment_officer` 账号登录
- 检查流程实例状态
- 查看后端日志确认流程是否正确启动

### Q2: 提交任务时返回400错误

**可能原因**：
- 必填字段未填写
- 字段值类型不匹配
- 表单数据格式错误

**解决方案**：
- 检查必填字段是否已填写
- 确认字段值类型正确（字符串、数字、枚举）
- 查看浏览器控制台的网络请求详情

### Q3: 流程卡在"执行支付"服务任务

**可能原因**：
- `PaymentService` Bean未正确注入
- 流程变量类型错误
- 后端服务未重启

**解决方案**：
- 检查后端日志中的错误信息
- 确认已重启后端服务以加载最新代码
- 验证流程变量是否正确设置

## 测试步骤

### 完整测试流程

1. **创建理赔案例**
   ```bash
   # 使用普通用户账号登录
   # 创建一个理赔案例
   # 案例会自动推进到支付阶段
   ```

2. **支付校验**
   ```bash
   # 登录：payment_officer / payment123
   # 查找"支付校验"任务
   # 选择"批准"
   # 提交任务
   ```

3. **支付确认**
   ```bash
   # 查找"支付确认"任务
   # 查看交易ID和支付信息
   # 选择"确认"
   # 提交任务
   ```

4. **验证结果**
   ```bash
   # 检查Case状态是否为"已支付"
   # 检查流程是否结束于"支付成功"
   # 查看通知是否发送
   ```

## 技术细节

### BPMN流程定义
- **流程ID**: `ClaimPaymentProcess`
- **流程名称**: 理赔支付流程
- **可执行**: true

### 关键流程变量
| 变量名 | 类型 | 用途 |
|--------|------|------|
| amount | Long/BigDecimal | 支付金额 |
| reference | String | 支付参考号 |
| payeeName | String | 收款人姓名 |
| validationResult | String | 校验结果：approved/rejected |
| confirmationResult | String | 确认结果：confirmed/disputed |
| transactionId | String | 交易ID（由系统生成） |
| paymentDate | String | 支付日期 |
| paymentStatus | String | 支付状态：completed/failed |

### Service Task调用
- `serviceTask_executePayment` → `${paymentService}`
- `serviceTask_updateCase` → `${paymentUpdateService}`
- `serviceTask_sendNotification` → `${notificationService}`

### 条件表达式
```xml
<!-- 校验批准 -->
${validationResult == 'approved'}

<!-- 确认完成 -->
${confirmationResult == 'confirmed'}

<!-- 支付成功 -->
${paymentStatus == 'completed'}
```

## 后续改进建议

1. **增强支付验证**
   - 添加支付金额范围验证
   - 检查收款人信息是否完整
   - 验证参考号格式

2. **改进用户体验**
   - 添加支付进度可视化
   - 显示支付历史记录
   - 提供支付详情导出功能

3. **增加审计功能**
   - 记录所有支付操作
   - 添加操作日志查询
   - 支持支付数据导出

4. **增强错误处理**
   - 添加支付失败重试机制
   - 改进错误消息提示
   - 支持异常情况回滚

## 总结

要让当前停在"支付校验"任务的支付流程走完，需要：

1. ✅ 使用支付专员账号（`payment_officer`）登录
2. ✅ 在任务列表中找到"支付校验"任务
3. ✅ 查看支付详情（只读字段）
4. ✅ 选择"校验结果"（批准或拒绝）
5. ✅ 提交任务
6. ✅ 如果批准，完成"支付确认"任务
7. ✅ 流程自动完成剩余步骤

所有必需的后端服务和监听器已创建并部署，后端服务已启动并加载了最新的代码修复。
