package com.study.platform.note.mapper;

/**
 * 只取笔记标题，给日程列表回填 noteTitle。包含回收站中的笔记，避免关联标题突然变空。
 */
public class NoteTitleRow {

    private Long id;
    private String title;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
