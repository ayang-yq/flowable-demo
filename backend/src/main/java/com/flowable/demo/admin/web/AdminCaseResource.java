package com.flowable.demo.admin.web;

import com.flowable.demo.admin.service.CaseRuntimeService;
import com.flowable.demo.admin.web.dto.CaseInstanceDTO;
import com.flowable.demo.admin.web.dto.CaseOperationRequest;
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
 * Admin Case 管理 REST API
 */
@Slf4j
@RestController
@RequestMapping("/admin/cases")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminCaseResource {

    private final CaseRuntimeService caseRuntimeService;

    /**
     * 查询 Case 实例列表
     * 
     * @param caseDefinitionKey Case 定义 Key (可选)
     * @param businessKey       业务 Key (可选)
     * @param state             状态: ACTIVE, COMPLETED, TERMINATED, SUSPENDED (可选)
     * @param startedAfter      开始时间之后 (可选)
     * @param pageable          分页参数
     * @return Case 实例列表
     */
    @GetMapping
    public ResponseEntity<Page<CaseInstanceDTO>> queryCaseInstances(
            @RequestParam(required = false) String caseDefinitionKey,
            @RequestParam(required = false) String businessKey,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startedAfter,
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("Query case instances: key={}, businessKey={}, state={}, page={}",
                caseDefinitionKey, businessKey, state, pageable.getPageNumber());

        Page<CaseInstanceDTO> cases = caseRuntimeService.queryCaseInstances(
                caseDefinitionKey,
                businessKey,
                state,
                startedAfter,
                pageable);

        return ResponseEntity.ok(cases);
    }

    /**
     * 获取 Case 实例详情
     * 
     * @param caseInstanceId Case 实例 ID
     * @return Case 实例详情(包含变量和 Plan Item Tree)
     */
    @GetMapping("/{caseInstanceId}")
    public ResponseEntity<CaseInstanceDTO> getCaseInstanceDetail(
            @PathVariable String caseInstanceId) {
        log.info("Get case instance detail: {}", caseInstanceId);

        CaseInstanceDTO caseInstance = caseRuntimeService.getCaseInstanceDetail(caseInstanceId);

        if (caseInstance == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(caseInstance);
    }

    /**
     * 终止 Case 实例
     * 
     * @param caseInstanceId Case 实例 ID
     * @param request        操作请求(包含原因)
     * @return 操作结果
     */
    @PostMapping("/{caseInstanceId}/terminate")
    public ResponseEntity<Map<String, Object>> terminateCase(
            @PathVariable String caseInstanceId,
            @RequestBody(required = false) CaseOperationRequest request) {
        log.info("Terminate case instance: {}", caseInstanceId);

        String reason = request != null ? request.getReason() : null;
        caseRuntimeService.terminateCase(caseInstanceId, reason);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Case 已终止");
        response.put("terminatedTime", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    /**
     * 挂起 Case 实例
     * 
     * @param caseInstanceId Case 实例 ID
     * @return 操作结果
     */
    @PostMapping("/{caseInstanceId}/suspend")
    public ResponseEntity<Map<String, Object>> suspendCase(
            @PathVariable String caseInstanceId) {
        log.info("Suspend case instance: {}", caseInstanceId);

        caseRuntimeService.suspendCase(caseInstanceId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("state", "SUSPENDED");

        return ResponseEntity.ok(response);
    }

    /**
     * 恢复 Case 实例
     * 
     * @param caseInstanceId Case 实例 ID
     * @return 操作结果
     */
    @PostMapping("/{caseInstanceId}/resume")
    public ResponseEntity<Map<String, Object>> resumeCase(
            @PathVariable String caseInstanceId) {
        log.info("Resume case instance: {}", caseInstanceId);

        caseRuntimeService.resumeCase(caseInstanceId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("state", "ACTIVE");

        return ResponseEntity.ok(response);
    }

    /**
     * 手动触发 Plan Item
     * 
     * @param caseInstanceId     Case 实例 ID
     * @param planItemInstanceId Plan Item 实例 ID
     * @return 操作结果
     */
    @PostMapping("/{caseInstanceId}/plan-items/{planItemInstanceId}/trigger")
    public ResponseEntity<Map<String, Object>> triggerPlanItem(
            @PathVariable String caseInstanceId,
            @PathVariable String planItemInstanceId) {
        log.info("Trigger plan item: caseId={}, planItemId={}", caseInstanceId, planItemInstanceId);

        caseRuntimeService.triggerPlanItem(planItemInstanceId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Plan Item 已触发");

        return ResponseEntity.ok(response);
    }
}
