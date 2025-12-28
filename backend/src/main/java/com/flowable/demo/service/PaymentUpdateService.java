package com.flowable.demo.service;

import com.flowable.demo.domain.model.ClaimCase;
import com.flowable.demo.domain.model.ClaimCase.ClaimStatus;
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
            if (claimCase.getApprovedAmount() != null) {
                claimCase.setPaidAmount(claimCase.getApprovedAmount());
            }
            claimCase.setPaymentDate(java.time.LocalDate.now());
        } else if ("REJECTED".equals(paymentStatus) || "PAYMENT_REJECTED".equals(paymentStatus)) {
            // 支付被拒绝
            log.info("Payment rejected, setting status to REJECTED");
            claimCase.setStatus(ClaimStatus.REJECTED);
        }
    }
}
