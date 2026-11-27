package com.study.platform.note.service;

/**
 * AI 模块可读的笔记正文。只包含摘要和问答需要的字段。
 */
public record NoteAiSnapshot(
        Long id,
        String title,
        String content
) {
}
