package com.flowable.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

/**
 * 支付失败监听器
 * 在支付流程失败时执行
 */
@Component("paymentFailureListener")
@Slf4j
public class PaymentFailureListener implements org.flowable.engine.delegate.ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) {
        log.info("Payment failure listener triggered for process instance: {}", execution.getProcessInstanceId());
        
        String caseInstanceId = (String) execution.getVariable("caseInstanceId");
        String paymentStatus = (String) execution.getVariable("paymentStatus");
        
        log.info("Payment failed - Case Instance ID: {}, Payment Status: {}", 
                caseInstanceId, paymentStatus);
        
        // 这里可以添加支付失败后的处理逻辑
        // 例如更新CMMN案例状态、发送通知等
    }
}
