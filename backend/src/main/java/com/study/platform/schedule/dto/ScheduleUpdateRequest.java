package com.study.platform.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Schema(description = "更新日程")
public record ScheduleUpdateRequest(
        @Schema(description = "标题")
        @NotBlank(message = "日程标题不能为空")
        @Size(max = 160, message = "日程标题长度不能超过 160")
        String title,

        @Schema(description = "详情，空则清空")
        @Size(max = 1000, message = "日程详情长度不能超过 1000")
        String description,

        @Schema(description = "关联笔记 ID，空则解除关联")
        Long noteId,

        @Schema(description = "开始时间", example = "2026-11-21 09:00:00")
        @NotNull(message = "开始时间不能为空")
        LocalDateTime startTime,

        @Schema(description = "结束时间，空则清空")
        LocalDateTime endTime,

        @Schema(description = "提醒时间，空则清空")
        LocalDateTime remindTime,

        @Schema(description = "优先级 1-4，缺省保持原值")
        @Min(value = 1, message = "优先级取值范围为 1 到 4")
        @Max(value = 4, message = "优先级取值范围为 1 到 4")
        Integer priority,

        @Schema(description = "状态 0 待办 / 1 已完成 / 2 已取消，缺省保持原值")
        @Min(value = 0, message = "状态取值范围为 0 到 2")
        @Max(value = 2, message = "状态取值范围为 0 到 2")
        Integer status
) {
}
