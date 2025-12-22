package com.flowable.demo.admin.service;

import com.flowable.demo.admin.adapter.FlowableBpmnAdapter;
import com.flowable.demo.admin.adapter.FlowableCmmnAdapter;
import com.flowable.demo.admin.adapter.FlowableRepositoryAdapter;
import com.flowable.demo.admin.web.dto.AdminStatisticsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.dmn.api.DmnDeployment;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Admin 统计服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatisticsService {

    private final FlowableRepositoryAdapter repositoryAdapter;
    private final FlowableCmmnAdapter cmmnAdapter;
    private final FlowableBpmnAdapter bpmnAdapter;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 获取系统统计信息
     */
    public AdminStatisticsDTO getStatistics() {
        // 模型统计
        AdminStatisticsDTO.ModelStatistics modelStats = getModelStatistics();

        // 部署统计
        AdminStatisticsDTO.DeploymentStatistics deploymentStats = getDeploymentStatistics();

        // Case 统计
        Map<String, Long> caseStats = cmmnAdapter.countCaseInstancesByState();

        // Process 统计
        Map<String, Long> processStats = bpmnAdapter.countProcessInstances();

        return AdminStatisticsDTO.builder()
                .models(modelStats)
                .deployments(deploymentStats)
                .cases(caseStats)
                .processes(processStats)
                .build();
    }

    /**
     * 获取模型统计
     */
    private AdminStatisticsDTO.ModelStatistics getModelStatistics() {
        List<CaseDefinition> caseDefinitions = repositoryAdapter.getAllCaseDefinitions();
        List<ProcessDefinition> processDefinitions = repositoryAdapter.getAllProcessDefinitions();
        List<DmnDeployment> dmnDeployments = repositoryAdapter.getDmnDeployments();

        // 按 Key 去重统计
        long cmmnCount = caseDefinitions.stream()
                .map(CaseDefinition::getKey)
                .distinct()
                .count();

        long bpmnCount = processDefinitions.stream()
                .map(ProcessDefinition::getKey)
                .distinct()
                .count();

        // For DMN, count deployments since DMN doesn't have definitions in the same way
        long dmnCount = dmnDeployments.size();

        log.info("Model statistics - CMMN: {}, BPMN: {}, DMN: {}", cmmnCount, bpmnCount, dmnCount);

        return AdminStatisticsDTO.ModelStatistics.builder()
                .total(cmmnCount + bpmnCount + dmnCount)
                .cmmn(cmmnCount)
                .bpmn(bpmnCount)
                .dmn(dmnCount)
                .build();
    }

    /**
     * 获取部署统计
     */
    private AdminStatisticsDTO.DeploymentStatistics getDeploymentStatistics() {
        List<CmmnDeployment> cmmnDeployments = repositoryAdapter.getCmmnDeployments();
        List<Deployment> bpmnDeployments = repositoryAdapter.getBpmnDeployments();
        List<DmnDeployment> dmnDeployments = repositoryAdapter.getDmnDeployments();

        long total = cmmnDeployments.size() + bpmnDeployments.size() + dmnDeployments.size();

        // 查找最新部署时间
        LocalDateTime lastCmmnDeployment = cmmnDeployments.stream()
                .map(CmmnDeployment::getDeploymentTime)
                .max(Comparator.naturalOrder())
                .map(this::toLocalDateTime)
                .orElse(null);

        LocalDateTime lastBpmnDeployment = bpmnDeployments.stream()
                .map(Deployment::getDeploymentTime)
                .max(Comparator.naturalOrder())
                .map(this::toLocalDateTime)
                .orElse(null);

        LocalDateTime lastDmnDeployment = dmnDeployments.stream()
                .map(DmnDeployment::getDeploymentTime)
                .max(Comparator.naturalOrder())
                .map(this::toLocalDateTime)
                .orElse(null);

        LocalDateTime lastDeployment = null;
        if (lastCmmnDeployment != null && lastBpmnDeployment != null && lastDmnDeployment != null) {
            lastDeployment = lastCmmnDeployment.isAfter(lastBpmnDeployment)
                    ? (lastCmmnDeployment.isAfter(lastDmnDeployment) ? lastCmmnDeployment : lastDmnDeployment)
                    : (lastBpmnDeployment.isAfter(lastDmnDeployment) ? lastBpmnDeployment : lastDmnDeployment);
        } else if (lastCmmnDeployment != null && lastBpmnDeployment != null) {
            lastDeployment = lastCmmnDeployment.isAfter(lastBpmnDeployment) ? lastCmmnDeployment : lastBpmnDeployment;
        } else if (lastCmmnDeployment != null && lastDmnDeployment != null) {
            lastDeployment = lastCmmnDeployment.isAfter(lastDmnDeployment) ? lastCmmnDeployment : lastDmnDeployment;
        } else if (lastBpmnDeployment != null && lastDmnDeployment != null) {
            lastDeployment = lastBpmnDeployment.isAfter(lastDmnDeployment) ? lastBpmnDeployment : lastDmnDeployment;
        } else if (lastCmmnDeployment != null) {
            lastDeployment = lastCmmnDeployment;
        } else if (lastBpmnDeployment != null) {
            lastDeployment = lastBpmnDeployment;
        } else if (lastDmnDeployment != null) {
            lastDeployment = lastDmnDeployment;
        }

        String lastDeploymentTime = lastDeployment != null
                ? lastDeployment.format(FORMATTER)
                : null;

        return AdminStatisticsDTO.DeploymentStatistics.builder()
                .total(total)
                .lastDeploymentTime(lastDeploymentTime)
                .build();
    }

    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
