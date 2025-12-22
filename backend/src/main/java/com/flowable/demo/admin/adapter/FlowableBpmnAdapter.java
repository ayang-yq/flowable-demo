package com.flowable.demo.admin.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Flowable BPMN 适配器
 * 封装 BPMN Process 运行态相关的 Flowable API 调用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlowableBpmnAdapter {

    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final RepositoryService repositoryService;

    // ==================== Process 实例查询 ====================

    /**
     * 分页查询 Process 实例
     */
    public Page<ProcessInstance> queryProcessInstances(
            String processDefinitionKey,
            String businessKey,
            LocalDateTime startedAfter,
            Pageable pageable) {
        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();

        if (processDefinitionKey != null && !processDefinitionKey.isEmpty()) {
            query.processDefinitionKey(processDefinitionKey);
        }
        if (businessKey != null && !businessKey.isEmpty()) {
            query.processInstanceBusinessKey(businessKey);
        }
        if (startedAfter != null) {
            query.startedAfter(toDate(startedAfter));
        }

        long total = query.count();
        List<ProcessInstance> instances = query
                .orderByStartTime().desc()
                .listPage((int) pageable.getOffset(), pageable.getPageSize());

        return new PageImpl<>(instances, pageable, total);
    }

    /**
     * 根据 ID 获取 Process 实例
     */
    public ProcessInstance getProcessInstance(String processInstanceId) {
        return runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
    }

    /**
     * 获取 Process 实例变量
     */
    public Map<String, Object> getProcessVariables(String processInstanceId) {
        return runtimeService.getVariables(processInstanceId);
    }

    /**
     * 获取当前活动节点 ID 列表
     */
    public List<String> getActiveActivityIds(String processInstanceId) {
        return runtimeService.getActiveActivityIds(processInstanceId);
    }

    /**
     * 统计 Process 实例数量
     */
    public Map<String, Long> countProcessInstances() {
        Map<String, Long> statistics = new HashMap<>();

        statistics.put("ACTIVE", runtimeService.createProcessInstanceQuery()
                .active()
                .count());

        statistics.put("SUSPENDED", runtimeService.createProcessInstanceQuery()
                .suspended()
                .count());

        statistics.put("COMPLETED", historyService.createHistoricProcessInstanceQuery()
                .finished()
                .count());

        return statistics;
    }

    // ==================== 流程图高亮数据 ====================

    /**
     * 获取流程图高亮数据
     */
    public ProcessDiagramHighlightData getProcessDiagramHighlight(String processInstanceId) {
        ProcessInstance processInstance = getProcessInstance(processInstanceId);

        if (processInstance == null) {
            // 查询历史实例
            return getHistoricProcessDiagramHighlight(processInstanceId);
        }

        return getActiveProcessDiagramHighlight(processInstance);
    }

    /**
     * 获取活动流程的高亮数据
     */
    private ProcessDiagramHighlightData getActiveProcessDiagramHighlight(ProcessInstance processInstance) {
        String processDefinitionId = processInstance.getProcessDefinitionId();

        // 当前活动节点
        List<String> activeActivityIds = runtimeService.getActiveActivityIds(processInstance.getId());

        // 已完成的活动节点
        List<HistoricActivityInstance> completedActivities = historyService
                .createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .finished()
                .orderByHistoricActivityInstanceEndTime().asc()
                .list();

        List<String> completedActivityIds = completedActivities.stream()
                .map(HistoricActivityInstance::getActivityId)
                .distinct()
                .collect(Collectors.toList());

        // 高亮的 Sequence Flow
        List<String> highlightedFlows = getHighlightedFlows(
                processDefinitionId,
                completedActivities);

        // 获取流程图 XML
        String diagramXml = getProcessDiagramXml(processDefinitionId);

        return ProcessDiagramHighlightData.builder()
                .processDefinitionId(processDefinitionId)
                .diagramXml(diagramXml)
                .highlightedActivities(activeActivityIds)
                .completedActivities(completedActivityIds)
                .highlightedFlows(highlightedFlows)
                .build();
    }

    /**
     * 获取历史流程的高亮数据
     */
    private ProcessDiagramHighlightData getHistoricProcessDiagramHighlight(String processInstanceId) {
        HistoricProcessInstance historicInstance = historyService
                .createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (historicInstance == null) {
            log.warn("Process instance not found: {}", processInstanceId);
            return null;
        }

        String processDefinitionId = historicInstance.getProcessDefinitionId();

        // 所有已完成的活动节点
        List<HistoricActivityInstance> completedActivities = historyService
                .createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .finished()
                .orderByHistoricActivityInstanceEndTime().asc()
                .list();

        List<String> completedActivityIds = completedActivities.stream()
                .map(HistoricActivityInstance::getActivityId)
                .distinct()
                .collect(Collectors.toList());

        // 高亮的 Sequence Flow
        List<String> highlightedFlows = getHighlightedFlows(
                processDefinitionId,
                completedActivities);

        // 获取流程图 XML
        String diagramXml = getProcessDiagramXml(processDefinitionId);

        return ProcessDiagramHighlightData.builder()
                .processDefinitionId(processDefinitionId)
                .diagramXml(diagramXml)
                .highlightedActivities(Collections.emptyList()) // 历史流程没有活动节点
                .completedActivities(completedActivityIds)
                .highlightedFlows(highlightedFlows)
                .build();
    }

    /**
     * 获取流程图 XML 内容
     */
    private String getProcessDiagramXml(String processDefinitionId) {
        ProcessDefinition processDefinition = repositoryService.getProcessDefinition(processDefinitionId);

        try {
            InputStream inputStream = repositoryService.getResourceAsStream(
                    processDefinition.getDeploymentId(),
                    processDefinition.getResourceName());

            byte[] bytes = inputStream.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to read process diagram XML for: {}", processDefinitionId, e);
            return null;
        }
    }

    /**
     * 计算高亮的 Sequence Flow
     */
    private List<String> getHighlightedFlows(
            String processDefinitionId,
            List<HistoricActivityInstance> completedActivities) {
        if (completedActivities.isEmpty()) {
            return Collections.emptyList();
        }

        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        List<String> highlightedFlows = new ArrayList<>();

        // 基于活动的执行顺序,推断已执行的 Sequence Flow
        for (int i = 0; i < completedActivities.size() - 1; i++) {
            HistoricActivityInstance current = completedActivities.get(i);
            HistoricActivityInstance next = completedActivities.get(i + 1);

            // 查找连接这两个活动的 Sequence Flow
            String flowId = findSequenceFlowBetween(
                    bpmnModel,
                    current.getActivityId(),
                    next.getActivityId());

            if (flowId != null) {
                highlightedFlows.add(flowId);
            }
        }

        return highlightedFlows.stream().distinct().collect(Collectors.toList());
    }

    /**
     * 查找两个活动之间的 Sequence Flow
     */
    private String findSequenceFlowBetween(
            BpmnModel bpmnModel,
            String sourceActivityId,
            String targetActivityId) {
        FlowElement sourceElement = bpmnModel.getFlowElement(sourceActivityId);

        if (sourceElement == null) {
            return null;
        }

        // 获取源节点的所有出口 Sequence Flow
        Collection<FlowElement> flowElements = bpmnModel.getMainProcess().getFlowElements();

        for (FlowElement flowElement : flowElements) {
            if (flowElement instanceof SequenceFlow) {
                SequenceFlow sequenceFlow = (SequenceFlow) flowElement;

                if (sequenceFlow.getSourceRef().equals(sourceActivityId) &&
                        sequenceFlow.getTargetRef().equals(targetActivityId)) {
                    return sequenceFlow.getId();
                }
            }
        }

        return null;
    }

    // ==================== Process 操作 ====================

    /**
     * 终止 Process 实例
     */
    public void terminateProcess(String processInstanceId, String reason) {
        log.info("Terminating process instance: {}, reason: {}", processInstanceId, reason);

        runtimeService.deleteProcessInstance(processInstanceId, reason);
        log.info("Process instance terminated: {}", processInstanceId);
    }

    /**
     * 挂起 Process 实例
     */
    public void suspendProcess(String processInstanceId) {
        log.info("Suspending process instance: {}", processInstanceId);
        runtimeService.suspendProcessInstanceById(processInstanceId);
        log.info("Process instance suspended: {}", processInstanceId);
    }

    /**
     * 恢复 Process 实例
     */
    public void resumeProcess(String processInstanceId) {
        log.info("Resuming process instance: {}", processInstanceId);
        runtimeService.activateProcessInstanceById(processInstanceId);
        log.info("Process instance resumed: {}", processInstanceId);
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
