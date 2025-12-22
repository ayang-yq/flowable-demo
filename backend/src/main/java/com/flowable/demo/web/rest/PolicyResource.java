package com.flowable.demo.web.rest;

import com.flowable.demo.domain.model.InsurancePolicy;
import com.flowable.demo.domain.repository.InsurancePolicyRepository;
import com.flowable.demo.web.rest.dto.InsurancePolicyDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 保单管理 REST API
 */
@RestController
@RequestMapping("/policies")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "保单管理", description = "保单的管理操作")
public class PolicyResource {

    private final InsurancePolicyRepository insurancePolicyRepository;

    /**
     * 获取所有保单（分页）
     */
    @GetMapping
    @Operation(summary = "获取保单列表", description = "分页获取所有保单")
    public ResponseEntity<Page<InsurancePolicyDTO>> getAllPolicies(Pageable pageable) {
        log.debug("REST request to get a page of InsurancePolicies");
        
        Page<InsurancePolicy> page = insurancePolicyRepository.findAll(pageable);
        Page<InsurancePolicyDTO> result = new PageImpl<>(
                page.getContent().stream().map(this::convertToDTO).collect(Collectors.toList()),
                pageable,
                page.getTotalElements()
        );
        
        return ResponseEntity.ok(result);
    }

    /**
     * 根据保单号获取保单
     */
    @GetMapping("/by-number/{policyNumber}")
    @Operation(summary = "根据保单号获取保单", description = "根据保单号获取保单详情")
    public ResponseEntity<InsurancePolicyDTO> getPolicyByNumber(
            @Parameter(description = "保单号") @PathVariable String policyNumber) {
        log.debug("REST request to get policy by number: {}", policyNumber);
        
        return insurancePolicyRepository.findByPolicyNumber(policyNumber)
                .map(this::convertToDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 根据ID获取保单
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取保单", description = "根据ID获取保单详情")
    public ResponseEntity<InsurancePolicyDTO> getPolicy(
            @Parameter(description = "保单ID") @PathVariable String id) {
        log.debug("REST request to get policy: {}", id);
        
        return insurancePolicyRepository.findById(UUID.fromString(id))
                .map(this::convertToDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 创建保单
     */
    @PostMapping
    @Operation(summary = "创建保单", description = "创建新的保单")
    public ResponseEntity<InsurancePolicyDTO> createPolicy(@RequestBody InsurancePolicyDTO policyDTO) {
        log.debug("REST request to create policy: {}", policyDTO.getPolicyNumber());

        if (insurancePolicyRepository.findByPolicyNumber(policyDTO.getPolicyNumber()).isPresent()) {
            return ResponseEntity.badRequest().build();
        }

        InsurancePolicy policy = convertToEntity(policyDTO);
        policy.setId(UUID.randomUUID());

        InsurancePolicy savedPolicy = insurancePolicyRepository.save(policy);
        return ResponseEntity.ok(convertToDTO(savedPolicy));
    }

    /**
     * 更新保单
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新保单", description = "更新指定保单的信息")
    public ResponseEntity<InsurancePolicyDTO> updatePolicy(
            @Parameter(description = "保单ID") @PathVariable String id,
            @RequestBody InsurancePolicyDTO policyDTO) {
        log.debug("REST request to update policy: {}", id);

        return insurancePolicyRepository.findById(UUID.fromString(id))
                .map(existingPolicy -> {
                    // 检查保单号是否与其他保单冲突
                    if (!existingPolicy.getPolicyNumber().equals(policyDTO.getPolicyNumber()) &&
                        insurancePolicyRepository.findByPolicyNumber(policyDTO.getPolicyNumber()).isPresent()) {
                        return ResponseEntity.badRequest().<InsurancePolicyDTO>build();
                    }

                    // 更新保单信息
                    updateEntityFromDTO(existingPolicy, policyDTO);
                    InsurancePolicy savedPolicy = insurancePolicyRepository.save(existingPolicy);
                    return ResponseEntity.ok(convertToDTO(savedPolicy));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 转换为 DTO
     */
    private InsurancePolicyDTO convertToDTO(InsurancePolicy policy) {
        InsurancePolicyDTO dto = new InsurancePolicyDTO();
        dto.setId(policy.getId().toString());
        dto.setPolicyNumber(policy.getPolicyNumber());
        dto.setPolicyholderName(policy.getPolicyHolderName());
        dto.setPolicyType(policy.getPolicyType());
        dto.setCoverageAmount(policy.getCoverageAmount().doubleValue());
        dto.setPremium(policy.getPremiumAmount().doubleValue());
        dto.setEffectiveDate(policy.getStartDate());
        dto.setExpiryDate(policy.getEndDate());
        dto.setStatus(policy.getStatus());
        
        return dto;
    }

    /**
     * 转换为实体
     */
    private InsurancePolicy convertToEntity(InsurancePolicyDTO dto) {
        InsurancePolicy policy = new InsurancePolicy();
        policy.setPolicyNumber(dto.getPolicyNumber());
        policy.setPolicyHolderName(dto.getPolicyholderName());
        policy.setPolicyType(dto.getPolicyType());
        policy.setCoverageAmount(java.math.BigDecimal.valueOf(dto.getCoverageAmount()));
        policy.setPremiumAmount(java.math.BigDecimal.valueOf(dto.getPremium()));
        policy.setStartDate(dto.getEffectiveDate());
        policy.setEndDate(dto.getExpiryDate());
        policy.setStatus(dto.getStatus());
        
        return policy;
    }

    /**
     * 从DTO更新实体
     */
    private void updateEntityFromDTO(InsurancePolicy policy, InsurancePolicyDTO dto) {
        policy.setPolicyNumber(dto.getPolicyNumber());
        policy.setPolicyHolderName(dto.getPolicyholderName());
        policy.setPolicyType(dto.getPolicyType());
        policy.setCoverageAmount(java.math.BigDecimal.valueOf(dto.getCoverageAmount()));
        policy.setPremiumAmount(java.math.BigDecimal.valueOf(dto.getPremium()));
        policy.setStartDate(dto.getEffectiveDate());
        policy.setEndDate(dto.getExpiryDate());
        policy.setStatus(dto.getStatus());
    }
}
