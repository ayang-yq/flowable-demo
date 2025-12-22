package com.flowable.demo.admin.service;

import com.flowable.demo.admin.adapter.FlowableRepositoryAdapter;
import com.flowable.demo.admin.web.dto.DeploymentDTO;
import com.flowable.demo.admin.web.dto.DeploymentRequest;
import com.flowable.demo.admin.web.dto.ModelDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.dmn.api.DmnDeployment;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 模型管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ModelManagementService {

    private final FlowableRepositoryAdapter repositoryAdapter;

    // ==================== 模型查询 ====================

    /**
     * 查询所有模型(分页)
     */
    public Page<ModelDTO> queryModels(String type, Pageable pageable) {
        List<ModelDTO> allModels = new ArrayList<>();

        if (type == null || "CMMN".equalsIgnoreCase(type)) {
            allModels.addAll(getCmmnModels());
        }
        if (type == null || "BPMN".equalsIgnoreCase(type)) {
            allModels.addAll(getBpmnModels());
        }
        if (type == null || "DMN".equalsIgnoreCase(type)) {
            allModels.addAll(getDmnModels());
        }

        // 分页
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allModels.size());
        List<ModelDTO> pageContent = allModels.subList(start, end);

        return new PageImpl<>(pageContent, pageable, allModels.size());
    }

    /**
     * 获取 CMMN 模型列表
     */
    private List<ModelDTO> getCmmnModels() {
        List<CaseDefinition> caseDefinitions = repositoryAdapter.getAllCaseDefinitions();

        // 按 Key 分组
        Map<String, List<CaseDefinition>> definitionsByKey = caseDefinitions.stream()
                .collect(Collectors.groupingBy(CaseDefinition::getKey));

        return definitionsByKey.entrySet().stream()
                .map(entry -> {
                    String key = entry.getKey();
                    List<CaseDefinition> versions = entry.getValue();

                    // 获取最新版本
                    CaseDefinition latest = versions.stream()
                            .max(Comparator.comparing(CaseDefinition::getVersion))
                            .orElse(versions.get(0));

                    return ModelDTO.builder()
                            .id(latest.getId())
                            .key(latest.getKey())
                            .name(latest.getName())
                            .type("CMMN")
                            .version(latest.getVersion())
                            .deployed(true)
                            .latestDeploymentId(latest.getDeploymentId())
                            .latestDeploymentTime(null) // TODO: Get from deployment
                            .tenantId(latest.getTenantId())
                            .description(latest.getDescription())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取 BPMN 模型列表
     */
    private List<ModelDTO> getBpmnModels() {
        List<ProcessDefinition> processDefinitions = repositoryAdapter.getAllProcessDefinitions();

        // 按 Key 分组
        Map<String, List<ProcessDefinition>> definitionsByKey = processDefinitions.stream()
                .collect(Collectors.groupingBy(ProcessDefinition::getKey));

        return definitionsByKey.entrySet().stream()
                .map(entry -> {
                    String key = entry.getKey();
                    List<ProcessDefinition> versions = entry.getValue();

                    // 获取最新版本
                    ProcessDefinition latest = versions.stream()
                            .max(Comparator.comparing(ProcessDefinition::getVersion))
                            .orElse(versions.get(0));

                    return ModelDTO.builder()
                            .id(latest.getId())
                            .key(latest.getKey())
                            .name(latest.getName())
                            .type("BPMN")
                            .version(latest.getVersion())
                            .deployed(true)
                            .latestDeploymentId(latest.getDeploymentId())
                            .latestDeploymentTime(null) // TODO: Get from deployment
                            .tenantId(latest.getTenantId())
                            .description(latest.getDescription())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取 DMN 模型列表
     */
    private List<ModelDTO> getDmnModels() {
        List<DmnDeployment> dmnDeployments = repositoryAdapter.getDmnDeployments();

        return dmnDeployments.stream()
                .map(deployment -> ModelDTO.builder()
                        .id(deployment.getId())
                        .key(deployment.getName())
                        .name(deployment.getName())
                        .type("DMN")
                        .version(1) // DMN deployments don't have versions in the same way
                        .deployed(true)
                        .latestDeploymentId(deployment.getId())
                        .latestDeploymentTime(toLocalDateTime(deployment.getDeploymentTime()))
                        .tenantId(deployment.getTenantId())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 获取模型详情(包含所有版本)
     */
    public ModelDTO getModelDetail(String modelKey, String modelType) {
        if ("CMMN".equalsIgnoreCase(modelType)) {
            return getCmmnModelDetail(modelKey);
        } else if ("BPMN".equalsIgnoreCase(modelType)) {
            return getBpmnModelDetail(modelKey);
        } else if ("DMN".equalsIgnoreCase(modelType)) {
            return getDmnModelDetail(modelKey);
        } else {
            throw new IllegalArgumentException("Unsupported model type: " + modelType);
        }
    }

    /**
     * 获取 CMMN 模型详情
     */
    private ModelDTO getCmmnModelDetail(String caseDefinitionKey) {
        List<CaseDefinition> versions = repositoryAdapter.getCaseDefinitionVersions(caseDefinitionKey);

        if (versions.isEmpty()) {
            return null;
        }

        CaseDefinition latest = versions.get(0);

        // 构建部署历史
        List<DeploymentDTO> deployments = versions.stream()
                .map(def -> DeploymentDTO.builder()
                        .id(def.getDeploymentId())
                        .definitionId(def.getId())
                        .definitionKey(def.getKey())
                        .version(def.getVersion())
                        .deploymentTime(null) // TODO: Get from CmmnDeployment
                        .active(def.getVersion() == latest.getVersion())
                        .tenantId(def.getTenantId())
                        .category(def.getCategory())
                        .build())
                .collect(Collectors.toList());

        // 获取 XML 内容
        String xmlContent = repositoryAdapter.getCmmnResourceContent(latest.getId());

        return ModelDTO.builder()
                .id(latest.getId())
                .key(latest.getKey())
                .name(latest.getName())
                .type("CMMN")
                .version(latest.getVersion())
                .deployed(true)
                .latestDeploymentId(latest.getDeploymentId())
                .latestDeploymentTime(null) // TODO: Get from deployment
                .tenantId(latest.getTenantId())
                .description(latest.getDescription())
                .deployments(deployments)
                .xmlContent(xmlContent)
                .build();
    }

    /**
     * 获取 BPMN 模型详情
     */
    private ModelDTO getBpmnModelDetail(String processDefinitionKey) {
        List<ProcessDefinition> versions = repositoryAdapter.getProcessDefinitionVersions(processDefinitionKey);

        if (versions.isEmpty()) {
            return null;
        }

        ProcessDefinition latest = versions.get(0);

        // 构建部署历史
        List<DeploymentDTO> deployments = versions.stream()
                .map(def -> DeploymentDTO.builder()
                        .id(def.getDeploymentId())
                        .definitionId(def.getId())
                        .definitionKey(def.getKey())
                        .version(def.getVersion())
                        .deploymentTime(null) // TODO: Get from Deployment
                        .active(def.getVersion() == latest.getVersion())
                        .tenantId(def.getTenantId())
                        .category(def.getCategory())
                        .build())
                .collect(Collectors.toList());

        // 获取 XML 内容
        String xmlContent = repositoryAdapter.getBpmnResourceContent(latest.getId());

        return ModelDTO.builder()
                .id(latest.getId())
                .key(latest.getKey())
                .name(latest.getName())
                .type("BPMN")
                .version(latest.getVersion())
                .deployed(true)
                .latestDeploymentId(latest.getDeploymentId())
                .latestDeploymentTime(null) // TODO: Get from deployment
                .tenantId(latest.getTenantId())
                .description(latest.getDescription())
                .deployments(deployments)
                .xmlContent(xmlContent)
                .build();
    }

    /**
     * 获取 DMN 模型详情
     */
    private ModelDTO getDmnModelDetail(String deploymentId) {
        List<DmnDeployment> deployments = repositoryAdapter.getDmnDeployments();
        
        DmnDeployment deployment = deployments.stream()
                .filter(d -> d.getId().equals(deploymentId))
                .findFirst()
                .orElse(null);

        if (deployment == null) {
            return null;
        }

        return ModelDTO.builder()
                .id(deployment.getId())
                .key(deployment.getName())
                .name(deployment.getName())
                .type("DMN")
                .version(1)
                .deployed(true)
                .latestDeploymentId(deployment.getId())
                .latestDeploymentTime(toLocalDateTime(deployment.getDeploymentTime()))
                .tenantId(deployment.getTenantId())
                .build();
    }

    // ==================== 模型部署 ====================

    /**
     * 部署模型(从资源文件)
     */
    @Transactional
    public DeploymentDTO deployModel(
            String resourceName,
            byte[] resourceBytes,
            String modelType,
            DeploymentRequest request) {
        log.info("Deploying model: {}, type: {}", resourceName, modelType);

        String deploymentName = request.getDeploymentName();
        if (deploymentName == null || deploymentName.isEmpty()) {
            deploymentName = generateDeploymentName(resourceName);
        }

        if ("CMMN".equalsIgnoreCase(modelType)) {
            CmmnDeployment deployment = repositoryAdapter.deployCmmnResource(
                    resourceName,
                    resourceBytes,
                    deploymentName,
                    request.getTenantId());

            return convertCmmnDeploymentToDTO(deployment);
        } else if ("BPMN".equalsIgnoreCase(modelType)) {
            Deployment deployment = repositoryAdapter.deployBpmnResource(
                    resourceName,
                    resourceBytes,
                    deploymentName,
                    request.getTenantId());

            return convertBpmnDeploymentToDTO(deployment);
        } else if ("DMN".equalsIgnoreCase(modelType)) {
            DmnDeployment deployment = repositoryAdapter.deployDmnResource(
                    resourceName,
                    resourceBytes,
                    deploymentName,
                    request.getTenantId());
            return convertDmnDeploymentToDTO(deployment);
        } else {
            throw new IllegalArgumentException("Unsupported model type: " + modelType);
        }
    }

    // ==================== 部署查询 ====================

    /**
     * 获取所有部署
     */
    public Page<DeploymentDTO> queryDeployments(String type, Pageable pageable) {
        List<DeploymentDTO> allDeployments = new ArrayList<>();

        if (type == null || "CMMN".equalsIgnoreCase(type)) {
            allDeployments.addAll(getCmmnDeployments());
        }
        if (type == null || "BPMN".equalsIgnoreCase(type)) {
            allDeployments.addAll(getBpmnDeployments());
        }
        if (type == null || "DMN".equalsIgnoreCase(type)) {
            allDeployments.addAll(getDmnDeployments());
        }

        // 分页
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allDeployments.size());
        List<DeploymentDTO> pageContent = allDeployments.subList(start, end);

        return new PageImpl<>(pageContent, pageable, allDeployments.size());
    }

    private List<DeploymentDTO> getCmmnDeployments() {
        return repositoryAdapter.getCmmnDeployments().stream()
                .map(this::convertCmmnDeploymentToDTO)
                .collect(Collectors.toList());
    }

    private List<DeploymentDTO> getBpmnDeployments() {
        return repositoryAdapter.getBpmnDeployments().stream()
                .map(this::convertBpmnDeploymentToDTO)
                .collect(Collectors.toList());
    }

    private List<DeploymentDTO> getDmnDeployments() {
        return repositoryAdapter.getDmnDeployments().stream()
                .map(this::convertDmnDeploymentToDTO)
                .collect(Collectors.toList());
    }

    // ==================== 工具方法 ====================

    private String generateDeploymentName(String resourceName) {
        String baseName = resourceName.replaceAll("\\.(cmmn|bpmn|dmn)$", "");
        return String.format("%s-%d", baseName, System.currentTimeMillis());
    }

    private DeploymentDTO convertCmmnDeploymentToDTO(CmmnDeployment deployment) {
        return DeploymentDTO.builder()
                .id(deployment.getId())
                .name(deployment.getName())
                .deploymentTime(toLocalDateTime(deployment.getDeploymentTime()))
                .tenantId(deployment.getTenantId())
                .category(deployment.getCategory())
                .build();
    }

    private DeploymentDTO convertBpmnDeploymentToDTO(Deployment deployment) {
        return DeploymentDTO.builder()
                .id(deployment.getId())
                .name(deployment.getName())
                .deploymentTime(toLocalDateTime(deployment.getDeploymentTime()))
                .tenantId(deployment.getTenantId())
                .category(deployment.getCategory())
                .build();
    }

    private DeploymentDTO convertDmnDeploymentToDTO(DmnDeployment deployment) {
        return DeploymentDTO.builder()
                .id(deployment.getId())
                .name(deployment.getName())
                .deploymentTime(toLocalDateTime(deployment.getDeploymentTime()))
                .tenantId(deployment.getTenantId())
                .category(deployment.getCategory())
                .build();
    }

    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
