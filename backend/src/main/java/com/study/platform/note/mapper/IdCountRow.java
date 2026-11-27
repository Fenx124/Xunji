package com.study.platform.note.mapper;

/**
 * 分组计数：id 是笔记本或标签，noteCount 是未删除笔记数。
 */
public class IdCountRow {

    private Long id;
    private Long noteCount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNoteCount() {
        return noteCount;
    }

    public void setNoteCount(Long noteCount) {
        this.noteCount = noteCount;
    }
}
