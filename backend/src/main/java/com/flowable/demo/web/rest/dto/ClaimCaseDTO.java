package com.flowable.demo.web.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "理赔案件DTO")
public class ClaimCaseDTO {
    
    @Schema(description = "案件ID")
    private String id;
    
    @Schema(description = "案件实例ID")
    private String caseInstanceId;
    
    @Schema(description = "案件编号")
    private String claimNumber;
    
    @Schema(description = "保单ID")
    private String policyId;
    
    @Schema(description = "保单信息")
    private InsurancePolicyDTO policy;
    
    @Schema(description = "索赔人姓名")
    private String claimantName;
    
    @Schema(description = "索赔人电话")
    private String claimantPhone;
    
    @Schema(description = "索赔人邮箱")
    private String claimantEmail;
    
    @Schema(description = "出险时间")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private String incidentDate;
    
    @Schema(description = "出险地点")
    private String incidentLocation;
    
    @Schema(description = "事故描述")
    private String incidentDescription;
    
    @Schema(description = "索赔金额")
    private Double claimedAmount;

    @Schema(description = "批准金额")
    private Double approvedAmount;

    @Schema(description = "理赔类型")
    private String claimType;
    
    @Schema(description = "严重程度")
    private String severity;
    
    @Schema(description = "理赔状态")
    private String status;
    
    @Schema(description = "分配给的用户ID")
    private String assignedToId;
    
    @Schema(description = "分配给的用户名")
    private String assignedToName;
    
    @Schema(description = "创建者ID")
    private String createdById;
    
    @Schema(description = "创建者姓名")
    private String createdByName;
    
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    @Schema(description = "文档列表")
    private List<ClaimDocumentDTO> documents;
    
    @Schema(description = "历史记录")
    private List<ClaimHistoryDTO> histories;
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getCaseInstanceId() {
        return caseInstanceId;
    }
    
    public void setCaseInstanceId(String caseInstanceId) {
        this.caseInstanceId = caseInstanceId;
    }
    
    public String getClaimNumber() {
        return claimNumber;
    }
    
    public void setClaimNumber(String claimNumber) {
        this.claimNumber = claimNumber;
    }
    
    public String getPolicyId() {
        return policyId;
    }
    
    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }
    
    public InsurancePolicyDTO getPolicy() {
        return policy;
    }
    
    public void setPolicy(InsurancePolicyDTO policy) {
        this.policy = policy;
    }
    
    public String getClaimantName() {
        return claimantName;
    }
    
    public void setClaimantName(String claimantName) {
        this.claimantName = claimantName;
    }
    
    public String getClaimantPhone() {
        return claimantPhone;
    }
    
    public void setClaimantPhone(String claimantPhone) {
        this.claimantPhone = claimantPhone;
    }
    
    public String getClaimantEmail() {
        return claimantEmail;
    }
    
    public void setClaimantEmail(String claimantEmail) {
        this.claimantEmail = claimantEmail;
    }
    
    public String getIncidentDate() {
        return incidentDate;
    }
    
    public void setIncidentDate(String incidentDate) {
        this.incidentDate = incidentDate;
    }
    
    public String getIncidentLocation() {
        return incidentLocation;
    }
    
    public void setIncidentLocation(String incidentLocation) {
        this.incidentLocation = incidentLocation;
    }
    
    public String getIncidentDescription() {
        return incidentDescription;
    }
    
    public void setIncidentDescription(String incidentDescription) {
        this.incidentDescription = incidentDescription;
    }
    
    public Double getClaimedAmount() {
        return claimedAmount;
    }
    
    public void setClaimedAmount(Double claimedAmount) {
        this.claimedAmount = claimedAmount;
    }

    public Double getApprovedAmount() {
        return approvedAmount;
    }

    public void setApprovedAmount(Double approvedAmount) {
        this.approvedAmount = approvedAmount;
    }

    public String getClaimType() {
        return claimType;
    }
    
    public void setClaimType(String claimType) {
        this.claimType = claimType;
    }
    
    public String getSeverity() {
        return severity;
    }
    
    public void setSeverity(String severity) {
        this.severity = severity;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getAssignedToId() {
        return assignedToId;
    }
    
    public void setAssignedToId(String assignedToId) {
        this.assignedToId = assignedToId;
    }
    
    public String getAssignedToName() {
        return assignedToName;
    }
    
    public void setAssignedToName(String assignedToName) {
        this.assignedToName = assignedToName;
    }
    
    public String getCreatedById() {
        return createdById;
    }
    
    public void setCreatedById(String createdById) {
        this.createdById = createdById;
    }
    
    public String getCreatedByName() {
        return createdByName;
    }
    
    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public List<ClaimDocumentDTO> getDocuments() {
        return documents;
    }
    
    public void setDocuments(List<ClaimDocumentDTO> documents) {
        this.documents = documents;
    }
    
    public List<ClaimHistoryDTO> getHistories() {
        return histories;
    }
    
    public void setHistories(List<ClaimHistoryDTO> histories) {
        this.histories = histories;
    }
}
