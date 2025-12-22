package com.flowable.demo.web.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "理赔文档DTO")
public class ClaimDocumentDTO {
    
    @Schema(description = "文档ID")
    private String id;
    
    @Schema(description = "案件ID")
    private String claimCaseId;
    
    @Schema(description = "文档类型")
    private String documentType;
    
    @Schema(description = "文档名称")
    private String documentName;
    
    @Schema(description = "文件路径")
    private String filePath;
    
    @Schema(description = "文件大小")
    private Long fileSize;
    
    @Schema(description = "上传人")
    private String uploadedBy;
    
    @Schema(description = "上传时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadTime;
    
    @Schema(description = "文档状态")
    private String status;
    
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
    
    public String getDocumentType() {
        return documentType;
    }
    
    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }
    
    public String getDocumentName() {
        return documentName;
    }
    
    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getUploadedBy() {
        return uploadedBy;
    }
    
    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }
    
    public LocalDateTime getUploadTime() {
        return uploadTime;
    }
    
    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getRemarks() {
        return remarks;
    }
    
    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
