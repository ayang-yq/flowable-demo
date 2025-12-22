package com.flowable.demo.admin.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 部署请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentRequest {

    /**
     * 部署名称(可选,默认自动生成)
     */
    private String deploymentName;

    /**
     * 租户 ID(可选)
     */
    private String tenantId;

    /**
     * 部署说明
     */
    private String description;
}
