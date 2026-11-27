package com.study.platform.ai.provider;

import com.study.platform.ai.config.AiProperties;
import com.study.platform.common.ErrorCode;
import com.study.platform.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 策略路由：有 Key 时先走远程适配器，失败或未配置 Key 时按开关切到本地规则。
 * 关闭降级时统一抛 50301，避免把供应商异常原样返回给前端。
 */
@Component
public class AiProviderRouter {

    private static final Logger log = LoggerFactory.getLogger(AiProviderRouter.class);
    public static final String UNAVAILABLE_MESSAGE = "AI 服务暂时不可用，请稍后重试";

    private final AiProvider remote;
    private final AiProvider fallback;
    private final AiProperties properties;

    @Autowired
    public AiProviderRouter(OpenAiCompatibleAiProvider remote,
                            LocalFallbackAiProvider fallback,
                            AiProperties properties) {
        this(remote, (AiProvider) fallback, properties);
    }

    /**
     * 测试可以换入会超时的远程桩。生产构造器始终注入真实适配器。
     */
    public AiProviderRouter(AiProvider remote, AiProvider fallback, AiProperties properties) {
        this.remote = remote;
        this.fallback = fallback;
        this.properties = properties;
    }

    public RoutedAiResult chat(String systemPrompt, String userPrompt) {
        if (!properties.hasApiKey()) {
            return useFallback(systemPrompt, userPrompt, null);
        }
        try {
            AiChatResult result = remote.chat(systemPrompt, userPrompt);
            return RoutedAiResult.of(result, false, remote.providerName(), remote.modelName());
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("远程 AI 调用失败，准备降级: {}", ex.getMessage());
            return useFallback(systemPrompt, userPrompt, null);
        }
    }

    public RoutedAiResult streamChat(String systemPrompt, String userPrompt, AiChunkHandler handler) {
        AiChunkHandler sink = handler == null ? (text, fallback) -> { } : handler;
        if (!properties.hasApiKey()) {
            return useFallback(systemPrompt, userPrompt, sink);
        }
        AtomicBoolean emitted = new AtomicBoolean(false);
        try {
            AiChatResult result = remote.streamChat(systemPrompt, userPrompt, chunk -> {
                if (chunk == null || chunk.isEmpty()) {
                    return;
                }
                emitted.set(true);
                sink.onChunk(chunk, false);
            });
            return RoutedAiResult.of(result, false, remote.providerName(), remote.modelName());
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("远程 AI 流式调用失败: {}", ex.getMessage());
            // 已经把远程增量推给客户端时，不能再把整段本地回答接在后面。
            if (emitted.get() || !properties.isFallbackEnabled()) {
                throw unavailable();
            }
            return useFallback(systemPrompt, userPrompt, sink);
        }
    }

    private RoutedAiResult useFallback(String systemPrompt, String userPrompt, AiChunkHandler handler) {
        if (!properties.isFallbackEnabled()) {
            throw unavailable();
        }
        if (handler == null) {
            AiChatResult result = fallback.chat(systemPrompt, userPrompt);
            return RoutedAiResult.of(result, true, fallback.providerName(), fallback.modelName());
        }
        AiChatResult result = fallback.streamChat(systemPrompt, userPrompt, chunk -> {
            if (chunk != null && !chunk.isEmpty()) {
                handler.onChunk(chunk, true);
            }
        });
        return RoutedAiResult.of(result, true, fallback.providerName(), fallback.modelName());
    }

    private static BusinessException unavailable() {
        return new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, UNAVAILABLE_MESSAGE);
    }
}
