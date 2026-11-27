package com.study.platform.note.support;

/**
 * 笔记正文字数：不计空白字符（空格、制表符、换行）。
 * 标点保留，中文按单个字符计数，供列表展示与后续统计使用。
 */
public final class NoteWordCounter {

    private NoteWordCounter() {
    }

    public static int countNonWhitespace(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        int count = 0;
        int length = content.length();
        for (int i = 0; i < length; i++) {
            if (!Character.isWhitespace(content.charAt(i))) {
                count++;
            }
        }
        return count;
    }
}
