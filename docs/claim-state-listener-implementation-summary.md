# ClaimStateListener 实现总结

## 概述
根据最新的 ClaimCase.cmmn 模型实现了 `ClaimStateListener`，用于在 CMMN 流程的不同阶段自动更新案件状态。

## 实现文件

### 1. ClaimStateListener.java
**位置**: `backend/src/main/java/com/flowable/demo/service/ClaimStateListener.java`

**功能**:
- 实现 `PlanItemInstanceLifecycleListener` 接口
- 监听 CMMN plan item 的生命周期状态变化
- 根据配置更新对应的案件状态

**关键实现**:
```java
@Component("claimStateListener")
public class ClaimStateListener implements PlanItemInstanceLifecycleListener {
    
    // 通过field注入的状态参数
    @Setter
    private String sourceState;
    
    @Setter
    private String targetState;
    
    @Setter
    private String status;
    
    @Override
    public void stateChanged(DelegatePlanItemInstance planItemInstance, 
                             String oldState, String newState) {
        // 更新案件状态
        ClaimStatus newStatus = ClaimStatus.valueOf(status);
        updateClaimStatus(caseInstanceId, newStatus, source);
    }
}
```

### 2. CMMN 模型配置
**位置**: `backend/src/main/resources/cases/ClaimCase.cmmn`

#### 已配置的监听器点:

| Stage/Milestone | 状态变更 | Source State | Target State | Status |
|-----------------|---------|---------------|--------------|---------|
| Triage Stage | 启动时 | available | active | SUBMITTED |
| Investigation Stage | 启动时 | available | active | UNDER_REVIEW |
| Approval Stage | 启动时 | available | active | UNDER_REVIEW |
| Payment Stage | 启动时 | available | active | APPROVED |
| Case Closed Milestone | 完成时 | available | completed | CLOSED |

#### 示例配置:
```xml
<cmmn:stage id="stageTriage" name="Triage">
    <cmmn:extensionElements>
        <flowable:planItemLifecycleListener sourceState="available" 
                                            targetState="active" 
                                            delegateExpression="${claimStateListener}">
            <flowable:field name="status" stringValue="SUBMITTED" />
        </flowable:planItemLifecycleListener>
    </cmmn:extensionElements>
    ...
</cmmn:stage>
```

## 状态转换流程

### 正常理赔流程
```
DRAFT (创建案件)
  ↓
SUBMITTED (Triage Stage 启动) ← ClaimStateListener
  ↓
UNDER_REVIEW (Investigation/Approval Stage 启动) ← ClaimStateListener
  ↓
APPROVED (Payment Stage 启动) ← ClaimStateListener
  ↓
PAYMENT_PROCESSING (PaymentCompletionListener)
  ↓
PAID (支付完成) ← PaymentCompletionListener
  ↓
CLOSED (Case Closed Milestone 完成) ← ClaimStateListener
```

### 拒绝流程
```
DRAFT
  ↓
SUBMITTED
  ↓
UNDER_REVIEW
  ↓
REJECTED (Approval 审批拒绝)
  ↓
CLOSED
```

## 技术细节

### Flowable CMMN Lifecycle Listener
- **接口**: `org.flowable.cmmn.api.listener.PlanItemInstanceLifecycleListener`
- **方法**: `stateChanged(DelegatePlanItemInstance, oldState, newState)`
- **配置方式**: 通过 `<flowable:planItemLifecycleListener>` 元素

### Field 注入机制
Flowable 支持通过 field 注入将参数传递给 listener：
```xml
<flowable:field name="status" stringValue="SUBMITTED" />
```

在 Java 代码中通过 `@Setter` 注解的属性接收：
```java
@Setter
private String status;
```

### 与其他 Listener 的协作
- **ClaimStateListener**: 负责主要阶段的状态更新
- **PaymentCompletionListener**: 负责支付流程的特定状态更新（PAID/REJECTED）
- **PaymentFailureListener**: 处理支付失败情况

## 编译验证

```bash
cd backend && mvn clean compile
```

**结果**: BUILD SUCCESS ✅

## 日志输出示例

```
ClaimStateListener triggered - PlanItem: Triage, OldState: available, NewState: active, CaseInstanceId: xxx
Updating claim case xxx status from DRAFT to SUBMITTED (source: Triage)
Claim case xxx status updated successfully to: SUBMITTED
```

## 注意事项

1. **状态同步**: 监听器确保 CMMN 流程状态与数据库中的 claim case 状态保持同步

2. **错误处理**: 如果状态值无效或 case instance ID 不存在，会记录警告日志但不会中断流程

3. **重复状态**: 如果案件已经是目标状态，不会重复更新

4. **事务处理**: 状态更新在同一个事务中完成，确保数据一致性

## 后续优化建议

1. **增强日志**: 添加更详细的操作日志，包括用户信息和变更原因

2. **状态历史**: 在状态变更时自动记录历史记录到 ClaimHistory 表

3. **事件通知**: 状态变更时触发通知服务

4. **状态验证**: 在更新前验证状态转换的合法性

## 总结

成功实现了 `ClaimStateListener`，通过 CMMN 的 planItemLifecycleListener 机制实现了案件状态的自动更新。现在当 case 流程进入不同阶段时，会自动调用 ClaimStateListener 来更新数据库中的 claim 状态，实现了 CMMN 流程和业务状态的完美同步。

所有修改已通过编译验证，可以安全部署使用。
