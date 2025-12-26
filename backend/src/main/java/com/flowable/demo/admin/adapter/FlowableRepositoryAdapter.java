package com.flowable.demo.admin.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.api.repository.CmmnDeploymentBuilder;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.api.DmnDecision;
import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.api.DmnDeploymentBuilder;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * Flowable Repository 适配器
 * 封装模型和部署相关的 Flowable API 调用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlowableRepositoryAdapter {

    private final RepositoryService repositoryService;
    private final CmmnRepositoryService cmmnRepositoryService;
    private final DmnRepositoryService dmnRepositoryService;

    // ==================== CMMN 相关 ====================

    /**
     * 获取所有 CMMN Case 定义
     */
    public List<CaseDefinition> getAllCaseDefinitions() {
        return cmmnRepositoryService.createCaseDefinitionQuery()
                .orderByCaseDefinitionVersion().desc()
                .list();
    }

    /**
     * 根据 Key 获取 Case 定义的所有版本
     */
    public List<CaseDefinition> getCaseDefinitionVersions(String caseDefinitionKey) {
        return cmmnRepositoryService.createCaseDefinitionQuery()
                .caseDefinitionKey(caseDefinitionKey)
                .orderByCaseDefinitionVersion().desc()
                .list();
    }

    /**
     * 获取最新版本的 Case 定义
     */
    public CaseDefinition getLatestCaseDefinition(String caseDefinitionKey) {
        return cmmnRepositoryService.createCaseDefinitionQuery()
                .caseDefinitionKey(caseDefinitionKey)
                .latestVersion()
                .singleResult();
    }

    /**
     * 获取 CMMN Model
     */
    public CmmnModel getCmmnModel(String caseDefinitionId) {
        return cmmnRepositoryService.getCmmnModel(caseDefinitionId);
    }

    /**
     * 部署 CMMN 资源
     */
    public CmmnDeployment deployCmmnResource(
            String resourceName,
            byte[] resourceBytes,
            String deploymentName,
            String tenantId) {
        log.info("Deploying CMMN resource: {}, deploymentName: {}, tenantId: {}",
                resourceName, deploymentName, tenantId);

        CmmnDeploymentBuilder builder = cmmnRepositoryService.createDeployment()
                .name(deploymentName)
                .addBytes(resourceName, resourceBytes);

        if (tenantId != null && !tenantId.isEmpty()) {
            builder.tenantId(tenantId);
        }

        CmmnDeployment deployment = builder.deploy();
        log.info("CMMN deployment successful: {}", deployment.getId());

        return deployment;
    }

    /**
     * 获取 CMMN 部署列表
     */
    public List<CmmnDeployment> getCmmnDeployments() {
        return cmmnRepositoryService.createDeploymentQuery()
                .orderByDeploymentTime().desc()
                .list();
    }

    /**
     * 获取 CMMN 资源内容
     */
    public String getCmmnResourceContent(String caseDefinitionId) {
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery()
                .caseDefinitionId(caseDefinitionId)
                .singleResult();

        if (caseDefinition == null) {
            return null;
        }

        try {
            byte[] resourceBytes = cmmnRepositoryService.getResourceAsStream(
                    caseDefinition.getDeploymentId(),
                    caseDefinition.getResourceName()).readAllBytes();

            return new String(resourceBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to read CMMN resource content for: {}", caseDefinitionId, e);
            return null;
        }
    }

    /**
     * 获取 CMMN 定义资源内容（通过部署 ID 和资源名称）
     */
    public String getCaseDefinitionResourceContent(String deploymentId, String resourceName) {
        try {
            byte[] resourceBytes = cmmnRepositoryService.getResourceAsStream(
                    deploymentId,
                    resourceName).readAllBytes();

            return new String(resourceBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to read CMMN resource content for deployment: {}, resource: {}",
                    deploymentId, resourceName, e);
            return null;
        }
    }

    // ==================== BPMN 相关 ====================

    /**
     * 获取所有 BPMN Process 定义
     */
    public List<ProcessDefinition> getAllProcessDefinitions() {
        return repositoryService.createProcessDefinitionQuery()
                .orderByProcessDefinitionVersion().desc()
                .list();
    }

    /**
     * 根据 Key 获取 Process 定义的所有版本
     */
    public List<ProcessDefinition> getProcessDefinitionVersions(String processDefinitionKey) {
        return repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processDefinitionKey)
                .orderByProcessDefinitionVersion().desc()
                .list();
    }

    /**
     * 获取最新版本的 Process 定义
     */
    public ProcessDefinition getLatestProcessDefinition(String processDefinitionKey) {
        return repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processDefinitionKey)
                .latestVersion()
                .singleResult();
    }

    /**
     * 部署 BPMN 资源
     */
    public Deployment deployBpmnResource(
            String resourceName,
            byte[] resourceBytes,
            String deploymentName,
            String tenantId) {
        log.info("Deploying BPMN resource: {}, deploymentName: {}, tenantId: {}",
                resourceName, deploymentName, tenantId);

        DeploymentBuilder builder = repositoryService.createDeployment()
                .name(deploymentName)
                .addBytes(resourceName, resourceBytes);

        if (tenantId != null && !tenantId.isEmpty()) {
            builder.tenantId(tenantId);
        }

        Deployment deployment = builder.deploy();
        log.info("BPMN deployment successful: {}", deployment.getId());

        return deployment;
    }

    /**
     * 获取 BPMN 部署列表
     */
    public List<Deployment> getBpmnDeployments() {
        return repositoryService.createDeploymentQuery()
                .orderByDeploymentTime().desc()
                .list();
    }

    /**
     * 获取 BPMN 资源内容
     */
    public String getBpmnResourceContent(String processDefinitionId) {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(processDefinitionId)
                .singleResult();

        if (processDefinition == null) {
            return null;
        }

        try {
            byte[] resourceBytes = repositoryService.getResourceAsStream(
                    processDefinition.getDeploymentId(),
                    processDefinition.getResourceName()).readAllBytes();

            return new String(resourceBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to read BPMN resource content for: {}", processDefinitionId, e);
            return null;
        }
    }

    // ==================== DMN 相关 ====================

    /**
     * 获取 DMN 部署数量（用于统计）
     */
    public long getDmnDeploymentCount() {
        try {
            long count = dmnRepositoryService.createDeploymentQuery().count();
            log.info("DMN deployment count: {}", count);
            return count;
        } catch (Exception e) {
            log.error("Error getting DMN deployment count", e);
            return 0L;
        }
    }

    /**
     * 部署 DMN 资源
     */
    public DmnDeployment deployDmnResource(
            String resourceName,
            byte[] resourceBytes,
            String deploymentName,
            String tenantId) {
        log.info("Deploying DMN resource: {}, deploymentName: {}, tenantId: {}",
                resourceName, deploymentName, tenantId);

        DmnDeploymentBuilder builder = dmnRepositoryService.createDeployment()
                .name(deploymentName)
                .addInputStream(resourceName, new ByteArrayInputStream(resourceBytes));

        if (tenantId != null && !tenantId.isEmpty()) {
            builder.tenantId(tenantId);
        }

        DmnDeployment deployment = builder.deploy();
        log.info("DMN deployment successful: {}", deployment.getId());

        return deployment;
    }

    /**
     * 获取 DMN 部署列表
     */
    public List<DmnDeployment> getDmnDeployments() {
        try {
            List<DmnDeployment> deployments = dmnRepositoryService.createDeploymentQuery()
                    .orderByDeploymentTime().desc()
                    .list();
            log.info("Found {} DMN deployments", deployments.size());
            return deployments;
        } catch (Exception e) {
            log.error("Error getting DMN deployments", e);
            return List.of();
        }
    }

    /**
     * 获取 DMN 决策表
     */
    public List<DmnDecision> getAllDmnDecisionTables() {
        try {
            List<DmnDecision> decisions = dmnRepositoryService.createDecisionQuery()
                    .orderByDecisionKey().asc()
                    .list();
            log.info("Found {} DMN decision tables", decisions.size());
            return decisions;
        } catch (Exception e) {
            log.error("Error getting DMN decision tables", e);
            return List.of();
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 生成部署名称
     */
    public String generateDeploymentName(String definitionKey, Integer version) {
        return String.format("%s-v%d-%d", definitionKey, version, System.currentTimeMillis());
    }

    /**
     * Date 转 LocalDateTime
     */
    protected LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
