package com.flowable.demo.web.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 批准理赔请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "理赔批准请求")
public class ApproveRequestDTO {

    @Schema(description = "批准金额", required = true)
    @NotNull(message = "批准金额不能为空")
    @Positive(message = "批准金额必须大于0")
    private BigDecimal approvedAmount;

    @Schema(description = "审批意见")
    private String comments;
}
