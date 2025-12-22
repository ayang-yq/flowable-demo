package com.flowable.demo.admin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 部署信息领域模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentInfo {

    /**
     * 部署 ID
     */
    private String id;

    /**
     * 部署名称
     */
    private String name;

    /**
     * 定义 ID
     */
    private String definitionId;

    /**
     * 定义 Key
     */
    private String definitionKey;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 部署时间
     */
    private LocalDateTime deploymentTime;

    /**
     * 部署人
     */
    private String deployedBy;

    /**
     * 是否激活
     */
    private Boolean active;

    /**
     * 租户 ID
     */
    private String tenantId;

    /**
     * 类别
     */
    private String category;
}
