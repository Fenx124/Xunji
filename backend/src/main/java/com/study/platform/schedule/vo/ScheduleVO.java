package com.study.platform.schedule.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "日程")
public record ScheduleVO(
        @Schema(description = "日程 ID")
        Long id,
        @Schema(description = "关联笔记 ID")
        Long noteId,
        @Schema(description = "关联笔记标题")
        String noteTitle,
        @Schema(description = "标题")
        String title,
        @Schema(description = "详情")
        String description,
        @Schema(description = "开始时间")
        LocalDateTime startTime,
        @Schema(description = "结束时间")
        LocalDateTime endTime,
        @Schema(description = "提醒时间")
        LocalDateTime remindTime,
        @Schema(description = "是否已发送提醒")
        @JsonProperty("reminded")
        boolean reminded,
        @Schema(description = "优先级 1-4")
        Integer priority,
        @Schema(description = "状态：0 待办，1 已完成，2 已取消")
        Integer status,
        @Schema(description = "来源：MANUAL / EBBINGHAUS / AI")
        String sourceType,
        @Schema(description = "完成时间")
        LocalDateTime completedAt,
        @Schema(description = "创建时间")
        LocalDateTime createdAt,
        @Schema(description = "更新时间")
        LocalDateTime updatedAt
) {
}
