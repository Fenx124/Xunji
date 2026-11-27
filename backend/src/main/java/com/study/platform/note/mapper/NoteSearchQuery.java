package com.study.platform.note.mapper;

/**
 * 笔记列表的动态查询条件。布尔字段用包装类型，方便 MyBatis 判断 null。
 */
public class NoteSearchQuery {

    private Long notebookId;
    private Long tagId;
    private String keyword;
    private Boolean useFulltext;
    private Integer favoriteFlag;

    public Long getNotebookId() {
        return notebookId;
    }

    public void setNotebookId(Long notebookId) {
        this.notebookId = notebookId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Boolean getUseFulltext() {
        return useFulltext;
    }

    public void setUseFulltext(Boolean useFulltext) {
        this.useFulltext = useFulltext;
    }

    public Integer getFavoriteFlag() {
        return favoriteFlag;
    }

    public void setFavoriteFlag(Integer favoriteFlag) {
        this.favoriteFlag = favoriteFlag;
    }
}
