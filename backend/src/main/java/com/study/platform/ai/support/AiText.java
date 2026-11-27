package com.study.platform.ai.support;

/**
 * 截断与 Token 粗估。中文按 Java char 计数，和摘要 300 字的展示口径一致。
 */
public final class AiText {

    private AiText() {
    }

    public static String emptyTo(String text, String fallback) {
        if (text == null || text.isBlank()) {
            return fallback;
        }
        return text.trim();
    }

    public static String clamp(String text, int maxChars) {
        if (text == null || maxChars <= 0) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.length() <= maxChars) {
            return trimmed;
        }
        int end = maxChars;
        if (Character.isHighSurrogate(trimmed.charAt(end - 1))) {
            end--;
        }
        if (end <= 0) {
            return "";
        }
        if (end >= trimmed.length()) {
            return trimmed;
        }
        return trimmed.substring(0, end);
    }

    public static int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return Math.max(1, text.length() / 2);
    }
}
