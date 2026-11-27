package com.study.platform.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "切换日程状态")
public record ScheduleStatusRequest(
        @Schema(description = "0 待办，1 已完成，2 已取消", example = "1")
        @NotNull(message = "状态不能为空")
        @Min(value = 0, message = "状态取值范围为 0 到 2")
        @Max(value = 2, message = "状态取值范围为 0 到 2")
        Integer status
) {
}
