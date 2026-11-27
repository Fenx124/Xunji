package com.study.platform.system.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiStatsMathTest {

    @Test
    void shouldClampDaysAndReturnZeroRatesForAnEmptyLog() {
        assertEquals(7, AiStatsMath.clampDays(null));
        assertEquals(1, AiStatsMath.clampDays(0));
        assertEquals(1, AiStatsMath.clampDays(-3));
        assertEquals(7, AiStatsMath.clampDays(7));
        assertEquals(90, AiStatsMath.clampDays(90));
        assertEquals(90, AiStatsMath.clampDays(91));

        assertEquals(0.0, AiStatsMath.successRate(0, 0));
        assertEquals(0L, AiStatsMath.averageLatency(0, 0));
        assertEquals(80.5, AiStatsMath.successRate(161, 200));
        assertEquals(15L, AiStatsMath.averageLatency(44, 3));
    }
}
