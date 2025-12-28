# 流程驱动状态管理与任务集成实施计划

## 一、项目概述

### 1.1 项目目标

**核心目标：**
1. **移除理赔详情页的直接状态更改** - 完全由CMMN+BPMN流程驱动Case状态演进
2. **移除单独的任务中心，将任务与理赔详情合并** - 提供更好的操作体验

### 1.2 实施原则

- **单一真相来源** - 流程引擎是状态的决定者
- **单向数据流** - 用户操作 → 完成任务 → 流程流转 → 监听器触发 → 更新数据库
- **事件驱动** - 使用Flowable监听器自动同步状态
- **用户体验优先** - 减少页面跳转，提供完整的上下文信息

### 1.3 预期效果

1. ✅ 所有状态变更由流程引擎控制，无手动干预
2. ✅ 用户在同一页面完成所有操作，无需跳转
3. ✅ 流程状态和数据库状态始终一致
4. ✅ 完整的操作审计追踪
5. ✅ 简化的代码结构，更易维护

---

## 二、总体架构设计

### 2.1 架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端层                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  理赔详情页 (ClaimDetail.tsx)                         │   │
│  │                                                         │   │
│  │  ┌──────────────────────────────────────────────────┐  │   │
│  │  │  待办任务卡片                                    │  │   │
│  │  │  - 显示当前待办任务                               │  │   │
│  │  │  - 点击即可处理                                  │  │   │
│  │  │  - 显示任务关键信息预览                          │  │   │
│  │  └──────────────────────────────────────────────────┘  │   │
│  │                                                         │   │
│  │  ┌──────────────────────────────────────────────────┐  │   │
│  │  │  标签页                                         │  │   │
│  │  │  [基本信息] [支付进度] [相关文档] [处理历史]    │  │   │
│  │  └──────────────────────────────────────────────────┘  │   │
│  │                                                         │   │
│  │  ┌──────────────────────────────────────────────────┐  │   │
│  │  │  任务处理模态框                                  │  │   │
│  │  │  - 显示完整上下文（理赔信息+支付信息）          │  │   │
│  │  │  - 提交任务变量                                 │  │   │
│  │  └──────────────────────────────────────────────────┘  │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ❌ 移除：TaskList.tsx（任务中心）                             │
└─────────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────────┐
│                        API层                                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ✅ 保留的API:                                                  │
│  ├─ GET  /api/cases/{id}                - 获取Case详情          │
│  ├─ POST /api/tasks/{id}/complete       - 完成任务（不修改状态）│
│  ├─ GET  /api/tasks/{id}/variables      - 获取任务变量          │
│  ├─ GET  /api/tasks/by-case/{caseId}    - 根据Case获取任务      │
│  └─ GET  /api/cases/my-tasks           - 获取我的待办任务      │
│                                                                 │
│  ❌ 废弃的API（标记为@Deprecated）：                            │
│  ├─ POST /api/cases/{id}/status        - 直接更新状态          │
│  ├─ POST /api/cases/{id}/approve       - 直接批准              │
│  ├─ POST /api/cases/{id}/reject        - 直接拒绝              │
│  └─ POST /api/cases/{id}/pay           - 直接支付              │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────────┐
│                        流程引擎层                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────┐      ┌──────────────────┐              │
│  │   CMMN引擎       │      │   BPMN引擎       │              │
│  │                  │      │                  │              │
│  │  Case Instance   │────→ │ Process Instance │              │
│  │                  │      │                  │              │
│  │  - Case Stage    │      │  - User Tasks    │              │
│  │  - Process Tasks │      │  - Service Tasks │              │
│  │  - Event List.   │      │  - Event List.   │              │
│  └──────────────────┘      └──────────────────┘              │
│           │                          │                        │
│           │ 触发监听器                │                        │
│           └──────────┬───────────────┘                        │
│                      ↓                                         │
│  ┌────────────────────────────────────────────────────────┐  │
│  │              监听器层（状态同步）                       │  │
│  ├────────────────────────────────────────────────────────┤  │
│  │  CmmnStageListener                                     │  │
│  │  ├─ SubmissionStage.start   → SUBMITTED               │  │
│  │  ├─ ReviewStage.start      → UNDER_REVIEW            │  │
│  │  ├─ ApprovalTask.complete  → APPROVED/REJECTED       │  │
│  │  ├─ PaymentStage.start     → PAYMENT_IN_PROGRESS      │  │
│  │  └─ ClosureStage.complete  → CLOSED                  │  │
│  │                                                        │  │
│  │  PaymentUpdateService (BPMN)                           │  │
│  │  ├─ startEvent              → PAYMENT_IN_PROGRESS    │  │
│  │  ├─ executePayment         → PAYING                 │  │
│  │  ├─ confirmPayment         → PAID                   │  │
│  │  ├─ paymentRejected        → REJECTED               │  │
│  │  └─ endEvent               → 触发Closure Stage      │  │
│  └────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────────┐
│                      数据库层                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ClaimCase表 (只由监听器更新)                                  │
│  ├─ status (ClaimStatus)     ← 只由监听器更新                │
│  ├─ paymentStatus (String)   ← 只由监听器更新                │
│  ├─ approvedAmount           ← 只由监听器更新                │
│  ├─ paidAmount              ← 只由监听器更新                │
│  ├─ transactionId           ← 只由监听器更新                │
│  └─ paymentDate             ← 只由监听器更新                │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 状态映射表

**CMMN → 数据库状态映射：**

| CMMN事件 | 当前状态 | 目标状态 | 说明 |
|---------|---------|---------|------|
| SubmissionStage.start | - | SUBMITTED | 理赔提交 |
| ReviewStage.start | SUBMITTED | UNDER_REVIEW | 进入审核 |
| ApprovalTask.complete(approved=true) | UNDER_REVIEW | APPROVED | 审批通过 |
| ApprovalTask.complete(approved=false) | UNDER_REVIEW | REJECTED | 审批拒绝 |
| PaymentStage.start | APPROVED | (保持APPROVED) | 进入支付 |
| ClosureStage.start | PAID/REJECTED | (保持) | 开始关闭 |
| ClosureStage.complete | PAID/REJECTED | CLOSED | 关闭Case |

**BPMN → 数据库状态映射：**

| BPMN事件 | 更新字段 | 说明 |
|---------|---------|------|
| startEvent | paymentStatus=PAYMENT_IN_PROGRESS | 开始支付流程 |
| executePayment | paymentStatus=PAYING, transactionId | 执行支付 |
| confirmPayment(确认) | paymentStatus=PAID, paidAmount, paymentDate | 支付确认 |
| confirmPayment(争议) | paymentStatus=DISPUTED | 支付争议 |
| paymentRejected | paymentStatus=PAYMENT_REJECTED, status=REJECTED | 支付被拒 |
| endEvent(成功) | status=PAID, 触发Closure Stage | 流程结束 |

---

## 三、分阶段实施计划

### 阶段0：准备工作（1天）

**目标：** 确保开发环境和数据准备就绪

**任务清单：**
- [ ] 创建分支 `feature/flow-driven-status`
- [ ] 备份当前数据库
- [ ] 准备测试数据（至少包含3个不同状态的理赔）
- [ ] 搭建本地测试环境

**验收标准：**
- 开发环境正常运行
- 测试数据可用
- 可以成功创建新理赔

---

### 阶段1：后端改进 - 监听器实现（3-4天）

#### 1.1 创建CmmnStageListener（1天）

**文件：** `backend/src/main/java/com/flowable/demo/service/CmmnStageListener.java`

**实现内容：**

```java
package com.flowable.demo.service;

import com.flowable.demo.domain.model.ClaimCase;
import com.flowable.demo.domain.model.ClaimCase.ClaimStatus;
import com.flowable.demo.domain.repository.ClaimCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.cmmn.api.delegate.CaseExecutionListener;
import org.flowable.cmmn.api.delegate.DelegateCaseExecution;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * CMMN阶段监听器
 * 监听CMMN Case的Stage变化，同步更新Case状态
 */
@Component("cmmnStageListener")
@Slf4j
@RequiredArgsConstructor
public class CmmnStageListener implements CaseExecutionListener {

    private final ClaimCaseRepository claimCaseRepository;

    @Override
    public void notify(DelegateCaseExecution caseExecution) {
        String caseInstanceId = caseExecution.getId();
        String planItemId = caseExecution.getCurrentPlanItem();
        String eventName = caseExecution.getEventName();
        
        log.info("CMMN Stage listener: caseInstanceId={}, planItemId={}, event={}", 
            caseInstanceId, planItemId, eventName);
        
        if (planItemId == null || eventName == null) {
            log.warn("PlanItem ID or EventName is null, skipping listener");
            return;
        }

        claimCaseRepository.findByCaseInstanceId(caseInstanceId).ifPresentOrElse(
            claimCase -> {
                log.info("Processing claim case: {} for plan item: {}", claimCase.getId(), planItemId);
                
                if ("start".equals(eventName)) {
                    updateStatusOnStageStart(claimCase, planItemId);
                } else if ("complete".equals(eventName)) {
                    updateStatusOnStageComplete(claimCase, planItemId, caseExecution);
                }
                
                claimCaseRepository.save(claimCase);
                log.info("Claim case {} status updated to: {}", claimCase.getId(), claimCase.getStatus());
            },
            () -> {
                log.warn("No claim case found for case instance: {}", caseInstanceId);
            }
        );
    }

    /**
     * Stage开始时更新状态
     */
    private void updateStatusOnStageStart(ClaimCase claimCase, String planItemId) {
        switch (planItemId) {
            case "planItemSubmission":
                log.info("Submission Stage started, setting status to SUBMITTED");
                claimCase.setStatus(ClaimStatus.SUBMITTED);
                break;
                
            case "planItemReview":
                log.info("Review Stage started, setting status to UNDER_REVIEW");
                claimCase.setStatus(ClaimStatus.UNDER_REVIEW);
                break;
                
            case "planItemPayment":
                log.info("Payment Stage started, keeping status as APPROVED");
                // Payment Stage开始时，状态保持为APPROVED
                // 设置paymentStatus为进行中
                claimCase.setPaymentStatus("PAYMENT_IN_PROGRESS");
                break;
                
            case "planItemClosure":
                log.info("Closure Stage started, status should already be PAID or REJECTED");
                // Closure Stage开始时，状态应该已经是PAID或REJECTED
                // 不需要改变
                break;
                
            default:
                log.debug("Unknown plan item: {}, no status change", planItemId);
                break;
        }
    }

    /**
     * Stage完成时更新状态
     */
    private void updateStatusOnStageComplete(ClaimCase claimCase, String planItemId, 
            DelegateCaseExecution caseExecution) {
        switch (planItemId) {
            case "planItemSubmission":
                log.info("Submission Stage completed, transitioning to Review");
                // Submission完成，进入Review
                break;
                
            case "planItemReview":
                log.info("Review Stage completed, checking approval status");
                // Review完成，检查是否被批准
                Object approvedObj = caseExecution.getVariable("approved");
                Boolean approved = approvedObj instanceof Boolean ? (Boolean) approvedObj : null;
                
                if (approved != null && approved) {
                    log.info("Claim approved, setting status to APPROVED");
                    claimCase.setStatus(ClaimStatus.APPROVED);
                    
                    // 设置批准金额
                    Object approvedAmountObj = caseExecution.getVariable("approvedAmount");
                    if (approvedAmountObj != null) {
                        try {
                            BigDecimal approvedAmount = new BigDecimal(approvedAmountObj.toString());
                            claimCase.setApprovedAmount(approvedAmount);
                            log.info("Approved amount set to: {}", approvedAmount);
                        } catch (NumberFormatException e) {
                            log.error("Invalid approved amount format: {}", approvedAmountObj, e);
                        }
                    }
                } else {
                    log.info("Claim rejected, setting status to REJECTED");
                    claimCase.setStatus(ClaimStatus.REJECTED);
                }
                break;
                
            case "planItemPayment":
                log.info("Payment Stage completed, status should already be updated by BPMN listeners");
                // Payment完成，状态由PaymentCompletionListener处理
                break;
                
            case "planItemClosure":
                log.info("Closure Stage completed, setting status to CLOSED");
                // Case完成，关闭
                claimCase.setStatus(ClaimStatus.CLOSED);
                break;
                
            default:
                log.debug("Unknown plan item: {}, no status change", planItemId);
                break;
        }
    }
}
```

**验收标准：**
- [ ] CmmnStageListener编译通过
- [ ] 在CMMN模型中注册监听器
- [ ] 编写单元测试

#### 1.2 更新PaymentUpdateService（0.5天）

**文件：** `backend/src/main/java/com/flowable/demo/service/PaymentUpdateService.java`

**改动内容：**

```java
@Override
public void execute(DelegateExecution execution) {
    log.info("Executing payment update service for process instance: {}", execution.getProcessInstanceId());
    
    String caseInstanceId = (String) execution.getVariable("caseInstanceId");
    String paymentStatus = (String) execution.getVariable("paymentStatus");
    String transactionId = (String) execution.getVariable("transactionId");
    String currentActivityId = execution.getCurrentActivityId();
    
    log.info("Payment update - Case Instance ID: {}, Payment Status: {}, Transaction ID: {}, Activity ID: {}", 
            caseInstanceId, paymentStatus, transactionId, currentActivityId);
    
    if (caseInstanceId == null) {
        log.warn("Case instance ID is null, cannot update payment status");
        return;
    }

    claimCaseRepository.findByCaseInstanceId(caseInstanceId).ifPresentOrElse(
        claimCase -> {
            log.info("Found claim case: {} for case instance: {}", claimCase.getId(), caseInstanceId);
            
            // 根据当前活动更新状态
            updateClaimStatusByActivity(claimCase, currentActivityId, execution);
            
            // 更新支付相关字段
            if (paymentStatus != null) {
                claimCase.setPaymentStatus(paymentStatus);
            }
            
            if (transactionId != null) {
                claimCase.setTransactionId(transactionId);
            }
            
            // 根据支付状态更新主状态
            updateMainStatusByPaymentStatus(claimCase, paymentStatus);
            
            claimCaseRepository.save(claimCase);
            log.info("Claim case {} updated: paymentStatus={}, mainStatus={}", 
                claimCase.getId(), claimCase.getPaymentStatus(), claimCase.getStatus());
        },
        () -> {
            log.warn("No claim case found for case instance: {}", caseInstanceId);
        }
    );
}

/**
 * 根据当前活动更新状态
 */
private void updateClaimStatusByActivity(ClaimCase claimCase, String activityId, 
        DelegateExecution execution) {
    if (activityId == null) {
        return;
    }
    
    switch (activityId) {
        case "startEvent_paymentStart":
            // 支付流程开始，更新为支付进行中
            log.info("Payment process started, setting paymentStatus to PAYMENT_IN_PROGRESS");
            claimCase.setPaymentStatus("PAYMENT_IN_PROGRESS");
            break;
            
        case "serviceTask_executePayment":
            // 执行支付
            log.info("Executing payment, setting paymentStatus to PAYING");
            claimCase.setPaymentStatus("PAYING");
            break;
            
        case "userTask_confirmPayment":
            // 等待确认
            log.info("Awaiting payment confirmation, setting paymentStatus to AWAITING_CONFIRMATION");
            claimCase.setPaymentStatus("AWAITING_CONFIRMATION");
            break;
            
        case "userTask_paymentRejected":
            // 支付被拒绝
            log.info("Payment rejected, setting paymentStatus to PAYMENT_REJECTED and status to REJECTED");
            claimCase.setPaymentStatus("PAYMENT_REJECTED");
            claimCase.setStatus(ClaimStatus.REJECTED);
            break;
            
        default:
            // 其他节点保持不变
            log.debug("Activity {} does not require status change", activityId);
            break;
    }
}

/**
 * 根据支付状态更新主状态
 */
private void updateMainStatusByPaymentStatus(ClaimCase claimCase, String paymentStatus) {
    if (paymentStatus == null) {
        return;
    }
    
    if ("PAID".equals(paymentStatus)) {
        // 支付成功，等待Closure Stage完成
        log.info("Payment successful, setting status to PAID");
        claimCase.setStatus(ClaimStatus.PAID);
        claimCase.setPaidAmount(claimCase.getApprovedAmount());
        claimCase.setPaymentDate(java.time.LocalDate.now());
    } else if ("REJECTED".equals(paymentStatus) || "PAYMENT_REJECTED".equals(paymentStatus)) {
        // 支付被拒绝
        log.info("Payment rejected, setting status to REJECTED");
        claimCase.setStatus(ClaimStatus.REJECTED);
    }
}
```

**验收标准：**
- [ ] PaymentUpdateService更新完成
- [ ] 编译通过
- [ ] 编写单元测试

#### 1.3 更新PaymentCompletionListener（0.5天）

**文件：** `backend/src/main/java/com/flowable/demo/service/PaymentCompletionListener.java`

**改动内容：**

```java
@Override
public void notify(DelegateExecution execution) {
    log.info("Payment completion listener triggered for process instance: {}", execution.getProcessInstanceId());
    
    String caseInstanceId = (String) execution.getVariable("caseInstanceId");
    String paymentStatus = (String) execution.getVariable("paymentStatus");
    String transactionId = (String) execution.getVariable("transactionId");
    
    log.info("Payment completed - Case Instance ID: {}, Payment Status: {}, Transaction ID: {}", 
            caseInstanceId, paymentStatus, transactionId);
    
    if (caseInstanceId == null) {
        log.warn("Case instance ID is null");
        return;
    }

    claimCaseRepository.findByCaseInstanceId(caseInstanceId).ifPresent(claimCase -> {
        if ("PAID".equals(paymentStatus)) {
            // 支付成功，触发CMMN Closure Stage
            log.info("Payment completed successfully for case {}, triggering Closure Stage", caseInstanceId);
            claimCase.setStatus(ClaimStatus.PAID);
            
            // 设置变量以触发Closure Stage
            // 注意：这里需要在CMMN模型中配置相应的Sentry
            try {
                if (cmmnRuntimeService != null) {
                    cmmnRuntimeService.setVariable(caseInstanceId, "paymentCompleted", true);
                    log.info("Set paymentCompleted=true for case instance: {}", caseInstanceId);
                }
            } catch (Exception e) {
                log.error("Failed to trigger Closure Stage", e);
            }
            
        } else if ("REJECTED".equals(paymentStatus)) {
            // 支付被拒绝
            log.info("Payment rejected for case {}, triggering Closure Stage (reject path)", caseInstanceId);
            claimCase.setStatus(ClaimStatus.REJECTED);
            
            // 触发Closure Stage（拒绝分支）
            try {
                if (cmmnRuntimeService != null) {
                    cmmnRuntimeService.setVariable(caseInstanceId, "paymentRejected", true);
                    log.info("Set paymentRejected=true for case instance: {}", caseInstanceId);
                }
            } catch (Exception e) {
                log.error("Failed to trigger Closure Stage", e);
            }
        }
        
        claimCaseRepository.save(claimCase);
        log.info("Claim case {} saved with status: {}", claimCase.getId(), claimCase.getStatus());
    });
}
```

**验收标准：**
- [ ] PaymentCompletionListener更新完成
- [ ] 编译通过
- [ ] 编写单元测试

#### 1.4 在CMMN模型中注册监听器（0.5天）

**文件：** `backend/src/main/resources/cases/ClaimCase.cmmn`

**改动内容：**

在每个Stage中添加监听器：

```xml
<!-- Submission Stage -->
<cmmn:planItem id="planItemSubmission" name="Submission Stage" definitionRef="humanTaskClaimSubmission">
    <cmmn:extensionElements>
        <flowable:taskListener event="start" delegateExpression="${cmmnStageListener}" />
        <flowable:taskListener event="complete" delegateExpression="${cmmnStageListener}" />
    </cmmn:extensionElements>
</cmmn:planItem>

<!-- Review Stage -->
<cmmn:planItem id="planItemReview" name="Review Stage" definitionRef="stageClaimReview">
    <cmmn:extensionElements>
        <flowable:taskListener event="start" delegateExpression="${cmmnStageListener}" />
        <flowable:taskListener event="complete" delegateExpression="${cmmnStageListener}" />
    </cmmn:extensionElements>
</cmmn:planItem>

<!-- Payment Stage -->
<cmmn:planItem id="planItemPayment" name="Payment Stage" definitionRef="stageClaimPayment">
    <cmmn:extensionElements>
        <flowable:taskListener event="start" delegateExpression="${cmmnStageListener}" />
        <flowable:taskListener event="complete" delegateExpression="${cmmnStageListener}" />
    </cmmn:extensionElements>
</cmmn:planItem>

<!-- Closure Stage -->
<cmmn:planItem id="planItemClosure" name="Closure Stage" definitionRef="stageClaimClosure">
    <cmmn:extensionElements>
        <flowable:taskListener event="start" delegateExpression="${cmmnStageListener}" />
        <flowable:taskListener event="complete" delegateExpression="${cmmnStageListener}" />
    </cmmn:extensionElements>
</cmmn:planItem>
```

**验收标准：**
- [ ] CMMN模型更新完成
- [ ] 模型可以成功部署
- [ ] 监听器被正确触发

#### 1.5 废弃直接更新状态的API（0.5天）

**文件：** `backend/src/main/java/com/flowable/demo/web/rest/CaseResource.java`

**改动内容：**

标记以下API为@Deprecated：

```java
/**
 * @deprecated 使用任务处理方式，不要直接更新状态
 *             状态变更由流程监听器自动处理
 *             此方法将在下个版本移除
 */
@Deprecated(since = "1.0", forRemoval = true)
@PostMapping("/{id}/status")
@Operation(
    summary = "更新理赔状态（已废弃）", 
    description = "【已废弃】请使用任务处理方式。状态变更由流程监听器自动处理。"
)
public ResponseEntity<ClaimCaseDTO> updateClaimCaseStatus(
        @PathVariable UUID id,
        @RequestParam String status,
        @RequestParam(required = false) String description,
        @RequestParam String userId) {
    log.warn("Direct status update is deprecated for claim {}. Please use task completion.", id);
    throw new UnsupportedOperationException(
        "Direct status update is deprecated. Please use task completion to let the process engine control status."
    );
}

/**
 * @deprecated 审批通过通过完成审批任务实现
 *             此方法将在下个版本移除
 */
@Deprecated(since = "1.0", forRemoval = true)
@PostMapping("/{id}/approve")
@Operation(
    summary = "批准理赔案件（已废弃）", 
    description = "【已废弃】请完成审批任务"
)
public ResponseEntity<ClaimCaseDTO> approveClaimCase(
        @PathVariable UUID id,
        @RequestParam String userId,
        @Valid @RequestBody ApproveRequestDTO approveRequestDTO) {
    log.warn("Direct approve is deprecated for claim {}. Please complete the approval task.", id);
    throw new UnsupportedOperationException(
        "Direct approve is deprecated. Please complete the approval task to let the process engine control status."
    );
}

/**
 * @deprecated 拒绝通过完成审批任务实现
 *             此方法将在下个版本移除
 */
@Deprecated(since = "1.0", forRemoval = true)
@PostMapping("/{id}/reject")
@Operation(
    summary = "拒绝理赔案件（已废弃）", 
    description = "【已废弃】请完成审批任务并设置approved=false"
)
public ResponseEntity<ClaimCaseDTO> rejectClaimCase(
        @PathVariable UUID id,
        @Valid @RequestBody RejectRequestDTO rejectRequestDTO) {
    log.warn("Direct reject is deprecated for claim {}. Please complete the approval task.", id);
    throw new UnsupportedOperationException(
        "Direct reject is deprecated. Please complete the approval task with approved=false to let the process engine control status."
    );
}

/**
 * @deprecated 支付通过BPMN流程实现
 *             此方法将在下个版本移除
 */
@Deprecated(since = "1.0", forRemoval = true)
@PostMapping("/{id}/pay")
@Operation(
    summary = "支付理赔案件（已废弃）", 
    description = "【已废弃】支付通过BPMN支付流程处理"
)
public ResponseEntity<ClaimCaseDTO> payClaimCase(
        @PathVariable String id,
        @Valid @RequestBody PaymentRequestDTO paymentRequestDTO) {
    log.warn("Direct pay is deprecated for claim {}. Please use the BPMN payment process.", id);
    throw new UnsupportedOperationException(
        "Direct pay is deprecated. Please process the payment through the BPMN payment workflow."
    );
}
```

**验收标准：**
- [ ] 所有直接状态变更API标记为@Deprecated
- [ ] API文档更新
- [ ] 单元测试更新

#### 1.6 编写监听器单元测试（0.5天）

**文件：** `backend/src/test/java/com/flowable/demo/service/CmmnStageListenerTest.java`

**验收标准：**
- [ ] 所有监听器都有单元测试
- [ ] 测试覆盖率达到80%以上
- [ ] 所有测试通过

---

### 阶段2：后端API改进（2天）

#### 2.1 添加获取Case待办任务的API（0.5天）

**文件：** `backend/src/main/java/com/flowable/demo/web/rest/TaskResource.java`

**新增方法：**

```java
/**
 * 根据Case实例ID获取相关任务
 */
@GetMapping("/by-case/{caseInstanceId}")
@Operation(summary = "根据Case获取任务", description = "获取与指定Case实例相关的所有任务")
public ResponseEntity<Page<TaskDTO>> getTasksByCaseInstanceId(
        @Parameter(description = "Case实例ID") @PathVariable String caseInstanceId,
        @Parameter(description = "只返回未完成的任务") @RequestParam(defaultValue = "false") boolean activeOnly,
        @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
    log.debug("REST request to get tasks by case instance: {}, activeOnly: {}", caseInstanceId, activeOnly);
    
    Page<TaskDTO> tasks = taskService.getTasksByCaseInstanceId(
        caseInstanceId, activeOnly, PageRequest.of(page, size));
    
    return ResponseEntity.ok(tasks);
}
```

**文件：** `backend/src/main/java/com/flowable/demo/service/TaskService.java`

**新增方法：**

```java
public Page<TaskDTO> getTasksByCaseInstanceId(String caseInstanceId, boolean activeOnly, Pageable pageable) {
    log.info("Getting tasks for case instance: {}, activeOnly: {}", caseInstanceId, activeOnly);
    
    List<org.flowable.task.api.Task> flowableTasks;
    
    if (activeOnly) {
        // 只查询未完成的任务
        flowableTasks = taskService.createTaskQuery()
            .processVariableValueEquals("caseInstanceId", caseInstanceId)
            .active()
            .orderByTaskCreateTime()
            .desc()
            .listPage((int) pageable.getOffset(), pageable.getPageSize());
    } else {
        // 查询所有任务
        flowableTasks = taskService.createTaskQuery()
            .processVariableValueEquals("caseInstanceId", caseInstanceId)
            .includeProcessVariables()
            .orderByTaskCreateTime()
            .desc()
            .listPage((int) pageable.getOffset(), pageable.getPageSize());
    }
    
    long total = taskService.createTaskQuery()
        .processVariableValueEquals("caseInstanceId", caseInstanceId)
        .count();
    
    List<TaskDTO> taskDTOs = flowableTasks.stream()
        .map(this::convertToTaskDTO)
        .collect(Collectors.toList());
    
    return new PageImpl<>(taskDTOs, pageable, total);
}
```

**验收标准：**
- [ ] API实现完成
- [ ] 编译通过
- [ ] 单元测试通过

#### 2.2 添加获取任务变量的API（0.5天）

**文件：** `backend/src/main/java/com/flowable/demo/web/rest/TaskResource.java`

**新增方法：**

```java
/**
 * 获取任务变量
 */
@GetMapping("/{taskId}/variables")
@Operation(summary = "获取任务变量", description = "获取指定任务的所有变量")
public ResponseEntity<Map<String, Object>> getTaskVariables(
        @Parameter(description = "任务ID") @PathVariable String taskId) {
    log.debug("REST request to get variables for task: {}", taskId);
    
    Map<String, Object> variables = taskService.getTaskVariables(taskId);
    
    return ResponseEntity.ok(variables);
}
```

**验收标准：**
- [ ] API实现完成
- [ ] 编译通过
- [ ] 单元测试通过

#### 2.3 改进任务完成API（0.5天）

**文件：** `backend/src/main/java/com/flowable/demo/web/rest/TaskResource.java`

**修改方法：**

```java
/**
 * 完成任务
 */
@PostMapping("/{taskId}/complete")
@Operation(summary = "完成任务", description = "完成指定任务并传递变量")
@Transactional
public ResponseEntity<Map<String, Object>> completeTask(
        @Parameter(description = "任务ID") @PathVariable String taskId,
        @RequestBody(required = false) Map<String, Object> variables) {
    log.debug("REST request to complete task: {} with variables: {}", taskId, variables);
    
    // 验证任务是否存在
    org.flowable.task.api.Task task = taskService.createTaskQuery()
        .taskId(taskId)
        .singleResult();
    
    if (task == null) {
        log.warn("Task not found: {}", taskId);
        return ResponseEntity.notFound().build();
    }
    
    log.info("Completing task: {}, name: {}, assignee: {}", taskId, task.getName(), task.getAssignee());
    
    // 如果有变量，设置任务变量
    if (variables != null && !variables.isEmpty()) {
        taskService.setVariables(taskId, variables);
        log.info("Set variables for task {}: {}", taskId, variables);
    }
    
    // 完成任务
    taskService.complete(taskId);
    
    log.info("Task {} completed successfully", taskId);
    
    Map<String, Object> response = new HashMap<>();
    response.put("success", true);
    response.put("taskId", taskId);
    response.put("message", "Task completed successfully");
    
    return ResponseEntity.ok(response);
}
```

**验收标准：**
- [ ] API改进完成
- [ ] 编译通过
- [ ] 单元测试通过

#### 2.4 API文档更新（0.5天）

**任务：**
- [ ] 更新Swagger/OpenAPI文档
- [ ] 添加示例和说明
- [ ] 标记废弃的API

**验收标准：**
- [ ] API文档完整
- [ ] 废弃API有明确说明
- [ ] 新API有使用示例

---

### 阶段3：前端重构 - 移除直接状态变更（2-3天）

#### 3.1 更新API服务层（0.5天）

**文件：** `frontend/src/services/api.ts`

**改动内容：**

```typescript
// ❌ 移除这些方法
// export const claimApi = {
//   updateClaimCaseStatus: (id: string, status: string, description?: string, userId?: string) => 
//     axios.post(`/api/cases/${id}/status`, null, { params: { status, description, userId } }),
//   approveClaimCase: (id: string, userId: string, data: ApproveRequestDTO) => 
//     axios.post(`/api/cases/${id}/approve?userId=${userId}`, data),
//   rejectClaimCase: (id: string, reason: string) => 
//     axios.post(`/api/cases/${id}/reject`, { reason }),
//   payClaim: (id: string, data: PaymentRequestDTO) => 
//     axios.post(`/api/cases/${id}/pay`, data),
// };

// ✅ 保留并增强任务相关方法
export const taskApi = {
  completeTask: (taskId: string, variables?: Record<string, any>) => 
    axios.post(`/api/tasks/${taskId}/complete`, variables),
  
  getTaskVariables: (taskId: string) => 
    axios.get<Record<string, any>>(`/api/tasks/${taskId}/variables`),
  
  getTasksByCaseInstanceId: (caseInstanceId: string, activeOnly: boolean = true) => 
    axios.get<PageResponse<FlowableTask>>(
      `/api/tasks/by-case/${caseInstanceId}`,
      { params: { activeOnly } }
    ),
};

// ✅ 保留只读API
export const claimApi = {
  getClaimCase: (id: string) => axios.get<ClaimCase>(`/api/cases/${id}`),
  getClaimCases: (params?: any) => axios.get<PageResponse<ClaimCase>>('/api/cases', { params }),
  getMyClaimCases: (userId: string, params?: any) => 
    axios.get<PageResponse<ClaimCase>>(`/api/cases/my-cases`, { params: { userId, ...params } }),
  createClaimCase: (data: ClaimCaseDTO) => axios.post<ClaimCase>('/api/cases', data),
  updateClaimCase: (id: string, data: ClaimCaseDTO) => axios.put<ClaimCase>(`/api/cases/${id}`, data),
};
```

**验收标准：**
- [ ] 直接状态变更API调用已移除
- [ ] 任务相关API已增强
- [ ] TypeScript类型定义正确

#### 3.2 重构理赔详情页 - 移除直接状态变更（1天）

**文件：** `frontend/src/components/ClaimDetail.tsx`

**改动内容：**

```typescript
// ❌ 移除这些状态和方法
// const [paymentModalVisible, setPaymentModalVisible] = useState(false);
// const [approveModalVisible, setApproveModalVisible] = useState(false);
// const [rejectModalVisible, setRejectModalVisible] = useState(false);

// const handlePay = async (values: any) => { ... };
// const handleApprove = async (values: any) => { ... };
// const handleReject = async (reason: string) => { ... };

// ✅ 添加任务相关状态
const [tasks, setTasks] = useState<FlowableTask[]>([]);
const [currentTask, setCurrentTask] = useState<FlowableTask | null>(null);
const [taskProcessingVisible, setTaskProcessingVisible] = useState(false);
const [taskVariables, setTaskVariables] = useState<Record<string, any>>({});

// ✅ 加载与当前理赔关联的任务
const loadRelatedTasks = useCallback(async () => {
  if (!claim?.caseInstanceId) return;
  
  try {
    const response = await taskApi.getTasksByCaseInstanceId(claim.caseInstanceId, true);
    setTasks(response.data.content);
    
    // 如果有待办任务，设置第一个为当前任务
    if (response.data.content.length > 0) {
      setCurrentTask(response.data.content[0]);
    }
  } catch (error) {
    console.error('Failed to load related tasks:', error);
  }
}, [claim?.caseInstanceId]);

useEffect(() => {
  loadRelatedTasks();
}, [loadRelatedTasks]);

// ❌ 移除这些按钮
// {claim.status === 'APPROVED' && (
//   <Button type="primary" icon={<DollarOutlined />}>支付</Button>
// )}

// {claim.status === 'UNDER_REVIEW' && (
//   <Space>
//     <Button type="primary" onClick={handleApprove}>批准</Button>
//     <Button danger onClick={handleReject}>拒绝</Button>
//   </Space>
// )}

// ✅ 只显示状态
<div>
  <Tag color={getStatusColor(claim.status)}>
    {getStatusText(claim.status)}
  </Tag>
</div>

// ✅ 添加待办任务卡片
{currentTask && (
  <Card 
    title="待办任务" 
    extra={
      <Tag color="processing">
        {tasks.length} 个任务待处理
      </Tag>
    }
    style={{ marginTop: 16, marginBottom: 16, border: '2px solid #1890ff' }}
  >
    <List
      dataSource={tasks}
      renderItem={(task) => (
        <List.Item
          key={task.id}
          style={{
            backgroundColor: task.id === currentTask?.id ? '#e6f7ff' : 'transparent',
            padding: 16,
            borderRadius: 8,
            marginBottom: 8,
            cursor: 'pointer'
          }}
          onClick={() => setCurrentTask(task)}
        >
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: '16px', fontWeight: 'bold', marginBottom: 8 }}>
              {task.name}
            </div>
            <div style={{ color: '#666', fontSize: '12px', marginBottom: 8 }}>
              任务ID: {task.id} | 创建时间: {new Date(task.createTime).toLocaleString()}
            </div>
            
            {/* 显示任务关键信息 */}
            {task.taskDefinitionKey === 'userTask_validatePayment' && (
              <Descriptions size="small" column={3}>
                <Descriptions.Item label="支付金额">
                  ¥{(task.processVariables?.amount || 0).toLocaleString()}
                </Descriptions.Item>
                <Descriptions.Item label="收款人">
                  {task.processVariables?.payeeName}
                </Descriptions.Item>
                <Descriptions.Item label="参考号">
                  {task.processVariables?.reference}
                </Descriptions.Item>
              </Descriptions>
            )}
            
            <div style={{ marginTop: 8 }}>
              <Button 
                type="primary" 
                onClick={(e) => {
                  e.stopPropagation();
                  handleProcessTask(task);
                }}
              >
                立即处理
              </Button>
            </div>
          </div>
        </List.Item>
      )}
    />
  </Card>
)}

// ❌ 移除这些模态框
// <Modal title="支付" visible={paymentModalVisible} ...> ... </Modal>
// <Modal title="批准理赔" visible={approveModalVisible} ...> ... </Modal>
// <Modal title="拒绝理赔" visible={rejectModalVisible} ...> ... </Modal>

// ✅ 添加统一的任务处理模态框
<Modal
  title={getTaskModalTitle()}
  visible={taskProcessingVisible}
  onCancel={() => {
    setTaskProcessingVisible(false);
    setTaskVariables({});
  }}
  onOk={() => form.submit()}
  width={800}
>
  {renderTaskProcessingForm()}
</Modal>
```

**验收标准：**
- [ ] 所有直接状态变更按钮已移除
- [ ] 直接状态变更API调用已移除
- [ ] 待办任务卡片正常显示
- [ ] 任务处理模态框正常工作

#### 3.3 添加任务处理功能（1天）

**文件：** `frontend/src/components/ClaimDetail.tsx`

**新增方法：**

```typescript
// 处理任务
const handleProcessTask = async (task: FlowableTask) => {
  setCurrentTask(task);
  
  // 加载任务变量
  try {
    const response = await taskApi.getTaskVariables(task.id);
    setTaskVariables(response.data);
    setTaskProcessingVisible(true);
  } catch (error) {
    console.error('Failed to load task variables:', error);
    message.error('加载任务信息失败');
  }
};

// 获取任务模态框标题
const getTaskModalTitle = () => {
  if (!currentTask) return '任务处理';
  
  switch (currentTask.taskDefinitionKey) {
    case 'userTask_validatePayment':
      return <><DollarOutlined /> 支付校验</>;
    case 'userTask_confirmPayment':
      return <><DollarOutlined /> 支付确认</>;
    case 'userTask_paymentRejected':
      return <><ExclamationCircleOutlined /> 支付被拒绝</>;
    case 'userTask_handleDispute':
      return <><WarningOutlined /> 处理支付争议</>;
    case 'humanTaskClaimReview':
      return <><AuditOutlined /> 理赔审核</>;
    case 'humanTaskFinalClaimApproval':
      return <><CheckCircleOutlined /> 最终审批</>;
    default:
      return <><FileTextOutlined /> {currentTask.name}</>;
  }
};

// 渲染任务处理表单
const renderTaskProcessingForm = () => {
  if (!currentTask) return null;
  
  switch (currentTask.taskDefinitionKey) {
    case 'userTask_validatePayment':
      return renderPaymentValidationForm();
    case 'userTask_confirmPayment':
      return renderPaymentConfirmationForm();
    case 'userTask_paymentRejected':
      return renderPaymentRejectedForm();
    case 'userTask_handleDispute':
      return renderDisputeHandlingForm();
    case 'humanTaskClaimReview':
      return renderClaimReviewForm();
    case 'humanTaskFinalClaimApproval':
      return renderFinalApprovalForm();
    default:
      return <div>未知任务类型</div>;
  }
};

// 支付校验表单
const renderPaymentValidationForm = () => (
  <div>
    <Card title="支付信息" size="small" style={{ marginBottom: 16 }}>
      <Descriptions bordered column={2}>
        <Descriptions.Item label="支付金额">
          ¥{(taskVariables.amount || 0).toLocaleString()}
        </Descriptions.Item>
        <Descriptions.Item label="收款人">
          {taskVariables.payeeName}
        </Descriptions.Item>
        <Descriptions.Item label="参考号">
          {taskVariables.reference}
        </Descriptions.Item>
        <Descriptions.Item label="批准金额">
          ¥{(claim?.approvedAmount || 0).toLocaleString()}
        </Descriptions.Item>
      </Descriptions>
    </Card>

    <Card title="关联理赔信息" size="small" style={{ marginBottom: 16 }}>
      <Descriptions bordered column={1}>
        <Descriptions.Item label="理赔号">{claim?.claimNumber}</Descriptions.Item>
        <Descriptions.Item label="理赔类型">{claim?.claimType}</Descriptions.Item>
        <Descriptions.Item label="事故描述">
          {claim?.incidentDescription}
        </Descriptions.Item>
        <Descriptions.Item label="事故地点">{claim?.incidentLocation}</Descriptions.Item>
      </Descriptions>
    </Card>

    <Form form={form} onFinish={handleSubmitTask} layout="vertical">
      <Form.Item 
        name="validationResult" 
        label="校验结果" 
        rules={[{ required: true, message: '请选择校验结果' }]}
      >
        <Radio.Group>
          <Radio value="approved">批准</Radio>
          <Radio value="rejected">拒绝</Radio>
        </Radio.Group>
      </Form.Item>
      <Form.Item name="validationComment" label="校验备注">
        <TextArea rows={4} placeholder="请输入校验备注（可选）" />
      </Form.Item>
    </Form>
  </div>
);

// 支付确认表单
const renderPaymentConfirmationForm = () => (
  <div>
    <Card title="交易信息" size="small" style={{ marginBottom: 16 }}>
      <Descriptions bordered column={2}>
        <Descriptions.Item label="交易ID">
          {taskVariables.transactionId}
        </Descriptions.Item>
        <Descriptions.Item label="交易时间">
          {new Date().toLocaleString()}
        </Descriptions.Item>
        <Descriptions.Item label="支付金额">
          ¥{(taskVariables.amount || 0).toLocaleString()}
        </Descriptions.Item>
        <Descriptions.Item label="收款人">
          {taskVariables.payeeName}
        </Descriptions.Item>
      </Descriptions>
    </Card>

    <Form form={form} onFinish={handleSubmitTask} layout="vertical">
      <Form.Item 
        name="confirmationResult" 
        label="确认结果" 
        rules={[{ required: true, message: '请选择确认结果' }]}
      >
        <Radio.Group>
          <Radio value="confirmed">确认</Radio>
          <Radio value="disputed">争议</Radio>
        </Radio.Group>
      </Form.Item>
      <Form.Item name="confirmationComment" label="确认备注">
        <TextArea rows={4} placeholder="请输入确认备注（可选）" />
      </Form.Item>
    </Form>
  </div>
);

// 最终审批表单
const renderFinalApprovalForm = () => (
  <div>
    <Card title="理赔信息" size="small" style={{ marginBottom: 16 }}>
      <Descriptions bordered column={2}>
        <Descriptions.Item label="理赔号">{claim?.claimNumber}</Descriptions.Item>
        <Descriptions.Item label="理赔类型">{claim?.claimType}</Descriptions.Item>
        <Descriptions.Item label="申请金额">
          ¥{(claim?.claimedAmount || 0).toLocaleString()}
        </Descriptions.Item>
        <Descriptions.Item label="申请人">{claim?.claimantName}</Descriptions.Item>
      </Descriptions>
    </Card>

    <Form form={form} onFinish={handleSubmitTask} layout="vertical">
      <Form.Item 
        name="approved" 
        label="审批结果" 
        rules={[{ required: true, message: '请选择审批结果' }]}
      >
        <Radio.Group>
          <Radio value={true}>批准</Radio>
          <Radio value={false}>拒绝</Radio>
        </Radio.Group>
      </Form.Item>
      <Form.Item 
        name="approvedAmount" 
        label="批准金额"
        dependencies={['approved']}
        rules={[
          ({ getFieldValue }) => ({
            validator(_, value) {
              if (getFieldValue('approved') === true && !value) {
                return Promise.reject(new Error('批准时必须填写批准金额'));
              }
              return Promise.resolve();
            },
          }),
        ]}
      >
        <InputNumber 
          style={{ width: '100%' }} 
          placeholder="请输入批准金额"
          formatter={(value) => `¥ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
          parser={(value) => value!.replace(/\¥\s?|(,*)/g, '')}
        />
      </Form.Item>
      <Form.Item name="reviewComments" label="审批意见">
        <TextArea rows={4} placeholder="请输入审批意见" />
      </Form.Item>
    </Form>
  </div>
);

// 提交任务
const handleSubmitTask = async (values: any) => {
  if (!currentTask) return;

  try {
    // 根据任务类型构建变量
    let variables: Record<string, any> = {};
    
    switch (currentTask.taskDefinitionKey) {
      case 'userTask_validatePayment':
        variables = {
          validationResult: values.validationResult,
          validationComment: values.validationComment
        };
        break;
      case 'userTask_confirmPayment':
        variables = {
          confirmationResult: values.confirmationResult,
          confirmationComment: values.confirmationComment
        };
        break;
      case 'humanTaskFinalClaimApproval':
        variables = {
          approved: values.approved,
          approvedAmount: values.approvedAmount,
          reviewComments: values.reviewComments
        };
        break;
      default:
        variables = values;
    }

    await taskApi.completeTask(currentTask.id, variables);
    message.success('任务处理成功');
    setTaskProcessingVisible(false);
    setTaskVariables({});
    
    // 重新加载任务和理赔信息
    loadRelatedTasks();
    loadClaimDetail();
  } catch (error) {
    console.error('Failed to complete task:', error);
    message.error('任务处理失败');
  }
};
```

**验收标准：**
- [ ] 所有任务类型都有对应的表单
- [ ] 任务变量正确加载
- [ ] 任务提交成功
- [ ] 状态正确更新

#### 3.4 集成BPMN流程可视化（0.5天）

**文件：** `frontend/src/components/ClaimDetail.tsx`

**改动内容：**

```typescript
import { BpmnSubprocessVisualizer } from './admin/BpmnSubprocessVisualizer';

const [paymentProcessData, setPaymentProcessData] = useState<BpmnSubprocessVisualizationDTO | null>(null);

// 加载支付流程状态
const loadPaymentProcessState = async () => {
  if (!claim?.caseInstanceId) return;
  
  try {
    const response = await adminApi.getPaymentProcessState(claim.caseInstanceId);
    setPaymentProcessData(response.data);
  } catch (error) {
    console.error('Failed to load payment process state:', error);
  }
};

// 在Tabs中添加支付进度标签页
<Tabs defaultActiveKey="basic">
  <TabPane tab="基本信息" key="basic">
    {/* 基本信息 */}
  </TabPane>
  
  <TabPane tab="支付进度" key="payment">
    {paymentProcessData ? (
      <BpmnSubprocessVisualizer data={paymentProcessData} />
    ) : (
      <Card>
        <Empty description="支付流程尚未启动" />
      </Card>
    )}
  </TabPane>
  
  <TabPane tab="相关文档" key="documents">
    {/* 相关文档 */}
  </TabPane>
  
  <TabPane tab="处理历史" key="history">
    {/* 处理历史 */}
  </TabPane>
</Tabs>
```

**验收标准：**
- [ ] BPMN流程可视化正常显示
- [ ] 支付进度正确显示
- [ ] 当前活动节点高亮

---

### 阶段4：移除任务中心页面（1天）

#### 4.1 从路由中移除任务列表页（0.5天）

**文件：** `frontend/src/App.tsx`

**改动内容：**

```typescript
// ❌ 移除任务列表路由
// <Route path="/tasks" element={<ProtectedRoute><TaskList /></ProtectedRoute>} />

// ✅ 保留理赔列表路由
<Route path="/claims" element={<ProtectedRoute><ClaimList /></ProtectedRoute>} />
<Route path="/claims/:id" element={<ProtectedRoute><ClaimDetail /></ProtectedRoute>} />
```

**验收标准：**
- [ ] 任务列表路由已移除
- [ ] 导航菜单中"任务中心"链接已移除

#### 4.2 更新导航菜单（0.5天）

**文件：** `frontend/src/components/Dashboard.tsx`

**改动内容：**

```typescript
// ❌ 移除任务中心菜单项
// <Menu.Item key="tasks" icon={<CheckSquareOutlined />}>
//   <Link to="/tasks">任务中心</Link>
// </Menu.Item>

// ✅ 在仪表板中添加"我的待办任务"卡片
<Card title="我的待办任务" extra={<Link to="/claims">查看全部</Link>}>
  {/* 显示前5个待办任务 */}
</Card>
```

**验收标准：**
- [ ] 导航菜单已更新
- [ ] 仪表板显示待办任务摘要

---

### 阶段5：测试与文档（2-3天）

#### 5.1 后端集成测试（1天）

**测试场景：**

1. **CMMN流程监听器测试**
   - [ ] 创建新理赔，验证状态自动变为SUBMITTED
   - [ ] 完成审核任务，验证状态变为UNDER_REVIEW
   - [ ] 批准理赔，验证状态变为APPROVED
   - [ ] 拒绝理赔，验证状态变为REJECTED

2. **BPMN支付流程测试**
   - [ ] 批准后自动启动支付流程
   - [ ] 完成支付校验任务，验证paymentStatus更新
   - [ ] 执行支付，验证transactionId生成
   - [ ] 完成支付确认，验证status变为PAID
   - [ ] 支付被拒绝，验证status变为REJECTED

3. **监听器集成测试**
   - [ ] 验证CmmnStageListener正确触发
   - [ ] 验证PaymentUpdateService正确更新
   - [ ] 验证PaymentCompletionListener正确触发
   - [ ] 验证状态同步一致性

4. **废弃API测试**
   - [ ] 验证直接更新状态API抛出异常
   - [ ] 验证直接批准API抛出异常
   - [ ] 验证直接拒绝API抛出异常
   - [ ] 验证直接支付API抛出异常

**验收标准：**
- [ ] 所有测试用例通过
- [ ] 测试覆盖率达到80%以上
- [ ] 性能测试通过

#### 5.2 前端集成测试（1天）

**测试场景：**

1. **理赔详情页测试**
   - [ ] 打开理赔详情，正确显示基本信息
   - [ ] 显示待办任务卡片
   - [ ] 点击"立即处理"正确打开模态框
   - [ ] 任务表单正确显示任务变量
   - [ ] 提交任务成功后状态正确更新
   - [ ] 支付进度可视化正确显示

2. **任务处理测试**
   - [ ] 完成审核任务，状态变为APPROVED
   - [ ] 完成支付校验任务，出现支付确认任务
   - [ ] 完成支付确认任务，状态变为PAID
   - [ ] 拒绝支付任务，状态变为REJECTED

3. **UI/UX测试**
   - [ ] 页面布局正确
   - [ ] 按钮位置合理
   - [ ] 模态框大小合适
   - [ ] 加载状态正常
   - [ ] 错误提示清晰

**验收标准：**
- [ ] 所有测试用例通过
- [ ] UI/UX符合设计要求
- [ ] 用户体验流畅

#### 5.3 端到端测试（0.5天）

**测试场景：**

1. **完整的理赔流程**
   - [ ] 创建理赔 → 审核 → 批准 → 支付校验 → 执行支付 → 支付确认 → 完成
   - [ ] 创建理赔 → 审核 → 批准 → 支付校验 → 支付被拒绝 → 完成
   - [ ] 创建理赔 → 审核 → 拒绝 → 完成

2. **多用户测试**
   - [ ] 理赔员创建理赔
   - [ ] 审核员审核理赔
   - [ ] 审批员审批理赔
   - [ ] 支付员处理支付

**验收标准：**
- [ ] 端到端流程正常
- [ ] 多用户协作正常
- [ ] 状态同步正确

#### 5.4 文档更新（0.5天）

**文档清单：**

1. **用户手册**
   - [ ] 更新理赔处理流程说明
   - [ ] 添加任务处理指南
   - [ ] 移除已废弃功能说明

2. **开发者文档**
   - [ ] 更新架构设计文档
   - [ ] 添加监听器实现说明
   - [ ] 添加状态管理说明

3. **API文档**
   - [ ] 标记废弃API
   - [ ] 添加新API文档
   - [ ] 更新示例代码

**验收标准：**
- [ ] 所有文档更新完成
- [ ] 文档清晰易懂
- [ ] 示例代码可用

---

## 四、风险评估与缓解措施

### 4.1 技术风险

| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|---------|
| 监听器执行失败 | 高 | 中 | 添加错误处理和日志，使用事务保证一致性 |
| 状态不同步 | 高 | 中 | 编写测试验证，添加监控告警 |
| 性能下降 | 中 | 低 | 优化查询，添加缓存 |
| 前端状态更新延迟 | 低 | 中 | 添加实时更新机制 |

### 4.2 业务风险

| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|---------|
| 用户操作习惯改变 | 高 | 高 | 提供培训，编写用户手册 |
| 现有数据兼容性 | 中 | 中 | 标记历史数据，提供迁移脚本 |
| 功能缺失 | 中 | 低 | 充分测试，收集用户反馈 |

### 4.3 项目风险

| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|---------|
| 进度延期 | 中 | 中 | 合理估算，预留缓冲时间 |
| 资源不足 | 中 | 低 | 提前协调资源 |
| 需求变更 | 高 | 低 | 严格控制需求变更 |

---

## 五、回滚计划

### 5.1 回滚触发条件

出现以下情况时考虑回滚：
1. 监听器频繁失败，导致状态不一致
2. 前端出现严重bug，无法正常使用
3. 性能严重下降，影响用户体验
4. 用户强烈反对，无法接受新流程

### 5.2 回滚步骤

**步骤1：代码回滚**
```bash
git revert <commit-hash>
```

**步骤2：数据库恢复**
```sql
-- 从备份恢复数据库
```

**步骤3：重新部署**
```bash
# 部署旧版本
```

**步骤4：验证**
- [ ] 功能正常
- [ ] 状态一致
- [ ] 性能正常

### 5.3 部分回滚方案

如果只是某个功能有问题，可以部分回滚：
1. 保留监听器，禁用前端改动
2. 保留API改动，禁用监听器
3. 保留监听器，修改业务逻辑

---

## 六、上线计划

### 6.1 预上线准备

- [ ] 所有测试通过
- [ ] 性能测试通过
- [ ] 文档完成
- [ ] 用户培训完成
- [ ] 监控告警配置完成

### 6.2 上线步骤

1. **灰度发布**（1周）
   - 10% 用户使用新版本
   - 收集反馈，修复问题

2. **逐步扩大**（1周）
   - 50% 用户使用新版本
   - 监控性能和错误

3. **全量上线**（1周）
   - 100% 用户使用新版本
   - 监控一周，确认稳定

4. **移除旧API**（1个月后）
   - 移除所有废弃API
   - 清理旧代码

### 6.3 上线后监控

**监控指标：**
- 监听器执行成功率
- 状态同步一致性
- API响应时间
- 用户操作成功率
- 错误日志数量

**告警阈值：**
- 监听器执行失败率 > 1%
- 状态不一致 > 0.1%
- API响应时间 > 2秒

---

## 七、时间估算

| 阶段 | 任务 | 预估时间 |
|------|------|---------|
| 阶段0 | 准备工作 | 1天 |
| 阶段1 | 后端改进 - 监听器实现 | 3-4天 |
| 阶段2 | 后端API改进 | 2天 |
| 阶段3 | 前端重构 | 2-3天 |
| 阶段4 | 移除任务中心 | 1天 |
| 阶段5 | 测试与文档 | 2-3天 |
| **总计** | | **11-14天** |

---

## 八、总结

### 8.1 核心成果

1. ✅ **完全基于流程驱动的状态管理**
   - 所有状态变更由流程引擎控制
   - 监听器自动同步状态
   - 无手动干预

2. ✅ **任务与理赔详情合并**
   - 用户在同一页面完成所有操作
   - 提供完整的上下文信息
   - 减少页面跳转

### 8.2 关键优势

1. **数据一致性**
   - 流程状态和数据库状态始终一致
   - 无状态漂移

2. **可追溯性**
   - 所有状态变更都有记录
   - 完整的审计追踪

3. **易维护性**
   - 状态逻辑集中在流程模型
   - 代码更简洁

4. **用户体验**
   - 操作流程更流畅
   - 信息更完整

### 8.3 后续优化方向

1. **实时更新**
   - 使用WebSocket实时推送状态变更
   - 用户无需刷新页面即可看到最新状态

2. **可视化增强**
   - 优化BPMN流程可视化
   - 添加更多交互功能

3. **智能推荐**
   - 根据任务类型推荐操作
   - 减少用户输入

---

## 附录：检查清单

### 后端检查清单

- [ ] CmmnStageListener实现并测试
- [ ] PaymentUpdateService更新并测试
- [ ] PaymentCompletionListener更新并测试
- [ ] CMMN模型监听器注册
- [ ] 废弃API标记
- [ ] 新API实现
- [ ] 单元测试编写
- [ ] 集成测试编写
- [ ] 性能测试通过

### 前端检查清单

- [ ] API服务层更新
- [ ] 直接状态变更移除
- [ ] 待办任务卡片添加
- [ ] 任务处理功能实现
- [ ] 任务表单实现
- [ ] BPMN流程可视化集成
- [ ] 任务列表页移除
- [ ] 导航菜单更新
- [ ] 单元测试编写
- [ ] 集成测试编写

### 文档检查清单

- [ ] 用户手册更新
- [ ] 开发者文档更新
- [ ] API文档更新
- [ ] 上线文档编写
- [ ] 回滚文档编写

### 测试检查清单

- [ ] 监听器测试
- [ ] 流程状态同步测试
- [ ] 前端功能测试
- [ ] UI/UX测试
- [ ] 端到端测试
- [ ] 性能测试
- [ ] 多用户测试

---

**文档版本：** v1.0  
**创建日期：** 2024-12-28  
**最后更新：** 2024-12-28  
**负责人：** 开发团队
