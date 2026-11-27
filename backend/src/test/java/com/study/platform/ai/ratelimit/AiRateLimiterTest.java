package com.study.platform.ai.ratelimit;

import com.study.platform.ai.config.AiProperties;
import com.study.platform.auth.service.UserAiQuotaQueryService;
import com.study.platform.common.RedisKeys;
import com.study.platform.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiRateLimiterTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 20);

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> values;

    @Mock
    private UserAiQuotaQueryService quota;

    private final Map<String, Long> counts = new HashMap<>();
    private AiProperties properties;
    private AiRateLimiter limiter;

    @BeforeEach
    void setUp() {
        properties = new AiProperties();
        properties.setRateLimitPerMinute(6);
        lenient().when(redis.opsForValue()).thenReturn(values);
        lenient().when(values.increment(anyString())).thenAnswer(invocation ->
                counts.merge(invocation.getArgument(0), 1L, Long::sum));
        lenient().when(redis.getExpire(anyString())).thenReturn(30L);
        lenient().when(redis.expire(anyString(), any(Duration.class))).thenReturn(true);
        lenient().when(quota.currentDailyLimit(anyLong())).thenReturn(50);
        limiter = new AiRateLimiter(redis, quota, properties, () -> DAY);
    }

    @Test
    void shouldRejectWhenPerMinuteCounterExceedsLimit() {
        properties.setRateLimitPerMinute(2);
        limiter.acquire(8L);
        limiter.acquire(8L);

        BusinessException ex = assertThrows(BusinessException.class, () -> limiter.acquire(8L));

        assertEquals(42901, ex.getErrorCode().getCode());
        assertEquals(AiRateLimiter.TOO_FREQUENT, ex.getMessage());
        assertEquals(3L, counts.get(RedisKeys.aiRateMin(8L)));
        assertEquals(2L, counts.get(RedisKeys.aiQuotaDay(8L, "20261120")));
        verify(redis).expire(RedisKeys.aiRateMin(8L), Duration.ofSeconds(60));
        verify(redis).expire(eq(RedisKeys.aiQuotaDay(8L, "20261120")), eq(Duration.ofHours(25)));
    }

    @Test
    void shouldRejectWhenDailyQuotaIsExceededOrDisabled() {
        properties.setRateLimitPerMinute(10);
        when(quota.currentDailyLimit(9L)).thenReturn(2);
        limiter.acquire(9L);
        limiter.acquire(9L);

        BusinessException exhausted = assertThrows(BusinessException.class, () -> limiter.acquire(9L));
        assertEquals(42901, exhausted.getErrorCode().getCode());
        assertEquals(AiRateLimiter.QUOTA_EXHAUSTED, exhausted.getMessage());

        when(quota.currentDailyLimit(1L)).thenReturn(0);
        BusinessException disabled = assertThrows(BusinessException.class, () -> limiter.acquire(1L));
        assertEquals(AiRateLimiter.QUOTA_EXHAUSTED, disabled.getMessage());
        verify(values, never()).increment(RedisKeys.aiRateMin(1L));
    }

    @Test
    void shouldFailOpenWhenRedisIsDown() {
        when(values.increment(anyString())).thenThrow(new RedisConnectionFailureException("down"));

        assertDoesNotThrow(() -> limiter.acquire(4L));
    }
}
