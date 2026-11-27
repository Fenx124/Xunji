package com.study.platform.ai.provider;

/**
 * 流式输出回调。fallback 表示这一段来自本地规则而不是远程模型。
 */
@FunctionalInterface
public interface AiChunkHandler {

    void onChunk(String text, boolean fallback);
}
