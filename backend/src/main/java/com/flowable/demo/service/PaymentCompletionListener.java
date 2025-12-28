package com.flowable.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

/**
 * 支付完成监听器
 * 在支付流程结束时执行
 */
@Component("paymentCompletionListener")
@Slf4j
public class PaymentCompletionListener implements org.flowable.engine.delegate.ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) {
        log.info("Payment completion listener triggered for process instance: {}", execution.getProcessInstanceId());
        
        String caseInstanceId = (String) execution.getVariable("caseInstanceId");
        String paymentStatus = (String) execution.getVariable("paymentStatus");
        String transactionId = (String) execution.getVariable("transactionId");
        
        log.info("Payment completed - Case Instance ID: {}, Payment Status: {}, Transaction ID: {}", 
                caseInstanceId, paymentStatus, transactionId);
        
        // 这里可以添加支付完成后的处理逻辑
        // 例如更新CMMN案例状态、发送通知等
    }
}
