package com.flowable.demo.admin.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 模型 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelDTO {

    private String id;
    private String key;
    private String name;
    private String type; // CMMN, BPMN, DMN
    private Integer version;
    private Boolean deployed;
    private String latestDeploymentId;
    private LocalDateTime latestDeploymentTime;
    private String createdBy;
    private LocalDateTime lastModified;
    private String tenantId;
    private String description;

    /**
     * 部署历史列表(详情页使用)
     */
    private List<DeploymentDTO> deployments;

    /**
     * XML 内容(详情页使用)
     */
    private String xmlContent;
}
