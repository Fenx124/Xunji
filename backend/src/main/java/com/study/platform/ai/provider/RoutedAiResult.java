package com.study.platform.ai.provider;

/**
 * 路由之后的调用结果。fallback=true 表示本次没有使用远程模型。
 */
public record RoutedAiResult(
        String content,
        int promptTokens,
        int completionTokens,
        boolean fallback,
        String provider,
        String modelName
) {
    public static RoutedAiResult of(AiChatResult result, boolean fallback, String provider, String modelName) {
        AiChatResult safe = result == null ? new AiChatResult("", 0, 0) : result;
        return new RoutedAiResult(
                safe.content(),
                safe.promptTokens(),
                safe.completionTokens(),
                fallback,
                provider == null ? "" : provider,
                modelName == null ? "" : modelName
        );
    }
}
