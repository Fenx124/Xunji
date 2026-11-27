package com.study.platform.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.platform.ai.support.AiText;

import java.io.IOException;

/**
 * 解析 OpenAI / DeepSeek chat.completions 的非流式 JSON 和流式 data 行。
 */
public final class OpenAiResponseReader {

    private OpenAiResponseReader() {
    }

    public static AiChatResult readCompletion(String body, ObjectMapper mapper) throws IOException {
        JsonNode root = mapper.readTree(body == null || body.isBlank() ? "{}" : body);
        String content = messageContent(root.path("choices").path(0).path("message"));
        if (content.isBlank()) {
            throw new IOException("AI 返回内容为空");
        }
        int promptTokens = root.path("usage").path("prompt_tokens").asInt(0);
        int completionTokens = root.path("usage").path("completion_tokens").asInt(0);
        if (promptTokens == 0 && completionTokens == 0) {
            completionTokens = AiText.estimateTokens(content);
        }
        return new AiChatResult(content, promptTokens, completionTokens);
    }

    public static String dataPayload(String line) {
        if (line == null) {
            return null;
        }
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith(":")) {
            return null;
        }
        if (!trimmed.startsWith("data:")) {
            return null;
        }
        return trimmed.substring(5).trim();
    }

    public static boolean isDone(String line) {
        return "[DONE]".equals(dataPayload(line));
    }

    /**
     * 取出 delta.content。没有增量时返回 null，调用方不要把它当成一段空回答发出去。
     */
    public static String readDeltaContent(String line, ObjectMapper mapper) {
        String payload = dataPayload(line);
        if (payload == null || "[DONE]".equals(payload)) {
            return null;
        }
        try {
            JsonNode content = mapper.readTree(payload).path("choices").path(0).path("delta").get("content");
            if (content != null && content.isTextual()) {
                String text = content.asText("");
                return text.isEmpty() ? null : text;
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    public static void absorbUsage(String line, ObjectMapper mapper, int[] usage) {
        String payload = dataPayload(line);
        if (payload == null || "[DONE]".equals(payload) || usage == null || usage.length < 2) {
            return;
        }
        try {
            JsonNode usageNode = mapper.readTree(payload).get("usage");
            if (usageNode == null || usageNode.isNull()) {
                return;
            }
            usage[0] = usageNode.path("prompt_tokens").asInt(usage[0]);
            usage[1] = usageNode.path("completion_tokens").asInt(usage[1]);
        } catch (Exception ignored) {
            // 单行损坏不影响已经收到的增量。
        }
    }

    private static String messageContent(JsonNode message) {
        JsonNode content = message.get("content");
        if (content == null || content.isNull()) {
            return "";
        }
        if (content.isTextual()) {
            return content.asText("");
        }
        if (content.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode part : content) {
                if (part.isTextual()) {
                    builder.append(part.asText());
                } else if (part.has("text")) {
                    builder.append(part.path("text").asText(""));
                }
            }
            return builder.toString();
        }
        return content.asText("");
    }
}
