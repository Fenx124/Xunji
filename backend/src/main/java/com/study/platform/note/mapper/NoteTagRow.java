package com.study.platform.note.mapper;

/**
 * 笔记与标签的连接行，供列表批量组装 TagSimpleVO。
 */
public class NoteTagRow {

    private Long noteId;
    private Long id;
    private String name;
    private String color;

    public Long getNoteId() {
        return noteId;
    }

    public void setNoteId(Long noteId) {
        this.noteId = noteId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
