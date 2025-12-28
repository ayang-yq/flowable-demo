package com.flowable.demo.service;

import com.flowable.demo.domain.model.ClaimCase;
import com.flowable.demo.domain.model.InsurancePolicy;
import com.flowable.demo.domain.model.User;
import com.flowable.demo.domain.repository.ClaimCaseRepository;
import com.flowable.demo.domain.repository.InsurancePolicyRepository;
import com.flowable.demo.domain.repository.UserRepository;
import com.flowable.demo.web.rest.dto.ApproveRequestDTO;
import com.flowable.demo.web.rest.dto.ClaimCaseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 理赔案件业务服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CaseService {

    private final ClaimCaseRepository claimCaseRepository;
    private final InsurancePolicyRepository insurancePolicyRepository;
    private final UserRepository userRepository;
    private final CmmnRuntimeService cmmnRuntimeService;
    private final CmmnTaskService cmmnTaskService;

    /**
     * 创建理赔案件
     */
    public ClaimCase createClaimCase(ClaimCaseDTO dto) {
        log.info("Creating claim case from DTO: {}", dto);

        // 获取保单信息
        InsurancePolicy policy = insurancePolicyRepository.findById(UUID.fromString(dto.getPolicyId()))
                .orElseThrow(() -> new IllegalArgumentException("Policy not found"));

        // 获取创建用户
        User createdBy = null;
        if (dto.getCreatedById() != null) {
            createdBy = userRepository.findById(UUID.fromString(dto.getCreatedById()))
                    .orElse(null);
        }

        // 1. 先创建并保存 ClaimCase（此时还没有 caseInstanceId，状态为 DRAFT）
        // 状态会在 CMMN 监听器触发后自动改为 SUBMITTED
        ClaimCase claimCase = ClaimCase.builder()
                .claimNumber(generateClaimNumber())
                .policy(policy)
                .claimantName(dto.getClaimantName())
                .claimantPhone(dto.getClaimantPhone())
                .claimantEmail(dto.getClaimantEmail())
                .incidentDate(LocalDate.parse(dto.getIncidentDate()))
                .incidentLocation(dto.getIncidentLocation())
                .incidentDescription(dto.getIncidentDescription())
                .claimedAmount(BigDecimal.valueOf(dto.getClaimedAmount()))
                .claimType(dto.getClaimType())
                .severity(dto.getSeverity() != null ? ClaimCase.Severity.valueOf(dto.getSeverity().toUpperCase())
                        : ClaimCase.Severity.LOW)
                .status(ClaimCase.ClaimStatus.DRAFT)
                .createdBy(createdBy)
                .build();

        // 保存到数据库
        claimCase = claimCaseRepository.save(claimCase);
        log.info("Claim case saved with ID: {}", claimCase.getId());

        // 2. 启动 Case 流程，传入 claimCaseId
        String caseInstanceId = startCaseProcessWithClaimCaseId(claimCase);
        
        if (caseInstanceId != null) {
            // 3. 更新 ClaimCase 的 caseInstanceId
            claimCase.setCaseInstanceId(caseInstanceId);
            claimCaseRepository.save(claimCase);
            log.info("Claim case {} updated with case instance ID: {}", claimCase.getId(), caseInstanceId);
        } else {
            log.warn("Failed to start case process for claim case {}", claimCase.getId());
        }

        return claimCase;
    }

    /**
     * 更新理赔案件
     */
    public ClaimCase updateClaimCase(ClaimCaseDTO dto) {
        log.debug("Updating claim case from DTO: {}", dto);

        ClaimCase claimCase = claimCaseRepository.findById(UUID.fromString(dto.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Claim case not found"));

        // 更新基本信息
        if (dto.getClaimantName() != null) {
            claimCase.setClaimantName(dto.getClaimantName());
        }
        if (dto.getClaimantPhone() != null) {
            claimCase.setClaimantPhone(dto.getClaimantPhone());
        }
        if (dto.getClaimantEmail() != null) {
            claimCase.setClaimantEmail(dto.getClaimantEmail());
        }
        if (dto.getIncidentDate() != null) {
            claimCase.setIncidentDate(LocalDate.parse(dto.getIncidentDate()));
        }
        if (dto.getIncidentLocation() != null) {
            claimCase.setIncidentLocation(dto.getIncidentLocation());
        }
        if (dto.getIncidentDescription() != null) {
            claimCase.setIncidentDescription(dto.getIncidentDescription());
        }
        if (dto.getClaimedAmount() != null) {
            claimCase.setClaimedAmount(BigDecimal.valueOf(dto.getClaimedAmount()));
        }
        if (dto.getClaimType() != null) {
            claimCase.setClaimType(dto.getClaimType());
        }
        if (dto.getSeverity() != null) {
            claimCase.setSeverity(ClaimCase.Severity.valueOf(dto.getSeverity().toUpperCase()));
        }

        return claimCaseRepository.save(claimCase);
    }

    /**
     * 分配理赔案件
     */
    public ClaimCase assignClaimCase(UUID caseId, UUID userId) {
        log.debug("Assigning claim case {} to user {}", caseId, userId);

        ClaimCase claimCase = claimCaseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Claim case not found"));

        User assignedTo = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        claimCase.assignTo(assignedTo);
        return claimCaseRepository.save(claimCase);
    }

    /**
     * 更新理赔案件状态
     */
    public ClaimCase updateClaimCaseStatus(UUID caseId, String status, String description, UUID userId) {
        log.debug("Updating claim case {} status to {} by user {}", caseId, status, userId);

        ClaimCase claimCase = claimCaseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Claim case not found"));

        User performedBy = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        claimCase.updateStatus(status, description, performedBy);
        return claimCaseRepository.save(claimCase);
    }

    /**
     * 删除理赔案件
     */
    public void deleteClaimCase(UUID caseId) {
        log.debug("Deleting claim case {}", caseId);

        ClaimCase claimCase = claimCaseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Claim case not found"));

        // 如果有运行的流程实例，先终止
        if (claimCase.getCaseInstanceId() != null) {
            try {
                cmmnRuntimeService.deleteCaseInstance(claimCase.getCaseInstanceId());
            } catch (Exception e) {
                log.warn("Failed to delete case instance: {}", e.getMessage());
            }
        }

        claimCaseRepository.delete(claimCase);
    }

    /**
     * 批准理赔案件
     */
    public ClaimCase approveClaimCase(UUID caseId, String userId, ApproveRequestDTO approveRequestDTO) {
        log.debug("Approving claim case {} by user {} with amount {}", caseId, userId, approveRequestDTO.getApprovedAmount());

        ClaimCase claimCase = claimCaseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Claim case not found"));

        User approvedBy = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 设置批准金额
        claimCase.setApprovedAmount(approveRequestDTO.getApprovedAmount());

        // 构建审批意见描述
        String description = "Claim approved by " + approvedBy.getFullName();
        if (approveRequestDTO.getComments() != null && !approveRequestDTO.getComments().isBlank()) {
            description += " - " + approveRequestDTO.getComments();
        }

        // 更新状态为已批准
        claimCase.updateStatus("APPROVED", description, approvedBy);
        claimCaseRepository.save(claimCase);

        // [关键] 完成CMMN任务以推动流程
        if (claimCase.getCaseInstanceId() != null) {
            try {
                // 设置审批结果变量
                Map<String, Object> variables = new HashMap<>();
                variables.put("approved", true);
                variables.put("approvedBy", approvedBy.getUsername());
                variables.put("approvedDate", LocalDateTime.now().toString());
                variables.put("approvedAmount", approveRequestDTO.getApprovedAmount());
                
                // [FIX] Set all required parameters for the ClaimPaymentProcess BPMN
                // These are required by the CMMN processTask which passes them to BPMN
                variables.put("paymentOfficer", approvedBy.getUsername());
                variables.put("paymentManager", "admin");
                
                // Payment information required by BPMN process
                variables.put("amount", approveRequestDTO.getApprovedAmount());
                variables.put("reference", "PAY-" + claimCase.getClaimNumber() + "-" + System.currentTimeMillis());
                variables.put("payeeName", claimCase.getClaimantName());
                
                // Case information for process tracking
                variables.put("claimId", claimCase.getId().toString());
                variables.put("caseInstanceId", claimCase.getCaseInstanceId());
                
                // 查找并完成Final Approval任务
                completeCmmnTask(claimCase.getCaseInstanceId(), "taskFinalApproval", variables);
                
                log.info("Approved claim case {} and completed CMMN taskFinalApproval", claimCase.getId());
            } catch (Exception e) {
                log.error("Failed to complete CMMN task for approval: {}", e.getMessage(), e);
            }
        }

        return claimCase;
    }

    /**
     * 拒绝理赔案件
     */
    public ClaimCase rejectClaimCase(UUID caseId, String reason) {
        log.debug("Rejecting claim case {} with reason: {}", caseId, reason);

        ClaimCase claimCase = claimCaseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Claim case not found"));

        // 更新状态为已拒绝
        claimCase.updateStatus("REJECTED", reason, claimCase.getCreatedBy());
        claimCaseRepository.save(claimCase);

        // [关键] 完成CMMN任务以推动流程
        if (claimCase.getCaseInstanceId() != null) {
            try {
                // 设置审批结果变量
                Map<String, Object> variables = new HashMap<>();
                variables.put("approved", false);
                variables.put("rejectReason", reason);
                
                // 查找并完成Final Approval任务
                completeCmmnTask(claimCase.getCaseInstanceId(), "taskFinalApproval", variables);
                
                log.info("Rejected claim case {} and completed CMMN taskFinalApproval", claimCase.getId());
            } catch (Exception e) {
                log.error("Failed to complete CMMN task for rejection: {}", e.getMessage(), e);
            }
        }

        return claimCase;
    }

    /**
     * 支付理赔案件
     */
    public ClaimCase payClaimCase(UUID caseId, java.math.BigDecimal paymentAmount, 
                                  java.time.LocalDate paymentDate, String paymentMethod,
                                  String paymentReference, String userId) {
        log.debug("Processing payment for claim case {} by user {}", caseId, userId);

        ClaimCase claimCase = claimCaseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Claim case not found"));

        User paidBy = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 检查是否可以支付（只有已批准状态才能支付）
        if (claimCase.getStatus() != ClaimCase.ClaimStatus.APPROVED) {
            throw new IllegalStateException("Claim case must be in APPROVED status to process payment. Current status: " 
                    + claimCase.getStatus());
        }

        // 更新状态为支付处理中
        claimCase.updateStatus("PAYMENT_PROCESSING", "Payment initiated by " + paidBy.getFullName(), paidBy);
        claimCaseRepository.save(claimCase);

        // [关键] 完成CMMN任务以推动流程
        if (claimCase.getCaseInstanceId() != null) {
            try {
                Map<String, Object> variables = new HashMap<>();
                variables.put("paymentAmount", paymentAmount);
                variables.put("paymentDate", paymentDate.toString());
                variables.put("paymentMethod", paymentMethod);
                variables.put("paymentReference", paymentReference);
                variables.put("paymentStatus", "COMPLETED");
                variables.put("paidBy", paidBy.getUsername());
                variables.put("paidDate", LocalDateTime.now().toString());
                
                // 查找并完成Process Payment任务
                completeCmmnTask(claimCase.getCaseInstanceId(), "taskProcessPayment", variables);
                
                log.info("Processed payment for claim case {} and completed CMMN taskProcessPayment", claimCase.getId());
            } catch (Exception e) {
                log.error("Failed to complete CMMN task for payment: {}", e.getMessage(), e);
            }
        }

        // 更新状态为已支付
        String paymentDescription = String.format("Payment processed: Amount=%s, Method=%s, Reference=%s",
                paymentAmount, paymentMethod, paymentReference);
        claimCase.updateStatus("PAID", paymentDescription, paidBy);
        
        return claimCaseRepository.save(claimCase);
    }

    /**
     * 获取理赔案件统计信息
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getClaimCaseStatistics() {
        log.debug("Getting claim case statistics");

        Map<String, Object> statistics = new HashMap<>();

        // 总理赔数
        long totalClaims = claimCaseRepository.count();
        statistics.put("totalClaims", totalClaims);

        // 待处理理赔（包括SUBMITTED和UNDER_REVIEW状态）
        long pendingClaims = claimCaseRepository.countByStatus(ClaimCase.ClaimStatus.SUBMITTED) +
                claimCaseRepository.countByStatus(ClaimCase.ClaimStatus.UNDER_REVIEW);
        statistics.put("pendingClaims", pendingClaims);

        // 已批准理赔
        long approvedClaims = claimCaseRepository.countByStatus(ClaimCase.ClaimStatus.APPROVED);
        statistics.put("approvedClaims", approvedClaims);

        // 已拒绝理赔
        long rejectedClaims = claimCaseRepository.countByStatus(ClaimCase.ClaimStatus.REJECTED);
        statistics.put("rejectedClaims", rejectedClaims);

        // 总金额（所有理赔申请的金额总和）
        Double totalAmount = claimCaseRepository.getTotalClaimedAmount();
        statistics.put("totalAmount", totalAmount != null ? totalAmount : 0.0);

        // 平均处理时间（小时）
        Double avgProcessingDays = claimCaseRepository.getAverageProcessingDays();
        statistics.put("averageProcessingTime", avgProcessingDays != null ? avgProcessingDays * 24 : 0.0);

        return statistics;
    }

    /**
     * 启动 Case 流程（接收已保存的 ClaimCase）
     * 通过 claimCaseId 变量传递给流程，监听器可以通过 claimCaseId 查找
     */
    private String startCaseProcessWithClaimCaseId(ClaimCase claimCase) {
        try {
            Map<String, Object> variables = new HashMap<>();
            // 传入 claimCaseId，监听器可以通过它查找 ClaimCase
            variables.put("claimCaseId", claimCase.getId().toString());
            variables.put("claimNumber", claimCase.getClaimNumber());
            variables.put("policyId", claimCase.getPolicy().getId().toString());
            variables.put("claimedAmount", claimCase.getClaimedAmount());
            variables.put("coverageAmount", claimCase.getPolicy().getCoverageAmount());
            variables.put("claimType", claimCase.getClaimType());
            variables.put("severity", claimCase.getSeverity().toString());
            variables.put("claimantName", claimCase.getClaimantName());
            variables.put("incidentDate", claimCase.getIncidentDate().toString());
            variables.put("incidentLocation", claimCase.getIncidentLocation());
            variables.put("incidentDescription", claimCase.getIncidentDescription());
            
            // Set claimAdjuster - use creator's username or a default adjuster
            if (claimCase.getCreatedBy() != null) {
                variables.put("claimAdjuster", claimCase.getCreatedBy().getUsername());
            } else {
                // Default claim adjuster if no creator is specified
                variables.put("claimAdjuster", "admin");
            }
            
            // Set other required role variables with default values
            variables.put("damageAssessor", "admin");
            variables.put("approverGroup", "managers");
            variables.put("paymentOfficer", "admin");
            variables.put("paymentManager", "admin");
            
            // Initialize the DMN output variable to null so it becomes Global
            variables.put("claimComplexity", null);
            // Initialize Approval decision
            variables.put("approved", null);
            // Initialize Payment status
            variables.put("paymentStatus", null);

            org.flowable.cmmn.api.runtime.CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("insuranceClaimCase")
                    .businessKey(claimCase.getClaimNumber())
                    .name("理赔案件 - " + claimCase.getClaimNumber())
                    .variables(variables)
                    .start();

            String caseInstanceId = caseInstance.getId();
            log.info("Started case process for claim case {} with instance ID: {}", claimCase.getId(), caseInstanceId);

            return caseInstanceId;

        } catch (Exception e) {
            log.error("Failed to start case process: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 更新 Case 流程变量（在 ClaimCase 保存后调用）
     */
    private void updateCaseProcessVariables(ClaimCase claimCase) {
        if (claimCase.getCaseInstanceId() == null) {
            return;
        }
        
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("claimCaseId", claimCase.getId().toString());
            variables.put("claimNumber", claimCase.getClaimNumber());
            variables.put("policyId", claimCase.getPolicy().getId().toString());
            variables.put("claimedAmount", claimCase.getClaimedAmount());
            variables.put("coverageAmount", claimCase.getPolicy().getCoverageAmount());
            variables.put("claimType", claimCase.getClaimType());
            variables.put("severity", claimCase.getSeverity().toString());
            variables.put("claimantName", claimCase.getClaimantName());
            variables.put("incidentDate", claimCase.getIncidentDate().toString());
            variables.put("incidentLocation", claimCase.getIncidentLocation());
            variables.put("incidentDescription", claimCase.getIncidentDescription());
            
            // Set all variables
            cmmnRuntimeService.setVariables(claimCase.getCaseInstanceId(), variables);
            
            log.info("Updated case process variables for claim case {} with instance ID {}",
                    claimCase.getId(), claimCase.getCaseInstanceId());

        } catch (Exception e) {
            log.warn("Failed to update case process variables for claim case {}: {}",
                    claimCase.getId(), e.getMessage());
        }
    }

    /**
     * 启动 Case 流程
     */
    private void startCaseProcess(ClaimCase claimCase) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("claimCaseId", claimCase.getId().toString());
            variables.put("claimNumber", claimCase.getClaimNumber());
            variables.put("policyId", claimCase.getPolicy().getId().toString());
            variables.put("claimedAmount", claimCase.getClaimedAmount());
            variables.put("coverageAmount", claimCase.getPolicy().getCoverageAmount());
            variables.put("claimType", claimCase.getClaimType());
            variables.put("severity", claimCase.getSeverity().toString());
            variables.put("claimantName", claimCase.getClaimantName());
            variables.put("incidentDate", claimCase.getIncidentDate().toString());
            variables.put("incidentLocation", claimCase.getIncidentLocation());
            variables.put("incidentDescription", claimCase.getIncidentDescription());
            
            // Set claimAdjuster - use creator's username or a default adjuster
            if (claimCase.getCreatedBy() != null) {
                variables.put("claimAdjuster", claimCase.getCreatedBy().getUsername());
            } else {
                // Default claim adjuster if no creator is specified
                variables.put("claimAdjuster", "admin");
            }
            
            // Set other required role variables with default values
            variables.put("damageAssessor", "admin");
            variables.put("approverGroup", "managers");
            variables.put("paymentOfficer", "admin");
            variables.put("paymentManager", "admin");
            
            // Initialize the DMN output variable to null so it becomes Global
            variables.put("claimComplexity", null);
            // Initialize Approval decision
            variables.put("approved", null);
            // Initialize Payment status
            variables.put("paymentStatus", null);

            org.flowable.cmmn.api.runtime.CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("insuranceClaimCase")
                    .businessKey(claimCase.getClaimNumber())
                    .name("理赔案件 - " + claimCase.getClaimNumber())
                    .variables(variables)
                    .start();

            // 更新案件的 Case 实例 ID
            claimCase.setCaseInstanceId(caseInstance.getId());
            claimCaseRepository.save(claimCase);

            log.info("Started case process for claim case {} with instance ID {}",
                    claimCase.getId(), caseInstance.getId());

        } catch (Exception e) {
            log.error("Failed to start case process for claim case {}: {}",
                    claimCase.getId(), e.getMessage(), e);
            // 不抛出异常，避免影响案件创建
        }
    }

    /**
     * 完成审核任务
     */
    public ClaimCase completeReviewTask(UUID caseId, String userId, String reviewComments, String reviewNotes) {
        log.debug("Completing review task for claim case {} by user {}", caseId, userId);

        ClaimCase claimCase = claimCaseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Claim case not found"));

        // Try to find user by UUID first, then by username as fallback
        User reviewedBy = null;
        try {
            reviewedBy = userRepository.findById(UUID.fromString(userId))
                    .orElse(null);
        } catch (IllegalArgumentException e) {
            // userId is not a valid UUID, try by username
        }
        
        if (reviewedBy == null) {
            reviewedBy = userRepository.findByUsername(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
        }

        // 完成CMMN任务以推动流程 - 不要强制改变状态，让流程自然演进
        if (claimCase.getCaseInstanceId() != null) {
            try {
                Map<String, Object> variables = new HashMap<>();
                variables.put("reviewedBy", reviewedBy.getUsername());
                variables.put("reviewedAt", LocalDateTime.now().toString());
                variables.put("reviewComments", reviewComments);
                variables.put("reviewNotes", reviewNotes);
                
                // CRITICAL: Include DMN decision table input variables
                // These are needed for the taskAssessComplexity decision task that will be triggered
                variables.put("policyType", claimCase.getPolicy().getPolicyType());
                variables.put("claimedAmount", claimCase.getClaimedAmount());
                variables.put("coverageAmount", claimCase.getPolicy().getCoverageAmount());
                variables.put("claimType", claimCase.getClaimType());
                variables.put("severity", claimCase.getSeverity().toString());
                
                // 查找并完成Review Claim任务
                completeCmmnTask(claimCase.getCaseInstanceId(), "taskReviewClaim", variables);
                
                log.info("Completed review task for claim case {}", claimCase.getId());
                
                // 只在状态为SUBMITTED时才更新为UNDER_REVIEW
                if (claimCase.getStatus() == ClaimCase.ClaimStatus.SUBMITTED) {
                    String description = "Review started by " + reviewedBy.getFullName();
                    if (reviewComments != null && !reviewComments.isBlank()) {
                        description += " - " + reviewComments;
                    }
                    claimCase.updateStatus("UNDER_REVIEW", description, reviewedBy);
                    claimCaseRepository.save(claimCase);
                } else {
                    // 如果已经是其他状态，只添加历史记录
                    claimCase.addHistory("REVIEW_COMPLETED", 
                            "Review completed by " + reviewedBy.getFullName() + 
                            (reviewComments != null && !reviewComments.isBlank() ? 
                                " - " + reviewComments : ""), 
                            reviewedBy);
                    claimCaseRepository.save(claimCase);
                }
            } catch (Exception e) {
                log.error("Failed to complete review CMMN task: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to complete review task: " + e.getMessage(), e);
            }
        }

        return claimCase;
    }

    /**
     * 查找并完成指定类型的CMMN任务
     */
    private void completeCmmnTask(String caseInstanceId, String taskDefinitionKey, 
                                    Map<String, Object> variables) {
        try {
            List<org.flowable.task.api.Task> tasks = cmmnTaskService.createTaskQuery()
                    .caseInstanceId(caseInstanceId)
                    .taskDefinitionKey(taskDefinitionKey)
                    .active()
                    .list();
            
            if (!tasks.isEmpty()) {
                org.flowable.task.api.Task task = tasks.get(0);
                cmmnTaskService.complete(task.getId(), variables);
                log.info("Completed CMMN task {} for case instance {}", taskDefinitionKey, caseInstanceId);
            } else {
                log.warn("No active task {} found for case instance {}", taskDefinitionKey, caseInstanceId);
            }
        } catch (Exception e) {
            log.error("Failed to complete CMMN task {}: {}", taskDefinitionKey, e.getMessage(), e);
        }
    }

    /**
     * 生成理赔案件编号
     * 使用 synchronized 确保线程安全，防止并发请求生成相同的理赔编号
     */
    private synchronized String generateClaimNumber() {
        String dateStr = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        long sequence = claimCaseRepository.countByCreatedAtAfter(
                LocalDateTime.now().withHour(0).withMinute(0).withSecond(0)) + 1;
        String claimNumber = "CLM" + dateStr + String.format("%04d", sequence);
        
        // 验证编号是否已存在（双重检查）
        while (claimCaseRepository.findByClaimNumber(claimNumber).isPresent()) {
            sequence++;
            claimNumber = "CLM" + dateStr + String.format("%04d", sequence);
            log.warn("Generated claim number {} already exists, trying next sequence: {}", 
                    claimNumber, sequence);
        }
        
        return claimNumber;
    }
}
