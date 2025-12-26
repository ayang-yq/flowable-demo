package com.flowable.demo.web.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 支付请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "理赔支付请求")
public class PaymentRequestDTO {

    @Schema(description = "支付金额", required = true)
    @NotNull(message = "支付金额不能为空")
    @Positive(message = "支付金额必须大于0")
    private BigDecimal paymentAmount;

    @Schema(description = "支付日期", required = true)
    @NotNull(message = "支付日期不能为空")
    private LocalDate paymentDate;

    @Schema(description = "支付方式", example = "TRANSFER")
    private String paymentMethod;

    @Schema(description = "支付参考号", example = "PAY-20241226-001")
    private String paymentReference;

    @Schema(description = "支付用户ID")
    private String userId;
}
