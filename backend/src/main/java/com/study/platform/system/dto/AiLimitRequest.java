package com.study.platform.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "调整 AI 每日配额")
public record AiLimitRequest(
        @Schema(description = "每日调用上限，0 到 10000", example = "100")
        @NotNull(message = "AI 每日配额不能为空")
        @Min(value = 0, message = "AI 每日配额需在 0 到 10000 之间")
        @Max(value = 10000, message = "AI 每日配额需在 0 到 10000 之间")
        Integer aiDailyLimit
) {
}
