package com.study.platform.note.support;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoteHistoryRulesTest {

    @Test
    void shouldSnapshotOnlyWhenTitleOrContentChanges() {
        assertFalse(NoteHistoryRules.needsSnapshot("线性代数", "行列式", "线性代数", "行列式"));
        assertFalse(NoteHistoryRules.needsSnapshot("线性代数", "行列式", "  线性代数  ", "行列式"));
        assertFalse(NoteHistoryRules.needsSnapshot("线性代数", "", "线性代数", ""));
        assertTrue(NoteHistoryRules.needsSnapshot("线性代数", "行列式", "高等代数", "行列式"));
        assertTrue(NoteHistoryRules.needsSnapshot("线性代数", "行列式", "线性代数", "特征值"));
        assertTrue(NoteHistoryRules.needsSnapshot("线性代数", "行列式", "高等代数", "特征值"));
        assertTrue(NoteHistoryRules.needsSnapshot("线性代数", null, "线性代数", ""));
        assertEquals("线性代数", NoteHistoryRules.persistedTitle("  线性代数  "));
    }

    @Test
    void shouldKeepAtMostTenNewestSnapshots() {
        assertTrue(NoteHistoryRules.idsBeyondKeep(null).isEmpty());
        assertTrue(NoteHistoryRules.idsBeyondKeep(List.of()).isEmpty());

        List<Long> ten = newest(10);
        assertTrue(NoteHistoryRules.idsBeyondKeep(ten).isEmpty());

        List<Long> eleven = newest(11);
        assertEquals(List.of(1L), NoteHistoryRules.idsBeyondKeep(eleven));

        List<Long> twelve = newest(12);
        assertEquals(12, twelve.size());
        assertEquals(List.of(2L, 1L), NoteHistoryRules.idsBeyondKeep(twelve));
        assertEquals(12, twelve.size());
    }

    private static List<Long> newest(int count) {
        List<Long> ids = new ArrayList<>(count);
        for (long id = count; id >= 1; id--) {
            ids.add(id);
        }
        return ids;
    }
}
