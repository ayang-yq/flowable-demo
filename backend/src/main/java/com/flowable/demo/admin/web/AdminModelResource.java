package com.flowable.demo.admin.web;

import com.flowable.demo.admin.service.ModelManagementService;
import com.flowable.demo.admin.web.dto.DeploymentDTO;
import com.flowable.demo.admin.web.dto.DeploymentRequest;
import com.flowable.demo.admin.web.dto.ModelDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Admin 模型管理 REST API
 */
@Slf4j
@RestController
@RequestMapping("/admin/models")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminModelResource {

    private final ModelManagementService modelManagementService;

    /**
     * 查询模型列表
     * 
     * @param type     模型类型: CMMN, BPMN, DMN (可选)
     * @param pageable 分页参数
     * @return 模型列表
     */
    @GetMapping
    public ResponseEntity<Page<ModelDTO>> queryModels(
            @RequestParam(required = false) String type,
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("Query models: type={}, page={}", type, pageable.getPageNumber());

        Page<ModelDTO> models = modelManagementService.queryModels(type, pageable);
        return ResponseEntity.ok(models);
    }

    /**
     * 获取模型详情
     * 
     * @param modelKey  模型 Key
     * @param modelType 模型类型: CMMN, BPMN
     * @return 模型详情(包含所有版本和 XML 内容)
     */
    @GetMapping("/{modelKey}")
    public ResponseEntity<ModelDTO> getModelDetail(
            @PathVariable String modelKey,
            @RequestParam String modelType) {
        log.info("Get model detail: key={}, type={}", modelKey, modelType);

        ModelDTO model = modelManagementService.getModelDetail(modelKey, modelType);

        if (model == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(model);
    }

    /**
     * 查询部署列表
     * 
     * @param type     模型类型: CMMN, BPMN, DMN (可选)
     * @param pageable 分页参数
     * @return 部署列表
     */
    @GetMapping("/deployments")
    public ResponseEntity<Page<DeploymentDTO>> queryDeployments(
            @RequestParam(required = false) String type,
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("Query deployments: type={}, page={}", type, pageable.getPageNumber());

        Page<DeploymentDTO> deployments = modelManagementService.queryDeployments(type, pageable);
        return ResponseEntity.ok(deployments);
    }

    /**
     * 部署模型(从文件上传)
     * 
     * @param file      模型文件(.cmmn, .bpmn, .dmn)
     * @param modelType 模型类型: CMMN, BPMN, DMN
     * @param name      部署名称(可选)
     * @param tenantId  租户ID(可选)
     * @return 部署信息
     */
    @PostMapping("/deploy")
    public ResponseEntity<DeploymentDTO> deployModel(
            @RequestParam("file") MultipartFile file,
            @RequestParam String modelType,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String tenantId) {
        try {
            log.info("Deploy model: file={}, type={}", file.getOriginalFilename(), modelType);

            DeploymentRequest request = new DeploymentRequest();
            if (name != null) {
                request.setDeploymentName(name);
            }
            if (tenantId != null) {
                request.setTenantId(tenantId);
            }

            DeploymentDTO deployment = modelManagementService.deployModel(
                    file.getOriginalFilename(),
                    file.getBytes(),
                    modelType,
                    request);

            return ResponseEntity.ok(deployment);
        } catch (Exception e) {
            log.error("Failed to deploy model", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
