package com.flowable.demo.admin.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * BPMN 子流程可视化 DTO
 * 用于在 CMMN 可视化中展开显示 processTask 对应的 BPMN 流程
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BpmnSubprocessVisualizationDTO {

    /**
     * 子流程实例 ID
     */
    private String processInstanceId;

    /**
     * 子流程定义 ID
     */
    private String processDefinitionId;

    /**
     * 子流程定义 Key
     */
    private String processDefinitionKey;

    /**
     * 子流程定义名称
     */
    private String processDefinitionName;

    /**
     * 流程图 XML 内容
     */
    private String bpmnXml;

    /**
     * 流程图 SVG 内容（由 Flowable ProcessDiagramGenerator 生成）
     */
    private String diagramSvg;

    /**
     * 活动节点状态列表
     */
    private List<ActivityStateDTO> activityStates;

    /**
     * 子流程状态
     */
    private String processInstanceState;

    /**
     * 子流程开始时间
     */
    private String startTime;

    /**
     * 子流程结束时间
     */
    private String endTime;
}
