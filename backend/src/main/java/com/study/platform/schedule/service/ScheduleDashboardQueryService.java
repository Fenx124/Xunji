package com.study.platform.schedule.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 看板读取日程统计的只读入口。看板不注入 ScheduleMapper。
 */
public interface ScheduleDashboardQueryService {

    record Completion(long completed, long nonCancelled) {
    }

    long countPending(Long userId, LocalDateTime startInclusive, LocalDateTime endExclusive);

    Completion monthCompletion(Long userId, LocalDateTime startInclusive, LocalDateTime endExclusive);

    Map<LocalDate, Long> countCompletedByDay(Long userId, LocalDateTime startInclusive, LocalDateTime endExclusive);
}
