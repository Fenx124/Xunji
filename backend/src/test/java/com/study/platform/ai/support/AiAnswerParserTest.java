package com.study.platform.ai.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.platform.ai.provider.AiChatResult;
import com.study.platform.ai.provider.OpenAiResponseReader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiAnswerParserTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldExtractJsonFromFencedAndSurroundingText() {
        String fenced = """
                以下是结果
                ```json
                {"summary":"进程负责资源，线程负责调度。","suggestedTags":["操作系统","进程","线程","复习"]}
                ```
                完毕
                """;

        SummaryDraft fencedDraft = AiAnswerParser.parseSummary(fenced, "操作系统", "进程与线程");
        assertEquals("进程负责资源，线程负责调度。", fencedDraft.summary());
        assertEquals(List.of("操作系统", "进程", "线程", "复习"), fencedDraft.tags());
        assertFalse(fencedDraft.summary().contains("```"));

        String bareFence = """
                ```
                {"summary":"只看代码块","suggestedTags":["A","B","C"]}
                ```
                """;
        assertEquals("只看代码块", AiAnswerParser.parseSummary(bareFence, "题", "正文").summary());

        String wrapped = "答案：{\"summary\":\"只看这段\",\"suggestedTags\":[\"A\",\"B\",\"C\",\"D\",\"E\",\"F\"]} 谢谢";
        SummaryDraft wrappedDraft = AiAnswerParser.parseSummary(wrapped, "题", "正文");
        assertEquals("只看这段", wrappedDraft.summary());
        assertEquals(List.of("A", "B", "C", "D", "E"), wrappedDraft.tags());
    }

    @Test
    void shouldFallBackToPlainTextWhenModelDoesNotReturnJson() {
        String raw = "进程是资源分配的基本单位，线程是CPU调度的基本单位。";
        SummaryDraft draft = AiAnswerParser.parseSummary(raw, "操作系统", "进程与线程的区别需要记住。");

        assertTrue(draft.summary().contains("进程"));
        assertFalse(draft.summary().contains("```"));
        assertTrue(draft.summary().length() <= 300);
        assertTrue(draft.tags().size() >= 3 && draft.tags().size() <= 5);
        assertNull(AiAnswerParser.extractJsonObject(raw));
    }

    @Test
    void shouldReadOpenAiCompletionAndStreamDelta() throws Exception {
        String body = """
                {"choices":[{"message":{"content":"你好"}}],"usage":{"prompt_tokens":3,"completion_tokens":1}}
                """;
        AiChatResult result = OpenAiResponseReader.readCompletion(body, mapper);
        assertEquals("你好", result.content());
        assertEquals(3, result.promptTokens());
        assertEquals(1, result.completionTokens());

        assertEquals("你", OpenAiResponseReader.readDeltaContent(
                "data: {\"choices\":[{\"delta\":{\"content\":\"你\"}}]}", mapper));
        assertNull(OpenAiResponseReader.readDeltaContent("data: [DONE]", mapper));
        assertTrue(OpenAiResponseReader.isDone("data: [DONE]"));
        assertNull(OpenAiResponseReader.readDeltaContent(": keep-alive", mapper));
    }

    @Test
    void shouldNormalizeScheduleJsonAndRepairPlainText() {
        String json = """
                ```json
                {"title":"交报告","description":"体系结构","startTime":"2026-11-21T20:00:00","endTime":"2026-11-21 19:00:00","remindTime":"2026-11-21 20:30:00","priority":9}
                ```
                """;
        ParsedScheduleDraft draft = AiAnswerParser.parseSchedule(
                json,
                "明天晚上8点前交报告，重要",
                java.time.LocalDateTime.of(2026, 11, 20, 15, 0)
        );

        assertEquals("交报告", draft.title());
        assertEquals(java.time.LocalDateTime.of(2026, 11, 21, 20, 0, 0), draft.startTime());
        assertEquals(java.time.LocalDateTime.of(2026, 11, 21, 21, 0, 0), draft.endTime());
        assertEquals(java.time.LocalDateTime.of(2026, 11, 21, 19, 45, 0), draft.remindTime());
        assertEquals(1, draft.priority());
    }
}
