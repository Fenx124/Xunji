package com.study.platform.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * AI 模块的 HTTP 客户端和 SSE 工作线程。业务调用不占用 Tomcat 请求线程写流。
 */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfiguration {

    @Bean
    public HttpClient aiHttpClient(AiProperties properties) {
        int timeout = Math.max(properties.getTimeoutSeconds(), 1);
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(timeout, 10)))
                .build();
    }

    @Bean(name = "aiStreamExecutor")
    public ThreadPoolTaskExecutor aiStreamExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("ai-sse-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }
}
