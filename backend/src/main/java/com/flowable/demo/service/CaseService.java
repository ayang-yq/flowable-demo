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
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.api.DmnDecision;
import org.flowable.dmn.api.DmnDecisionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
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
    private final DmnRepositoryService dmnRepositoryService;
    private final DmnDecisionService dmnDecisionService;

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

        // 创建理赔案件
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
                .status(ClaimCase.ClaimStatus.SUBMITTED)
                .createdBy(createdBy)
                .build();

        // 保存案件
        claimCase = claimCaseRepository.save(claimCase);

        // 启动 Case 流程
        startCaseProcess(claimCase);

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

        // 如果有 Case 实例，触发完成事件
        if (claimCase.getCaseInstanceId() != null) {
            try {
                // 设置审批结果变量
                Map<String, Object> variables = new HashMap<>();
                variables.put("approved", true);
                variables.put("approvedBy", approvedBy.getUsername());
                variables.put("approvedDate", LocalDateTime.now().toString());
                variables.put("approvedAmount", approveRequestDTO.getApprovedAmount());
                
                cmmnRuntimeService.setVariables(claimCase.getCaseInstanceId(), variables);
                
                log.info("Set approval variables for case instance {}", claimCase.getCaseInstanceId());
            } catch (Exception e) {
                log.warn("Failed to set approval variables: {}", e.getMessage());
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

        // 如果有 Case 实例，触发完成事件
        if (claimCase.getCaseInstanceId() != null) {
            try {
                // 设置审批结果变量
                Map<String, Object> variables = new HashMap<>();
                variables.put("approved", false);
                variables.put("rejectReason", reason);
                
                cmmnRuntimeService.setVariables(claimCase.getCaseInstanceId(), variables);
                
                log.info("Set rejection variables for case instance {}", claimCase.getCaseInstanceId());
            } catch (Exception e) {
                log.warn("Failed to set rejection variables: {}", e.getMessage());
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

        // 如果有 Case 实例，设置支付相关变量
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
                
                cmmnRuntimeService.setVariables(claimCase.getCaseInstanceId(), variables);
                
                log.info("Set payment variables for case instance {}", claimCase.getCaseInstanceId());
            } catch (Exception e) {
                log.warn("Failed to set payment variables: {}", e.getMessage());
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
            
            // Initialize the DMN output variable to null so it becomes Global
            variables.put("claimComplexity", null);

            // --- Temporary DMN Debugging ---
            log.debug("Attempting to execute DMN for debugging...");
            Map<String, Object> dmnInputVariables = new HashMap<>();
            dmnInputVariables.put("policyType", claimCase.getPolicy().getPolicyType());
            dmnInputVariables.put("claimedAmount", claimCase.getClaimedAmount());
            dmnInputVariables.put("coverageAmount", claimCase.getPolicy().getCoverageAmount());
            dmnInputVariables.put("claimType", claimCase.getClaimType());
            dmnInputVariables.put("severity", claimCase.getSeverity().toString());
            // Initialize Approval decision (CRITICAL FIX)
            variables.put("approved", null); 

            // Initialize Payment status (Prevent future error)
            variables.put("paymentStatus", null);

            DmnDecision decision = dmnRepositoryService.createDecisionQuery().decisionKey("ClaimDecisionTable").latestVersion().singleResult();
            if (decision != null) {
                log.debug("Found DMN Decision Table with key: {}", decision.getKey());
                Map<String, Object> dmnResult = dmnDecisionService.executeDecisionWithSingleResult(
                    dmnDecisionService.createExecuteDecisionBuilder()
                        .decisionKey("ClaimDecisionTable")
                        .variables(dmnInputVariables)
                );
                log.debug("DMN execution result: {}", dmnResult);
                if (dmnResult != null) {
                    log.debug("DMN result entry: {}", dmnResult);
                    if (dmnResult.containsKey("claimComplexity")) {
                        log.debug("DMN output 'claimComplexity': {}", dmnResult.get("claimComplexity"));
                    } else {
                        log.warn("DMN result does not contain 'claimComplexity' key.");
                    }
                } else {
                    log.warn("DMN execution returned no result.");
                }
            } else {
                log.error("DMN Decision Table 'ClaimDecisionTable' not found.");
            }
            // --- End Temporary DMN Debugging ---

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
     * 生成理赔案件编号
     */
    private String generateClaimNumber() {
        String dateStr = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        long sequence = claimCaseRepository.countByCreatedAtAfter(
                LocalDateTime.now().withHour(0).withMinute(0).withSecond(0)) + 1;
        return "CLM" + dateStr + String.format("%04d", sequence);
    }
}
