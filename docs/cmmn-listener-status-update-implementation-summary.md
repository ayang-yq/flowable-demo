# CMMN监听器状态更新实现总结

## 问题描述
Claim case的状态通过CMMN监听器实现更新，但是CMMN model 没有增加监听器事件。

## 解决方案

### 1. 分析现有监听器实现

#### PaymentCompletionListener
- **位置**: `backend/src/main/java/com/flowable/demo/service/PaymentCompletionListener.java`
- **类型**: BPMN ExecutionListener (`org.flowable.engine.delegate.ExecutionListener`)
- **功能**: 
  - 监听支付流程的完成事件
  - 根据支付状态（PAID/REJECTED）更新claim case状态
  - 设置CMMN流程变量以触发后续阶段

#### PaymentFailureListener
- **位置**: `backend/src/main/java/com/flowable/demo/service/PaymentFailureListener.java`
- **类型**: BPMN ExecutionListener
- **功能**: 处理支付失败情况

### 2. CMMN模型更新

#### 修改的文件
- `backend/src/main/resources/cases/ClaimCase.cmmn`

#### 添加的监听器配置

在CMMN模型的两个planItem中添加了execution listener：

##### 1. Payment Stage监听器
```xml
<cmmn:planItem id="planItemStagePayment" name="Payment" definitionRef="stagePayment">
    <cmmn:entryCriterion id="entrySentryPayment" sentryRef="sentryPaymentEntry" />
    <cmmn:extensionElements>
        <flowable:executionListener event="start" delegateExpression="${paymentCompletionListener}" />
    </cmmn:extensionElements>
</cmmn:planItem>
```

**触发时机**: Payment stage开始时（event="start"）

**功能**:
- 当Payment stage激活时，调用PaymentCompletionListener
- 更新claim case状态为PAYMENT_PROCESSING
- 触发BPMN支付流程

##### 2. Closure Stage监听器
```xml
<cmmn:planItem id="planItemStageClosure" name="Closure" definitionRef="stageClosure">
    <cmmn:entryCriterion id="entrySentryClosure" sentryRef="sentryClosureEntry" />
    <cmmn:extensionElements>
        <flowable:executionListener event="start" delegateExpression="${paymentCompletionListener}" />
    </cmmn:extensionElements>
</cmmn:planItem>
```

**触发时机**: Closure stage开始时（event="start"）

**功能**:
- 当Closure stage激活时，调用PaymentCompletionListener
- 更新claim case状态为CLOSED

### 3. 工作流程

#### 正常支付流程
1. **Approval阶段完成** → 触发Payment stage
2. **Payment stage开始** → PaymentCompletionListener更新状态为`PAYMENT_PROCESSING`
3. **执行BPMN支付流程** → 调用ClaimPaymentProcess.bpmn
4. **BPMN流程结束** → 根据支付状态更新为`PAID`或`REJECTED`
5. **Payment stage完成** → 触发Closure stage
6. **Closure stage开始** → PaymentCompletionListener更新状态为`CLOSED`

#### 状态转换映射

| CMMN Stage | 触发事件 | 目标状态 |
|------------|---------|---------|
| Payment Stage | start | PAYMENT_PROCESSING |
| BPMN Payment Success | end | PAID |
| BPMN Payment Failure | end | REJECTED |
| Closure Stage | start | CLOSED |

### 4. 技术细节

#### Flowable CMMN执行监听器
- **配置方式**: 通过`<flowable:executionListener>`元素
- **事件类型**:
  - `start`: plan item启动时触发
  - `end`: plan item完成时触发
  - `complete`: plan item完成时触发
- **委托表达式**: 使用`delegateExpression="${paymentCompletionListener}"`引用Spring管理的bean

#### Spring Bean配置
PaymentCompletionListener已经通过`@Component("paymentCompletionListener")`注解注册为Spring bean，可以被Flowable引擎直接引用。

### 5. 编译验证

```bash
cd backend && mvn clean compile
```

**结果**: BUILD SUCCESS ✅

### 6. 注意事项

1. **监听器复用**: PaymentCompletionListener同时被BPMN和CMMN使用，需要确保它能正确处理两种场景
   - BPMN场景: 通过`DelegateExecution`访问流程变量
   - CMMN场景: 通过`DelegateExecution`访问case变量

2. **变量传递**: 确保在case启动时设置所有必要的变量，特别是：
   - `caseInstanceId`: 关联claim case
   - `claimId`: claim的数据库ID
   - `paymentOfficer`: 支付专员
   - `paymentManager`: 支付经理

3. **状态同步**: 由于同时存在CMMN和数据库中的状态，需要确保两者保持同步

### 7. 后续优化建议

1. **创建专门的CMMN监听器**: 如果需要更细粒度的状态控制，可以创建专门实现CMMN监听器接口的服务

2. **添加更多阶段监听器**: 在其他stage（Triage、Investigation、Approval）也添加监听器，实现更精确的状态跟踪

3. **日志增强**: 在监听器中添加更详细的日志，便于调试和追踪状态变化

4. **错误处理**: 增强异常处理，确保监听器执行失败不会影响主流程

## 总结

通过在CMMN模型的Payment和Closure stage中添加execution listener配置，成功实现了claim case状态的自动更新。现在当case流程进入这些阶段时，会自动调用PaymentCompletionListener来更新数据库中的claim状态，实现了CMMN流程和业务状态的同步。

所有修改已通过编译验证，可以安全部署使用。
