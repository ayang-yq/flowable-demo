package com.flowable.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Service;

/**
 * 通知服务
 * 用于BPMN流程中发送通知
 */
@Service("notificationService")
@Slf4j
public class NotificationService implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        log.info("Executing notification service for process instance: {}", execution.getProcessInstanceId());
        
        String notificationType = (String) execution.getVariable("notificationType");
        String claimId = (String) execution.getVariable("claimId");
        String paymentStatus = (String) execution.getVariable("paymentStatus");
        
        log.info("Sending notification - Type: {}, Claim ID: {}, Payment Status: {}", 
                notificationType, claimId, paymentStatus);
        
        // 模拟发送通知
        if (notificationType == null) {
            log.warn("Notification type is null, skipping notification");
            return;
        }
        
        switch (notificationType) {
            case "payment_completed":
                sendPaymentCompletedNotification(claimId, paymentStatus);
                break;
            case "payment_failed":
                sendPaymentFailedNotification(claimId);
                break;
            default:
                log.warn("Unknown notification type: {}", notificationType);
        }
    }
    
    private void sendPaymentCompletedNotification(String claimId, String paymentStatus) {
        log.info("Payment completed notification sent for claim: {}. Status: {}", claimId, paymentStatus);
        // 这里可以集成实际的邮件/短信/推送通知服务
    }
    
    private void sendPaymentFailedNotification(String claimId) {
        log.info("Payment failed notification sent for claim: {}", claimId);
        // 这里可以集成实际的邮件/短信/推送通知服务
    }
}
