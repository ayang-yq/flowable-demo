# Claim处理流程与CMMN Case集成分析

## 问题概述

当前Claim处理流程与CMMN Case模型存在以下脱节问题：

## 1. 当前架构分析

### 1.1 Claim处理流程 (CaseService.java)

当理赔案件创建时，系统会：
1. 创建ClaimCase实体（存储在数据库）
2. 调用`startCaseProcess()`启动CMMN case实例
3. 保存caseInstanceId到ClaimCase实体

然而，后续的业务操作存在以下问题：
- `approveClaimCase()` - 仅设置变量，不触发CMMN任务完成
- `rejectClaimCase()` - 仅设置变量，不触发CMMN任务完成  
- `payClaimCase()` - 仅设置变量，不触发CMMN任务完成

这些操作只是简单更新了`ClaimCase.status`和设置了CMMN变量，但**没有实际推动CMMN工作流的进展**。

### 1.2 CMMN Case模型 (ClaimCase.cmmn)

CMMN模型定义了以下阶段和任务：

```
Triage Stage (分诊阶段)
├── taskReviewClaim (审查理赔申请) - assignedTo: claimAdjuster
└── taskAssessComplexity (评估复杂度) - uses DMN

Investigation Stage (调查阶段) - 条件: claimComplexity == 'complex'
├── taskGatherDocuments (收集缺失文档) - assignedTo: claimAdjuster
└── taskAssessDamage (评估损失) - assignedTo: damageAssessor

Approval Stage (审批阶段)
└── taskFinalApproval (最终审批) - candidateGroups: approverGroup

Payment Stage (支付阶段) - 条件: approved == true
└── taskProcessPayment (处理支付) - calls BPMN process

Closure Stage (关闭阶段)
└── taskNotifyCustomer (通知客户)
```

### 1.3 脱节问题总结

| 问题 | 描述 | 影响 |
|------|------|------|
| **双重状态管理** | ClaimCase有自己的状态机，CMMN有独立的状态管理 | 两个状态不同步 |
| **业务操作不驱动流程** | approve/reject/pay只设置变量，不完成任务 | CMMN流程停滞 |
| **任务完成不更新业务状态** | TaskResource完成任务时，不更新ClaimCase.status | 业务状态过期 |
| **变量更新 vs 任务完成** | 当前只设置变量，没有触发任务完成 | 流程引擎无法推进 |

## 2. 设计解决方案

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                     业务层 (Business Layer)                    │
│  CaseService.approveClaimCase() / rejectClaimCase()           │
└──────────────────────┬────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                   流程引擎层 (Flowable Engine)                 │
│  1. 查找当前活跃的CMMN任务                                      │
│  2. 验证任务类型与操作匹配                                      │
│  3. 完成CMMN任务 (cmmnTaskService.complete())                  │
│  4. 设置流程变量                                               │
└──────────────────────┬────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                   业务实体层 (Domain Entity)                   │
│  ClaimCase.status 根据CMMN流程进展自动更新                      │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 任务与业务操作映射

| CMMN任务 | Task Definition Key | 对应业务操作 | 触发方式 |
|----------|---------------------|-------------|----------|
| taskReviewClaim | taskReviewClaim | assignClaimCase | 自动完成 |
| taskAssessComplexity | taskAssessComplexity | DMN自动执行 | 自动完成 |
| taskGatherDocuments | taskGatherDocuments | updateClaimCase | 用户完成 |
| taskAssessDamage | taskAssessDamage | updateClaimCase | 用户完成 |
| taskFinalApproval | taskFinalApproval | approveClaimCase / rejectClaimCase | API触发完成 |
| taskProcessPayment | taskProcessPayment | payClaimCase | API触发完成 |
| taskNotifyCustomer | taskNotifyCustomer | 自动通知 | 自动完成 |

### 2.3 状态同步策略

使用Flowable的**事件监听器**实现状态同步：

```java
@Component
public class CaseCompleteListener implements FlowableEventListener {
    
    @Override
    public void onEvent(FlowableEvent event) {
        if (event instanceof FlowableEntityEvent) {
            FlowableEntityEvent entityEvent = (FlowableEntityEvent) event;
            if (entityEvent.getEntity() instanceof TaskEntity) {
                TaskEntity task = (TaskEntity) entityEvent;
                
                // 根据完成的任务更新ClaimCase状态
                String taskDefinitionKey = task.getTaskDefinitionKey();
                String caseInstanceId = task.getScopeId();
                
                updateClaimCaseStatus(caseInstanceId, taskDefinitionKey);
            }
        }
    }
    
    private void updateClaimCaseStatus(String caseInstanceId, String taskDefinitionKey) {
        // 查找对应的ClaimCase
        ClaimCase claimCase = claimCaseRepository.findByCaseInstanceId(caseInstanceId)
            .orElseThrow(() -> new RuntimeException("ClaimCase not found"));
        
        // 根据任务类型更新状态
        switch (taskDefinitionKey) {
            case "taskReviewClaim":
                claimCase.setStatus(ClaimStatus.UNDER_REVIEW);
                break;
            case "taskAssessComplexity":
                // DMN评估完成，根据结果决定是否进入调查阶段
                break;
            case "taskAssessDamage":
                claimCase.setStatus(ClaimStatus.INVESTIGATING);
                break;
            case "taskFinalApproval":
                // 审批完成后，根据approved变量决定状态
                break;
            case "taskProcessPayment":
                claimCase.setStatus(ClaimStatus.PAYMENT_PROCESSING);
                break;
            case "taskNotifyCustomer":
                claimCase.setStatus(ClaimStatus.CLOSED);
                break;
        }
        
        claimCaseRepository.save(claimCase);
    }
}
```

### 2.4 修改后的业务操作流程

#### 2.4.1 批准理赔 (approveClaimCase)

```java
public ClaimCase approveClaimCase(UUID caseId, String userId, ApproveRequestDTO approveRequestDTO) {
    ClaimCase claimCase = claimCaseRepository.findById(caseId)
        .orElseThrow(() -> new IllegalArgumentException("Claim case not found"));
    
    // 1. 验证状态
    if (!claimCase.canApprove()) {
        throw new IllegalStateException("Claim cannot be approved in current status");
    }
    
    User approvedBy = userRepository.findById(UUID.fromString(userId))
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
    
    // 2. 设置批准金额
    claimCase.setApprovedAmount(approveRequestDTO.getApprovedAmount());
    
    // 3. 构建审批意见
    String description = "Claim approved by " + approvedBy.getFullName();
    if (approveRequestDTO.getComments() != null && !approveRequestDTO.getComments().isBlank()) {
        description += " - " + approveRequestDTO.getComments();
    }
    
    // 4. 更新业务状态
    claimCase.updateStatus("APPROVED", description, approvedBy);
    claimCaseRepository.save(claimCase);
    
    // 5. [关键] 完成CMMN任务以推动流程
    if (claimCase.getCaseInstanceId() != null) {
        try {
            // 查找Final Approval任务
            List<Task> tasks = cmmnTaskService.createTaskQuery()
                .caseInstanceId(claimCase.getCaseInstanceId())
                .taskDefinitionKey("taskFinalApproval")
                .active()
                .list();
            
            if (!tasks.isEmpty()) {
                Task task = tasks.get(0);
                
                // 设置任务变量
                Map<String, Object> variables = new HashMap<>();
                variables.put("approved", true);
                variables.put("approvedBy", approvedBy.getUsername());
                variables.put("approvedDate", LocalDateTime.now().toString());
                variables.put("approvedAmount", approveRequestDTO.getApprovedAmount());
                
                // 完成任务，触发CMMN流程进入Payment阶段
                cmmnTaskService.complete(task.getId(), variables);
                
                log.info("Completed taskFinalApproval for claim case {}", claimCase.getId());
            } else {
                log.warn("No active taskFinalApproval found for claim case {}", claimCase.getId());
            }
        } catch (Exception e) {
            log.error("Failed to complete CMMN task: {}", e.getMessage(), e);
            // 不抛出异常，避免影响业务操作
        }
    }
    
    return claimCase;
}
```

#### 2.4.2 拒绝理赔 (rejectClaimCase)

```java
public ClaimCase rejectClaimCase(UUID caseId, String reason) {
    ClaimCase claimCase = claimCaseRepository.findById(caseId)
        .orElseThrow(() -> new IllegalArgumentException("Claim case not found"));
    
    // 1. 验证状态
    if (!claimCase.canReject()) {
        throw new IllegalStateException("Claim cannot be rejected in current status");
    }
    
    // 2. 更新业务状态
    claimCase.updateStatus("REJECTED", reason, claimCase.getCreatedBy());
    claimCaseRepository.save(claimCase);
    
    // 3. [关键] 完成CMMN任务以推动流程
    if (claimCase.getCaseInstanceId() != null) {
        try {
            // 查找Final Approval任务
            List<Task> tasks = cmmnTaskService.createTaskQuery()
                .caseInstanceId(claimCase.getCaseInstanceId())
                .taskDefinitionKey("taskFinalApproval")
                .active()
                .list();
            
            if (!tasks.isEmpty()) {
                Task task = tasks.get(0);
                
                // 设置任务变量
                Map<String, Object> variables = new HashMap<>();
                variables.put("approved", false);
                variables.put("rejectReason", reason);
                
                // 完成任务，触发CMMN流程的exit criterion
                cmmnTaskService.complete(task.getId(), variables);
                
                log.info("Completed taskFinalApproval (rejected) for claim case {}", claimCase.getId());
            } else {
                log.warn("No active taskFinalApproval found for claim case {}", claimCase.getId());
            }
        } catch (Exception e) {
            log.error("Failed to complete CMMN task: {}", e.getMessage(), e);
        }
    }
    
    return claimCase;
}
```

#### 2.4.3 支付理赔 (payClaimCase)

```java
public ClaimCase payClaimCase(UUID caseId, BigDecimal paymentAmount, 
                              LocalDate paymentDate, String paymentMethod,
                              String paymentReference, String userId) {
    ClaimCase claimCase = claimCaseRepository.findById(caseId)
        .orElseThrow(() -> new IllegalArgumentException("Claim case not found"));
    
    // 1. 验证状态
    if (claimCase.getStatus() != ClaimCase.ClaimStatus.APPROVED) {
        throw new IllegalStateException("Claim must be in APPROVED status to process payment");
    }
    
    User paidBy = userRepository.findById(UUID.fromString(userId))
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
    
    // 2. 更新业务状态为支付处理中
    claimCase.updateStatus("PAYMENT_PROCESSING", "Payment initiated by " + paidBy.getFullName(), paidBy);
    claimCaseRepository.save(claimCase);
    
    // 3. [关键] 完成CMMN任务以推动流程
    if (claimCase.getCaseInstanceId() != null) {
        try {
            // 查找Process Payment任务
            List<Task> tasks = cmmnTaskService.createTaskQuery()
                .caseInstanceId(claimCase.getCaseInstanceId())
                .taskDefinitionKey("taskProcessPayment")
                .active()
                .list();
            
            if (!tasks.isEmpty()) {
                Task task = tasks.get(0);
                
                // 设置任务变量
                Map<String, Object> variables = new HashMap<>();
                variables.put("paymentAmount", paymentAmount);
                variables.put("paymentDate", paymentDate.toString());
                variables.put("paymentMethod", paymentMethod);
                variables.put("paymentReference", paymentReference);
                variables.put("paymentStatus", "COMPLETED");
                variables.put("paidBy", paidBy.getUsername());
                variables.put("paidDate", LocalDateTime.now().toString());
                
                // 完成任务，触发CMMN流程进入Closure阶段
                cmmnTaskService.complete(task.getId(), variables);
                
                log.info("Completed taskProcessPayment for claim case {}", claimCase.getId());
            } else {
                log.warn("No active taskProcessPayment found for claim case {}", claimCase.getId());
            }
        } catch (Exception e) {
            log.error("Failed to complete CMMN task: {}", e.getMessage(), e);
        }
    }
    
    // 4. 更新业务状态为已支付
    String paymentDescription = String.format("Payment processed: Amount=%s, Method=%s, Reference=%s",
            paymentAmount, paymentMethod, paymentReference);
    claimCase.updateStatus("PAID", paymentDescription, paidBy);
    claimCaseRepository.save(claimCase);
    
    return claimCase;
}
```

### 2.5 状态映射表

| CMMN阶段/任务 | ClaimCase状态 | 说明 |
|--------------|----------------|------|
| Case Started | SUBMITTED | 案件刚创建 |
| taskReviewClaim completed | UNDER_REVIEW | 审查完成 |
| taskAssessComplexity completed | UNDER_REVIEW | 复杂度评估完成 |
| taskGatherDocuments (complex) | INVESTIGATING | 收集文档中 |
| taskAssessDamage completed | INVESTIGATING | 损失评估完成 |
| taskFinalApproval completed (approved=true) | APPROVED | 审批通过 |
| taskFinalApproval completed (approved=false) | REJECTED | 审批拒绝 |
| taskProcessPayment completed | PAID | 支付完成 |
| taskNotifyCustomer completed | CLOSED | 案件关闭 |

## 3. 实施计划

### 3.1 Phase 1: 修改CaseService

- [ ] 修改`approveClaimCase()`方法，添加任务完成逻辑
- [ ] 修改`rejectClaimCase()`方法，添加任务完成逻辑
- [ ] 修改`payClaimCase()`方法，添加任务完成逻辑
- [ ] 添加任务查找辅助方法

### 3.2 Phase 2: 实现事件监听器

- [ ] 创建`CaseCompleteListener`监听器
- [ ] 注册监听器到Flowable配置
- [ ] 实现状态同步逻辑

### 3.3 Phase 3: 测试验证

- [ ] 创建完整流程的集成测试
- [ ] 验证状态同步正确性
- [ ] 验证CMMN流程可视化正确显示

### 3.4 Phase 4: 文档更新

- [ ] 更新README.md
- [ ] 添加集成说明文档
- [ ] 更新API文档

## 4. 关键代码片段

### 4.1 任务查找辅助方法

```java
/**
 * 查找并完成指定类型的CMMN任务
 */
private void completeCmmnTask(String caseInstanceId, String taskDefinitionKey, 
                              Map<String, Object> variables) {
    try {
        List<Task> tasks = cmmnTaskService.createTaskQuery()
            .caseInstanceId(caseInstanceId)
            .taskDefinitionKey(taskDefinitionKey)
            .active()
            .list();
        
        if (!tasks.isEmpty()) {
            Task task = tasks.get(0);
            cmmnTaskService.complete(task.getId(), variables);
            log.info("Completed CMMN task {} for case instance {}", taskDefinitionKey, caseInstanceId);
        } else {
            log.warn("No active task {} found for case instance {}", taskDefinitionKey, caseInstanceId);
        }
    } catch (Exception e) {
        log.error("Failed to complete CMMN task {}: {}", taskDefinitionKey, e.getMessage(), e);
    }
}
```

## 5. 预期效果

### 5.1 流程一致性
- 业务操作直接驱动CMMN流程进展
- CMMN流程状态与ClaimCase状态保持同步
- 流程可视化能够正确反映当前状态

### 5.2 可追溯性
- 每个业务操作都有对应的CMMN任务记录
- 可以通过Flowable历史记录查看完整流程路径
- 状态变更都有明确的触发事件

### 5.3 扩展性
- 基于事件的架构便于添加新的监听器
- 任务与业务操作的映射清晰，易于维护
- 支持多种流程变体（简单/复杂理赔）

## 6. 风险与注意事项

### 6.1 潜在风险
- **事务边界**: 需要确保业务操作和任务完成在同一事务中
- **并发控制**: 多个用户同时操作可能导致冲突
- **异常处理**: CMMN任务失败不应影响业务数据一致性

### 6.2 缓解措施
- 使用`@Transactional`确保事务一致性
- 添加任务状态校验，防止重复完成
- 实现完善的日志记录和异常处理
- 添加单元测试和集成测试覆盖各种场景

## 7. 总结

通过以上修改，Claim处理流程将与CMMN Case模型完全联动：

1. **业务操作驱动流程**: approve/reject/pay等操作会完成对应的CMMN任务
2. **状态自动同步**: 通过事件监听器保持业务状态与流程状态同步
3. **可视化可追溯**: 管理员可以通过CMMN可视化查看完整的流程进展

这种架构遵循了"业务驱动流程"的原则，既保持了业务逻辑的独立性，又充分利用了Flowable工作流引擎的能力。
