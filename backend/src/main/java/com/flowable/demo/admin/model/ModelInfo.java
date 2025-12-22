package com.flowable.demo.admin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 模型信息领域模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelInfo {

    /**
     * 模型 ID
     */
    private String id;

    /**
     * 模型 Key
     */
    private String key;

    /**
     * 模型名称
     */
    private String name;

    /**
     * 模型类型: CMMN, BPMN, DMN
     */
    private String type;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 是否已部署
     */
    private Boolean deployed;

    /**
     * 最新部署 ID
     */
    private String latestDeploymentId;

    /**
     * 最新部署时间
     */
    private LocalDateTime latestDeploymentTime;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 最后修改时间
     */
    private LocalDateTime lastModified;

    /**
     * 租户 ID
     */
    private String tenantId;

    /**
     * 模型描述
     */
    private String description;
}
