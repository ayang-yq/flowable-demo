package com.flowable.demo.admin.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Plan Item 状态 DTO
 * 用于 CMMN 模型节点状态高亮
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanItemStateDTO {

    /**
     * Plan Item 实例 ID
     */
    private String id;

    /**
     * Plan Item 定义 ID (对应 CMMN XML 中的 elementId，用于关联 SVG 节点)
     */
    private String planItemDefinitionId;

    /**
     * Plan Item 名称
     */
    private String name;

    /**
     * Plan Item 类型: HUMAN_TASK, STAGE, MILESTONE, PROCESS_TASK, etc.
     */
    private String type;

    /**
     * 当前状态: active, available, completed, terminated, suspended
     */
    private String state;

    /**
     * 所属 Stage 实例 ID
     */
    private String stageInstanceId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 完成时间
     */
    private LocalDateTime completedTime;

    /**
     * 终止时间
     */
    private LocalDateTime terminatedTime;
}
