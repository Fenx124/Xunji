package com.study.platform.note.support;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoteKeywordTest {

    @Test
    void shouldSkipBlankKeyword() {
        assertEquals(NoteKeyword.Mode.NONE, NoteKeyword.parse(null).mode());
        assertNull(NoteKeyword.parse("   ").value());
        assertEquals(NoteKeyword.Mode.NONE, NoteKeyword.parse(" \n ").mode());
    }

    @Test
    void shouldUseLikeForSingleCharacterBecauseNgramIgnoresIt() {
        NoteKeyword.Parsed parsed = NoteKeyword.parse(" 中 ");

        assertEquals(NoteKeyword.Mode.LIKE, parsed.mode());
        assertEquals("中", parsed.value());
    }

    @Test
    void shouldUseFulltextWhenAtLeastTwoCharacters() {
        NoteKeyword.Parsed parsed = NoteKeyword.parse("  笔记检索  ");

        assertEquals(NoteKeyword.Mode.FULLTEXT, parsed.mode());
        assertEquals("笔记检索", parsed.value());
    }
}

class NotebookNamesTest {

    @Test
    void shouldNotThrowWhenUncategorizedNoteLooksUpImmutableMap() {
        assertNull(NotebookNames.lookup(Map.of(), null));
        assertNull(NotebookNames.lookup(Map.of(1L, "数据库原理"), null));
        assertNull(NotebookNames.lookup(Map.of(1L, "数据库原理"), 9L));
        assertEquals("数据库原理", NotebookNames.lookup(Map.of(1L, "数据库原理"), 1L));
        assertNull(NotebookNames.lookup(null, 1L));
    }
}

class NoteFlagsTest {

    @Test
    void shouldToggleWhenRequestOmitsTheTarget() {
        assertFalse(NoteFlags.resolve(null, true));
        assertTrue(NoteFlags.resolve(null, false));
    }

    @Test
    void shouldHonorExplicitTarget() {
        assertTrue(NoteFlags.resolve(true, false));
        assertFalse(NoteFlags.resolve(false, true));
        assertEquals(1, NoteFlags.toInt(true));
        assertEquals(0, NoteFlags.toInt(false));
        assertTrue(NoteFlags.isOn(1));
        assertFalse(NoteFlags.isOn(0));
        assertFalse(NoteFlags.isOn(null));
    }
}

class NotePageQueryTest {

    @Test
    void shouldApplyDefaultsAndCaps() {
        assertEquals(1L, NotePageQuery.normalizePageNum(0));
        assertEquals(1L, NotePageQuery.normalizePageNum(-3));
        assertEquals(20L, NotePageQuery.normalizePageSize(0));
        assertEquals(100L, NotePageQuery.normalizePageSize(500));
        assertEquals(40L, NotePageQuery.offset(3, 20));
        assertEquals(0L, NotePageQuery.offset(1, 20));
    }
}
