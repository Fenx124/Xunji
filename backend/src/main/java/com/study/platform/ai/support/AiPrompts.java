package com.study.platform.ai.support;

import com.study.platform.common.config.JacksonConfig;

import java.time.LocalDateTime;

/**
 * 发给模型的提示词。任务类型标记同时给本地降级策略识别用。
 */
public final class AiPrompts {

    public static final String TASK_SUMMARY = "SUMMARY";
    public static final String TASK_NOTE_QA = "NOTE_QA";
    public static final String TASK_SCHEDULE_PARSE = "SCHEDULE_PARSE";

    private static final int SUMMARY_CONTENT_LIMIT = 8000;
    private static final int QA_EXCERPT_LIMIT = 4000;

    private AiPrompts() {
    }

    public static String summarySystem() {
        return """
                你是学习笔记助手。任务类型：SUMMARY。
                只输出一个 JSON 对象，不要 Markdown 代码块，不要额外解释。
                格式：{"summary":"不超过300字的中文摘要","suggestedTags":["标签1","标签2","标签3"]}
                suggestedTags 给 3 到 5 个不超过 12 字的中文标签。
                """;
    }

    public static String summaryUser(String title, String content) {
        return "笔记标题：" + nullToEmpty(title) + "\n笔记正文：\n" + AiText.clamp(content, SUMMARY_CONTENT_LIMIT);
    }

    public static String qaSystem() {
        return """
                你是学习问答助手。任务类型：NOTE_QA。
                用简洁中文回答学员问题。若提供了笔记摘录，回答必须引用其中的要点，不要编造摘录里没有的事实。
                直接输出回答正文，不要 JSON。
                """;
    }

    public static String qaUser(String title, String content, String question) {
        if ((title == null || title.isBlank()) && (content == null || content.isBlank())) {
            return "学员问题：\n" + nullToEmpty(question);
        }
        return "笔记标题：" + nullToEmpty(title)
                + "\n笔记摘录：\n" + AiText.clamp(content, QA_EXCERPT_LIMIT)
                + "\n学员问题：\n" + nullToEmpty(question);
    }

    public static String scheduleSystem(LocalDateTime now) {
        String clock = now.format(JacksonConfig.DATE_TIME_FORMATTER);
        return """
                你是学习日程解析器。任务类型：SCHEDULE_PARSE。
                当前时间：%s（Asia/Shanghai）。
                只输出一个 JSON 对象，不要 Markdown 代码块。
                格式：{"title":"","description":"","startTime":"yyyy-MM-dd HH:mm:ss","endTime":"yyyy-MM-dd HH:mm:ss","remindTime":"yyyy-MM-dd HH:mm:ss","priority":2}
                priority 只能是 1、2、3、4。1=重要且紧急，2=重要不紧急，3=紧急不重要，4=不重要不紧急。
                同时出现“重要”和“紧急/截止/DDL/某点前”时用 1；只有“重要”用 2；只有“紧急/截止/DDL/某点前”用 3。
                时间必须是 yyyy-MM-dd HH:mm:ss。
                """.formatted(clock);
    }

    public static String scheduleUser(String text) {
        return nullToEmpty(text);
    }

    private static String nullToEmpty(String text) {
        return text == null ? "" : text.trim();
    }
}
