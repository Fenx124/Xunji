package com.study.platform.dashboard.support;

import com.study.platform.dashboard.vo.DashboardOverviewVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DashboardMathTest {

    @Test
    void shouldBuildSevenAndNinetyDayBucketsInOrder() {
        LocalDate end = LocalDate.of(2026, 11, 20);

        List<LocalDate> week = DashboardMath.lastDays(end, 7);
        assertEquals(7, week.size());
        assertEquals(LocalDate.of(2026, 11, 14), week.get(0));
        assertEquals(end, week.get(6));
        assertEquals(week.get(0).plusDays(1), week.get(1));

        List<LocalDate> heat = DashboardMath.lastDays(end, 90);
        assertEquals(90, heat.size());
        assertEquals(end.minusDays(89), heat.get(0));
        assertEquals(end, heat.get(89));
        assertEquals(heat.get(10).plusDays(1), heat.get(11));
    }

    @Test
    void shouldRoundCompletionRateToOneDecimalAndTreatEmptyAsZero() {
        assertEquals(0.0, DashboardMath.completionRate(0, 0));
        assertEquals(0.0, DashboardMath.completionRate(0, 4));
        assertEquals(0.0, DashboardMath.completionRate(-1, 4));
        assertEquals(80.5, DashboardMath.completionRate(161, 200));
        assertEquals(33.3, DashboardMath.completionRate(1, 3), 0.001);
        assertEquals(66.7, DashboardMath.completionRate(2, 3), 0.001);
        assertEquals(100.0, DashboardMath.completionRate(5, 5));
    }

    @Test
    void shouldFillQuietDaysWithZeroAndSumTheThreeSources() {
        LocalDate end = LocalDate.of(2026, 11, 20);
        List<LocalDate> week = DashboardMath.lastDays(end, 7);
        Map<LocalDate, Long> notes = Map.of(end, 2L);
        Map<LocalDate, Long> schedules = Map.of(end, 1L);
        Map<LocalDate, Long> focus = Map.of(LocalDate.of(2026, 11, 19), 4L);

        List<DashboardOverviewVO.HeatCell> cells = DashboardMath.heatmap(week, notes, schedules, focus);

        assertEquals(7, cells.size());
        assertEquals("2026-11-14", cells.get(0).date());
        assertEquals(0L, cells.get(0).count());
        assertEquals("2026-11-19", cells.get(5).date());
        assertEquals(4L, cells.get(5).count());
        assertEquals(3L, cells.get(6).count());

        List<DashboardOverviewVO.TrendPoint> trend = DashboardMath.weeklyTrend(week, notes, schedules, focus);
        assertEquals(0L, trend.get(0).notesUpdated());
        assertEquals(0L, trend.get(0).schedulesCompleted());
        assertEquals(0L, trend.get(0).focusMinutes());
        assertEquals(4L, trend.get(5).focusMinutes());
        assertEquals(2L, trend.get(6).notesUpdated());
        assertEquals(1L, trend.get(6).schedulesCompleted());
    }
}
