package com.study.platform.ai.support;

import java.time.LocalDateTime;

/**
 * 规范化之后的日程字段。时间均已落到 Asia/Shanghai 的本地时钟。
 */
public record ParsedScheduleDraft(
        String title,
        String description,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocalDateTime remindTime,
        int priority
) {
}
