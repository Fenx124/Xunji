package com.study.platform.ai.ratelimit;

import com.study.platform.ai.config.AiProperties;
import com.study.platform.auth.service.UserAiQuotaQueryService;
import com.study.platform.common.ErrorCode;
import com.study.platform.common.RedisKeys;
import com.study.platform.common.StudyDates;
import com.study.platform.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

/**
 * Redis 计数器限流。分钟键 TTL 60 秒，日配额键 TTL 25 小时。
 * Redis 不可用时只记警告并放行，避免缓存抖动把 AI 接口打成 500。
 */
@Component
public class AiRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(AiRateLimiter.class);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
    static final String TOO_FREQUENT = "AI 请求过于频繁，请稍后再试";
    static final String QUOTA_EXHAUSTED = "今日 AI 调用配额已用尽";

    private final StringRedisTemplate stringRedisTemplate;
    private final UserAiQuotaQueryService quotaQueryService;
    private final AiProperties properties;
    private final Supplier<LocalDate> today;

    @Autowired
    public AiRateLimiter(StringRedisTemplate stringRedisTemplate,
                         UserAiQuotaQueryService quotaQueryService,
                         AiProperties properties) {
        this(stringRedisTemplate, quotaQueryService, properties, () -> LocalDate.now(StudyDates.ZONE));
    }

    public AiRateLimiter(StringRedisTemplate stringRedisTemplate,
                         UserAiQuotaQueryService quotaQueryService,
                         AiProperties properties,
                         Supplier<LocalDate> today) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.quotaQueryService = quotaQueryService;
        this.properties = properties;
        this.today = today == null ? () -> LocalDate.now(StudyDates.ZONE) : today;
    }

    public void acquire(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或 Token 无效");
        }
        int dailyLimit = quotaQueryService.currentDailyLimit(userId);
        if (dailyLimit <= 0) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, QUOTA_EXHAUSTED);
        }
        int perMinute = Math.max(properties.getRateLimitPerMinute(), 1);
        String day = today.get().format(DAY);
        long minuteCount;
        try {
            minuteCount = increment(RedisKeys.aiRateMin(userId), Duration.ofSeconds(60));
        } catch (RuntimeException ex) {
            log.warn("AI 分钟限流计数失败，本次放行 userId={}", userId, ex);
            return;
        }
        if (minuteCount > perMinute) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, TOO_FREQUENT);
        }
        long dayCount;
        try {
            dayCount = increment(RedisKeys.aiQuotaDay(userId, day), Duration.ofHours(25));
        } catch (RuntimeException ex) {
            log.warn("AI 日配额计数失败，本次放行 userId={}", userId, ex);
            return;
        }
        if (dayCount > dailyLimit) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, QUOTA_EXHAUSTED);
        }
    }

    private long increment(String key, Duration ttl) {
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count == null) {
            throw new IllegalStateException("Redis 计数返回空");
        }
        try {
            if (count == 1L || ttlMissing(key)) {
                stringRedisTemplate.expire(key, ttl);
            }
        } catch (RuntimeException ex) {
            log.warn("设置 AI 限流 TTL 失败 key={}", key, ex);
        }
        return count;
    }

    private boolean ttlMissing(String key) {
        Long ttl = stringRedisTemplate.getExpire(key);
        return ttl == null || ttl < 0;
    }
}
