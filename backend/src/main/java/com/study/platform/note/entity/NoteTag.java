package com.study.platform.note.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 笔记-标签关联，对应表 note_tag（复合主键 note_id + tag_id）。
 * 禁止使用 selectById / deleteById：MyBatis-Plus 只会按 note_id 命中，会误删该笔记的全部标签。
 */
@TableName("note_tag")
public class NoteTag {

    @TableId(value = "note_id", type = IdType.INPUT)
    private Long noteId;
    private Long tagId;

    public Long getNoteId() {
        return noteId;
    }

    public void setNoteId(Long noteId) {
        this.noteId = noteId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }
}
