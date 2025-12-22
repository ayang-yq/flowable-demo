package com.flowable.demo.admin.service;

import com.flowable.demo.admin.adapter.FlowableBpmnAdapter;
import com.flowable.demo.admin.adapter.ProcessDiagramHighlightData;
import com.flowable.demo.admin.web.dto.ProcessDiagramDTO;
import com.flowable.demo.admin.web.dto.ProcessInstanceDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.runtime.ProcessInstance;
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
 * Process 运行态管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProcessRuntimeService {

    private final FlowableBpmnAdapter bpmnAdapter;
    private final HistoryService historyService;

    // ==================== Process 查询 ====================

    /**
     * 分页查询 Process 实例
     */
    public Page<ProcessInstanceDTO> queryProcessInstances(
            String processDefinitionKey,
            String businessKey,
            LocalDateTime startedAfter,
            Pageable pageable) {
        Page<ProcessInstance> instances = bpmnAdapter.queryProcessInstances(
                processDefinitionKey,
                businessKey,
                startedAfter,
                pageable);

        return instances.map(this::convertToDTO);
    }

    /**
     * 获取 Process 实例详情
     */
    public ProcessInstanceDTO getProcessInstanceDetail(String processInstanceId) {
        ProcessInstance processInstance = bpmnAdapter.getProcessInstance(processInstanceId);

        if (processInstance == null) {
            log.warn("Process instance not found: {}", processInstanceId);
            return null;
        }

        // 获取变量
        Map<String, Object> variables = bpmnAdapter.getProcessVariables(processInstanceId);

        // 获取当前活动节点
        List<String> activeActivityIds = bpmnAdapter.getActiveActivityIds(processInstanceId);

        // 获取活动历史
        List<HistoricActivityInstance> historicActivities = historyService
                .createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime().asc()
                .list();

        // 当前活动节点详情
        List<ProcessInstanceDTO.ActivityInfo> currentActivities = historicActivities.stream()
                .filter(activity -> activeActivityIds.contains(activity.getActivityId()))
                .map(this::convertToActivityInfo)
                .collect(Collectors.toList());

        // 已完成的活动节点
        List<ProcessInstanceDTO.ActivityInfo> completedActivities = historicActivities.stream()
                .filter(activity -> activity.getEndTime() != null)
                .map(this::convertToActivityInfo)
                .collect(Collectors.toList());

        return ProcessInstanceDTO.builder()
                .id(processInstance.getId())
                .processDefinitionId(processInstance.getProcessDefinitionId())
                .processDefinitionKey(processInstance.getProcessDefinitionKey())
                .processDefinitionName(processInstance.getProcessDefinitionName())
                .processDefinitionVersion(processInstance.getProcessDefinitionVersion())
                .businessKey(processInstance.getBusinessKey())
                .state(processInstance.isSuspended() ? "SUSPENDED" : "ACTIVE")
                .startTime(toLocalDateTime(processInstance.getStartTime()))
                .startUserId(processInstance.getStartUserId())
                .tenantId(processInstance.getTenantId())
                .currentActivityIds(activeActivityIds)
                .variables(variables)
                .currentActivities(currentActivities)
                .completedActivities(completedActivities)
                .build();
    }

    /**
     * 获取流程图高亮数据
     */
    public ProcessDiagramDTO getProcessDiagram(String processInstanceId) {
        ProcessDiagramHighlightData highlightData = bpmnAdapter.getProcessDiagramHighlight(processInstanceId);

        if (highlightData == null) {
            return null;
        }

        return ProcessDiagramDTO.builder()
                .processDefinitionId(highlightData.getProcessDefinitionId())
                .diagramXml(highlightData.getDiagramXml())
                .highlightedActivities(highlightData.getHighlightedActivities())
                .completedActivities(highlightData.getCompletedActivities())
                .highlightedFlows(highlightData.getHighlightedFlows())
                .build();
    }

    // ==================== Process 操作 ====================

    /**
     * 终止 Process 实例
     */
    @Transactional
    public void terminateProcess(String processInstanceId, String reason) {
        log.info("Terminating process instance: {}, reason: {}", processInstanceId, reason);
        bpmnAdapter.terminateProcess(processInstanceId, reason);
    }

    /**
     * 挂起 Process 实例
     */
    @Transactional
    public void suspendProcess(String processInstanceId) {
        log.info("Suspending process instance: {}", processInstanceId);
        bpmnAdapter.suspendProcess(processInstanceId);
    }

    /**
     * 恢复 Process 实例
     */
    @Transactional
    public void resumeProcess(String processInstanceId) {
        log.info("Resuming process instance: {}", processInstanceId);
        bpmnAdapter.resumeProcess(processInstanceId);
    }

    // ==================== 工具方法 ====================

    /**
     * 转换为 DTO(列表用)
     */
    private ProcessInstanceDTO convertToDTO(ProcessInstance processInstance) {
        List<String> activeActivityIds = bpmnAdapter.getActiveActivityIds(processInstance.getId());

        return ProcessInstanceDTO.builder()
                .id(processInstance.getId())
                .processDefinitionId(processInstance.getProcessDefinitionId())
                .processDefinitionKey(processInstance.getProcessDefinitionKey())
                .processDefinitionName(processInstance.getProcessDefinitionName())
                .processDefinitionVersion(processInstance.getProcessDefinitionVersion())
                .businessKey(processInstance.getBusinessKey())
                .state(processInstance.isSuspended() ? "SUSPENDED" : "ACTIVE")
                .startTime(toLocalDateTime(processInstance.getStartTime()))
                .startUserId(processInstance.getStartUserId())
                .tenantId(processInstance.getTenantId())
                .currentActivityIds(activeActivityIds)
                .build();
    }

    /**
     * 转换活动实例为 ActivityInfo
     */
    private ProcessInstanceDTO.ActivityInfo convertToActivityInfo(HistoricActivityInstance activity) {
        Long duration = null;
        if (activity.getEndTime() != null && activity.getStartTime() != null) {
            duration = activity.getDurationInMillis();
        }

        return ProcessInstanceDTO.ActivityInfo.builder()
                .activityId(activity.getActivityId())
                .activityName(activity.getActivityName())
                .activityType(activity.getActivityType())
                .startTime(toLocalDateTime(activity.getStartTime()))
                .endTime(toLocalDateTime(activity.getEndTime()))
                .assignee(activity.getAssignee())
                .durationInMillis(duration)
                .build();
    }

    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
