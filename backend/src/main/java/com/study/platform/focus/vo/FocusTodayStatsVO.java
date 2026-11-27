package com.study.platform.focus.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "今日专注统计")
public record FocusTodayStatsVO(
        @Schema(description = "今日完成次数", example = "2")
        long completedCount,
        @Schema(description = "今日实际专注分钟", example = "50")
        long totalMinutes,
        @Schema(description = "今日全部场次，含中止", example = "3")
        long totalSessions
) {
}
