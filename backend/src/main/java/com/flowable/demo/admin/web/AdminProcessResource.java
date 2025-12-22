package com.flowable.demo.admin.web;

import com.flowable.demo.admin.service.ProcessRuntimeService;
import com.flowable.demo.admin.web.dto.CaseOperationRequest;
import com.flowable.demo.admin.web.dto.ProcessDiagramDTO;
import com.flowable.demo.admin.web.dto.ProcessInstanceDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Admin Process 管理 REST API
 */
@Slf4j
@RestController
@RequestMapping("/admin/processes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminProcessResource {

    private final ProcessRuntimeService processRuntimeService;

    /**
     * 查询 Process 实例列表
     * 
     * @param processDefinitionKey Process 定义 Key (可选)
     * @param businessKey          业务 Key (可选)
     * @param startedAfter         开始时间之后 (可选)
     * @param pageable             分页参数
     * @return Process 实例列表
     */
    @GetMapping
    public ResponseEntity<Page<ProcessInstanceDTO>> queryProcessInstances(
            @RequestParam(required = false) String processDefinitionKey,
            @RequestParam(required = false) String businessKey,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startedAfter,
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("Query process instances: key={}, businessKey={}, page={}",
                processDefinitionKey, businessKey, pageable.getPageNumber());

        Page<ProcessInstanceDTO> processes = processRuntimeService.queryProcessInstances(
                processDefinitionKey,
                businessKey,
                startedAfter,
                pageable);

        return ResponseEntity.ok(processes);
    }

    /**
     * 获取 Process 实例详情
     * 
     * @param processInstanceId Process 实例 ID
     * @return Process 实例详情(包含变量和活动历史)
     */
    @GetMapping("/{processInstanceId}")
    public ResponseEntity<ProcessInstanceDTO> getProcessInstanceDetail(
            @PathVariable String processInstanceId) {
        log.info("Get process instance detail: {}", processInstanceId);

        ProcessInstanceDTO processInstance = processRuntimeService.getProcessInstanceDetail(processInstanceId);

        if (processInstance == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(processInstance);
    }

    /**
     * 获取 Process 流程图(高亮数据)
     * 
     * @param processInstanceId Process 实例 ID
     * @return 流程图 XML 和高亮数据
     */
    @GetMapping("/{processInstanceId}/diagram")
    public ResponseEntity<ProcessDiagramDTO> getProcessDiagram(
            @PathVariable String processInstanceId) {
        log.info("Get process diagram: {}", processInstanceId);

        ProcessDiagramDTO diagram = processRuntimeService.getProcessDiagram(processInstanceId);

        if (diagram == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(diagram);
    }

    /**
     * 终止 Process 实例
     * 
     * @param processInstanceId Process 实例 ID
     * @param request           操作请求(包含原因)
     * @return 操作结果
     */
    @PostMapping("/{processInstanceId}/terminate")
    public ResponseEntity<Map<String, Object>> terminateProcess(
            @PathVariable String processInstanceId,
            @RequestBody(required = false) CaseOperationRequest request) {
        log.info("Terminate process instance: {}", processInstanceId);

        String reason = request != null ? request.getReason() : null;
        processRuntimeService.terminateProcess(processInstanceId, reason);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Process 已终止");
        response.put("terminatedTime", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    /**
     * 挂起 Process 实例
     * 
     * @param processInstanceId Process 实例 ID
     * @return 操作结果
     */
    @PostMapping("/{processInstanceId}/suspend")
    public ResponseEntity<Map<String, Object>> suspendProcess(
            @PathVariable String processInstanceId) {
        log.info("Suspend process instance: {}", processInstanceId);

        processRuntimeService.suspendProcess(processInstanceId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("state", "SUSPENDED");

        return ResponseEntity.ok(response);
    }

    /**
     * 恢复 Process 实例
     * 
     * @param processInstanceId Process 实例 ID
     * @return 操作结果
     */
    @PostMapping("/{processInstanceId}/resume")
    public ResponseEntity<Map<String, Object>> resumeProcess(
            @PathVariable String processInstanceId) {
        log.info("Resume process instance: {}", processInstanceId);

        processRuntimeService.resumeProcess(processInstanceId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("state", "ACTIVE");

        return ResponseEntity.ok(response);
    }
}
