package com.study.platform.ai.provider;

import com.study.platform.ai.support.AiAnswerParser;
import com.study.platform.ai.support.AiPrompts;
import com.study.platform.ai.support.ParsedScheduleDraft;
import com.study.platform.ai.support.SummaryDraft;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalFallbackAiProviderTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 20, 15, 0, 0);

    private final LocalFallbackAiProvider provider = new LocalFallbackAiProvider(() -> NOW);

    @Test
    void shouldSummarizeHeadingsIntoJson() {
        String content = """
                # 进程
                进程是程序的一次执行。
                # 线程
                线程是进程内的执行单元。
                内存管理负责地址空间。
                """;
        AiChatResult result = provider.chat(
                AiPrompts.summarySystem(),
                AiPrompts.summaryUser("操作系统", content)
        );

        assertFalse(result.content().contains("```"));
        SummaryDraft draft = AiAnswerParser.parseSummary(result.content(), "操作系统", content);
        assertTrue(draft.summary().contains("操作系统"));
        assertTrue(draft.summary().contains("进程"));
        assertTrue(draft.summary().contains("线程"));
        assertTrue(draft.summary().length() <= 300);
        assertTrue(draft.tags().size() >= 3 && draft.tags().size() <= 5);
        assertTrue(draft.tags().contains("操作系统"));
        assertEquals("local", provider.providerName());
        assertEquals("local-rules", provider.modelName());
    }

    @Test
    void shouldParseChineseSchedulePhrases() {
        assertSchedule(
                "明天晚上8点前交软件体系结构报告，重要且紧急",
                "交软件体系结构报告",
                LocalDateTime.of(2026, 11, 21, 19, 30, 0),
                LocalDateTime.of(2026, 11, 21, 20, 0, 0),
                LocalDateTime.of(2026, 11, 21, 19, 15, 0),
                1
        );
        assertSchedule(
                "下周三上午10点复习数据库事务与锁",
                "复习数据库事务与锁",
                LocalDateTime.of(2026, 11, 25, 10, 0, 0),
                LocalDateTime.of(2026, 11, 25, 11, 0, 0),
                LocalDateTime.of(2026, 11, 25, 9, 45, 0),
                2
        );
        assertSchedule(
                "周五晚上八点前交体系结构报告，很重要",
                "交体系结构报告",
                LocalDateTime.of(2026, 11, 20, 19, 30, 0),
                LocalDateTime.of(2026, 11, 20, 20, 0, 0),
                LocalDateTime.of(2026, 11, 20, 19, 15, 0),
                1
        );
        assertSchedule(
                "明天下午三点复习高数",
                "复习高数",
                LocalDateTime.of(2026, 11, 21, 15, 0, 0),
                LocalDateTime.of(2026, 11, 21, 16, 0, 0),
                LocalDateTime.of(2026, 11, 21, 14, 45, 0),
                2
        );
        assertSchedule(
                "下周一上午9点小组讨论",
                "小组讨论",
                LocalDateTime.of(2026, 11, 23, 9, 0, 0),
                LocalDateTime.of(2026, 11, 23, 10, 0, 0),
                LocalDateTime.of(2026, 11, 23, 8, 45, 0),
                2
        );
        assertSchedule(
                "后天晚上七点半提交实验，紧急",
                "提交实验",
                LocalDateTime.of(2026, 11, 22, 19, 30, 0),
                LocalDateTime.of(2026, 11, 22, 20, 30, 0),
                LocalDateTime.of(2026, 11, 22, 19, 15, 0),
                3
        );
    }

    @Test
    void shouldStreamLocalQaInSmallChunks() {
        String system = AiPrompts.qaSystem();
        String user = AiPrompts.qaUser("操作系统", "进程是资源分配单位。线程是调度单位。", "进程和线程有什么区别？");
        List<String> chunks = new ArrayList<>();
        AiChatResult result = provider.streamChat(system, user, chunks::add);

        assertTrue(chunks.size() > 1);
        assertEquals(result.content(), String.join("", chunks));
        assertTrue(result.content().contains("本地降级"));
        assertTrue(result.content().contains("操作系统"));
        assertTrue(result.content().contains("进程和线程有什么区别？"));
        assertTrue(result.content().contains("未配置远程 API Key 或远程服务超时"));
        chunks.forEach(chunk -> assertTrue(chunk.length() <= 16));
    }

    private void assertSchedule(String text,
                                String title,
                                LocalDateTime start,
                                LocalDateTime end,
                                LocalDateTime remind,
                                int priority) {
        AiChatResult result = provider.chat(AiPrompts.scheduleSystem(NOW), AiPrompts.scheduleUser(text));
        assertFalse(result.content().contains("```"));
        ParsedScheduleDraft draft = AiAnswerParser.parseSchedule(result.content(), text, NOW);
        assertEquals(title, draft.title(), text);
        assertEquals(start, draft.startTime(), text);
        assertEquals(end, draft.endTime(), text);
        assertEquals(remind, draft.remindTime(), text);
        assertEquals(priority, draft.priority(), text);
    }
}
