package com.study.platform.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "启用或禁用用户")
public record UserStatusRequest(
        @Schema(description = "1 正常，0 禁用", example = "0")
        @NotNull(message = "账号状态不能为空")
        @Min(value = 0, message = "账号状态只能是 0 或 1")
        @Max(value = 1, message = "账号状态只能是 0 或 1")
        Integer status
) {
}
