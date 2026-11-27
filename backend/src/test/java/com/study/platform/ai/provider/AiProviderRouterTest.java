package com.study.platform.ai.provider;

import com.study.platform.ai.config.AiProperties;
import com.study.platform.ai.support.AiPrompts;
import com.study.platform.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiProviderRouterTest {

    private final StubProvider remote = new StubProvider();
    private final LocalFallbackAiProvider fallback = new LocalFallbackAiProvider(
            () -> LocalDateTime.of(2026, 11, 20, 15, 0)
    );

    @Test
    void shouldUseLocalFallbackWhenKeyMissingOrRemoteFails() {
        AiProperties enabled = properties("", true);
        AiProviderRouter router = new AiProviderRouter(remote, fallback, enabled);

        RoutedAiResult missingKey = router.chat(AiPrompts.summarySystem(), AiPrompts.summaryUser("操作系统", "# 进程\n进程是执行实例。"));
        assertTrue(missingKey.fallback());
        assertEquals("local", missingKey.provider());
        assertEquals(0, remote.calls);
        assertTrue(missingKey.content().contains("suggestedTags"));

        enabled.setApiKey("sk-test");
        remote.error = new IllegalStateException("timeout");
        RoutedAiResult timedOut = router.chat(AiPrompts.qaSystem(), AiPrompts.qaUser(null, null, "什么是事务？"));
        assertTrue(timedOut.fallback());
        assertTrue(timedOut.content().contains("本地降级"));
        assertEquals(1, remote.calls);
    }

    @Test
    void shouldThrow50301WhenFallbackDisabled() {
        AiProperties disabled = properties("  ", false);
        AiProviderRouter router = new AiProviderRouter(remote, fallback, disabled);

        BusinessException missingKey = assertThrows(BusinessException.class,
                () -> router.chat(AiPrompts.summarySystem(), "笔记标题：空\n笔记正文：\n无"));
        assertEquals(50301, missingKey.getErrorCode().getCode());
        assertEquals(AiProviderRouter.UNAVAILABLE_MESSAGE, missingKey.getMessage());
        assertEquals(0, remote.calls);

        disabled.setApiKey("sk-test");
        remote.error = new IllegalStateException("timeout");
        BusinessException remoteDown = assertThrows(BusinessException.class,
                () -> router.chat(AiPrompts.scheduleSystem(LocalDateTime.of(2026, 11, 20, 15, 0)), "明天开会"));
        assertEquals(50301, remoteDown.getErrorCode().getCode());
        assertEquals(AiProviderRouter.UNAVAILABLE_MESSAGE, remoteDown.getMessage());
    }

    @Test
    void shouldKeepRemoteResultWhenCallSucceeds() {
        AiProperties properties = properties("sk-test", true);
        AiProviderRouter router = new AiProviderRouter(remote, fallback, properties);

        RoutedAiResult result = router.chat("系统", "用户");
        assertFalse(result.fallback());
        assertEquals("remote-ok", result.content());
        assertEquals("deepseek", result.provider());
        assertEquals(4, result.promptTokens());
        assertEquals(5, result.completionTokens());
    }

    @Test
    void shouldStreamFallbackOnlyBeforeAnyRemoteChunk() {
        AiProperties enabled = properties("sk-test", true);
        remote.error = new IllegalStateException("timeout");
        AiProviderRouter router = new AiProviderRouter(remote, fallback, enabled);
        List<Boolean> flags = new ArrayList<>();
        StringBuilder text = new StringBuilder();

        RoutedAiResult result = router.streamChat(
                AiPrompts.qaSystem(),
                AiPrompts.qaUser("线性代数", "矩阵是矩形数表。", "什么是矩阵？"),
                (chunk, fallback) -> {
                    flags.add(fallback);
                    text.append(chunk);
                }
        );

        assertTrue(result.fallback());
        assertFalse(flags.isEmpty());
        assertTrue(flags.stream().allMatch(flag -> flag));
        assertEquals(result.content(), text.toString());
        assertTrue(result.content().contains("本地降级"));

        AiProperties disabled = properties("sk-test", false);
        AiProviderRouter blocked = new AiProviderRouter(remote, fallback, disabled);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> blocked.streamChat(AiPrompts.qaSystem(), "学员问题：\n你好", (chunk, fallback) -> {
                    throw new AssertionError("不应推送增量");
                }));
        assertEquals(50301, ex.getErrorCode().getCode());
    }

    private static AiProperties properties(String apiKey, boolean fallbackEnabled) {
        AiProperties properties = new AiProperties();
        properties.setApiKey(apiKey);
        properties.setFallbackEnabled(fallbackEnabled);
        properties.setProvider("deepseek");
        properties.setModel("deepseek-chat");
        return properties;
    }

    private static final class StubProvider implements AiProvider {
        private RuntimeException error;
        private int calls;

        @Override
        public String providerName() {
            return "deepseek";
        }

        @Override
        public String modelName() {
            return "deepseek-chat";
        }

        @Override
        public AiChatResult chat(String systemPrompt, String userPrompt) {
            calls++;
            if (error != null) {
                throw error;
            }
            return new AiChatResult("remote-ok", 4, 5);
        }

        @Override
        public AiChatResult streamChat(String systemPrompt, String userPrompt, Consumer<String> onChunk) {
            calls++;
            if (error != null) {
                throw error;
            }
            onChunk.accept("remote-ok");
            return new AiChatResult("remote-ok", 4, 5);
        }
    }
}
