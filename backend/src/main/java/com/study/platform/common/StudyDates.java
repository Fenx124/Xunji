package com.study.platform.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 业务日界统一用 Asia/Shanghai。
 * 完成率按百分比保留 1 位小数，分母为 0 时返回 0.0。
 */
public final class StudyDates {

    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private StudyDates() {
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    /**
     * 以 end 为最后一天、向前共 days 天，按时间正序排列。days=7 时包含 end 当天。
     */
    public static List<LocalDate> inclusiveDaysEnding(LocalDate end, int days) {
        if (end == null) {
            throw new IllegalArgumentException("end");
        }
        if (days < 1) {
            throw new IllegalArgumentException("days");
        }
        LocalDate start = end.minusDays(days - 1L);
        List<LocalDate> dates = new ArrayList<>(days);
        for (int i = 0; i < days; i++) {
            dates.add(start.plusDays(i));
        }
        return dates;
    }

    public static LocalDateTime startOf(LocalDate date) {
        return date.atStartOfDay();
    }

    public static LocalDateTime nextDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay();
    }

    /**
     * numerator / denominator * 100，四舍五入到 1 位小数。任一端不是正数时返回 0.0。
     */
    public static double percent(long numerator, long denominator) {
        if (numerator <= 0 || denominator <= 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
