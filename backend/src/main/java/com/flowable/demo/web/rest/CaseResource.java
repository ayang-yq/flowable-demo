package com.flowable.demo.web.rest;

import com.flowable.demo.domain.model.ClaimCase;
import com.flowable.demo.domain.repository.ClaimCaseRepository;
import com.flowable.demo.domain.repository.InsurancePolicyRepository;
import com.flowable.demo.domain.repository.UserRepository;
import com.flowable.demo.service.CaseService;
import com.flowable.demo.web.rest.dto.ApproveRequestDTO;
import com.flowable.demo.web.rest.dto.ClaimCaseDTO;
import com.flowable.demo.web.rest.dto.InsurancePolicyDTO;
import com.flowable.demo.web.rest.dto.PaymentRequestDTO;
import com.flowable.demo.web.rest.dto.RejectRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 理赔案件管理 REST API
 */
@RestController
@RequestMapping("/cases")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "理赔案件管理", description = "理赔案件的增删改查和流程操作")
public class CaseResource {

    private final CaseService caseService;
    private final ClaimCaseRepository claimCaseRepository;
    private final InsurancePolicyRepository insurancePolicyRepository;
    private final UserRepository userRepository;
    private final CmmnRuntimeService cmmnRuntimeService;
    private final CmmnTaskService cmmnTaskService;

    /**
     * 创建新理赔案件
     */
    @PostMapping
    @Operation(summary = "创建理赔案件", description = "创建新的理赔案件并启动 Case 流程")
    @Transactional
    public ResponseEntity<ClaimCaseDTO> createClaimCase(@Valid @RequestBody ClaimCaseDTO claimCaseDTO)
            throws URISyntaxException {
        log.debug("REST request to create ClaimCase : {}", claimCaseDTO);

        if (claimCaseDTO.getId() != null) {
            throw new IllegalArgumentException("A new claimCase cannot already have an ID");
        }

        // 验证保单是否存在
        if (!insurancePolicyRepository.existsById(UUID.fromString(claimCaseDTO.getPolicyId()))) {
            throw new IllegalArgumentException("Invalid policy ID");
        }

        ClaimCase result = caseService.createClaimCase(claimCaseDTO);
        ClaimCaseDTO resultDTO = convertToDTO(result);

        return ResponseEntity.created(new URI("/api/cases/" + result.getId()))
                .body(resultDTO);
    }

    /**
     * 更新理赔案件
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新理赔案件", description = "更新指定ID的理赔案件信息")
    @Transactional
    public ResponseEntity<ClaimCaseDTO> updateClaimCase(
            @Parameter(description = "案件ID") @PathVariable UUID id,
            @Valid @RequestBody ClaimCaseDTO claimCaseDTO) {
        log.debug("REST request to update ClaimCase : {}, {}", id, claimCaseDTO);

        if (!claimCaseRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        claimCaseDTO.setId(id.toString());
        ClaimCase result = caseService.updateClaimCase(claimCaseDTO);
        ClaimCaseDTO resultDTO = convertToDTO(result);

        return ResponseEntity.ok(resultDTO);
    }

    /**
     * 获取所有理赔案件（分页）
     */
    @GetMapping
    @Operation(summary = "获取理赔案件列表", description = "分页获取所有理赔案件")
    public ResponseEntity<Page<ClaimCaseDTO>> getAllClaimCases(Pageable pageable) {
        log.debug("REST request to get a page of ClaimCases");

        Page<ClaimCase> page = claimCaseRepository.findAll(pageable);
        Page<ClaimCaseDTO> result = new PageImpl<>(
                page.getContent().stream().map(this::convertToDTO).collect(Collectors.toList()),
                pageable,
                page.getTotalElements());

        return ResponseEntity.ok(result);
    }

    /**
     * 获取指定理赔案件
     * 支持通过 ClaimCase UUID 或 Flowable Case Instance ID 查询
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取理赔案件", description = "根据ID获取指定的理赔案件详情")
    public ResponseEntity<ClaimCaseDTO> getClaimCase(@Parameter(description = "案件ID") @PathVariable String id) {
        log.debug("REST request to get ClaimCase : {}", id);

        // First, try to find by ClaimCase UUID
        try {
            UUID uuid = UUID.fromString(id);
            return claimCaseRepository.findById(uuid)
                    .map(this::convertToDTO)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> {
                        // If not found by UUID, try to find by Case Instance ID
                        log.debug("Not found by UUID, trying Case Instance ID: {}", id);
                        return claimCaseRepository.findByCaseInstanceId(id)
                                .map(this::convertToDTO)
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.notFound().build());
                    });
        } catch (IllegalArgumentException e) {
            // If id is not a valid UUID, try to find by Case Instance ID directly
            log.debug("Invalid UUID format, trying Case Instance ID: {}", id);
            return claimCaseRepository.findByCaseInstanceId(id)
                    .map(this::convertToDTO)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }
    }

    /**
     * 删除理赔案件
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除理赔案件", description = "删除指定ID的理赔案件")
    @Transactional
    public ResponseEntity<Void> deleteClaimCase(@Parameter(description = "案件ID") @PathVariable UUID id) {
        log.debug("REST request to delete ClaimCase : {}", id);

        if (!claimCaseRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        caseService.deleteClaimCase(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 分配理赔案件
     */
    @PostMapping("/{id}/assign")
    @Operation(summary = "分配理赔案件", description = "将理赔案件分配给指定用户")
    @Transactional
    public ResponseEntity<ClaimCaseDTO> assignClaimCase(
            @Parameter(description = "案件ID") @PathVariable UUID id,
            @Parameter(description = "用户ID") @RequestParam UUID userId) {
        log.debug("REST request to assign ClaimCase : {} to User : {}", id, userId);

        ClaimCase result = caseService.assignClaimCase(id, userId);
        ClaimCaseDTO resultDTO = convertToDTO(result);

        return ResponseEntity.ok(resultDTO);
    }

    /**
     * 更新理赔案件状态
     */
    @PostMapping("/{id}/status")
    @Operation(summary = "更新理赔状态", description = "更新理赔案件的状态")
    @Transactional
    public ResponseEntity<ClaimCaseDTO> updateClaimCaseStatus(
            @Parameter(description = "案件ID") @PathVariable UUID id,
            @Parameter(description = "新状态") @RequestParam String status,
            @Parameter(description = "操作描述") @RequestParam(required = false) String description,
            @Parameter(description = "操作用户ID") @RequestParam String userId) {
        log.debug("REST request to update ClaimCase status : {} to {}", id, status);

        ClaimCase result = caseService.updateClaimCaseStatus(id, status, description, UUID.fromString(userId));
        ClaimCaseDTO resultDTO = convertToDTO(result);

        return ResponseEntity.ok(resultDTO);
    }

    /**
     * 根据状态查询理赔案件
     */
    @GetMapping("/by-status")
    @Operation(summary = "根据状态查询", description = "根据状态查询理赔案件")
    public ResponseEntity<Page<ClaimCaseDTO>> getClaimCasesByStatus(
            @Parameter(description = "状态") @RequestParam String status,
            Pageable pageable) {
        log.debug("REST request to get ClaimCases by status : {}", status);

        Page<ClaimCase> page = claimCaseRepository.findByStatus(ClaimCase.ClaimStatus.valueOf(status.toUpperCase()),
                pageable);
        Page<ClaimCaseDTO> result = new PageImpl<>(
                page.getContent().stream().map(this::convertToDTO).collect(Collectors.toList()),
                pageable,
                page.getTotalElements());

        return ResponseEntity.ok(result);
    }

    /**
     * 根据分配用户查询理赔案件
     */
    @GetMapping("/by-assignee")
    @Operation(summary = "根据分配用户查询", description = "根据分配用户查询理赔案件")
    public ResponseEntity<Page<ClaimCaseDTO>> getClaimCasesByAssignee(
            @Parameter(description = "用户ID") @RequestParam UUID userId,
            Pageable pageable) {
        log.debug("REST request to get ClaimCases by assignee : {}", userId);

        return userRepository.findById(userId)
                .map(user -> {
                    Page<ClaimCase> page = claimCaseRepository.findByAssignedTo(user, pageable);
                    Page<ClaimCaseDTO> result = new PageImpl<>(
                            page.getContent().stream().map(this::convertToDTO).collect(Collectors.toList()),
                            pageable,
                            page.getTotalElements());
                    return ResponseEntity.ok(result);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 根据保单查询理赔案件
     */
    @GetMapping("/by-policy/{policyId}")
    @Operation(summary = "根据保单查询", description = "根据保单ID查询相关的理赔案件")
    public ResponseEntity<Page<ClaimCaseDTO>> getClaimCasesByPolicy(
            @Parameter(description = "保单ID") @PathVariable UUID policyId,
            Pageable pageable) {
        log.debug("REST request to get ClaimCases by policy : {}", policyId);

        return insurancePolicyRepository.findById(policyId)
                .map(policy -> {
                    Page<ClaimCase> page = claimCaseRepository.findByPolicy(policy, pageable);
                    Page<ClaimCaseDTO> result = new PageImpl<>(
                            page.getContent().stream().map(this::convertToDTO).collect(Collectors.toList()),
                            pageable,
                            page.getTotalElements());
                    return ResponseEntity.ok(result);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 搜索理赔案件
     */
    @GetMapping("/search")
    @Operation(summary = "搜索理赔案件", description = "根据关键词搜索理赔案件")
    public ResponseEntity<Page<ClaimCaseDTO>> searchClaimCases(
            @Parameter(description = "搜索关键词") @RequestParam String keyword,
            Pageable pageable) {
        log.debug("REST request to search ClaimCases with keyword : {}", keyword);

        Page<ClaimCase> page = claimCaseRepository.searchByKeyword(keyword, pageable);
        Page<ClaimCaseDTO> result = new PageImpl<>(
                page.getContent().stream().map(this::convertToDTO).collect(Collectors.toList()),
                pageable,
                page.getTotalElements());

        return ResponseEntity.ok(result);
    }

    /**
     * 获取我的理赔案件
     */
    @GetMapping("/my-cases")
    @Operation(summary = "获取我的理赔案件", description = "获取当前用户相关的理赔案件")
    public ResponseEntity<Page<ClaimCaseDTO>> getMyClaimCases(
            @Parameter(description = "用户ID") @RequestParam UUID userId,
            Pageable pageable) {
        log.debug("REST request to get my ClaimCases for user : {}", userId);

        Page<ClaimCase> page = claimCaseRepository.findMyClaimCases(userId, pageable);
        Page<ClaimCaseDTO> result = new PageImpl<>(
                page.getContent().stream().map(this::convertToDTO).collect(Collectors.toList()),
                pageable,
                page.getTotalElements());

        return ResponseEntity.ok(result);
    }

    /**
     * 批准理赔案件
     */
    @PostMapping("/{id}/approve")
    @Operation(summary = "批准理赔案件", description = "批准指定的理赔案件")
    @Transactional
    public ResponseEntity<ClaimCaseDTO> approveClaimCase(
            @Parameter(description = "案件ID") @PathVariable UUID id,
            @Parameter(description = "批准用户ID") @RequestParam String userId,
            @Valid @RequestBody ApproveRequestDTO approveRequestDTO) {
        log.debug("REST request to approve ClaimCase : {} by user : {}", id, userId);

        ClaimCase result = caseService.approveClaimCase(id, userId, approveRequestDTO);
        ClaimCaseDTO resultDTO = convertToDTO(result);

        return ResponseEntity.ok(resultDTO);
    }

    /**
     * 拒绝理赔案件
     */
    @PostMapping("/{id}/reject")
    @Operation(summary = "拒绝理赔案件", description = "拒绝指定的理赔案件")
    @Transactional
    public ResponseEntity<ClaimCaseDTO> rejectClaimCase(
            @Parameter(description = "案件ID") @PathVariable UUID id,
            @Valid @RequestBody RejectRequestDTO rejectRequestDTO) {
        log.debug("REST request to reject ClaimCase : {} with reason : {}", id, rejectRequestDTO.getReason());

        ClaimCase result = caseService.rejectClaimCase(id, rejectRequestDTO.getReason());
        ClaimCaseDTO resultDTO = convertToDTO(result);

        return ResponseEntity.ok(resultDTO);
    }

    /**
     * 支付理赔案件
     */
    @PostMapping("/{id}/pay")
    @Operation(summary = "支付理赔案件", description = "处理指定理赔案件的支付")
    @Transactional
    public ResponseEntity<ClaimCaseDTO> payClaimCase(
            @Parameter(description = "案件ID") @PathVariable String id,
            @Valid @RequestBody PaymentRequestDTO paymentRequestDTO) {
        log.debug("REST request to pay ClaimCase : {} with payment request : {}", id, paymentRequestDTO);

        // 尝试将 ID 解析为 UUID 或 Case Instance ID
        UUID caseId;
        try {
            caseId = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            // 如果不是有效的 UUID，尝试通过 Case Instance ID 查找
            ClaimCase claimCase = claimCaseRepository.findByCaseInstanceId(id)
                    .orElseThrow(() -> new IllegalArgumentException("Claim case not found with id: " + id));
            caseId = claimCase.getId();
        }

        // 验证案件是否存在
        if (!claimCaseRepository.existsById(caseId)) {
            return ResponseEntity.notFound().build();
        }

        // 默认支付方式
        String paymentMethod = paymentRequestDTO.getPaymentMethod() != null 
                ? paymentRequestDTO.getPaymentMethod() 
                : "TRANSFER";

        // 默认支付参考号
        String paymentReference = paymentRequestDTO.getPaymentReference() != null 
                ? paymentRequestDTO.getPaymentReference() 
                : "PAY-" + java.time.LocalDate.now().toString() + "-" + caseId.toString().substring(0, 8);

        ClaimCase result = caseService.payClaimCase(
                caseId,
                paymentRequestDTO.getPaymentAmount(),
                paymentRequestDTO.getPaymentDate(),
                paymentMethod,
                paymentReference,
                paymentRequestDTO.getUserId()
        );
        ClaimCaseDTO resultDTO = convertToDTO(result);

        return ResponseEntity.ok(resultDTO);
    }

    /**
     * 完成审核任务
     */
    @PostMapping("/{id}/complete-review")
    @Operation(summary = "完成审核任务", description = "完成理赔审核任务并推动流程")
    @Transactional
    public ResponseEntity<ClaimCaseDTO> completeReviewTask(
            @Parameter(description = "案件ID") @PathVariable String id,
            @Parameter(description = "审核用户ID") @RequestParam String userId,
            @RequestBody(required = false) java.util.Map<String, String> reviewData) {
        log.debug("REST request to complete review task for ClaimCase : {} by user : {}", id, userId);

        // 尝试将 ID 解析为 UUID 或 Case Instance ID
        UUID caseId;
        try {
            caseId = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            // 如果不是有效的 UUID，尝试通过 Case Instance ID 查找
            ClaimCase claimCase = claimCaseRepository.findByCaseInstanceId(id)
                    .orElseThrow(() -> new IllegalArgumentException("Claim case not found with id: " + id));
            caseId = claimCase.getId();
        }

        String reviewComments = reviewData != null ? reviewData.get("reviewComments") : null;
        String reviewNotes = reviewData != null ? reviewData.get("reviewNotes") : null;

        ClaimCase result = caseService.completeReviewTask(caseId, userId, reviewComments, reviewNotes);
        ClaimCaseDTO resultDTO = convertToDTO(result);

        return ResponseEntity.ok(resultDTO);
    }

    /**
     * 获取案件统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取统计信息", description = "获取理赔案件的统计信息")
    public ResponseEntity<Object> getClaimCaseStatistics() {
        log.debug("REST request to get ClaimCase statistics");

        Object statistics = caseService.getClaimCaseStatistics();
        return ResponseEntity.ok(statistics);
    }

    /**
     * 转换为 DTO
     */
    private ClaimCaseDTO convertToDTO(ClaimCase claimCase) {
        ClaimCaseDTO dto = new ClaimCaseDTO();
        dto.setId(claimCase.getId().toString());
        dto.setCaseInstanceId(claimCase.getCaseInstanceId());
        dto.setClaimNumber(claimCase.getClaimNumber());

        // 设置保单信息
        if (claimCase.getPolicy() != null) {
            dto.setPolicyId(claimCase.getPolicy().getId().toString());

            // 创建保单 DTO
            InsurancePolicyDTO policyDTO = new InsurancePolicyDTO();
            policyDTO.setId(claimCase.getPolicy().getId().toString());
            policyDTO.setPolicyNumber(claimCase.getPolicy().getPolicyNumber());
            policyDTO.setPolicyholderName(claimCase.getPolicy().getPolicyHolderName());
            policyDTO.setPolicyType(claimCase.getPolicy().getPolicyType());
            policyDTO.setCoverageAmount(claimCase.getPolicy().getCoverageAmount() != null
                    ? claimCase.getPolicy().getCoverageAmount().doubleValue()
                    : null);
            policyDTO.setPremium(claimCase.getPolicy().getPremiumAmount() != null
                    ? claimCase.getPolicy().getPremiumAmount().doubleValue()
                    : null);
            policyDTO.setEffectiveDate(claimCase.getPolicy().getStartDate());
            policyDTO.setExpiryDate(claimCase.getPolicy().getEndDate());
            policyDTO.setStatus(claimCase.getPolicy().getStatus());

            dto.setPolicy(policyDTO);
        }

        dto.setClaimantName(claimCase.getClaimantName());
        dto.setClaimantPhone(claimCase.getClaimantPhone());
        dto.setClaimantEmail(claimCase.getClaimantEmail());

        if (claimCase.getIncidentDate() != null) {
            dto.setIncidentDate(claimCase.getIncidentDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }

        dto.setIncidentLocation(claimCase.getIncidentLocation());
        dto.setIncidentDescription(claimCase.getIncidentDescription());

        if (claimCase.getClaimedAmount() != null) {
            dto.setClaimedAmount(claimCase.getClaimedAmount().doubleValue());
        }

        if (claimCase.getApprovedAmount() != null) {
            dto.setApprovedAmount(claimCase.getApprovedAmount().doubleValue());
        }

        dto.setClaimType(claimCase.getClaimType());
        dto.setSeverity(claimCase.getSeverity() != null ? claimCase.getSeverity().name() : null);
        dto.setStatus(claimCase.getStatus() != null ? claimCase.getStatus().name() : null);

        if (claimCase.getAssignedTo() != null) {
            dto.setAssignedToId(claimCase.getAssignedTo().getId().toString());
            dto.setAssignedToName(claimCase.getAssignedTo().getFullName());
        }

        if (claimCase.getCreatedBy() != null) {
            dto.setCreatedById(claimCase.getCreatedBy().getId().toString());
            dto.setCreatedByName(claimCase.getCreatedBy().getFullName());
        }

        dto.setCreatedAt(claimCase.getCreatedAt());
        dto.setUpdatedAt(claimCase.getUpdatedAt());

        // 设置文档和历史记录为空列表，避免前端 null 错误
        dto.setDocuments(new java.util.ArrayList<>());
        dto.setHistories(new java.util.ArrayList<>());

        return dto;
    }
}
