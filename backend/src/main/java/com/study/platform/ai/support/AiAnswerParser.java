package com.study.platform.ai.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.platform.common.config.JacksonConfig;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从模型输出里抽出 JSON。模型经常套一层 ```json 代码块，或在 JSON 前后加解释。
 */
public final class AiAnswerParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter CLOCK = JacksonConfig.DATE_TIME_FORMATTER;
    private static final Pattern FENCE = Pattern.compile("```(?:json|JSON)?\\s*([\\s\\S]*?)```");
    private static final int SUMMARY_LIMIT = 300;

    private AiAnswerParser() {
    }

    public static String extractJsonObject(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String text = raw.trim();
        Matcher fence = FENCE.matcher(text);
        if (fence.find()) {
            text = fence.group(1).trim();
        }
        int start = text.indexOf('{');
        if (start < 0) {
            return null;
        }
        int end = matchingBrace(text, start);
        if (end < 0) {
            return null;
        }
        return text.substring(start, end + 1);
    }

    public static SummaryDraft parseSummary(String raw, String title, String content) {
        String json = extractJsonObject(raw);
        if (json != null) {
            try {
                JsonNode node = MAPPER.readTree(json);
                String summary = firstText(node, "summary", "content");
                List<String> tags = readTags(node);
                if (summary == null || summary.isBlank()) {
                    summary = plainSummary(raw, title, content);
                }
                if (tags.size() < 3) {
                    tags = mergeTags(tags, NoteSummaryComposer.suggestTags(title, content + "\n" + summary));
                }
                return new SummaryDraft(clampSummary(summary), limitTags(tags));
            } catch (Exception ignored) {
                // 模型把 JSON 写坏时，退回纯文本抽取。
            }
        }
        return new SummaryDraft(
                clampSummary(plainSummary(raw, title, content)),
                limitTags(NoteSummaryComposer.suggestTags(title, (content == null ? "" : content) + "\n" + nullToEmpty(raw)))
        );
    }

    public static ParsedScheduleDraft parseSchedule(String raw, String originalText, LocalDateTime now) {
        LocalDateTime clock = now == null ? LocalDateTime.of(2026, 1, 1, 9, 0) : now;
        ParsedScheduleDraft rules = ChineseScheduleParser.parse(originalText == null ? "" : originalText, clock);
        String json = extractJsonObject(raw);
        if (json == null) {
            return rules;
        }
        try {
            JsonNode node = MAPPER.readTree(json);
            String title = firstText(node, "title");
            String description = firstText(node, "description");
            LocalDateTime start = parseTime(firstText(node, "startTime", "start_time"));
            LocalDateTime end = parseTime(firstText(node, "endTime", "end_time"));
            LocalDateTime remind = parseTime(firstText(node, "remindTime", "remind_time"));
            int priority = readPriority(node, originalText);
            if (title == null || title.isBlank()) {
                title = rules.title();
            }
            if (start == null) {
                start = rules.startTime();
                end = end == null ? rules.endTime() : end;
                remind = remind == null ? rules.remindTime() : remind;
            }
            if (end != null && start != null && end.isBefore(start)) {
                end = start.plusHours(1);
            }
            if (end == null && start != null) {
                end = start.plusHours(1);
            }
            if (remind != null && start != null && !remind.isBefore(start)) {
                remind = start.minusMinutes(15);
            }
            if (remind == null && start != null) {
                remind = start.minusMinutes(15);
            }
            if (description == null || description.isBlank()) {
                description = rules.description();
            }
            title = AiText.clamp(title, 160);
            if (title.isBlank()) {
                title = "学习日程";
            }
            if (description != null) {
                description = AiText.clamp(description, 1000);
                if (description.isBlank()) {
                    description = null;
                }
            }
            if (priority < 1 || priority > 4) {
                priority = rules.priority();
            }
            return new ParsedScheduleDraft(title, description, start, end, remind, priority);
        } catch (Exception ignored) {
            return rules;
        }
    }

    private static String plainSummary(String raw, String title, String content) {
        String text = raw == null ? "" : raw.trim();
        text = FENCE.matcher(text).replaceAll(" ");
        text = text.replaceAll("\\s+", " ").trim();
        if (text.isBlank() || text.startsWith("{") || text.length() < 4) {
            return NoteSummaryComposer.compose(title, content).summary();
        }
        return text;
    }

    private static String clampSummary(String summary) {
        String clamped = AiText.clamp(summary, SUMMARY_LIMIT);
        if (clamped.isBlank()) {
            return "内容较少，建议补充关键要点。";
        }
        return clamped;
    }

    private static List<String> readTags(JsonNode node) {
        JsonNode tags = node.get("suggestedTags");
        if (tags == null) {
            tags = node.get("tags");
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (tags == null || tags.isNull()) {
            return List.of();
        }
        if (tags.isTextual()) {
            for (String part : tags.asText().split("[,，、\\s]+")) {
                addTag(values, part);
            }
            return new ArrayList<>(values);
        }
        if (tags.isArray()) {
            for (JsonNode item : tags) {
                if (item.isTextual()) {
                    addTag(values, item.asText());
                }
            }
        }
        return new ArrayList<>(values);
    }

    private static List<String> mergeTags(List<String> primary, List<String> extra) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (primary != null) {
            primary.forEach(tag -> addTag(values, tag));
        }
        if (extra != null) {
            extra.forEach(tag -> addTag(values, tag));
        }
        return new ArrayList<>(values);
    }

    private static List<String> limitTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return NoteSummaryComposer.suggestTags("", "");
        }
        if (tags.size() > 5) {
            return List.copyOf(tags.subList(0, 5));
        }
        if (tags.size() < 3) {
            return limitTags(mergeTags(tags, NoteSummaryComposer.suggestTags("", "")));
        }
        return List.copyOf(tags);
    }

    private static void addTag(Set<String> tags, String raw) {
        if (raw == null) {
            return;
        }
        String tag = raw.trim();
        if (tag.isEmpty() || tag.length() > 20) {
            return;
        }
        tags.add(tag);
    }

    private static int readPriority(JsonNode node, String originalText) {
        JsonNode priority = node.get("priority");
        if (priority == null || priority.isNull()) {
            return ChineseScheduleParser.priorityOf(originalText);
        }
        if (priority.isNumber()) {
            return priority.asInt();
        }
        if (priority.isTextual()) {
            try {
                return Integer.parseInt(priority.asText().trim());
            } catch (NumberFormatException ignored) {
                return ChineseScheduleParser.priorityOf(originalText);
            }
        }
        return ChineseScheduleParser.priorityOf(originalText);
    }

    public static LocalDateTime parseTime(String text) {
        if (text == null || text.isBlank() || "null".equalsIgnoreCase(text.trim())) {
            return null;
        }
        String value = text.trim().replace('T', ' ');
        if (value.length() >= 19) {
            value = value.substring(0, 19);
        } else if (value.length() == 16) {
            value = value + ":00";
        }
        try {
            return LocalDateTime.parse(value, CLOCK);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(text.trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException again) {
                return null;
            }
        }
    }

    private static String firstText(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode child = node.get(name);
            if (child != null && !child.isNull() && child.isTextual()) {
                String text = child.asText().trim();
                if (!text.isEmpty() && !"null".equalsIgnoreCase(text)) {
                    return text;
                }
            }
        }
        return null;
    }

    private static String nullToEmpty(String text) {
        return text == null ? "" : text;
    }

    private static int matchingBrace(String text, int start) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < text.length(); i++) {
            char current = text.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }
            if (current == '"') {
                inString = true;
            } else if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
}
