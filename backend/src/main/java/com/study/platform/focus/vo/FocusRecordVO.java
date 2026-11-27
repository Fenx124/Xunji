package com.study.platform.focus.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "专注记录")
public record FocusRecordVO(
        @Schema(description = "记录 ID")
        Long id,
        @Schema(description = "关联笔记 ID")
        Long noteId,
        @Schema(description = "关联笔记标题")
        String noteTitle,
        @Schema(description = "关联日程 ID")
        Long scheduleId,
        @Schema(description = "关联日程标题")
        String scheduleTitle,
        @Schema(description = "计划专注分钟")
        Integer plannedMinutes,
        @Schema(description = "实际专注分钟")
        Integer actualMinutes,
        @Schema(description = "COMPLETED / ABORTED")
        String status,
        @Schema(description = "开始时间")
        LocalDateTime startedAt,
        @Schema(description = "结束时间")
        LocalDateTime endedAt,
        @Schema(description = "专注小结")
        String remark,
        @Schema(description = "入库时间")
        LocalDateTime createdAt
) {
}
