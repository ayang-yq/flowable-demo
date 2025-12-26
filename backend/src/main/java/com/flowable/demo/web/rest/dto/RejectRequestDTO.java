package com.flowable.demo.web.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 拒绝理赔请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "理赔拒绝请求")
public class RejectRequestDTO {

    @Schema(description = "拒绝原因", required = true)
    @NotBlank(message = "拒绝原因不能为空")
    private String reason;
}
