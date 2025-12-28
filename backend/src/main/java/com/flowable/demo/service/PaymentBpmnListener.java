package com.flowable.demo.service;

import com.flowable.demo.domain.model.ClaimCase;
import com.flowable.demo.domain.model.ClaimCase.ClaimStatus;
import com.flowable.demo.domain.model.ClaimCase.PaymentStatus;
import com.flowable.demo.domain.repository.ClaimCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

/**
 * 支付流程监听器（统一处理BPMN支付流程事件）
 * 合并了PaymentCompletionListener和PaymentFailureListener的功能
 * 根据支付流程的不同阶段和结果更新CASE状态和支付状态
 */
@Component("paymentBpmnListener")
@Slf4j
@RequiredArgsConstructor
public class PaymentBpmnListener implements org.flowable.engine.delegate.ExecutionListener {

    private final ClaimCaseRepository claimCaseRepository;

    @Override
    public void notify(DelegateExecution execution) {
        String eventName = execution.getEventName();
        String activityId = execution.getCurrentActivityId();
        String processInstanceId = execution.getProcessInstanceId();
        String caseInstanceId = (String) execution.getVariable("caseInstanceId");
        
        log.info("PaymentBpmnListener triggered - Event: {}, Activity: {}, ProcessInstance: {}, CaseInstance: {}",
                eventName, activityId, processInstanceId, caseInstanceId);
        
        if (caseInstanceId == null) {
            log.warn("Case instance ID is null, cannot update claim case");
            return;
        }
        
        claimCaseRepository.findByCaseInstanceId(caseInstanceId).ifPresentOrElse(
            claimCase -> {
                handleEvent(claimCase, eventName, activityId, execution);
            },
            () -> {
                log.warn("Claim case not found for case instance ID: {}", caseInstanceId);
            }
        );
    }

    /**
     * 处理不同的事件类型和活动节点
     */
    private void handleEvent(ClaimCase claimCase, String eventName, String activityId, DelegateExecution execution) {
        if ("start".equals(eventName)) {
            handleStartEvent(claimCase, activityId, execution);
        } else if ("end".equals(eventName)) {
            handleEndEvent(claimCase, activityId, execution);
        } else {
            log.debug("Unhandled event type: {} for activity: {}", eventName, activityId);
        }
        
        claimCaseRepository.save(claimCase);
        log.info("Claim case {} saved - Payment Status: {}, Claim Status: {}", 
                claimCase.getId(), claimCase.getPaymentStatus(), claimCase.getStatus());
    }

    /**
     * 处理节点的开始事件
     */
    private void handleStartEvent(ClaimCase claimCase, String activityId, DelegateExecution execution) {
        log.info("Handling start event for activity: {}", activityId);
        
        switch (activityId) {
            case "startEvent_paymentStart":
                // 支付流程开始，确保状态为处理中
                log.info("Payment process started, ensuring paymentStatus is PROCESSING");
                if (claimCase.getPaymentStatus() == null || 
                    claimCase.getPaymentStatus() == PaymentStatus.NOT_STARTED) {
                    claimCase.setStatus(ClaimStatus.PAYMENT_PROCESSING);
                    claimCase.setPaymentStatus(PaymentStatus.PROCESSING);
                }
                break;
                
            case "userTask_validatePayment":
                // 支付校验任务开始
                log.info("Payment validation task started, paymentStatus remains PROCESSING");
                claimCase.setPaymentStatus(PaymentStatus.PROCESSING);
                break;
                
            case "serviceTask_executePayment":
                // 执行支付任务开始
                log.info("Payment execution task started, paymentStatus remains PROCESSING");
                claimCase.setPaymentStatus(PaymentStatus.PROCESSING);
                break;
                
            case "userTask_confirmPayment":
                // 支付确认任务开始
                log.info("Payment confirmation task started, paymentStatus remains PROCESSING");
                claimCase.setPaymentStatus(PaymentStatus.PROCESSING);
                break;
                
            case "userTask_paymentRejected":
                // 支付被拒绝任务开始
                log.info("Payment rejection task started, setting paymentStatus to PAYMENT_REJECTED");
                claimCase.setPaymentStatus(PaymentStatus.PAYMENT_REJECTED);
                break;
                
            case "userTask_handleDispute":
                // 处理争议任务开始
                log.info("Dispute handling task started, setting paymentStatus to DISPUTED");
                claimCase.setPaymentStatus(PaymentStatus.DISPUTED);
                break;
                
            default:
                log.debug("Unhandled start event activity: {}", activityId);
                break;
        }
    }

    /**
     * 处理流程的结束事件
     */
    private void handleEndEvent(ClaimCase claimCase, String activityId, DelegateExecution execution) {
        String paymentStatus = (String) execution.getVariable("paymentStatus");
        String transactionId = (String) execution.getVariable("transactionId");
        String caseInstanceId = claimCase.getCaseInstanceId();
        
        log.info("Handling end event for activity: {}, paymentStatus: {}", activityId, paymentStatus);
        
        if ("endEvent_paymentSuccess".equals(activityId)) {
            // 支付成功结束
            log.info("Payment process ended successfully for case {}, updating status to PAID", caseInstanceId);
            
            claimCase.setPaymentStatus(PaymentStatus.PAID);
            claimCase.setStatus(ClaimStatus.PAID);
            
            if (transactionId != null) {
                claimCase.setTransactionId(transactionId);
            }
            claimCase.setPaymentDate(java.time.LocalDate.now());
            
            // 从流程变量中获取支付金额
            Object paidAmount = execution.getVariable("amount");
            if (paidAmount != null) {
                if (paidAmount instanceof Number) {
                    claimCase.setPaidAmount(java.math.BigDecimal.valueOf(((Number) paidAmount).doubleValue()));
                } else if (paidAmount instanceof String) {
                    try {
                        claimCase.setPaidAmount(new java.math.BigDecimal((String) paidAmount));
                    } catch (NumberFormatException e) {
                        log.warn("Failed to parse paid amount: {}", paidAmount);
                    }
                }
            }
            
            log.info("Claim case {} updated - Payment Status: PAID, Claim Status: PAID, Transaction ID: {}", 
                    claimCase.getId(), transactionId);
                    
        } else if ("endEvent_paymentFailed".equals(activityId)) {
            // 支付失败结束
            log.info("Payment process ended with failure for case {}, paymentStatus: {}", caseInstanceId, paymentStatus);
            
            if ("rejected".equals(paymentStatus)) {
                // 支付被拒绝
                claimCase.setPaymentStatus(PaymentStatus.PAYMENT_REJECTED);
                claimCase.setStatus(ClaimStatus.REJECTED);
                
                // 获取拒绝原因
                String rejectionReason = (String) execution.getVariable("rejectionReason");
                if (rejectionReason != null) {
                    claimCase.addHistory("PAYMENT_REJECTED", 
                        String.format("Payment rejected. Reason: %s", rejectionReason), 
                        claimCase.getAssignedTo());
                }
                
                log.info("Claim case {} updated - Payment Status: PAYMENT_REJECTED, Claim Status: REJECTED", 
                        claimCase.getId());
                        
            } else if ("disputed".equals(paymentStatus)) {
                // 支付争议中
                claimCase.setPaymentStatus(PaymentStatus.DISPUTED);
                claimCase.setStatus(ClaimStatus.PAYMENT_PROCESSING);
                
                log.info("Claim case {} updated - Payment Status: DISPUTED, Claim Status: PAYMENT_PROCESSING", 
                        claimCase.getId());
                        
            } else {
                // 其他失败情况（如调查中、取消等）
                claimCase.setPaymentStatus(PaymentStatus.PAYMENT_FAILED);
                claimCase.setStatus(ClaimStatus.PAYMENT_PROCESSING);
                
                log.info("Claim case {} updated - Payment Status: PAYMENT_FAILED, Claim Status: PAYMENT_PROCESSING", 
                        claimCase.getId());
            }
        }
    }
}
