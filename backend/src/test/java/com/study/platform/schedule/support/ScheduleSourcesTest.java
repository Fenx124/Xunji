package com.study.platform.schedule.support;

import com.study.platform.common.exception.BusinessException;
import com.study.platform.schedule.entity.Schedule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScheduleSourcesTest {

    @Test
    void shouldAcceptManualAndAiOnly() {
        assertEquals(Schedule.SOURCE_MANUAL, ScheduleSources.forCreate(null));
        assertEquals(Schedule.SOURCE_MANUAL, ScheduleSources.forCreate("  "));
        assertEquals(Schedule.SOURCE_MANUAL, ScheduleSources.forCreate("manual"));
        assertEquals(Schedule.SOURCE_AI, ScheduleSources.forCreate("AI"));

        BusinessException ex = assertThrows(BusinessException.class, () -> ScheduleSources.forCreate("EBBINGHAUS"));
        assertEquals(40001, ex.getErrorCode().getCode());
    }
}
