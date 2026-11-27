package com.study.platform.schedule.support;

import com.study.platform.common.ErrorCode;
import com.study.platform.common.exception.BusinessException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 日程查询时间：yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss。
 * 只有日期时，开始边界取当天 00:00:00，结束边界取当天 23:59:59。
 */
public final class ScheduleTimeParser {

    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final String DATE_TIME_HINT = "时间格式应为 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss";
    public static final String DATE_HINT = "复习基准日期格式应为 yyyy-MM-dd";

    private ScheduleTimeParser() {
    }

    public static LocalDateTime parseBound(String raw, boolean endInclusive) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String text = raw.trim();
        try {
            if (text.length() == 10) {
                LocalDate date = LocalDate.parse(text, DATE);
                return endInclusive ? date.atTime(23, 59, 59) : date.atStartOfDay();
            }
            return LocalDateTime.parse(text, DATE_TIME);
        } catch (DateTimeParseException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, DATE_TIME_HINT);
        }
    }

    public static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, DATE_HINT);
        }
        try {
            return LocalDate.parse(raw.trim(), DATE);
        } catch (DateTimeParseException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, DATE_HINT);
        }
    }
}
