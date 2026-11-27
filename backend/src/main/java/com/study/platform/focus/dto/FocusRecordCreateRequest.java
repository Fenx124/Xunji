package com.study.platform.focus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Schema(description = "上报一次专注记录")
public record FocusRecordCreateRequest(
        @Schema(description = "关联笔记 ID，可空")
        Long noteId,

        @Schema(description = "关联日程 ID，可空")
        Long scheduleId,

        @Schema(description = "计划专注分钟", example = "25")
        @NotNull(message = "计划专注分钟数不能为空")
        @Min(value = 1, message = "计划专注分钟数需在 1 到 240 之间")
        @Max(value = 240, message = "计划专注分钟数需在 1 到 240 之间")
        Integer plannedMinutes,

        @Schema(description = "实际专注分钟", example = "25")
        @NotNull(message = "实际专注分钟数不能为空")
        @Min(value = 0, message = "实际专注分钟数需在 0 到 300 之间")
        @Max(value = 300, message = "实际专注分钟数需在 0 到 300 之间")
        Integer actualMinutes,

        @Schema(description = "COMPLETED 或 ABORTED", example = "COMPLETED")
        @NotBlank(message = "专注状态不能为空")
        @Pattern(regexp = "COMPLETED|ABORTED", message = "专注状态只能是 COMPLETED 或 ABORTED")
        String status,

        @Schema(description = "开始时间", example = "2026-11-20 21:00:00")
        @NotNull(message = "开始时间不能为空")
        LocalDateTime startedAt,

        @Schema(description = "结束时间", example = "2026-11-20 21:25:00")
        @NotNull(message = "结束时间不能为空")
        LocalDateTime endedAt,

        @Schema(description = "专注小结")
        @Size(max = 255, message = "专注备注长度不能超过 255")
        String remark
) {
}
