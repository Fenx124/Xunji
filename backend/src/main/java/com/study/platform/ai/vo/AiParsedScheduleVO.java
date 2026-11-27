package com.study.platform.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "自然语言解析出的日程，确认后由前端调用创建日程接口")
public record AiParsedScheduleVO(
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
        @Schema(description = "优先级 1-4")
        int priority,
        @Schema(description = "是否走了本地降级")
        boolean fallback
) {
}
