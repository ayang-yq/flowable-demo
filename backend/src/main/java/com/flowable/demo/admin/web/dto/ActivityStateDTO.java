package com.flowable.demo.admin.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * BPMN 活动节点状态 DTO
 * 用于表示 BPMN 流程中每个节点的执行状态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityStateDTO {

    /**
     * 节点 ID（对应 BPMN XML 中的 activity id）
     */
    private String activityId;

    /**
     * 节点名称
     */
    private String activityName;

    /**
     * 节点类型（userTask, serviceTask, gateway 等）
     */
    private String activityType;

    /**
     * 节点状态
     * - active: 正在执行
     * - completed: 已完成
     * - available: 可用但未执行
     * - terminated: 已终止
     */
    private String state;

    /**
     * 父流程实例 ID
     */
    private String processInstanceId;

    /**
     * 开始时间
     */
    private String startTime;

    /**
     * 结束时间
     */
    private String endTime;
}
