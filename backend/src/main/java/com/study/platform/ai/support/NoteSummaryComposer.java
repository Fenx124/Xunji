package com.study.platform.ai.support;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 从标题、Markdown 标题和关键句拼一段不超过 300 字的中文摘要，并给出 3 到 5 个标签。
 */
public final class NoteSummaryComposer {

    private static final int SUMMARY_LIMIT = 300;
    private static final List<String> KEYWORDS = List.of(
            "操作系统", "数据结构", "计算机网络", "数据库", "高数", "线性代数",
            "英语", "Java", "Spring", "算法", "复习", "体系结构"
    );
    private static final List<String> PADDING = List.of("学习笔记", "复习要点", "课程笔记", "知识整理", "重点摘要");

    private NoteSummaryComposer() {
    }

    public static SummaryDraft compose(String title, String content) {
        String safeTitle = title == null ? "" : title.trim();
        String safeContent = content == null ? "" : content;
        List<String> points = collectPoints(safeContent);
        String summary = buildSummary(safeTitle, points, safeContent);
        List<String> tags = suggestTags(safeTitle, points, safeContent);
        return new SummaryDraft(summary, tags);
    }

    public static List<String> suggestTags(String title, String content) {
        return suggestTags(title == null ? "" : title.trim(), collectPoints(content == null ? "" : content), content == null ? "" : content);
    }

    private static List<String> collectPoints(String content) {
        List<String> points = new ArrayList<>();
        for (String line : content.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("#")) {
                continue;
            }
            String heading = trimmed.replaceFirst("^#+\\s*", "").trim();
            if (!heading.isEmpty()) {
                points.add(heading);
            }
        }
        String withoutHeadings = content.replaceAll("(?m)^#+.*$", " ");
        for (String sentence : withoutHeadings.split("[。！？!?\\n\\r]+")) {
            if (points.size() >= 6) {
                break;
            }
            String trimmed = sentence.replaceAll("\\s+", " ").trim();
            if (trimmed.length() >= 6) {
                points.add(trimmed);
            }
        }
        return points;
    }

    private static String buildSummary(String title, List<String> points, String content) {
        StringBuilder builder = new StringBuilder();
        if (!title.isEmpty()) {
            builder.append('《').append(title).append('》');
        }
        if (points.isEmpty()) {
            String plain = content.replaceAll("\\s+", " ").trim();
            if (plain.isEmpty()) {
                builder.append("内容较少，建议补充关键要点。");
            } else {
                builder.append("要点：").append(plain);
            }
        } else {
            builder.append("要点：");
            int index = 1;
            for (String point : points) {
                if (index > 4) {
                    break;
                }
                builder.append(index++).append('.').append(point).append('；');
            }
        }
        String summary = AiText.clamp(builder.toString(), SUMMARY_LIMIT);
        if (summary.isBlank()) {
            return "内容较少，建议补充关键要点。";
        }
        return summary;
    }

    private static List<String> suggestTags(String title, List<String> points, String content) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        addTag(tags, shorten(title, 12));
        for (String point : points) {
            if (tags.size() >= 5) {
                break;
            }
            if (point.length() <= 12) {
                addTag(tags, point);
            }
        }
        String haystack = title + "\n" + content;
        for (String keyword : KEYWORDS) {
            if (tags.size() >= 5) {
                break;
            }
            if (haystack.contains(keyword)) {
                addTag(tags, keyword);
            }
        }
        for (String pad : PADDING) {
            if (tags.size() >= 3) {
                break;
            }
            addTag(tags, pad);
        }
        if (tags.size() > 5) {
            return new ArrayList<>(tags).subList(0, 5);
        }
        return List.copyOf(tags);
    }

    private static String shorten(String text, int max) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return "";
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
}
