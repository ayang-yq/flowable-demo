package com.flowable.demo.admin.service;

import com.flowable.demo.admin.adapter.FlowableCmmnAdapter;
import com.flowable.demo.admin.adapter.FlowableRepositoryAdapter;
import com.flowable.demo.admin.model.PlanItemTreeNode;
import com.flowable.demo.admin.web.dto.CaseInstanceDTO;
import com.flowable.demo.admin.web.dto.CmmnCaseVisualizationDTO;
import com.flowable.demo.admin.web.dto.PlanItemStateDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Case 运行态管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CaseRuntimeService {

    private final FlowableCmmnAdapter cmmnAdapter;
    private final FlowableRepositoryAdapter repositoryAdapter;
    private final CmmnRepositoryService cmmnRepositoryService;
    private final CmmnRuntimeService cmmnRuntimeService;
    private final CmmnHistoryService cmmnHistoryService;

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

    // ==================== CMMN 可视化 ====================

    /**
     * 获取 CMMN 可视化数据
     * 参考 Flowable UI 6.8 设计，提供模型 XML 和运行态数据
     *
     * @param caseInstanceId Case 实例 ID
     * @return CMMN 可视化 DTO（包含 XML 和 Plan Item 状态）
     */
    public CmmnCaseVisualizationDTO getCaseVisualizationData(String caseInstanceId) {
        log.info("Getting CMMN visualization data for case instance: {}", caseInstanceId);

        // 1. 获取 Case 实例
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstanceId)
                .singleResult();

        if (caseInstance == null) {
            throw new RuntimeException("Case instance not found: " + caseInstanceId);
        }

        // 2. 获取 CMMN XML
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery()
                .caseDefinitionId(caseInstance.getCaseDefinitionId())
                .singleResult();

        // 从部署资源中获取 CMMN XML
        String cmmnXml = repositoryAdapter.getCaseDefinitionResourceContent(
                caseDefinition.getDeploymentId(),
                caseDefinition.getResourceName()
        );

        // 3. 获取运行态 Plan Items
        List<PlanItemInstance> runtimePlanItems = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstanceId)
                .list();

        // 4. 获取历史 Plan Items（用于已完成节点的展示）
        List<HistoricPlanItemInstance> historicPlanItems = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceCaseInstanceId(caseInstanceId)
                .list();

        // 5. 合并运行态和历史态数据
        List<PlanItemStateDTO> allPlanItems = mergePlanItems(runtimePlanItems, historicPlanItems);

        return CmmnCaseVisualizationDTO.builder()
                .caseInstanceId(caseInstanceId)
                .caseDefinitionId(caseInstance.getCaseDefinitionId())
                .cmmnXml(cmmnXml)
                .planItems(allPlanItems)
                .build();
    }

    /**
     * 合并运行态和历史态 Plan Items
     * 优先使用运行态数据，补充历史态数据
     */
    private List<PlanItemStateDTO> mergePlanItems(
            List<PlanItemInstance> runtimePlanItems,
            List<HistoricPlanItemInstance> historicPlanItems) {

        Map<String, PlanItemStateDTO> mergedMap = runtimePlanItems.stream()
                .collect(Collectors.toMap(
                        PlanItemInstance::getElementId,
                        this::convertRuntimePlanItem,
                        (existing, replacement) -> existing
                ));

        // 补充历史态数据（运行态中不存在的）
        for (HistoricPlanItemInstance historicItem : historicPlanItems) {
            String elementId = historicItem.getElementId();
            if (!mergedMap.containsKey(elementId)) {
                mergedMap.put(elementId, convertHistoricPlanItem(historicItem));
            }
        }

        return mergedMap.values().stream()
                .collect(Collectors.toList());
    }

    /**
     * 转换运行态 Plan Item 为 DTO
     */
    private PlanItemStateDTO convertRuntimePlanItem(PlanItemInstance planItem) {
        return PlanItemStateDTO.builder()
                .id(planItem.getId())
                .planItemDefinitionId(planItem.getElementId())
                .name(planItem.getName())
                .type(planItem.getPlanItemDefinitionType())
                .state(planItem.getState())
                .stageInstanceId(planItem.getStageInstanceId())
                .createTime(toLocalDateTime(planItem.getCreateTime()))
                .completedTime(toLocalDateTime(planItem.getCompletedTime()))
                .terminatedTime(toLocalDateTime(planItem.getTerminatedTime()))
                .build();
    }

    /**
     * 转换历史态 Plan Item 为 DTO
     */
    private PlanItemStateDTO convertHistoricPlanItem(HistoricPlanItemInstance planItem) {
        return PlanItemStateDTO.builder()
                .id(planItem.getId())
                .planItemDefinitionId(planItem.getElementId())
                .name(planItem.getName())
                .type(planItem.getPlanItemDefinitionType())
                .state(planItem.getState())
                .stageInstanceId(planItem.getStageInstanceId())
                .createTime(toLocalDateTime(planItem.getCreateTime()))
                .completedTime(toLocalDateTime(planItem.getCompletedTime()))
                .terminatedTime(toLocalDateTime(planItem.getTerminatedTime()))
                .build();
    }
}
