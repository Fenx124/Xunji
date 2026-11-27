package com.study.platform.note.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NoteWordCounterTest {

    @Test
    void shouldIgnoreNullBlankAndWhitespace() {
        assertEquals(0, NoteWordCounter.countNonWhitespace(null));
        assertEquals(0, NoteWordCounter.countNonWhitespace(""));
        assertEquals(0, NoteWordCounter.countNonWhitespace(" \n\t\r"));
    }

    @Test
    void shouldCountLettersPunctuationAndCjk() {
        assertEquals(4, NoteWordCounter.countNonWhitespace("a b\nc\td"));
        assertEquals(4, NoteWordCounter.countNonWhitespace("你好 世界"));
        assertEquals(8, NoteWordCounter.countNonWhitespace("Hello, 世界"));
        assertEquals(5, NoteWordCounter.countNonWhitespace("# 标题\n\n正文"));
    }
}
