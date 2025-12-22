package com.flowable.demo.admin.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 部署 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentDTO {

    private String id;
    private String name;
    private String definitionId;
    private String definitionKey;
    private Integer version;
    private LocalDateTime deploymentTime;
    private String deployedBy;
    private Boolean active;
    private String tenantId;
    private String category;
}
