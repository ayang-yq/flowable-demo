package com.flowable.demo.web.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "理赔历史记录DTO")
public class ClaimHistoryDTO {
    
    @Schema(description = "历史记录ID")
    private String id;
    
    @Schema(description = "案件ID")
    private String claimCaseId;
    
    @Schema(description = "操作类型")
    private String actionType;
    
    @Schema(description = "操作描述")
    private String actionDescription;
    
    @Schema(description = "操作人")
    private String operator;
    
    @Schema(description = "操作时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime operateTime;
    
    @Schema(description = "操作前状态")
    private String beforeStatus;
    
    @Schema(description = "操作后状态")
    private String afterStatus;
    
    @Schema(description = "备注")
    private String remarks;
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getClaimCaseId() {
        return claimCaseId;
    }
    
    public void setClaimCaseId(String claimCaseId) {
        this.claimCaseId = claimCaseId;
    }
    
    public String getActionType() {
        return actionType;
    }
    
    public void setActionType(String actionType) {
        this.actionType = actionType;
    }
    
    public String getActionDescription() {
        return actionDescription;
    }
    
    public void setActionDescription(String actionDescription) {
        this.actionDescription = actionDescription;
    }
    
    public String getOperator() {
        return operator;
    }
    
    public void setOperator(String operator) {
        this.operator = operator;
    }
    
    public LocalDateTime getOperateTime() {
        return operateTime;
    }
    
    public void setOperateTime(LocalDateTime operateTime) {
        this.operateTime = operateTime;
    }
    
    public String getBeforeStatus() {
        return beforeStatus;
    }
    
    public void setBeforeStatus(String beforeStatus) {
        this.beforeStatus = beforeStatus;
    }
    
    public String getAfterStatus() {
        return afterStatus;
    }
    
    public void setAfterStatus(String afterStatus) {
        this.afterStatus = afterStatus;
    }
    
    public String getRemarks() {
        return remarks;
    }
    
    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
