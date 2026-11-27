package com.study.platform.ai.provider;

/**
 * 一次模型调用的文本和 Token 用量。降级标记由路由层单独携带。
 */
public record AiChatResult(
        String content,
        int promptTokens,
        int completionTokens
) {
    public AiChatResult {
        content = content == null ? "" : content;
        promptTokens = Math.max(promptTokens, 0);
        completionTokens = Math.max(completionTokens, 0);
    }
}
