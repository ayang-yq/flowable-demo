package com.flowable.demo.admin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Process 实例信息领域模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessInstanceInfo {

    /**
     * Process 实例 ID
     */
    private String id;

    /**
     * Process 定义 ID
     */
    private String processDefinitionId;

    /**
     * Process 定义 Key
     */
    private String processDefinitionKey;

    /**
     * Process 定义名称
     */
    private String processDefinitionName;

    /**
     * Process 定义版本
     */
    private Integer processDefinitionVersion;

    /**
     * 业务 Key
     */
    private String businessKey;

    /**
     * 状态: ACTIVE, COMPLETED, SUSPENDED
     */
    private String state;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 启动用户 ID
     */
    private String startUserId;

    /**
     * 租户 ID
     */
    private String tenantId;

    /**
     * 当前活动节点 ID 列表
     */
    private List<String> currentActivityIds;

    /**
     * Process 变量
     */
    private Map<String, Object> variables;
}
