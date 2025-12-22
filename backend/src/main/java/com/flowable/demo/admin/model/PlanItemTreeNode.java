package com.flowable.demo.admin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Plan Item 树节点(用于 CMMN 可视化)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanItemTreeNode {

    /**
     * Plan Item ID
     */
    private String id;

    /**
     * Plan Item 名称
     */
    private String name;

    /**
     * 元素 ID
     */
    private String elementId;

    /**
     * 类型: STAGE, HUMAN_TASK, SERVICE_TASK, MILESTONE, etc.
     */
    private String type;

    /**
     * 状态: ACTIVE, AVAILABLE, COMPLETED, TERMINATED, DISABLED
     */
    private String state;

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

    /**
     * 分配人
     */
    private String assignee;

    /**
     * 子节点
     */
    @Builder.Default
    private List<PlanItemTreeNode> children = new ArrayList<>();

    /**
     * 是否可重复
     */
    private Boolean repeatable;

    /**
     * 是否必需
     */
    private Boolean required;
}
