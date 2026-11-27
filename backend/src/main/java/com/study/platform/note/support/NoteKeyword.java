package com.study.platform.note.support;

/**
 * 关键词检索策略。
 * MySQL ngram_token_size 默认为 2，单字不会进入全文索引，必须退回 LIKE。
 */
public final class NoteKeyword {

    public enum Mode {
        NONE,
        LIKE,
        FULLTEXT
    }

    public record Parsed(Mode mode, String value) {
    }

    private NoteKeyword() {
    }

    public static Parsed parse(String raw) {
        if (raw == null) {
            return new Parsed(Mode.NONE, null);
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return new Parsed(Mode.NONE, null);
        }
        if (value.length() == 1) {
            return new Parsed(Mode.LIKE, value);
        }
        return new Parsed(Mode.FULLTEXT, value);
    }
}
