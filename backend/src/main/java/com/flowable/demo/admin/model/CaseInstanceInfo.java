package com.flowable.demo.admin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Case 实例信息领域模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseInstanceInfo {

    /**
     * Case 实例 ID
     */
    private String id;

    /**
     * Case 定义 ID
     */
    private String caseDefinitionId;

    /**
     * Case 定义 Key
     */
    private String caseDefinitionKey;

    /**
     * Case 定义名称
     */
    private String caseDefinitionName;

    /**
     * Case 定义版本
     */
    private Integer caseDefinitionVersion;

    /**
     * 业务 Key
     */
    private String businessKey;

    /**
     * 状态: ACTIVE, COMPLETED, TERMINATED, SUSPENDED
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
     * 激活的 Plan Item 数量
     */
    private Integer activePlanItems;

    /**
     * 已完成的 Plan Item 数量
     */
    private Integer completedPlanItems;

    /**
     * Case 变量
     */
    private Map<String, Object> variables;
}
