package com.study.platform.schedule.support;

import java.time.LocalDateTime;

/**
 * 提醒时间被改到未来时，把 reminded 复位为未发送，让后续扫描可以再次提醒。
 */
public final class ScheduleRemindPolicy {

    private ScheduleRemindPolicy() {
    }

    public static boolean shouldReset(LocalDateTime previous, LocalDateTime next, LocalDateTime now) {
        if (next == null || now == null) {
            return false;
        }
        if (next.equals(previous)) {
            return false;
        }
        return next.isAfter(now);
    }
}
