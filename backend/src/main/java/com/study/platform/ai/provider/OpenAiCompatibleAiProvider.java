package com.study.platform.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.study.platform.ai.config.AiProperties;
import com.study.platform.ai.support.AiText;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * OpenAI Chat Completions 兼容适配器。DeepSeek 走同一套 /chat/completions 协议。
 * Key 只从配置读取，禁止写死在代码里。
 */
@Component
public class OpenAiCompatibleAiProvider implements AiProvider {

    private final AiProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleAiProvider(AiProperties properties, HttpClient httpClient, ObjectMapper objectMapper) {
        this.properties = properties;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String providerName() {
        if (properties.getProvider() == null || properties.getProvider().isBlank()) {
            return "deepseek";
        }
        return properties.getProvider().trim();
    }

    @Override
    public String modelName() {
        if (properties.getModel() == null || properties.getModel().isBlank()) {
            return "deepseek-chat";
        }
        return properties.getModel().trim();
    }

    @Override
    public AiChatResult chat(String systemPrompt, String userPrompt) {
        try {
            HttpRequest request = buildRequest(systemPrompt, userPrompt, false);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new IOException("AI HTTP " + response.statusCode() + ": " + AiText.clamp(response.body(), 300));
            }
            return OpenAiResponseReader.readCompletion(response.body(), objectMapper);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI 请求被中断", ex);
        } catch (IOException ex) {
            throw new IllegalStateException(ex.getMessage() == null ? "AI 请求失败" : ex.getMessage(), ex);
        }
    }

    @Override
    public AiChatResult streamChat(String systemPrompt, String userPrompt, Consumer<String> onChunk) {
        try {
            HttpRequest request = buildRequest(systemPrompt, userPrompt, true);
            HttpResponse<Stream<String>> response = httpClient.send(request, HttpResponse.BodyHandlers.ofLines());
            try (Stream<String> lines = response.body()) {
                if (response.statusCode() / 100 != 2) {
                    String error = lines.limit(20).reduce("", (left, right) -> left + "\n" + right);
                    throw new IOException("AI HTTP " + response.statusCode() + ": " + AiText.clamp(error, 300));
                }
                StringBuilder answer = new StringBuilder();
                int[] usage = new int[2];
                Iterator<String> iterator = lines.iterator();
                while (iterator.hasNext()) {
                    String line = iterator.next();
                    if (OpenAiResponseReader.isDone(line)) {
                        break;
                    }
                    OpenAiResponseReader.absorbUsage(line, objectMapper, usage);
                    String delta = OpenAiResponseReader.readDeltaContent(line, objectMapper);
                    if (delta == null) {
                        continue;
                    }
                    answer.append(delta);
                    if (onChunk != null) {
                        onChunk.accept(delta);
                    }
                }
                if (answer.isEmpty()) {
                    throw new IOException("AI 流式返回为空");
                }
                if (usage[0] == 0 && usage[1] == 0) {
                    usage[0] = AiText.estimateTokens(systemPrompt) + AiText.estimateTokens(userPrompt);
                    usage[1] = AiText.estimateTokens(answer.toString());
                }
                return new AiChatResult(answer.toString(), usage[0], usage[1]);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI 请求被中断", ex);
        } catch (IOException ex) {
            throw new IllegalStateException(ex.getMessage() == null ? "AI 请求失败" : ex.getMessage(), ex);
        }
    }

    private HttpRequest buildRequest(String systemPrompt, String userPrompt, boolean stream) throws IOException {
        String apiKey = properties.getApiKey() == null ? "" : properties.getApiKey().trim();
        if (apiKey.isEmpty()) {
            throw new IOException("AI API Key 未配置");
        }
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", modelName());
        body.put("temperature", 0.2);
        ArrayNode messages = body.putArray("messages");
        messages.addObject().put("role", "system").put("content", systemPrompt == null ? "" : systemPrompt);
        messages.addObject().put("role", "user").put("content", userPrompt == null ? "" : userPrompt);
        if (stream) {
            body.put("stream", true);
            body.putObject("stream_options").put("include_usage", true);
        }
        int timeout = Math.max(properties.getTimeoutSeconds(), 1);
        return HttpRequest.newBuilder()
                .uri(endpoint())
                .timeout(Duration.ofSeconds(timeout))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();
    }

    private URI endpoint() throws IOException {
        String base = properties.getBaseUrl() == null ? "" : properties.getBaseUrl().trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.isBlank()) {
            throw new IOException("AI base-url 未配置");
        }
        return URI.create(base + "/chat/completions");
    }
}
