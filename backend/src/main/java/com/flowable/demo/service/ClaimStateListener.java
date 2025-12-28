package com.flowable.demo.service;

import com.flowable.demo.domain.model.ClaimCase.ClaimStatus;
import com.flowable.demo.domain.model.ClaimCase.PaymentStatus;
import com.flowable.demo.domain.repository.ClaimCaseRepository;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.common.engine.api.delegate.Expression;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 案件状态监听器
 * 在CMMN流程的不同阶段更新案件状态
 */
@Component("claimStateListener")
@Slf4j
public class ClaimStateListener implements org.flowable.cmmn.api.listener.PlanItemInstanceLifecycleListener  {

    private final ClaimCaseRepository claimCaseRepository;
    private final CmmnRuntimeService cmmnRuntimeService;

    // 通过field注入设置的claim状态
    @Setter
    private Expression status;

    // 构造函数注入ClaimCaseRepository和CmmnRuntimeService
    public ClaimStateListener(ClaimCaseRepository claimCaseRepository, CmmnRuntimeService cmmnRuntimeService) {
        this.claimCaseRepository = claimCaseRepository;
        this.cmmnRuntimeService = cmmnRuntimeService;
    }

    @Override
    public void stateChanged(DelegatePlanItemInstance planItemInstance, String oldState, String newState) {
        log.info("ClaimStateListener triggered - PlanItem: {}, OldState: {}, NewState: {}, CaseInstanceId: {}",
                planItemInstance.getName(), oldState, newState, planItemInstance.getCaseInstanceId());

        String statusValue = null;
        if (status == null) {
            log.debug("No status field provided for plan item: {}, skipping status update", planItemInstance.getName());
            return;
        }

        if (status != null) {
            Object value = status.getValue(planItemInstance);
            if (value != null) {
                statusValue = value.toString();
            }
        }

        String caseInstanceId = planItemInstance.getCaseInstanceId();
        
        if (caseInstanceId == null) {
            log.warn("Case instance ID is null, skipping status update");
            return;
        }

        try {
            ClaimStatus newStatus = ClaimStatus.valueOf(statusValue);
            updateClaimStatus(planItemInstance, newStatus);
        } catch (IllegalArgumentException e) {
            log.error("Invalid status value: {} for case instance: {}", status, caseInstanceId, e);
        }
    }

    /**
     * 更新案件状态
     * 首先尝试通过 caseInstanceId 查找，如果找不到则尝试通过 claimCaseId 变量查找
     */
    private void updateClaimStatus(DelegatePlanItemInstance planItemInstance, ClaimStatus newStatus) {
        String caseInstanceId = planItemInstance.getCaseInstanceId();
        String source = planItemInstance.getName();
        
        // 首先尝试通过 caseInstanceId 查找
        Optional<com.flowable.demo.domain.model.ClaimCase> claimCaseOpt = claimCaseRepository.findByCaseInstanceId(caseInstanceId);
        
        if (claimCaseOpt.isPresent()) {
            updateCaseStatus(claimCaseOpt.get(), newStatus, source, "caseInstanceId");
        } else {
            // 如果通过 caseInstanceId 找不到，尝试通过 claimCaseId 变量查找
            log.info("No claim case found with case instance ID: {}, attempting to find by claimCaseId variable", caseInstanceId);
            
            Object claimCaseIdObj = planItemInstance.getVariable("claimCaseId");
            if (claimCaseIdObj != null) {
                String claimCaseId = claimCaseIdObj.toString();
                try {
                    java.util.UUID uuid = java.util.UUID.fromString(claimCaseId);
                    claimCaseRepository.findById(uuid).ifPresentOrElse(
                        claimCase -> {
                            updateCaseStatus(claimCase, newStatus, source, "claimCaseId");
                        },
                        () -> log.warn("No claim case found with claimCaseId (UUID): {}", claimCaseId)
                    );
                } catch (IllegalArgumentException e) {
                    log.error("Invalid claimCaseId format: {}", claimCaseId, e);
                }
            } else {
                log.warn("claimCaseId variable not found in plan item instance variables");
            }
        }
    }
    
    /**
     * 执行案件状态更新
     */
    private void updateCaseStatus(com.flowable.demo.domain.model.ClaimCase claimCase, ClaimStatus newStatus, 
                                  String source, String matchMethod) {
        ClaimStatus oldStatus = claimCase.getStatus();
        
        if (oldStatus == newStatus) {
            log.info("Claim case {} already has status: {}, no update needed (found via {})", 
                    claimCase.getId(), newStatus, matchMethod);
            return;
        }
        
        log.info("Updating claim case {} status from {} to {} (source: {}, found via {})", 
                claimCase.getId(), oldStatus, newStatus, source, matchMethod);
        
        claimCase.setStatus(newStatus);
        
        // 当状态转换为PAYMENT_PROCESSING时，初始化支付状态为PROCESSING
        if (newStatus == ClaimStatus.PAYMENT_PROCESSING) {
            log.info("Initializing payment status to PROCESSING for case {}", claimCase.getId());
            claimCase.setPaymentStatus(PaymentStatus.PROCESSING);
        }
        
        // 当状态更新为CLOSED时，终止 Case 实例
        if (newStatus == ClaimStatus.CLOSED) {
            log.info("Status changed to CLOSED, terminating case instance: {}", claimCase.getCaseInstanceId());
            terminateCaseInstance(claimCase.getCaseInstanceId());
        }
        
        claimCaseRepository.save(claimCase);
        
        log.info("Claim case {} status updated successfully to: {}, payment status: {} (found via {})", 
                claimCase.getId(), claimCase.getStatus(), claimCase.getPaymentStatus(), matchMethod);
    }
    
    /**
     * 终止 Case 实例
     */
    private void terminateCaseInstance(String caseInstanceId) {
        if (caseInstanceId == null || caseInstanceId.isEmpty()) {
            log.warn("Case instance ID is null or empty, cannot terminate case");
            return;
        }
        
        try {
            log.info("Terminating case instance: {}", caseInstanceId);
            cmmnRuntimeService.terminateCaseInstance(caseInstanceId);
            log.info("Case instance {} terminated successfully", caseInstanceId);
        } catch (Exception e) {
            log.error("Error terminating case instance: {}", caseInstanceId, e);
        }
    }

    @Override
    public String getSourceState() {
        return null; // 接受任何起始状态（由 XML sourceState 属性控制）
    }

    @Override
    public String getTargetState() {
        return null; // 接受任何目标状态（由 XML targetState 属性控制）
    }
}
