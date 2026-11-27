package com.study.platform.ai.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要按用户做 AI 频次和日配额检查的接口。
 * 切面在进入方法前执行，因此流式接口的 429 会以 JSON 返回。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AiRateLimit {
}
