package com.flowable.demo.web.rest;

import com.flowable.demo.domain.model.ClaimCase;
import com.flowable.demo.domain.model.Role;
import com.flowable.demo.domain.model.User;
import com.flowable.demo.domain.repository.ClaimCaseRepository;
import com.flowable.demo.domain.repository.UserRepository;
import com.flowable.demo.web.rest.dto.TaskDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 任务管理 REST API
 */
@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "任务管理", description = "Flowable 任务的管理操作")
public class TaskResource {

    private final CmmnTaskService cmmnTaskService;
    private final TaskService taskService;
    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final ClaimCaseRepository claimCaseRepository;
    private final UserRepository userRepository;

    /**
     * 获取我的待办任务
     */
    @GetMapping("/my-tasks")
    @Operation(summary = "获取我的待办任务", description = "获取当前用户的待办任务列表")
    public ResponseEntity<Page<TaskDTO>> getMyTasks(
            @Parameter(description = "用户ID或用户名") @RequestParam String userId,
            Pageable pageable) {
        log.debug("REST request to get my tasks for user: {}", userId);
        
        // Try to find user by UUID first, then use username for Flowable query
        String flowableUserId = userId;
        try {
            User user = userRepository.findById(UUID.fromString(userId)).orElse(null);
            if (user != null) {
                flowableUserId = user.getUsername();
                log.debug("Found user with UUID {}, using Flowable username: {}", userId, flowableUserId);
            }
        } catch (IllegalArgumentException e) {
            // userId is already a username, use it directly
            log.debug("userId is a username, using it directly: {}", userId);
        }
        
        // Get tasks from both CMMN and BPMN engines
        List<Task> cmmnTasks = cmmnTaskService.createTaskQuery()
                .taskAssignee(flowableUserId)
                .orderByTaskCreateTime().desc()
                .list();
        
        List<Task> bpmnTasks = taskService.createTaskQuery()
                .taskAssignee(flowableUserId)
                .orderByTaskCreateTime().desc()
                .list();
        
        // Merge both lists and remove duplicates
        Set<String> taskIds = new HashSet<>();
        List<Task> allTasks = new ArrayList<>();
        
        for (Task task : cmmnTasks) {
            if (taskIds.add(task.getId())) {
                allTasks.add(task);
            }
        }
        
        for (Task task : bpmnTasks) {
            if (taskIds.add(task.getId())) {
                allTasks.add(task);
            }
        }
        
        // Sort by creation time
        allTasks.sort((t1, t2) -> t2.getCreateTime().compareTo(t1.getCreateTime()));
        
        // Apply pagination
        long total = allTasks.size();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allTasks.size());
        List<Task> pagedTasks = start < allTasks.size() ? allTasks.subList(start, end) : new ArrayList<>();
        
        Page<TaskDTO> result = new PageImpl<>(
                pagedTasks.stream().map(this::convertToDTO).collect(Collectors.toList()),
                pageable,
                total
        );
        
        log.debug("Returning {} tasks for user {} (Flowable userId: {}, total: {})", 
                pagedTasks.size(), userId, flowableUserId, total);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取可认领的任务
     * 支持两种方式：
     * 1. 任务直接分配给用户作为候选人 (taskCandidateUser)
     * 2. 任务分配给候选组，而用户拥有对应的角色
     */
    @GetMapping("/claimable")
    @Operation(summary = "获取可认领任务", description = "获取当前用户可以认领的任务列表")
    public ResponseEntity<Page<TaskDTO>> getClaimableTasks(
            @Parameter(description = "用户ID") @RequestParam String userId,
            Pageable pageable) {
        log.debug("REST request to get claimable tasks for user: {}", userId);
        
        // Try to find user by UUID first, then by username
        User user = null;
        try {
            user = userRepository.findById(UUID.fromString(userId))
                    .orElse(null);
        } catch (IllegalArgumentException e) {
            // userId is not a valid UUID, try by username
        }
        
        if (user == null) {
            user = userRepository.findByUsername(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
        }
        
        String username = user.getUsername();
        
        // Get all tasks with candidate users directly from both CMMN and BPMN
        List<Task> tasksByCandidateUser = new ArrayList<>();
        tasksByCandidateUser.addAll(cmmnTaskService.createTaskQuery()
                .taskCandidateUser(username)
                .active()
                .list());
        tasksByCandidateUser.addAll(taskService.createTaskQuery()
                .taskCandidateUser(username)
                .active()
                .list());
        
        // Get all tasks with candidate groups that the user's roles match
        List<Task> tasksByCandidateGroups = new ArrayList<>();
        
        // Map role names to Flowable group names
        for (Role role : user.getRoles()) {
            String flowableGroup = mapRoleToGroup(role.getName());
            if (flowableGroup != null) {
                // Query both CMMN and BPMN engines
                tasksByCandidateGroups.addAll(cmmnTaskService.createTaskQuery()
                        .taskCandidateGroup(flowableGroup)
                        .active()
                        .list());
                tasksByCandidateGroups.addAll(taskService.createTaskQuery()
                        .taskCandidateGroup(flowableGroup)
                        .active()
                        .list());
                log.debug("Found {} tasks for user {} with role {} (group: {})", 
                        tasksByCandidateGroups.size(), userId, role.getName(), flowableGroup);
            }
        }
        
        // Merge both lists and remove duplicates
        Set<String> taskIds = new HashSet<>();
        List<Task> allTasks = new ArrayList<>();
        
        for (Task task : tasksByCandidateUser) {
            if (taskIds.add(task.getId())) {
                allTasks.add(task);
            }
        }
        
        for (Task task : tasksByCandidateGroups) {
            if (taskIds.add(task.getId())) {
                allTasks.add(task);
            }
        }
        
        // Sort by creation time
        allTasks.sort((t1, t2) -> t2.getCreateTime().compareTo(t1.getCreateTime()));
        
        // Apply pagination
        long total = allTasks.size();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allTasks.size());
        List<Task> pagedTasks = start < allTasks.size() ? allTasks.subList(start, end) : new ArrayList<>();
        
        Page<TaskDTO> result = new PageImpl<>(
                pagedTasks.stream().map(this::convertToDTO).collect(Collectors.toList()),
                pageable,
                total
        );
        
        log.debug("Returning {} claimable tasks for user {} (total: {})", pagedTasks.size(), userId, total);
        return ResponseEntity.ok(result);
    }
    
    /**
     * Map application role names to Flowable group names
     */
    private String mapRoleToGroup(String roleName) {
        switch (roleName) {
            case "ADMIN":
                return "managers"; // Admin can see manager tasks
            case "MANAGER":
                return "managers";
            case "APPROVER":
                return "managers"; // Approvers are also in managers group for approval tasks
            case "CLAIM_HANDLER":
                return null; // No candidate group mapping needed for claim handlers
            case "FINANCE":
                return null; // No candidate group mapping needed for finance
            default:
                return null;
        }
    }

    /**
     * 认领任务
     */
    @PostMapping("/{taskId}/claim")
    @Operation(summary = "认领任务", description = "认领指定的任务")
    public ResponseEntity<Void> claimTask(
            @Parameter(description = "任务ID") @PathVariable String taskId,
            @Parameter(description = "用户ID") @RequestParam String userId) {
        log.debug("REST request to claim task {} for user: {}", taskId, userId);
        
        try {
            cmmnTaskService.claim(taskId, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to claim task {}: {}", taskId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 取消认领任务
     */
    @PostMapping("/{taskId}/unclaim")
    @Operation(summary = "取消认领任务", description = "取消认领指定的任务")
    public ResponseEntity<Void> unclaimTask(
            @Parameter(description = "任务ID") @PathVariable String taskId) {
        log.debug("REST request to unclaim task: {}", taskId);
        
        try {
            cmmnTaskService.unclaim(taskId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to unclaim task {}: {}", taskId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 分配任务
     */
    @PostMapping("/{taskId}/assign")
    @Operation(summary = "分配任务", description = "将任务分配给指定用户")
    public ResponseEntity<Void> assignTask(
            @Parameter(description = "任务ID") @PathVariable String taskId,
            @Parameter(description = "用户ID") @RequestParam String userId) {
        log.debug("REST request to assign task {} to user: {}", taskId, userId);
        
        try {
            cmmnTaskService.setAssignee(taskId, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to assign task {} to user {}: {}", taskId, userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 完成任务
     */
    @PostMapping("/{taskId}/complete")
    @Operation(summary = "完成任务", description = "完成指定的任务")
    public ResponseEntity<Void> completeTask(
            @Parameter(description = "任务ID") @PathVariable String taskId,
            @RequestBody(required = false) Map<String, Object> variables) {
        log.debug("REST request to complete task: {}", taskId);
        
        try {
            if (variables == null) {
                variables = new HashMap<>();
            }
            
            // Get task info to check task type
            Task task = cmmnTaskService.createTaskQuery().taskId(taskId).singleResult();
            if (task != null) {
                String taskKey = task.getTaskDefinitionKey();
                log.debug("Task definition key: {}, processInstanceId: {}, scopeId: {}", 
                    taskKey, task.getProcessInstanceId(), task.getScopeId());
                
                // Check if it's a review claim task - add DMN input variables
                if ("taskReviewClaim".equals(taskKey)) {
                    log.debug("Completing review claim task - adding DMN input variables");
                    addDmnInputVariables(task, variables);
                }
                
                // Check if it's a payment validation task - ensure enum values are strings
                if ("userTask_validatePayment".equals(taskKey)) {
                    log.debug("Completing payment validation task with variables: {}", variables);
                    // Ensure validation result is a string
                    Object validationResult = variables.get("validationResult");
                    if (validationResult != null) {
                        variables.put("validationResult", validationResult.toString());
                        log.debug("Set validationResult to: {}", variables.get("validationResult"));
                    }
                }
                
                // Check if it's a payment confirmation task
                if ("userTask_confirmPayment".equals(taskKey)) {
                    log.debug("Completing payment confirmation task with variables: {}", variables);
                    // Ensure confirmation result is a string
                    Object confirmationResult = variables.get("confirmationResult");
                    if (confirmationResult != null) {
                        variables.put("confirmationResult", confirmationResult.toString());
                        log.debug("Set confirmationResult to: {}", variables.get("confirmationResult"));
                    }
                }
                
                // Check if it's a dispute handling task
                if ("userTask_handleDispute".equals(taskKey)) {
                    log.debug("Completing dispute handling task with variables: {}", variables);
                    // Ensure dispute resolution is a string
                    Object disputeResolution = variables.get("disputeResolution");
                    if (disputeResolution != null) {
                        variables.put("disputeResolution", disputeResolution.toString());
                        log.debug("Set disputeResolution to: {}", variables.get("disputeResolution"));
                    }
                }
                
                // Check if it's a payment rejected task - set paymentStatus to "rejected"
                if ("userTask_paymentRejected".equals(taskKey)) {
                    log.debug("Completing payment rejected task - setting paymentStatus to 'rejected'");
                    variables.put("paymentStatus", "rejected");
                    log.debug("Set paymentStatus to: {}", variables.get("paymentStatus"));
                }
            }
            
            log.debug("Completing task {} with variables: {}", taskId, variables);
            
            // Determine if task is from BPMN or CMMN engine
            // BPMN tasks have processInstanceId, CMMN tasks have scopeId
            boolean isBpmnTask = task != null && task.getProcessInstanceId() != null;
            
            if (isBpmnTask) {
                log.debug("Task is from BPMN process, using taskService.complete()");
                taskService.complete(taskId, variables);
            } else {
                log.debug("Task is from CMMN case, using cmmnTaskService.complete()");
                cmmnTaskService.complete(taskId, variables);
            }
            
            log.debug("Task {} completed successfully", taskId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to complete task {}: {}", taskId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Add DMN decision table input variables for the decision task
     * These are required by the taskAssessComplexity decision task
     */
    private void addDmnInputVariables(Task task, Map<String, Object> variables) {
        String caseInstanceId = task.getScopeId();
        if (caseInstanceId != null) {
            // Find the claim case by case instance ID
            claimCaseRepository.findByCaseInstanceId(caseInstanceId).ifPresent(claimCase -> {
                log.debug("Found claim case {} for task {}", claimCase.getId(), task.getId());
                
                // CRITICAL: Include DMN decision table input variables
                // These are needed for the taskAssessComplexity decision task that will be triggered
                variables.put("policyType", claimCase.getPolicy().getPolicyType());
                variables.put("claimedAmount", claimCase.getClaimedAmount());
                variables.put("coverageAmount", claimCase.getPolicy().getCoverageAmount());
                variables.put("claimType", claimCase.getClaimType());
                variables.put("severity", claimCase.getSeverity().toString());
                
                log.debug("Added DMN input variables: policyType={}, claimedAmount={}, coverageAmount={}, claimType={}, severity={}",
                        variables.get("policyType"), variables.get("claimedAmount"), variables.get("coverageAmount"),
                        variables.get("claimType"), variables.get("severity"));
            });
        }
    }

    /**
     * 获取任务详情
     */
    @GetMapping("/{taskId}")
    @Operation(summary = "获取任务详情", description = "根据ID获取任务的详细信息")
    public ResponseEntity<TaskDTO> getTask(
            @Parameter(description = "任务ID") @PathVariable String taskId) {
        log.debug("REST request to get task: {}", taskId);
        
        Task task = cmmnTaskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();
        
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(convertToDTO(task));
    }

    /**
     * 获取任务变量
     */
    @GetMapping("/{taskId}/variables")
    @Operation(summary = "获取任务变量", description = "获取指定任务的变量信息")
    public ResponseEntity<Map<String, Object>> getTaskVariables(
            @Parameter(description = "任务ID") @PathVariable String taskId) {
        log.debug("REST request to get task variables: {}", taskId);
        
        try {
            Map<String, Object> variables = cmmnTaskService.getVariables(taskId);
            log.debug("Retrieved {} variables for task {}", variables.size(), taskId);
            return ResponseEntity.ok(variables);
        } catch (Exception e) {
            log.error("Failed to get variables for task {}: {}", taskId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取历史任务
     */
    @GetMapping("/history")
    @Operation(summary = "获取历史任务", description = "获取用户的历史任务列表")
    public ResponseEntity<Page<HistoricTaskInstance>> getHistoricTasks(
            @Parameter(description = "用户ID") @RequestParam String userId,
            Pageable pageable) {
        log.debug("REST request to get historic tasks for user: {}", userId);
        
        List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(userId)
                .finished()
                .orderByTaskCreateTime().desc()
                .listPage((int) pageable.getOffset(), pageable.getPageSize());
        
        long total = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(userId)
                .finished()
                .count();
        
        Page<HistoricTaskInstance> result = new PageImpl<>(tasks, pageable, total);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取任务统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取任务统计", description = "获取任务的统计信息")
    public ResponseEntity<Map<String, Object>> getTaskStatistics(
            @Parameter(description = "用户ID或用户名") @RequestParam(required = false) String userId) {
        log.debug("REST request to get task statistics for user: {}", userId);
        
        Map<String, Object> statistics = new HashMap<>();
        
        if (userId != null) {
            // Try to find user by UUID first, then use username for Flowable query
            String flowableUserId = userId;
            try {
                User user = userRepository.findById(UUID.fromString(userId)).orElse(null);
                if (user != null) {
                    flowableUserId = user.getUsername();
                    log.debug("Found user with UUID {}, using Flowable username: {}", userId, flowableUserId);
                }
            } catch (IllegalArgumentException e) {
                // userId is already a username, use it directly
                log.debug("userId is a username, using it directly: {}", userId);
            }
            
            // 我的待办任务数 - include both CMMN and BPMN
            long myTasksCount = cmmnTaskService.createTaskQuery()
                    .taskAssignee(flowableUserId)
                    .count()
                    + taskService.createTaskQuery()
                    .taskAssignee(flowableUserId)
                    .count();
            statistics.put("myTasksCount", myTasksCount);
            
            // 可认领任务数 - include both CMMN and BPMN
            long claimableTasksCount = cmmnTaskService.createTaskQuery()
                    .taskCandidateUser(flowableUserId)
                    .active()
                    .count()
                    + taskService.createTaskQuery()
                    .taskCandidateUser(flowableUserId)
                    .active()
                    .count();
            statistics.put("claimableTasksCount", claimableTasksCount);
            
            // 今日完成任务数
            Date todayStart = java.sql.Date.valueOf(java.time.LocalDate.now());
            long todayCompletedCount = historyService.createHistoricTaskInstanceQuery()
                    .taskAssignee(flowableUserId)
                    .finished()
                    .taskCompletedAfter(todayStart)
                    .count();
            statistics.put("todayCompletedCount", todayCompletedCount);
            
            log.debug("Task statistics for user {}: myTasks={}, claimable={}, todayCompleted={}", 
                    userId, myTasksCount, claimableTasksCount, todayCompletedCount);
        }
        
        // 总待办任务数 - include both CMMN and BPMN
        long totalActiveTasks = cmmnTaskService.createTaskQuery()
                .active()
                .count()
                + taskService.createTaskQuery()
                .active()
                .count();
        statistics.put("totalActiveTasks", totalActiveTasks);
        
        return ResponseEntity.ok(statistics);
    }

    /**
     * 获取特定理赔案件的任务
     */
    @GetMapping("/by-case/{caseInstanceId}")
    @Operation(summary = "获取理赔案件任务", description = "获取指定理赔案件的所有任务")
    public ResponseEntity<Map<String, Object>> getTasksByCase(
            @Parameter(description = "案件实例ID") @PathVariable String caseInstanceId,
            @Parameter(description = "用户ID") @RequestParam(required = false) String userId) {
        log.debug("REST request to get tasks for case instance: {}", caseInstanceId);
        
        // Get CMMN active tasks
        List<Task> cmmnActiveTasks = cmmnTaskService.createTaskQuery()
                .scopeId(caseInstanceId)
                .active()
                .orderByTaskCreateTime()
                .asc()
                .list();
        
        // Get BPMN active tasks - first find process instances with the caseInstanceId variable
        List<org.flowable.engine.runtime.ProcessInstance> bpmnProcessInstances = runtimeService
                .createProcessInstanceQuery()
                .variableValueEquals("caseInstanceId", caseInstanceId)
                .list();
        
        log.debug("Found {} BPMN process instances for case {}", bpmnProcessInstances.size(), caseInstanceId);
        
        List<Task> bpmnActiveTasks = new ArrayList<>();
        for (org.flowable.engine.runtime.ProcessInstance processInstance : bpmnProcessInstances) {
            List<Task> tasks = taskService.createTaskQuery()
                    .processInstanceId(processInstance.getId())
                    .active()
                    .list();
            bpmnActiveTasks.addAll(tasks);
            log.debug("Process instance {} has {} active tasks", processInstance.getId(), tasks.size());
        }
        
        // Merge active tasks
        Set<String> taskIds = new HashSet<>();
        List<Task> allActiveTasks = new ArrayList<>();
        
        for (Task task : cmmnActiveTasks) {
            if (taskIds.add(task.getId())) {
                allActiveTasks.add(task);
            }
        }
        
        for (Task task : bpmnActiveTasks) {
            if (taskIds.add(task.getId())) {
                allActiveTasks.add(task);
            }
        }
        
        log.debug("Found {} active tasks for case {} (CMMN: {}, BPMN: {})", 
                allActiveTasks.size(), caseInstanceId, cmmnActiveTasks.size(), bpmnActiveTasks.size());
        
        // 获取历史任务 - combine CMMN and BPMN historic tasks
        List<HistoricTaskInstance> cmmnHistoricTasks = historyService.createHistoricTaskInstanceQuery()
                .scopeId(caseInstanceId)
                .finished()
                .orderByHistoricTaskInstanceEndTime()
                .desc()
                .list();
        
        // Get historic BPMN tasks - find process instances first
        List<org.flowable.engine.runtime.ProcessInstance> bpmnHistoricProcessInstances = runtimeService
                .createProcessInstanceQuery()
                .variableValueEquals("caseInstanceId", caseInstanceId)
                .includeProcessVariables()
                .list();
        
        Set<String> historicProcessInstanceIds = bpmnHistoricProcessInstances.stream()
                .map(org.flowable.engine.runtime.ProcessInstance::getId)
                .collect(Collectors.toSet());
        
        List<HistoricTaskInstance> bpmnHistoricTasks = new ArrayList<>();
        for (String processInstanceId : historicProcessInstanceIds) {
            List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .finished()
                    .orderByHistoricTaskInstanceEndTime()
                    .desc()
                    .list();
            bpmnHistoricTasks.addAll(tasks);
        }
        
        // Merge historic tasks
        Set<String> historicTaskIds = new HashSet<>();
        List<HistoricTaskInstance> allHistoricTasks = new ArrayList<>();
        
        for (HistoricTaskInstance task : cmmnHistoricTasks) {
            if (historicTaskIds.add(task.getId())) {
                allHistoricTasks.add(task);
            }
        }
        
        for (HistoricTaskInstance task : bpmnHistoricTasks) {
            if (historicTaskIds.add(task.getId())) {
                allHistoricTasks.add(task);
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("activeTasks", allActiveTasks.stream().map(this::convertToDTO).collect(Collectors.toList()));
        result.put("historicTasks", allHistoricTasks.stream().map(this::convertToHistoricDTO).collect(Collectors.toList()));
        
        // 如果提供了用户ID，过滤并标记当前用户可以执行的任务
        if (userId != null) {
            List<Task> availableForMeRaw = allActiveTasks.stream()
                    .filter(task -> isTaskAvailableForUser(task, userId))
                    .collect(Collectors.toList());
            result.put("availableForMe", availableForMeRaw.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList()));
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 检查任务是否对指定用户可用（可执行）
     */
    private boolean isTaskAvailableForUser(Task task, String userId) {
        // Convert userId (UUID) to username for comparison with Flowable task assignee
        String username = userId;
        try {
            User user = userRepository.findById(UUID.fromString(userId)).orElse(null);
            if (user != null) {
                username = user.getUsername();
                log.debug("Converted userId {} to username {}", userId, username);
            }
        } catch (IllegalArgumentException e) {
            // userId is already a username, use it directly
            log.debug("userId is a username, using it directly: {}", userId);
        }
        
        // 任务已分配给该用户
        if (username.equals(task.getAssignee())) {
            log.debug("Task {} is assigned to user {} (username: {})", task.getId(), userId, username);
            return true;
        }
        
        // 任务是该用户的候选任务
        List<org.flowable.identitylink.api.IdentityLink> identityLinks = 
                cmmnTaskService.getIdentityLinksForTask(task.getId());
        for (org.flowable.identitylink.api.IdentityLink link : identityLinks) {
            if ("candidate".equals(link.getType())) {
                if (username.equals(link.getUserId())) {
                    return true;
                }
                if (link.getGroupId() != null) {
                    // 检查用户是否属于该组
                    try {
                        User user = userRepository.findById(UUID.fromString(userId))
                                .orElse(userRepository.findByUsername(userId).orElse(null));
                        if (user != null) {
                            for (Role role : user.getRoles()) {
                                String flowableGroup = mapRoleToGroup(role.getName());
                                if (flowableGroup != null && flowableGroup.equals(link.getGroupId())) {
                                    return true;
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.debug("Error checking group membership for user {}: {}", userId, e.getMessage());
                    }
                }
            }
        }
        
        log.debug("Task {} is not available for user {} (username: {})", task.getId(), userId, username);
        return false;
    }

    /**
     * 转换历史任务为DTO
     */
    private TaskDTO convertToHistoricDTO(HistoricTaskInstance historicTask) {
        TaskDTO dto = new TaskDTO();
        dto.setId(historicTask.getId());
        dto.setName(historicTask.getName());
        dto.setDescription(historicTask.getDescription());
        dto.setAssignee(historicTask.getAssignee());
        dto.setOwner(historicTask.getOwner());
        dto.setProcessInstanceId(historicTask.getProcessInstanceId());
        dto.setCaseInstanceId(historicTask.getScopeId());
        dto.setTaskDefinitionKey(historicTask.getTaskDefinitionKey());
        dto.setFormKey(historicTask.getFormKey());
        dto.setPriority(historicTask.getPriority());
        
        if (historicTask.getCreateTime() != null) {
            dto.setCreateTime(convertToLocalDateTime(historicTask.getCreateTime()));
        }
        if (historicTask.getEndTime() != null) {
            dto.setDueDate(convertToLocalDateTime(historicTask.getEndTime()));
        }
        
        return dto;
    }

    /**
     * 转换为 DTO
     */
    private TaskDTO convertToDTO(Task task) {
        TaskDTO dto = new TaskDTO();
        dto.setId(task.getId());
        dto.setName(task.getName());
        dto.setDescription(task.getDescription());
        dto.setAssignee(task.getAssignee());
        dto.setOwner(task.getOwner());
        dto.setProcessInstanceId(task.getProcessInstanceId());
        dto.setCaseInstanceId(task.getScopeId());
        dto.setTaskDefinitionKey(task.getTaskDefinitionKey());
        dto.setFormKey(task.getFormKey());
        dto.setPriority(task.getPriority());
        
        // 转换日期类型
        if (task.getCreateTime() != null) {
            dto.setCreateTime(convertToLocalDateTime(task.getCreateTime()));
        }
        if (task.getDueDate() != null) {
            dto.setDueDate(convertToLocalDateTime(task.getDueDate()));
        }
        
        dto.setCategory(task.getCategory());
        dto.setTenantId(task.getTenantId());
        dto.setSuspended(task.isSuspended());
        
        // 获取候选用户和组
        dto.setCandidateUsers(cmmnTaskService.getIdentityLinksForTask(task.getId()).stream()
                .filter(link -> "candidate".equals(link.getType()) && link.getUserId() != null)
                .map(link -> link.getUserId())
                .collect(Collectors.toList()));
        
        dto.setCandidateGroups(cmmnTaskService.getIdentityLinksForTask(task.getId()).stream()
                .filter(link -> "candidate".equals(link.getType()) && link.getGroupId() != null)
                .map(link -> link.getGroupId())
                .collect(Collectors.toList()));
        
        return dto;
    }
    
    /**
     * Date 转 LocalDateTime
     */
    private LocalDateTime convertToLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
