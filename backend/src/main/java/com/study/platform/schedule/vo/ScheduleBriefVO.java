package com.study.platform.schedule.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "笔记详情里的关联日程摘要")
public record ScheduleBriefVO(
        @Schema(description = "日程 ID")
        Long id,
        @Schema(description = "标题")
        String title,
        @Schema(description = "开始时间")
        LocalDateTime startTime,
        @Schema(description = "状态：0 待办，1 已完成，2 已取消")
        Integer status,
        @Schema(description = "优先级 1-4")
        Integer priority,
        @Schema(description = "来源：MANUAL / EBBINGHAUS / AI")
        String sourceType
) {
}
