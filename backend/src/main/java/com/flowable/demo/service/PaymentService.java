package com.flowable.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 支付服务
 * 用于BPMN流程中的支付执行
 */
@Service("paymentService")
@Slf4j
public class PaymentService implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        log.info("Executing payment service for process instance: {}", execution.getProcessInstanceId());
        
        // 获取支付参数（处理可能的BigDecimal类型）
        Object amountObj = execution.getVariable("amount");
        Long amount = null;
        if (amountObj instanceof Number) {
            amount = ((Number) amountObj).longValue();
        }
        String reference = (String) execution.getVariable("reference");
        String payeeName = (String) execution.getVariable("payeeName");
        
        log.info("Payment details - Amount: {}, Reference: {}, Payee: {}", amount, reference, payeeName);
        
        // 模拟支付执行
        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String paymentDate = java.time.LocalDate.now().toString();
        
        // 设置流程变量
        execution.setVariable("transactionId", transactionId);
        execution.setVariable("paymentDate", paymentDate);
        execution.setVariable("paymentStatus", "completed");
        
        log.info("Payment executed successfully. Transaction ID: {}, Date: {}", transactionId, paymentDate);
    }
    
    /**
     * 模拟支付执行（不用于流程，直接调用）
     */
    public Map<String, Object> executePayment(Long amount, String reference, String payeeName) {
        Map<String, Object> result = new HashMap<>();
        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String paymentDate = java.time.LocalDate.now().toString();
        
        result.put("transactionId", transactionId);
        result.put("paymentDate", paymentDate);
        result.put("paymentStatus", "completed");
        
        log.info("Payment executed. Transaction ID: {}, Amount: {}, Reference: {}", transactionId, amount, reference);
        
        return result;
    }
}
