package com.study.platform.focus.support;

import com.study.platform.common.ErrorCode;
import com.study.platform.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FocusRecordRulesTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 20, 21, 0, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 11, 20, 21, 25, 0);

    @Test
    void shouldAcceptBoundaryValuesAndEqualClockTimes() {
        assertDoesNotThrow(() -> FocusRecordRules.validate(1, 0, "COMPLETED", START, START, null));
        assertDoesNotThrow(() -> FocusRecordRules.validate(240, 300, "ABORTED", START, END, "a".repeat(255)));
        assertDoesNotThrow(() -> FocusRecordRules.validateOptionalId(null, "笔记"));
        assertDoesNotThrow(() -> FocusRecordRules.validateDateOrder(START, END));
        assertDoesNotThrow(() -> FocusRecordRules.validateDateOrder(null, END));
    }

    @Test
    void shouldRejectOutOfRangeMinutesStatusAndClockOrder() {
        assertBadRequest(() -> FocusRecordRules.validate(0, 10, "COMPLETED", START, END, null));
        assertBadRequest(() -> FocusRecordRules.validate(241, 10, "COMPLETED", START, END, null));
        assertBadRequest(() -> FocusRecordRules.validate(25, -1, "COMPLETED", START, END, null));
        assertBadRequest(() -> FocusRecordRules.validate(25, 301, "COMPLETED", START, END, null));
        assertBadRequest(() -> FocusRecordRules.validate(25, 10, "DONE", START, END, null));
        assertBadRequest(() -> FocusRecordRules.validate(25, 10, "completed", START, END, null));
        assertBadRequest(() -> FocusRecordRules.validate(25, 10, "COMPLETED", END, START, null));
        assertBadRequest(() -> FocusRecordRules.validate(25, 10, "COMPLETED", null, END, null));
        assertBadRequest(() -> FocusRecordRules.validate(25, 10, "COMPLETED", START, END, "a".repeat(256)));
        assertBadRequest(() -> FocusRecordRules.validateOptionalId(0L, "日程"));
        assertBadRequest(() -> FocusRecordRules.validateDateOrder(END, START));
    }

    private static void assertBadRequest(Runnable action) {
        BusinessException ex = assertThrows(BusinessException.class, action::run);
        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
        assertEquals(40001, ex.getErrorCode().getCode());
    }
}
