package com.study.platform.focus.service;

import com.study.platform.common.DayTotalRow;
import com.study.platform.focus.mapper.FocusRecordMapper;
import com.study.platform.focus.mapper.FocusTodayRow;
import com.study.platform.focus.vo.FocusTodayStatsVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FocusStatsQueryServiceImpl implements FocusStatsQueryService {

    private final FocusRecordMapper focusRecordMapper;

    public FocusStatsQueryServiceImpl(FocusRecordMapper focusRecordMapper) {
        this.focusRecordMapper = focusRecordMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public FocusTodayStatsVO todayStats(Long userId, LocalDateTime startInclusive, LocalDateTime endExclusive) {
        FocusTodayRow row = focusRecordMapper.summarize(userId, startInclusive, endExclusive);
        if (row == null) {
            return new FocusTodayStatsVO(0L, 0L, 0L);
        }
        return new FocusTodayStatsVO(
                value(row.getCompletedCount()),
                value(row.getTotalMinutes()),
                value(row.getTotalSessions())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public long sumMinutes(Long userId, LocalDateTime startInclusive, LocalDateTime endExclusive) {
        Long total = focusRecordMapper.sumMinutes(userId, startInclusive, endExclusive);
        return total == null ? 0L : total;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<LocalDate, Long> countStartedByDay(Long userId, LocalDateTime startInclusive, LocalDateTime endExclusive) {
        return toMap(focusRecordMapper.countStartedByDay(userId, startInclusive, endExclusive));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<LocalDate, Long> sumMinutesByDay(Long userId, LocalDateTime startInclusive, LocalDateTime endExclusive) {
        return toMap(focusRecordMapper.sumMinutesByDay(userId, startInclusive, endExclusive));
    }

    private static Map<LocalDate, Long> toMap(List<DayTotalRow> rows) {
        Map<LocalDate, Long> map = new HashMap<>();
        if (rows == null) {
            return map;
        }
        for (DayTotalRow row : rows) {
            if (row.getBucket() == null || row.getBucket().isBlank()) {
                continue;
            }
            map.put(LocalDate.parse(row.getBucket()), value(row.getTotal()));
        }
        return map;
    }

    private static long value(Long number) {
        return number == null ? 0L : number;
    }
}
