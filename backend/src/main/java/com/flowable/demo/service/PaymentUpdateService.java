package com.flowable.demo.service;

import com.flowable.demo.domain.model.ClaimCase;
import com.flowable.demo.domain.repository.ClaimCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Service;

/**
 * 支付更新服务
 * 用于BPMN流程中更新Case支付状态
 */
@Service("paymentUpdateService")
@Slf4j
@RequiredArgsConstructor
public class PaymentUpdateService implements JavaDelegate {

    private final ClaimCaseRepository claimCaseRepository;

    @Override
    public void execute(DelegateExecution execution) {
        log.info("Executing payment update service for process instance: {}", execution.getProcessInstanceId());
        
        String caseInstanceId = (String) execution.getVariable("caseInstanceId");
        String paymentStatus = (String) execution.getVariable("paymentStatus");
        String transactionId = (String) execution.getVariable("transactionId");
        
        log.info("Payment update - Case Instance ID: {}, Payment Status: {}, Transaction ID: {}", 
                caseInstanceId, paymentStatus, transactionId);
        
        if (caseInstanceId != null) {
            claimCaseRepository.findByCaseInstanceId(caseInstanceId).ifPresentOrElse(
                claimCase -> {
                    log.info("Found claim case: {} for case instance: {}", claimCase.getId(), caseInstanceId);
                    // 更新支付状态
                    claimCase.setPaymentStatus(paymentStatus);
                    if (transactionId != null) {
                        claimCase.setTransactionId(transactionId);
                    }
                    claimCaseRepository.save(claimCase);
                    log.info("Claim case {} updated with payment status: {}", claimCase.getId(), paymentStatus);
                },
                () -> {
                    log.warn("No claim case found for case instance: {}", caseInstanceId);
                }
            );
        } else {
            log.warn("Case instance ID is null, cannot update payment status");
        }
    }
}
