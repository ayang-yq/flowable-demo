package com.flowable.demo.admin.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Admin 统计信息 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatisticsDTO {

    /**
     * 模型统计
     */
    private ModelStatistics models;

    /**
     * 部署统计
     */
    private DeploymentStatistics deployments;

    /**
     * Case 统计
     */
    private Map<String, Long> cases;

    /**
     * Process 统计
     */
    private Map<String, Long> processes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelStatistics {
        private Long total;
        private Long cmmn;
        private Long bpmn;
        private Long dmn;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeploymentStatistics {
        private Long total;
        private String lastDeploymentTime;
    }
}
