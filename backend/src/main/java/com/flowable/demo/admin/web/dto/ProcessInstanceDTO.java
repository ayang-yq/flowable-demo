package com.flowable.demo.admin.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Process 实例 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessInstanceDTO {

    private String id;
    private String processDefinitionId;
    private String processDefinitionKey;
    private String processDefinitionName;
    private Integer processDefinitionVersion;
    private String businessKey;
    private String state;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String startUserId;
    private String tenantId;
    private List<String> currentActivityIds;

    /**
     * Process 变量(详情页使用)
     */
    private Map<String, Object> variables;

    /**
     * 当前活动节点详情(详情页使用)
     */
    private List<ActivityInfo> currentActivities;

    /**
     * 已完成的活动节点(详情页使用)
     */
    private List<ActivityInfo> completedActivities;

    /**
     * 活动节点信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityInfo {
        private String activityId;
        private String activityName;
        private String activityType;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String assignee;
        private Long durationInMillis;
    }
}
