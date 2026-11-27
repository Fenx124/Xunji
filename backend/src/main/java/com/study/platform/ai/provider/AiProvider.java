package com.study.platform.ai.provider;

import java.util.function.Consumer;

/**
 * 策略接口：远程兼容协议和本地规则都实现同一组调用。
 */
public interface AiProvider {

    String providerName();

    String modelName();

    AiChatResult chat(String systemPrompt, String userPrompt);

    AiChatResult streamChat(String systemPrompt, String userPrompt, Consumer<String> onChunk);
}
