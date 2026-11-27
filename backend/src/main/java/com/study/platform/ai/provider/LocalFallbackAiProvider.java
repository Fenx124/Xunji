package com.study.platform.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.study.platform.ai.support.AiPrompts;
import com.study.platform.ai.support.AiText;
import com.study.platform.common.StudyDates;
import com.study.platform.ai.support.ChineseScheduleParser;
import com.study.platform.ai.support.NoteSummaryComposer;
import com.study.platform.ai.support.ParsedScheduleDraft;
import com.study.platform.ai.support.SummaryDraft;
import com.study.platform.common.config.JacksonConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 不访问外网的规则降级。Key 为空、远程超时或远程报错且允许降级时由路由切到这里。
 */
@Component
public class LocalFallbackAiProvider implements AiProvider {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern NOW = Pattern.compile("当前时间：(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})");
    private static final int CHUNK_SIZE = 16;

    private final Supplier<LocalDateTime> clock;

    @Autowired
    public LocalFallbackAiProvider() {
        this(StudyDates::now);
    }

    public LocalFallbackAiProvider(Supplier<LocalDateTime> clock) {
        this.clock = clock == null ? StudyDates::now : clock;
    }

    @Override
    public String providerName() {
        return "local";
    }

    @Override
    public String modelName() {
        return "local-rules";
    }

    @Override
    public AiChatResult chat(String systemPrompt, String userPrompt) {
        String content = render(systemPrompt, userPrompt);
        return new AiChatResult(
                content,
                AiText.estimateTokens(systemPrompt) + AiText.estimateTokens(userPrompt),
                AiText.estimateTokens(content)
        );
    }

    @Override
    public AiChatResult streamChat(String systemPrompt, String userPrompt, Consumer<String> onChunk) {
        String content = render(systemPrompt, userPrompt);
        emit(content, onChunk);
        return new AiChatResult(
                content,
                AiText.estimateTokens(systemPrompt) + AiText.estimateTokens(userPrompt),
                AiText.estimateTokens(content)
        );
    }

    private String render(String systemPrompt, String userPrompt) {
        String system = systemPrompt == null ? "" : systemPrompt;
        String user = userPrompt == null ? "" : userPrompt;
        if (system.contains(AiPrompts.TASK_SCHEDULE_PARSE)) {
            return scheduleJson(user, resolveNow(system));
        }
        if (system.contains(AiPrompts.TASK_NOTE_QA)) {
            return qaAnswer(user);
        }
        return summaryJson(user);
    }

    private String summaryJson(String userPrompt) {
        String title = labeled(userPrompt, "笔记标题：");
        String content = section(userPrompt, "笔记正文：", null);
        SummaryDraft draft = NoteSummaryComposer.compose(title, content);
        try {
            ObjectNode node = MAPPER.createObjectNode();
            node.put("summary", draft.summary());
            ArrayNode tags = node.putArray("suggestedTags");
            draft.tags().forEach(tags::add);
            return MAPPER.writeValueAsString(node);
        } catch (Exception ex) {
            throw new IllegalStateException("本地摘要序列化失败", ex);
        }
    }

    private String scheduleJson(String userPrompt, LocalDateTime now) {
        ParsedScheduleDraft draft = ChineseScheduleParser.parse(userPrompt, now);
        try {
            ObjectNode node = MAPPER.createObjectNode();
            node.put("title", draft.title());
            if (draft.description() == null) {
                node.putNull("description");
            } else {
                node.put("description", draft.description());
            }
            node.put("startTime", ChineseScheduleParser.format(draft.startTime()));
            node.put("endTime", ChineseScheduleParser.format(draft.endTime()));
            node.put("remindTime", ChineseScheduleParser.format(draft.remindTime()));
            node.put("priority", draft.priority());
            return MAPPER.writeValueAsString(node);
        } catch (Exception ex) {
            throw new IllegalStateException("本地日程序列化失败", ex);
        }
    }

    private String qaAnswer(String userPrompt) {
        String title = labeled(userPrompt, "笔记标题：");
        String excerpt = section(userPrompt, "笔记摘录：", "\n学员问题：");
        String question = questionOf(userPrompt);
        String safeTitle = title.isBlank() ? "未指定笔记" : title;
        String safeExcerpt = excerpt.replaceAll("\\s+", " ").trim();
        if (safeExcerpt.length() > 120) {
            safeExcerpt = safeExcerpt.substring(0, 120) + "…";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("【本地降级】未配置远程 API Key 或远程服务超时，以下为本地规则回答。\n");
        builder.append("笔记：").append(safeTitle).append('\n');
        builder.append("问题：").append(question).append('\n');
        if (!safeExcerpt.isBlank()) {
            builder.append("摘录：").append(safeExcerpt).append('\n');
        }
        builder.append("建议：先定位与问题相关的小标题和关键句，再回看原文。配置 API Key 后可获得模型的详细解答。");
        return builder.toString();
    }

    private LocalDateTime resolveNow(String systemPrompt) {
        Matcher matcher = NOW.matcher(systemPrompt == null ? "" : systemPrompt);
        if (matcher.find()) {
            try {
                return LocalDateTime.parse(matcher.group(1), JacksonConfig.DATE_TIME_FORMATTER);
            } catch (DateTimeParseException ignored) {
                return clock.get();
            }
        }
        return clock.get();
    }

    private static void emit(String content, Consumer<String> onChunk) {
        if (onChunk == null || content == null || content.isEmpty()) {
            return;
        }
        int count = content.codePointCount(0, content.length());
        int index = 0;
        int emitted = 0;
        while (emitted < count) {
            int take = Math.min(CHUNK_SIZE, count - emitted);
            int end = content.offsetByCodePoints(index, take);
            onChunk.accept(content.substring(index, end));
            index = end;
            emitted += take;
        }
    }

    private static String labeled(String prompt, String label) {
        int index = prompt.indexOf(label);
        if (index < 0) {
            return "";
        }
        int from = index + label.length();
        int end = prompt.indexOf('\n', from);
        if (end < 0) {
            end = prompt.length();
        }
        return prompt.substring(from, end).trim();
    }

    private static String section(String prompt, String startLabel, String endLabel) {
        int index = prompt.indexOf(startLabel);
        if (index < 0) {
            return "";
        }
        int from = index + startLabel.length();
        int end = endLabel == null ? -1 : prompt.indexOf(endLabel, from);
        if (end < 0) {
            end = prompt.length();
        }
        return prompt.substring(from, end).trim();
    }

    private static String questionOf(String prompt) {
        String marker = "学员问题：";
        int index = prompt.lastIndexOf(marker);
        if (index < 0) {
            return prompt.trim();
        }
        return prompt.substring(index + marker.length()).trim();
    }
}
