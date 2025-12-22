package com.flowable.demo.admin.adapter;

import com.flowable.demo.admin.model.CaseInstanceInfo;
import com.flowable.demo.admin.model.PlanItemTreeNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceQuery;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Flowable CMMN 适配器
 * 封装 CMMN Case 运行态相关的 Flowable API 调用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlowableCmmnAdapter {

    private final CmmnRuntimeService cmmnRuntimeService;
    private final CmmnHistoryService cmmnHistoryService;
    // ==================== Case 实例查询 ====================

    /**
     * 分页查询 Case 实例
     */
    public Page<CaseInstance> queryCaseInstances(
            String caseDefinitionKey,
            String businessKey,
            String state,
            LocalDateTime startedAfter,
            Pageable pageable) {
        CaseInstanceQuery query = cmmnRuntimeService.createCaseInstanceQuery();

        if (caseDefinitionKey != null && !caseDefinitionKey.isEmpty()) {
            query.caseDefinitionKey(caseDefinitionKey);
        }
        if (businessKey != null && !businessKey.isEmpty()) {
            query.caseInstanceBusinessKey(businessKey);
        }
        if (state != null && !state.isEmpty()) {
            query.caseInstanceState(state);
        }
        if (startedAfter != null) {
            query.caseInstanceStartedAfter(toDate(startedAfter));
        }

        long total = query.count();
        List<CaseInstance> instances = query
                .orderByStartTime().desc()
                .listPage((int) pageable.getOffset(), pageable.getPageSize());

        return new PageImpl<>(instances, pageable, total);
    }

    /**
     * 根据 ID 获取 Case 实例
     */
    public CaseInstance getCaseInstance(String caseInstanceId) {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstanceId)
                .singleResult();

        if (caseInstance == null) {
            log.warn("Case instance not found: {}", caseInstanceId);
        }

        return caseInstance;
    }

    /**
     * 获取 Case 实例变量
     */
    public Map<String, Object> getCaseVariables(String caseInstanceId) {
        return cmmnRuntimeService.getVariables(caseInstanceId);
    }

    /**
     * 统计 Case 实例数量(按状态)
     */
    public Map<String, Long> countCaseInstancesByState() {
        Map<String, Long> statistics = new HashMap<>();

        statistics.put("ACTIVE", cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceState("active")
                .count());

        statistics.put("COMPLETED", cmmnHistoryService.createHistoricCaseInstanceQuery()
                .finished()
                .count());

        // TODO: Flowable 7.x CMMN API - 确认如何查询 terminated 状态
        statistics.put("TERMINATED", 0L);

        return statistics;
    }

    // ==================== Plan Item 查询 ====================

    /**
     * 获取 Case 的 Plan Item Tree
     */
    public PlanItemTreeNode getCasePlanItemTree(String caseInstanceId) {
        List<PlanItemInstance> planItems = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstanceId)
                .orderByCreateTime().asc()
                .list();

        if (planItems.isEmpty()) {
            log.warn("No plan items found for case instance: {}", caseInstanceId);
            return null;
        }

        return buildPlanItemTree(planItems);
    }

    /**
     * 构建 Plan Item 树结构
     */
    private PlanItemTreeNode buildPlanItemTree(List<PlanItemInstance> planItems) {
        // 按 Stage ID 分组
        Map<String, List<PlanItemInstance>> itemsByStage = planItems.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getStageInstanceId() != null ? item.getStageInstanceId() : "root"));

        // 查找根节点(通常是 Case Plan Model)
        PlanItemInstance rootItem = planItems.stream()
                .filter(item -> item.getStageInstanceId() == null)
                .findFirst()
                .orElse(planItems.get(0));

        return buildTreeNode(rootItem, itemsByStage);
    }

    /**
     * 递归构建树节点
     */
    private PlanItemTreeNode buildTreeNode(
            PlanItemInstance planItem,
            Map<String, List<PlanItemInstance>> itemsByStage) {
        PlanItemTreeNode node = PlanItemTreeNode.builder()
                .id(planItem.getId())
                .name(planItem.getName())
                .elementId(planItem.getElementId())
                .type(planItem.getPlanItemDefinitionType())
                .state(planItem.getState())
                .createTime(toLocalDateTime(planItem.getCreateTime()))
                .completedTime(toLocalDateTime(planItem.getCompletedTime()))
                .terminatedTime(toLocalDateTime(planItem.getTerminatedTime()))
                .children(new ArrayList<>())
                .build();

        // 查找子节点
        List<PlanItemInstance> children = itemsByStage.getOrDefault(planItem.getId(), Collections.emptyList());
        for (PlanItemInstance child : children) {
            node.getChildren().add(buildTreeNode(child, itemsByStage));
        }

        return node;
    }

    /**
     * 统计 Plan Item 数量
     */
    public Map<String, Integer> countPlanItems(String caseInstanceId) {
        Map<String, Integer> counts = new HashMap<>();

        counts.put("active", (int) cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstanceId)
                .planItemInstanceStateActive()
                .count());

        counts.put("available", (int) cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstanceId)
                .planItemInstanceStateAvailable()
                .count());

        counts.put("completed", (int) cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstanceId)
                .planItemInstanceStateCompleted()
                .count());

        counts.put("terminated", (int) cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstanceId)
                .planItemInstanceStateTerminated()
                .count());

        return counts;
    }

    // ==================== Case 操作 ====================

    /**
     * 终止 Case 实例
     */
    public void terminateCase(String caseInstanceId, String reason) {
        log.info("Terminating case instance: {}, reason: {}", caseInstanceId, reason);

        // 保存终止原因到变量
        if (reason != null && !reason.isEmpty()) {
            cmmnRuntimeService.setVariable(caseInstanceId, "terminationReason", reason);
        }

        cmmnRuntimeService.terminateCaseInstance(caseInstanceId);
        log.info("Case instance terminated: {}", caseInstanceId);
    }

    /**
     * 挂起 Case 实例
     * TODO: Flowable 7.x CMMN API 需要确认正确的挂起方法
     */
    public void suspendCase(String caseInstanceId) {
        log.info("Suspending case instance: {}", caseInstanceId);
        // TODO: 确认 Flowable 7.x 的正确 API
        // cmmnRuntimeService.suspendCaseInstance(caseInstanceId);
        throw new UnsupportedOperationException("Suspend case not yet implemented for Flowable 7.x");
    }

    /**
     * 恢复 Case 实例
     * TODO: Flowable 7.x CMMN API 需要确认正确的恢复方法
     */
    public void resumeCase(String caseInstanceId) {
        log.info("Resuming case instance: {}", caseInstanceId);
        // TODO: 确认 Flowable 7.x 的正确 API
        // cmmnRuntimeService.activateCaseInstance(caseInstanceId);
        throw new UnsupportedOperationException("Resume case not yet implemented for Flowable 7.x");
    }

    /**
     * 手动触发 Plan Item
     */
    public void triggerPlanItem(String planItemInstanceId) {
        log.info("Triggering plan item: {}", planItemInstanceId);
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstanceId);
        log.info("Plan item triggered: {}", planItemInstanceId);
    }

    // ==================== 历史查询 ====================

    /**
     * 查询历史 Case 实例
     */
    public Page<HistoricCaseInstance> queryHistoricCaseInstances(
            String caseDefinitionKey,
            String businessKey,
            Boolean finished,
            Pageable pageable) {
        var query = cmmnHistoryService.createHistoricCaseInstanceQuery();

        if (caseDefinitionKey != null && !caseDefinitionKey.isEmpty()) {
            query.caseDefinitionKey(caseDefinitionKey);
        }
        if (businessKey != null && !businessKey.isEmpty()) {
            query.caseInstanceBusinessKey(businessKey);
        }
        if (finished != null) {
            if (finished) {
                query.finished();
            } else {
                query.unfinished();
            }
        }

        long total = query.count();
        List<HistoricCaseInstance> instances = query
                .orderByStartTime().desc()
                .listPage((int) pageable.getOffset(), pageable.getPageSize());

        return new PageImpl<>(instances, pageable, total);
    }

    // ==================== 工具方法 ====================

    /**
     * LocalDateTime 转 Date
     */
    private Date toDate(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * Date 转 LocalDateTime
     */
    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
