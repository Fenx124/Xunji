package com.study.platform.schedule.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.platform.common.DayTotalRow;
import com.study.platform.schedule.entity.Schedule;
import com.study.platform.schedule.mapper.CompletionCountRow;
import com.study.platform.schedule.mapper.ScheduleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScheduleDashboardQueryServiceImpl implements ScheduleDashboardQueryService {

    private final ScheduleMapper scheduleMapper;

    public ScheduleDashboardQueryServiceImpl(ScheduleMapper scheduleMapper) {
        this.scheduleMapper = scheduleMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public long countPending(Long userId, LocalDateTime startInclusive, LocalDateTime endExclusive) {
        Long total = scheduleMapper.selectCount(new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getUserId, userId)
                .eq(Schedule::getStatus, Schedule.STATUS_TODO)
                .ge(Schedule::getStartTime, startInclusive)
                .lt(Schedule::getStartTime, endExclusive));
        return total == null ? 0L : total;
    }

    @Override
    @Transactional(readOnly = true)
    public Completion monthCompletion(Long userId, LocalDateTime startInclusive, LocalDateTime endExclusive) {
        CompletionCountRow row = scheduleMapper.summarizeCompletion(userId, startInclusive, endExclusive);
        if (row == null) {
            return new Completion(0L, 0L);
        }
        long completed = row.getCompleted() == null ? 0L : row.getCompleted();
        long counted = row.getCounted() == null ? 0L : row.getCounted();
        return new Completion(completed, counted);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<LocalDate, Long> countCompletedByDay(Long userId, LocalDateTime startInclusive, LocalDateTime endExclusive) {
        return toMap(scheduleMapper.countCompletedByDay(userId, startInclusive, endExclusive));
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
            map.put(LocalDate.parse(row.getBucket()), row.getTotal() == null ? 0L : row.getTotal());
        }
        return map;
    }
}
