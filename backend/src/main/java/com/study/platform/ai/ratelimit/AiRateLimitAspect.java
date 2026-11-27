package com.study.platform.ai.ratelimit;

import com.study.platform.auth.security.SecurityUtils;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 在 AI 接口真正执行前扣减 Redis 次数。
 * 用前置通知而不是包在业务里，这样配额用尽时还没有创建 SseEmitter，异常处理器能返回标准 JSON。
 */
@Aspect
@Component
@Order(0)
public class AiRateLimitAspect {

    private final AiRateLimiter aiRateLimiter;

    public AiRateLimitAspect(AiRateLimiter aiRateLimiter) {
        this.aiRateLimiter = aiRateLimiter;
    }

    @Before("@annotation(com.study.platform.ai.ratelimit.AiRateLimit)")
    public void beforeAiCall() {
        aiRateLimiter.acquire(SecurityUtils.getUserId());
    }
}
