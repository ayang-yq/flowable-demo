package com.flowable.demo.admin.web;

import com.flowable.demo.admin.service.AdminStatisticsService;
import com.flowable.demo.admin.web.dto.AdminStatisticsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin 统计 REST API
 */
@Slf4j
@RestController
@RequestMapping("/admin/statistics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminStatisticsResource {

    private final AdminStatisticsService adminStatisticsService;

    /**
     * 获取系统统计信息
     * 
     * @return 统计信息(模型、部署、Case、Process)
     */
    @GetMapping
    public ResponseEntity<AdminStatisticsDTO> getStatistics() {
        log.info("Get admin statistics");

        AdminStatisticsDTO statistics = adminStatisticsService.getStatistics();
        return ResponseEntity.ok(statistics);
    }
}
