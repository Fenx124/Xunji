package com.study.platform.focus.service;

import com.study.platform.focus.vo.FocusTodayStatsVO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 看板读取专注统计的只读入口。看板不注入 FocusRecordMapper。
 */
public interface FocusStatsQueryService {

    FocusTodayStatsVO todayStats(Long userId, LocalDateTime startInclusive, LocalDateTime endExclusive);

    long sumMinutes(Long userId, LocalDateTime startInclusive, LocalDateTime endExclusive);

    Map<LocalDate, Long> countStartedByDay(Long userId, LocalDateTime startInclusive, LocalDateTime endExclusive);

    Map<LocalDate, Long> sumMinutesByDay(Long userId, LocalDateTime startInclusive, LocalDateTime endExclusive);
}
