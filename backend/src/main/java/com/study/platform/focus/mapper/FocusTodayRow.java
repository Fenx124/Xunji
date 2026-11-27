package com.study.platform.focus.mapper;

/**
 * 今日专注聚合：完成次数、实际分钟、全部场次。
 */
public class FocusTodayRow {

    private Long completedCount;
    private Long totalMinutes;
    private Long totalSessions;

    public Long getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(Long completedCount) {
        this.completedCount = completedCount;
    }

    public Long getTotalMinutes() {
        return totalMinutes;
    }

    public void setTotalMinutes(Long totalMinutes) {
        this.totalMinutes = totalMinutes;
    }

    public Long getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(Long totalSessions) {
        this.totalSessions = totalSessions;
    }
}
