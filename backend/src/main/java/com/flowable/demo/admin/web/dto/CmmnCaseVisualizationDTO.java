package com.flowable.demo.admin.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * CMMN Case 运行态可视化 DTO
 * 参考 Flowable UI 6.8 设计，提供模型和运行态数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CmmnCaseVisualizationDTO {

    /**
     * Case 实例 ID
     */
    private String caseInstanceId;

    /**
     * Case 定义 ID
     */
    private String caseDefinitionId;

    /**
     * CMMN XML 内容（用于 cmmn-js 渲染）
     */
    private String cmmnXml;

    /**
     * Plan Item 实例列表（用于状态高亮）
     */
    private List<PlanItemStateDTO> planItems;
}
