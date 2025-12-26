package com.flowable.demo.admin.service;

import com.flowable.demo.admin.adapter.FlowableCmmnAdapter;
import com.flowable.demo.admin.model.PlanItemTreeNode;
import com.flowable.demo.admin.web.dto.CaseInstanceDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

/**
 * Case 运行态管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CaseRuntimeService {

    private final FlowableCmmnAdapter cmmnAdapter;

    // ==================== Case 查询 ====================

    /**
     * 分页查询 Case 实例
     */
    public Page<CaseInstanceDTO> queryCaseInstances(
            String caseDefinitionKey,
            String businessKey,
            String state,
            LocalDateTime startedAfter,
            Pageable pageable) {
        Page<CaseInstance> instances = cmmnAdapter.queryCaseInstances(
                caseDefinitionKey,
                businessKey,
                state,
                startedAfter,
                pageable);

        return instances.map(this::convertToDTO);
    }

    /**
     * 获取 Case 实例详情
     */
    public CaseInstanceDTO getCaseInstanceDetail(String caseInstanceId) {
        CaseInstance caseInstance = cmmnAdapter.getCaseInstance(caseInstanceId);

        if (caseInstance == null) {
            log.warn("Case instance not found: {}", caseInstanceId);
            return null;
        }

        // 获取变量
        Map<String, Object> variables = cmmnAdapter.getCaseVariables(caseInstanceId);

        // Get businessKey - if null, try to get claimNumber from variables
        String businessKey = caseInstance.getBusinessKey();
        if ((businessKey == null || businessKey.isEmpty())) {
            businessKey = (String) variables.get("claimNumber");
        }

        // 获取 Plan Item Tree
        PlanItemTreeNode planItemTree = cmmnAdapter.getCasePlanItemTree(caseInstanceId);

        // 统计 Plan Item 数量
        Map<String, Integer> planItemCounts = cmmnAdapter.countPlanItems(caseInstanceId);

        return CaseInstanceDTO.builder()
                .id(caseInstance.getId())
                .caseDefinitionId(caseInstance.getCaseDefinitionId())
                .caseDefinitionKey(caseInstance.getCaseDefinitionKey())
                .caseDefinitionName(caseInstance.getCaseDefinitionName())
                .caseDefinitionVersion(caseInstance.getCaseDefinitionVersion())
                .businessKey(businessKey)
                .state(caseInstance.getState())
                .startTime(toLocalDateTime(caseInstance.getStartTime()))
                .startUserId(caseInstance.getStartUserId())
                .tenantId(caseInstance.getTenantId())
                .activePlanItems(planItemCounts.getOrDefault("active", 0))
                .completedPlanItems(planItemCounts.getOrDefault("completed", 0))
                .variables(variables)
                .planItemTree(planItemTree)
                .build();
    }

    // ==================== Case 操作 ====================

    /**
     * 终止 Case 实例
     */
    @Transactional
    public void terminateCase(String caseInstanceId, String reason) {
        log.info("Terminating case instance: {}, reason: {}", caseInstanceId, reason);
        cmmnAdapter.terminateCase(caseInstanceId, reason);
    }

    /**
     * 挂起 Case 实例
     */
    @Transactional
    public void suspendCase(String caseInstanceId) {
        log.info("Suspending case instance: {}", caseInstanceId);
        cmmnAdapter.suspendCase(caseInstanceId);
    }

    /**
     * 恢复 Case 实例
     */
    @Transactional
    public void resumeCase(String caseInstanceId) {
        log.info("Resuming case instance: {}", caseInstanceId);
        cmmnAdapter.resumeCase(caseInstanceId);
    }

    /**
     * 手动触发 Plan Item
     */
    @Transactional
    public void triggerPlanItem(String planItemInstanceId) {
        log.info("Triggering plan item: {}", planItemInstanceId);
        cmmnAdapter.triggerPlanItem(planItemInstanceId);
    }

    // ==================== 工具方法 ====================

    /**
     * 转换为 DTO(列表用)
     */
    private CaseInstanceDTO convertToDTO(CaseInstance caseInstance) {
        // 统计 Plan Item 数量
        Map<String, Integer> planItemCounts = cmmnAdapter.countPlanItems(caseInstance.getId());

        // Get businessKey - if null, try to get claimNumber from variables
        String businessKey = caseInstance.getBusinessKey();
        if ((businessKey == null || businessKey.isEmpty())) {
            Map<String, Object> variables = cmmnAdapter.getCaseVariables(caseInstance.getId());
            businessKey = (String) variables.get("claimNumber");
        }

        return CaseInstanceDTO.builder()
                .id(caseInstance.getId())
                .caseDefinitionId(caseInstance.getCaseDefinitionId())
                .caseDefinitionKey(caseInstance.getCaseDefinitionKey())
                .caseDefinitionName(caseInstance.getCaseDefinitionName())
                .caseDefinitionVersion(caseInstance.getCaseDefinitionVersion())
                .businessKey(businessKey)
                .state(caseInstance.getState())
                .startTime(toLocalDateTime(caseInstance.getStartTime()))
                .startUserId(caseInstance.getStartUserId())
                .tenantId(caseInstance.getTenantId())
                .activePlanItems(planItemCounts.getOrDefault("active", 0))
                .completedPlanItems(planItemCounts.getOrDefault("completed", 0))
                .build();
    }

    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
