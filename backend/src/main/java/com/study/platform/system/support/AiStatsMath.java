package com.study.platform.system.support;

import com.study.platform.common.StudyDates;

/**
 * AI 用量的比率、平均耗时，以及 days 参数夹紧到 1..90。
 */
public final class AiStatsMath {

    public static final int DEFAULT_DAYS = 7;
    public static final int MIN_DAYS = 1;
    public static final int MAX_DAYS = 90;

    private AiStatsMath() {
    }

    public static int clampDays(Integer days) {
        if (days == null) {
            return DEFAULT_DAYS;
        }
        if (days < MIN_DAYS) {
            return MIN_DAYS;
        }
        return Math.min(days, MAX_DAYS);
    }

    /** 成功次数 / 总次数 * 100，保留 1 位小数。没有调用时为 0.0。 */
    public static double successRate(long successCalls, long totalCalls) {
        return StudyDates.percent(successCalls, totalCalls);
    }

    public static long averageLatency(long totalLatencyMs, long totalCalls) {
        if (totalCalls <= 0 || totalLatencyMs <= 0) {
            return 0L;
        }
        return Math.round((double) totalLatencyMs / (double) totalCalls);
    }
}
