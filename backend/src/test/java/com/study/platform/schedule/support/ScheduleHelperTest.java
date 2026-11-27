package com.study.platform.schedule.support;

import com.study.platform.common.ErrorCode;
import com.study.platform.common.exception.BusinessException;
import com.study.platform.schedule.entity.Schedule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EbbinghausPlannerTest {

    @Test
    void shouldGenerateFiveIntervalsAtFixedClockTimes() {
        List<EbbinghausPlanner.ReviewSlot> slots = EbbinghausPlanner.generate(LocalDate.of(2026, 11, 20), "线性代数");

        assertEquals(List.of(1, 2, 4, 7, 15), slots.stream().map(EbbinghausPlanner.ReviewSlot::daysLater).toList());
        assertEquals(List.of(1, 2, 3, 4, 5), slots.stream().map(EbbinghausPlanner.ReviewSlot::sequence).toList());
        assertEquals("复习：线性代数（第1次·1天后）", slots.get(0).title());
        assertEquals("复习：线性代数（第2次·2天后）", slots.get(1).title());
        assertEquals("复习：线性代数（第3次·4天后）", slots.get(2).title());
        assertEquals("复习：线性代数（第4次·7天后）", slots.get(3).title());
        assertEquals("复习：线性代数（第5次·15天后）", slots.get(4).title());

        assertEquals(LocalDateTime.of(2026, 11, 21, 9, 0, 0), slots.get(0).startTime());
        assertEquals(LocalDateTime.of(2026, 11, 21, 9, 30, 0), slots.get(0).endTime());
        assertEquals(LocalDateTime.of(2026, 11, 21, 8, 45, 0), slots.get(0).remindTime());
        assertEquals(LocalDateTime.of(2026, 11, 22, 9, 0, 0), slots.get(1).startTime());
        assertEquals(LocalDateTime.of(2026, 11, 24, 9, 0, 0), slots.get(2).startTime());
        assertEquals(LocalDateTime.of(2026, 11, 27, 9, 0, 0), slots.get(3).startTime());
        assertEquals(LocalDateTime.of(2026, 12, 5, 9, 0, 0), slots.get(4).startTime());
        assertEquals(LocalDateTime.of(2026, 12, 5, 8, 45, 0), slots.get(4).remindTime());

        assertEquals(2, Schedule.PRIORITY_IMPORTANT_NOT_URGENT);
        assertEquals(0, Schedule.STATUS_TODO);
        assertEquals("EBBINGHAUS", Schedule.SOURCE_EBBINGHAUS);
        assertEquals("MANUAL", Schedule.SOURCE_MANUAL);
    }

    @Test
    void shouldTruncateTitleToColumnLimit() {
        EbbinghausPlanner.ReviewSlot slot = EbbinghausPlanner.generate(LocalDate.of(2026, 11, 20), "题".repeat(200)).get(0);

        assertEquals(160, slot.title().length());
        assertTrue(slot.title().startsWith("复习："));
        assertTrue(slot.title().endsWith("（第1次·1天后）"));
    }

    @Test
    void shouldRejectMissingBaseDate() {
        assertThrows(IllegalArgumentException.class, () -> EbbinghausPlanner.generate(null, "笔记"));
    }
}

class ScheduleTimeParserTest {

    @Test
    void shouldExpandDateOnlyBounds() {
        assertNull(ScheduleTimeParser.parseBound(null, false));
        assertNull(ScheduleTimeParser.parseBound("  ", true));
        assertEquals(LocalDateTime.of(2026, 11, 20, 0, 0, 0), ScheduleTimeParser.parseBound("2026-11-20", false));
        assertEquals(LocalDateTime.of(2026, 11, 20, 23, 59, 59), ScheduleTimeParser.parseBound("2026-11-20", true));
        assertEquals(LocalDateTime.of(2026, 11, 21, 9, 0, 0),
                ScheduleTimeParser.parseBound("2026-11-21 09:00:00", true));
    }

    @Test
    void shouldRejectBadDateText() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> ScheduleTimeParser.parseBound("2026/11/20", false));
        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
        assertThrows(BusinessException.class, () -> ScheduleTimeParser.parseDate("2026-11-20 09:00:00"));
        assertEquals(LocalDate.of(2026, 11, 20), ScheduleTimeParser.parseDate("2026-11-20"));
    }
}

class ScheduleStatusTransitionsTest {

    private final LocalDateTime now = LocalDateTime.of(2026, 11, 20, 10, 0, 0);
    private final LocalDateTime earlier = LocalDateTime.of(2026, 11, 19, 10, 0, 0);

    @Test
    void shouldKeepCompletedAtWhenStatusDoesNotChange() {
        ScheduleStatusTransitions.Result result = ScheduleStatusTransitions.apply(1, earlier, 1, now);

        assertEquals(1, result.status());
        assertEquals(earlier, result.completedAt());
    }

    @Test
    void shouldStampCompletionAndClearItOnOtherTransitions() {
        ScheduleStatusTransitions.Result done = ScheduleStatusTransitions.apply(0, null, 1, now);
        assertEquals(1, done.status());
        assertEquals(now, done.completedAt());

        ScheduleStatusTransitions.Result reopened = ScheduleStatusTransitions.apply(1, earlier, 0, now);
        assertEquals(0, reopened.status());
        assertNull(reopened.completedAt());

        ScheduleStatusTransitions.Result cancelled = ScheduleStatusTransitions.apply(1, earlier, 2, now);
        assertEquals(2, cancelled.status());
        assertNull(cancelled.completedAt());
    }
}

class ScheduleRemindPolicyTest {

    private final LocalDateTime now = LocalDateTime.of(2026, 11, 20, 10, 0, 0);

    @Test
    void shouldResetOnlyWhenRemindTimeMovesIntoTheFuture() {
        assertFalse(ScheduleRemindPolicy.shouldReset(now.plusHours(1), now.plusHours(1), now));
        assertFalse(ScheduleRemindPolicy.shouldReset(now.plusHours(1), null, now));
        assertFalse(ScheduleRemindPolicy.shouldReset(now.plusHours(2), now.minusMinutes(1), now));
        assertFalse(ScheduleRemindPolicy.shouldReset(null, now, now));
        assertTrue(ScheduleRemindPolicy.shouldReset(now.plusHours(1), now.plusHours(3), now));
        assertTrue(ScheduleRemindPolicy.shouldReset(null, now.plusMinutes(1), now));
    }
}
