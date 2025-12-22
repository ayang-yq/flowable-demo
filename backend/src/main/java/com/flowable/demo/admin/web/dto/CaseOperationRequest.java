package com.flowable.demo.admin.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Case 操作请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseOperationRequest {

    /**
     * 操作原因/说明
     */
    private String reason;
}
