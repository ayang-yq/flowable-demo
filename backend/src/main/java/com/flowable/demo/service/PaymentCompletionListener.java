package com.flowable.demo.service;

import com.flowable.demo.domain.model.ClaimCase;
import com.flowable.demo.domain.model.ClaimCase.ClaimStatus;
import com.flowable.demo.domain.repository.ClaimCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

/**
 * 支付完成监听器
 * 在支付流程结束时执行
 */
@Component("paymentCompletionListener")
@Slf4j
@RequiredArgsConstructor
public class PaymentCompletionListener implements org.flowable.engine.delegate.ExecutionListener {

    private final ClaimCaseRepository claimCaseRepository;
    private final CmmnRuntimeService cmmnRuntimeService;

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
}
