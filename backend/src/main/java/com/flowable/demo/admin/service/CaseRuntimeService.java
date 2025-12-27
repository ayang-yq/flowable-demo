package com.flowable.demo.admin.service;

import com.flowable.demo.admin.adapter.FlowableBpmnAdapter;
import com.flowable.demo.admin.adapter.FlowableCmmnAdapter;
import com.flowable.demo.admin.adapter.FlowableRepositoryAdapter;
import com.flowable.demo.admin.model.PlanItemTreeNode;
import com.flowable.demo.admin.web.dto.ActivityStateDTO;
import com.flowable.demo.admin.web.dto.BpmnSubprocessVisualizationDTO;
import com.flowable.demo.admin.web.dto.CaseInstanceDTO;
import com.flowable.demo.admin.web.dto.CmmnCaseVisualizationDTO;
import com.flowable.demo.admin.web.dto.PlanItemStateDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ActivityInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.image.impl.DefaultProcessDiagramGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
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
    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final RepositoryService repositoryService;
    private final ProcessEngine processEngine;

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

    // ==================== BPMN 子流程可视化 ====================

    /**
     * 获取 BPMN 子流程可视化数据
     * 用于在 CMMN 可视化中展开显示 processTask 对应的 BPMN 流程
     * 使用 Flowable 的 ProcessDiagramGenerator API 直接生成带状态高亮的流程图
     *
     * @param planItemInstanceId PlanItem 实例 ID（processTask 实例）
     * @return BPMN 子流程可视化 DTO
     */
    public BpmnSubprocessVisualizationDTO getSubprocessVisualizationData(String planItemInstanceId) {
        log.info("Getting BPMN subprocess visualization for plan item: {}", planItemInstanceId);

        // 1. 获取 PlanItem 实例
        PlanItemInstance planItem = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceId(planItemInstanceId)
                .singleResult();

        if (planItem == null) {
            log.warn("PlanItem instance not found: {}", planItemInstanceId);
            return null;
        }

        // 检查是否为 processTask
        if (!"processtask".equals(planItem.getPlanItemDefinitionType())) {
            log.warn("PlanItem is not a processTask: {}", planItem.getPlanItemDefinitionType());
            return null;
        }

        // 2. 获取关联的 Process 实例
        Date planItemCreateTime = planItem.getCreateTime();
        log.info("PlanItem: id={}, type={}, caseInstanceId={}, createTime={}", 
                planItem.getId(), planItem.getPlanItemDefinitionType(), 
                planItem.getCaseInstanceId(), planItemCreateTime);

        // 方法1：尝试通过 superProcessInstanceId（caseInstanceId）查询
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .superProcessInstanceId(planItem.getCaseInstanceId())
                .singleResult();

        if (processInstance != null) {
            log.info("Found process instance via superProcessInstanceId: id={}, startTime={}", 
                    processInstance.getId(), processInstance.getStartTime());
        } else {
            log.info("No process instance found via superProcessInstanceId");
        }

        // 方法2：如果没有找到，尝试通过变量 caseInstanceId 关联
        if (processInstance == null) {
            List<ProcessInstance> byVariable = runtimeService.createProcessInstanceQuery()
                    .variableValueEquals("caseInstanceId", planItem.getCaseInstanceId())
                    .orderByStartTime()
                    .desc()
                    .list();
            log.info("Found {} process instances by variable caseInstanceId", byVariable.size());
            processInstance = byVariable.stream().findFirst().orElse(null);
        }

        // 方法3：如果仍然没有找到，尝试按时间匹配
        if (processInstance == null) {
            // 获取 processTask 创建时间前后的 process 实例
            List<ProcessInstance> allInstances = runtimeService.createProcessInstanceQuery()
                    .orderByStartTime()
                    .desc()
                    .list();
            
            log.info("All process instances count: {}", allInstances.size());
            for (ProcessInstance p : allInstances) {
                long diff = Math.abs(planItemCreateTime.getTime() - p.getStartTime().getTime());
                log.info("  Process: id={}, startTime={}, diff={}ms", 
                        p.getId(), p.getStartTime(), diff);
            }
            
            // 找到最接近 planItem 创建时间的实例（30秒内）
            processInstance = allInstances.stream()
                    .filter(p -> {
                        Date processTime = p.getStartTime();
                        long diff = Math.abs(planItemCreateTime.getTime() - processTime.getTime());
                        boolean match = diff < 30000; // 30秒内
                        if (match) {
                            log.info("  Matched process: id={}, startTime={}, diff={}ms", 
                                    p.getId(), p.getStartTime(), diff);
                        }
                        return match;
                    })
                    .findFirst()
                    .orElse(null);
        }

        if (processInstance == null) {
            // 尝试从历史查询
            org.flowable.engine.history.HistoricProcessInstance historicProcessInstance =
                    historyService.createHistoricProcessInstanceQuery()
                            .superProcessInstanceId(planItem.getCaseInstanceId())
                            .singleResult();
            
            if (historicProcessInstance == null) {
                historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                        .variableValueEquals("caseInstanceId", planItem.getCaseInstanceId())
                        .orderByProcessInstanceStartTime()
                        .desc()
                        .list()
                        .stream()
                        .findFirst()
                        .orElse(null);
            }
            
            if (historicProcessInstance == null) {
                // 最后尝试：按时间匹配历史实例
                List<org.flowable.engine.history.HistoricProcessInstance> historicInstances =
                        historyService.createHistoricProcessInstanceQuery()
                                .orderByProcessInstanceStartTime()
                                .desc()
                                .list();
                
                historicProcessInstance = historicInstances.stream()
                        .filter(p -> {
                            Date planItemTime = planItem.getCreateTime();
                            Date processTime = p.getStartTime();
                            long diff = Math.abs(planItemTime.getTime() - processTime.getTime());
                            return diff < 30000; // 30秒内
                        })
                        .findFirst()
                        .orElse(null);
                
                if (historicProcessInstance == null) {
                    log.error("Process instance not found for planItem: {}", planItemInstanceId);
                    return null;
                }
            }
            
            return buildHistoricSubprocessVisualization(historicProcessInstance);
        }

        // 3. 获取流程定义
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(processInstance.getProcessDefinitionId())
                .singleResult();

        // 4. 获取BPMN XML
        String bpmnXml = repositoryAdapter.getProcessDefinitionResourceContent(
                processDefinition.getDeploymentId(),
                processDefinition.getResourceName()
        );

        // 5. 获取活动节点状态列表
        List<ActivityStateDTO> activityStates = getActivityStates(processInstance.getId());

        return BpmnSubprocessVisualizationDTO.builder()
                .processInstanceId(processInstance.getId())
                .processDefinitionId(processInstance.getProcessDefinitionId())
                .processDefinitionKey(processInstance.getProcessDefinitionKey())
                .processDefinitionName(processDefinition.getName())
                .bpmnXml(bpmnXml)
                .activityStates(activityStates)
                .processInstanceState(processInstance.isSuspended() ? "suspended" : "active")
                .startTime(formatDateTime(processInstance.getStartTime()))
                .endTime(null)
                .build();
    }

    /**
     * 获取活动节点状态
     */
    private List<ActivityStateDTO> getActivityStates(String processInstanceId) {
        // 获取运行态活动
        List<ActivityInstance> runtimeActivities = runtimeService.createActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .list();

        // 获取历史态活动
        List<HistoricActivityInstance> historicActivities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime()
                .asc()
                .list();

        Map<String, ActivityStateDTO> activityStateMap = new java.util.HashMap<>();

        // 添加运行态活动（状态为 active）
        for (ActivityInstance activity : runtimeActivities) {
            activityStateMap.put(activity.getActivityId(),
                    ActivityStateDTO.builder()
                            .activityId(activity.getActivityId())
                            .activityName(activity.getActivityName())
                            .activityType(activity.getActivityType())
                            .state("active")
                            .processInstanceId(activity.getProcessInstanceId())
                            .startTime(formatDateTime(activity.getStartTime()))
                            .endTime(null)
                            .build());
        }

        // 添加历史态活动（状态为 completed，排除运行态中已存在的）
        for (HistoricActivityInstance historic : historicActivities) {
            if (!activityStateMap.containsKey(historic.getActivityId())) {
                activityStateMap.put(historic.getActivityId(),
                        ActivityStateDTO.builder()
                                .activityId(historic.getActivityId())
                                .activityName(historic.getActivityName())
                                .activityType(historic.getActivityType())
                                .state("completed")
                                .processInstanceId(historic.getProcessInstanceId())
                                .startTime(formatDateTime(historic.getStartTime()))
                                .endTime(formatDateTime(historic.getEndTime()))
                                .build());
            }
        }

        return new java.util.ArrayList<>(activityStateMap.values());
    }

    /**
     * 构建历史子流程可视化数据
     */
    private BpmnSubprocessVisualizationDTO buildHistoricSubprocessVisualization(
            org.flowable.engine.history.HistoricProcessInstance historicProcessInstance) {
        
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(historicProcessInstance.getProcessDefinitionId())
                .singleResult();

        String bpmnXml = repositoryAdapter.getProcessDefinitionResourceContent(
                processDefinition.getDeploymentId(),
                processDefinition.getResourceName()
        );

        List<ActivityStateDTO> activityStates = getHistoricActivityStates(
                historicProcessInstance.getId());

        return BpmnSubprocessVisualizationDTO.builder()
                .processInstanceId(historicProcessInstance.getId())
                .processDefinitionId(historicProcessInstance.getProcessDefinitionId())
                .processDefinitionKey(historicProcessInstance.getProcessDefinitionKey())
                .processDefinitionName(processDefinition.getName())
                .bpmnXml(bpmnXml)
                .activityStates(activityStates)
                .processInstanceState("completed")
                .startTime(formatDateTime(historicProcessInstance.getStartTime()))
                .endTime(formatDateTime(historicProcessInstance.getEndTime()))
                .build();
    }

    /**
     * 获取历史活动状态
     */
    private List<ActivityStateDTO> getHistoricActivityStates(String processInstanceId) {
        List<HistoricActivityInstance> historicActivities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime()
                .asc()
                .list();

        return historicActivities.stream()
                .map(historic -> ActivityStateDTO.builder()
                        .activityId(historic.getActivityId())
                        .activityName(historic.getActivityName())
                        .activityType(historic.getActivityType())
                        .state("completed")
                        .processInstanceId(historic.getProcessInstanceId())
                        .startTime(formatDateTime(historic.getStartTime()))
                        .endTime(formatDateTime(historic.getEndTime()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 格式化日期时间
     */
    private String formatDateTime(Date date) {
        if (date == null) {
            return null;
        }
        LocalDateTime localDateTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        return localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    // ==================== BPMN 流程图生成 ====================

    /**
     * 获取 BPMN 子流程流程图 SVG
     * 使用 Flowable ProcessDiagramGenerator 生成带状态高亮的流程图
     *
     * @param planItemInstanceId PlanItem 实例 ID（processTask 实例）
     * @return 流程图 SVG 字符串或 base64 PNG
     */
    public String getSubprocessDiagramSvg(String planItemInstanceId) {
        log.info("Generating BPMN subprocess diagram for plan item: {}", planItemInstanceId);

        // 1. 获取 PlanItem 实例
        PlanItemInstance planItem = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceId(planItemInstanceId)
                .singleResult();

        if (planItem == null) {
            log.warn("PlanItem instance not found: {}", planItemInstanceId);
            return null;
        }

        if (!"processtask".equals(planItem.getPlanItemDefinitionType())) {
            log.warn("PlanItem is not a processTask: {}", planItem.getPlanItemDefinitionType());
            return null;
        }

        // 2. 查找关联的 Process 实例
        Date planItemCreateTime = planItem.getCreateTime();
        ProcessInstance processInstance = null;

        // 尝试通过 superProcessInstanceId 查询
        processInstance = runtimeService.createProcessInstanceQuery()
                .superProcessInstanceId(planItem.getCaseInstanceId())
                .singleResult();

        // 如果没找到，尝试通过变量 caseInstanceId 关联
        if (processInstance == null) {
            processInstance = runtimeService.createProcessInstanceQuery()
                    .variableValueEquals("caseInstanceId", planItem.getCaseInstanceId())
                    .orderByStartTime()
                    .desc()
                    .list()
                    .stream()
                    .findFirst()
                    .orElse(null);
        }

        // 如果仍然没找到，尝试按时间匹配
        if (processInstance == null) {
            List<ProcessInstance> allInstances = runtimeService.createProcessInstanceQuery()
                    .orderByStartTime()
                    .desc()
                    .list();
            
            processInstance = allInstances.stream()
                    .filter(p -> {
                        Date processTime = p.getStartTime();
                        long diff = Math.abs(planItemCreateTime.getTime() - processTime.getTime());
                        return diff < 30000; // 30秒内
                    })
                    .findFirst()
                    .orElse(null);
        }

        // 如果是历史实例
        if (processInstance == null) {
            org.flowable.engine.history.HistoricProcessInstance historicProcessInstance =
                    historyService.createHistoricProcessInstanceQuery()
                            .superProcessInstanceId(planItem.getCaseInstanceId())
                            .singleResult();
            
            if (historicProcessInstance == null) {
                historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                        .variableValueEquals("caseInstanceId", planItem.getCaseInstanceId())
                        .orderByProcessInstanceStartTime()
                        .desc()
                        .list()
                        .stream()
                        .findFirst()
                        .orElse(null);
            }
            
            if (historicProcessInstance == null) {
                List<org.flowable.engine.history.HistoricProcessInstance> historicInstances =
                        historyService.createHistoricProcessInstanceQuery()
                                .orderByProcessInstanceStartTime()
                                .desc()
                                .list();
                
                historicProcessInstance = historicInstances.stream()
                        .filter(p -> {
                            Date planItemTime = planItem.getCreateTime();
                            Date processTime = p.getStartTime();
                            long diff = Math.abs(planItemTime.getTime() - processTime.getTime());
                            return diff < 30000;
                        })
                        .findFirst()
                        .orElse(null);
                
                if (historicProcessInstance == null) {
                    log.error("Process instance not found for planItem: {}", planItemInstanceId);
                    return null;
                }
            }
            
            return generateHistoricProcessDiagramSvg(historicProcessInstance);
        }

        // 3. 生成流程图
        return generateProcessDiagramSvg(processInstance);
    }

    /**
     * 生成活动流程图的 SVG
     * 使用自定义字体支持中文
     */
    private String generateProcessDiagramSvg(ProcessInstance processInstance) {
        try {
            log.info("Generating process diagram SVG for process instance: {}", processInstance.getId());
            
            // 获取流程定义
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(processInstance.getProcessDefinitionId())
                    .singleResult();

            log.info("Process definition: {} ({})", processDefinition.getName(), processDefinition.getId());

            // 获取当前活动节点ID
            List<String> activeActivityIds = runtimeService.getActiveActivityIds(processInstance.getId());
            log.info("Active activity IDs: {}", activeActivityIds);

            // 获取历史活动
            List<HistoricActivityInstance> historicActivities = historyService
                    .createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .finished()
                    .orderByHistoricActivityInstanceEndTime().asc()
                    .list();
            
            List<String> completedActivityIds = historicActivities.stream()
                    .map(HistoricActivityInstance::getActivityId)
                    .distinct()
                    .collect(Collectors.toList());
            
            log.info("Completed activity IDs: {}", completedActivityIds);

            // 获取BPMN模型
            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());

            // 使用自定义字体生成流程图
            DefaultProcessDiagramGenerator generator = new DefaultProcessDiagramGenerator();
            
            // 使用 Microsoft YaHei 字体
            java.io.InputStream diagramStream = generator.generateDiagram(
                    bpmnModel,
                    "png",
                    activeActivityIds,
                    completedActivityIds,
                    "Microsoft YaHei",  // activity font
                    "Microsoft YaHei",  // label font
                    "Microsoft YaHei",  // annotation font
                    Thread.currentThread().getContextClassLoader(),
                    1.0,
                    true
            );

            if (diagramStream == null) {
                log.error("generateDiagram returned null");
                return null;
            }

            byte[] bytes = diagramStream.readAllBytes();
            log.info("Generated diagram bytes: {}", bytes.length);
            
            // 返回 base64 编码的 PNG data URL，前端可以显示
            String base64Png = java.util.Base64.getEncoder().encodeToString(bytes);
            return "data:image/png;base64," + base64Png;

        } catch (Exception e) {
            log.error("Failed to generate process diagram with Chinese font", e);
            return null;
        }
    }

    /**
     * 生成历史流程图的 PNG
     * 使用自定义字体支持中文
     */
    private String generateHistoricProcessDiagramSvg(
            org.flowable.engine.history.HistoricProcessInstance historicProcessInstance) {
        try {
            log.info("Generating historic process diagram for process instance: {}", historicProcessInstance.getId());
            
            // 获取流程定义
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(historicProcessInstance.getProcessDefinitionId())
                    .singleResult();

            log.info("Historic process definition: {} ({})", processDefinition.getName(), processDefinition.getId());

            // 获取历史活动
            List<HistoricActivityInstance> historicActivities = historyService
                    .createHistoricActivityInstanceQuery()
                    .processInstanceId(historicProcessInstance.getId())
                    .finished()
                    .orderByHistoricActivityInstanceEndTime().asc()
                    .list();
            
            List<String> completedActivityIds = historicActivities.stream()
                    .map(HistoricActivityInstance::getActivityId)
                    .distinct()
                    .collect(Collectors.toList());
            
            log.info("Historic completed activity IDs: {}", completedActivityIds);

            // 获取BPMN模型
            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());

            // 使用自定义字体生成流程图
            DefaultProcessDiagramGenerator generator = new DefaultProcessDiagramGenerator();
            
            // 使用 Microsoft YaHei 字体
            java.io.InputStream diagramStream = generator.generateDiagram(
                    bpmnModel,
                    "png",
                    Collections.emptyList(),  // 无活动节点
                    completedActivityIds,
                    "Microsoft YaHei",  // activity font
                    "Microsoft YaHei",  // label font
                    "Microsoft YaHei",  // annotation font
                    Thread.currentThread().getContextClassLoader(),
                    1.0,
                    true
            );

            if (diagramStream == null) {
                log.error("generateDiagram returned null");
                return null;
            }

            byte[] bytes = diagramStream.readAllBytes();
            log.info("Generated historic diagram bytes: {}", bytes.length);
            
            // 返回 base64 编码的 PNG data URL
            String base64Png = java.util.Base64.getEncoder().encodeToString(bytes);
            return "data:image/png;base64," + base64Png;

        } catch (Exception e) {
            log.error("Failed to generate historic process diagram with Chinese font", e);
            return null;
        }
    }
}
