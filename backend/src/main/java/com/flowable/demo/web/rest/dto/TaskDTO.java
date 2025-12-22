package com.flowable.demo.web.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "任务DTO")
public class TaskDTO {
    
    @Schema(description = "任务ID")
    private String id;
    
    @Schema(description = "任务名称")
    private String name;
    
    @Schema(description = "任务描述")
    private String description;
    
    @Schema(description = "分配人")
    private String assignee;
    
    @Schema(description = "任务所有者")
    private String owner;
    
    @Schema(description = "流程实例ID")
    private String processInstanceId;
    
    @Schema(description = "案件实例ID")
    private String caseInstanceId;
    
    @Schema(description = "任务定义Key")
    private String taskDefinitionKey;
    
    @Schema(description = "表单Key")
    private String formKey;
    
    @Schema(description = "优先级")
    private Integer priority;
    
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    @Schema(description = "截止时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dueDate;
    
    @Schema(description = "分类")
    private String category;
    
    @Schema(description = "租户ID")
    private String tenantId;
    
    @Schema(description = "是否暂停")
    private Boolean suspended;
    
    @Schema(description = "候选用户列表")
    private List<String> candidateUsers;
    
    @Schema(description = "候选组列表")
    private List<String> candidateGroups;
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getAssignee() {
        return assignee;
    }
    
    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }
    
    public String getOwner() {
        return owner;
    }
    
    public void setOwner(String owner) {
        this.owner = owner;
    }
    
    public String getProcessInstanceId() {
        return processInstanceId;
    }
    
    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }
    
    public String getCaseInstanceId() {
        return caseInstanceId;
    }
    
    public void setCaseInstanceId(String caseInstanceId) {
        this.caseInstanceId = caseInstanceId;
    }
    
    public String getTaskDefinitionKey() {
        return taskDefinitionKey;
    }
    
    public void setTaskDefinitionKey(String taskDefinitionKey) {
        this.taskDefinitionKey = taskDefinitionKey;
    }
    
    public String getFormKey() {
        return formKey;
    }
    
    public void setFormKey(String formKey) {
        this.formKey = formKey;
    }
    
    public Integer getPriority() {
        return priority;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    
    public LocalDateTime getDueDate() {
        return dueDate;
    }
    
    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
    
    public Boolean getSuspended() {
        return suspended;
    }
    
    public void setSuspended(Boolean suspended) {
        this.suspended = suspended;
    }
    
    public List<String> getCandidateUsers() {
        return candidateUsers;
    }
    
    public void setCandidateUsers(List<String> candidateUsers) {
        this.candidateUsers = candidateUsers;
    }
    
    public List<String> getCandidateGroups() {
        return candidateGroups;
    }
    
    public void setCandidateGroups(List<String> candidateGroups) {
        this.candidateGroups = candidateGroups;
    }
}
