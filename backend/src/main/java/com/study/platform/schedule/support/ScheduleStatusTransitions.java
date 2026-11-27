package com.study.platform.schedule.support;

import java.time.LocalDateTime;

/**
 * 日程状态迁移：进入已完成时写入 completedAt，回到待办或取消时清空。
 * 状态没变则保留原来的完成时间。
 */
public final class ScheduleStatusTransitions {

    public record Result(int status, LocalDateTime completedAt) {
    }

    private ScheduleStatusTransitions() {
    }

    public static Result apply(int oldStatus, LocalDateTime oldCompletedAt, int newStatus, LocalDateTime now) {
        if (newStatus < 0 || newStatus > 2) {
            throw new IllegalArgumentException("status 取值范围为 0 到 2");
        }
        if (newStatus == oldStatus) {
            return new Result(oldStatus, oldCompletedAt);
        }
        if (newStatus == 1) {
            return new Result(1, now);
        }
        return new Result(newStatus, null);
    }
}
