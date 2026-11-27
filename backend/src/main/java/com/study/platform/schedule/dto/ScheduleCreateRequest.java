package com.study.platform.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Schema(description = "新建日程")
public record ScheduleCreateRequest(
        @Schema(description = "标题", example = "复习高数")
        @NotBlank(message = "日程标题不能为空")
        @Size(max = 160, message = "日程标题长度不能超过 160")
        String title,

        @Schema(description = "详情")
        @Size(max = 1000, message = "日程详情长度不能超过 1000")
        String description,

        @Schema(description = "关联笔记 ID")
        Long noteId,

        @Schema(description = "开始时间", example = "2026-11-21 09:00:00")
        @NotNull(message = "开始时间不能为空")
        LocalDateTime startTime,

        @Schema(description = "结束时间", example = "2026-11-21 09:30:00")
        LocalDateTime endTime,

        @Schema(description = "提醒时间", example = "2026-11-21 08:45:00")
        LocalDateTime remindTime,

        @Schema(description = "优先级 1-4，缺省为 2", example = "2")
        @Min(value = 1, message = "优先级取值范围为 1 到 4")
        @Max(value = 4, message = "优先级取值范围为 1 到 4")
        Integer priority,

        @Schema(description = "来源：MANUAL 或 AI，缺省 MANUAL。艾宾浩斯来源不能由该字段写入", example = "MANUAL")
        @Size(max = 20, message = "日程来源长度不能超过 20")
        String sourceType
) {
}
