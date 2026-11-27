package com.study.platform.dashboard.support;

import com.study.platform.common.StudyDates;
import com.study.platform.dashboard.vo.DashboardOverviewVO;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 把笔记、日程、专注三份按日计数对齐到连续日期轴上。
 * 安静的日子补 0，方便 ECharts 日历和折线直接消费。
 */
public final class DashboardMath {

    private DashboardMath() {
    }

    public static double completionRate(long completed, long nonCancelled) {
        return StudyDates.percent(completed, nonCancelled);
    }

    public static List<LocalDate> lastDays(LocalDate end, int days) {
        return StudyDates.inclusiveDaysEnding(end, days);
    }

    public static List<DashboardOverviewVO.HeatCell> heatmap(List<LocalDate> days,
                                                             Map<LocalDate, Long> notes,
                                                             Map<LocalDate, Long> schedules,
                                                             Map<LocalDate, Long> focus) {
        List<DashboardOverviewVO.HeatCell> cells = new ArrayList<>(days.size());
        for (LocalDate day : days) {
            long count = value(notes, day) + value(schedules, day) + value(focus, day);
            cells.add(new DashboardOverviewVO.HeatCell(day.toString(), count));
        }
        return cells;
    }

    public static List<DashboardOverviewVO.TrendPoint> weeklyTrend(List<LocalDate> days,
                                                                   Map<LocalDate, Long> notesUpdated,
                                                                   Map<LocalDate, Long> schedulesCompleted,
                                                                   Map<LocalDate, Long> focusMinutes) {
        List<DashboardOverviewVO.TrendPoint> points = new ArrayList<>(days.size());
        for (LocalDate day : days) {
            points.add(new DashboardOverviewVO.TrendPoint(
                    day.toString(),
                    value(notesUpdated, day),
                    value(schedulesCompleted, day),
                    value(focusMinutes, day)
            ));
        }
        return points;
    }

    public static long value(Map<LocalDate, Long> map, LocalDate day) {
        if (map == null || day == null) {
            return 0L;
        }
        Long number = map.get(day);
        return number == null ? 0L : number;
    }
}
