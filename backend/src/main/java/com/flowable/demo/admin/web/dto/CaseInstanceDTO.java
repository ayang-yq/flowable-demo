package com.flowable.demo.admin.web.dto;

import com.flowable.demo.admin.model.PlanItemTreeNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Case 实例 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseInstanceDTO {

    private String id;
    private String caseDefinitionId;
    private String caseDefinitionKey;
    private String caseDefinitionName;
    private Integer caseDefinitionVersion;
    private String businessKey;
    private String state;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String startUserId;
    private String tenantId;
    private Integer activePlanItems;
    private Integer completedPlanItems;

    /**
     * Case 变量(详情页使用)
     */
    private Map<String, Object> variables;

    /**
     * Plan Item Tree(详情页使用)
     */
    private PlanItemTreeNode planItemTree;
}
