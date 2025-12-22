package com.flowable.demo.admin.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Process 流程图 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessDiagramDTO {

    /**
     * Process 定义 ID
     */
    private String processDefinitionId;

    /**
     * 流程图 XML 内容
     */
    private String diagramXml;

    /**
     * 高亮的活动节点 ID 列表(当前正在执行)
     */
    private List<String> highlightedActivities;

    /**
     * 已完成的活动节点 ID 列表
     */
    private List<String> completedActivities;

    /**
     * 高亮的 Sequence Flow ID 列表(已执行的连线)
     */
    private List<String> highlightedFlows;
}
