package com.study.platform.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.platform.note.entity.NoteHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 笔记历史快照。实体没有 deleted 字段，内置方法不会拼接逻辑删除条件。
 * 列表查询不取出正文，避免把 MEDIUMTEXT 整列带回。
 */
@Mapper
public interface NoteHistoryMapper extends BaseMapper<NoteHistory> {

    @Select("""
            SELECT id, note_id, version_no, title, change_note, created_at
            FROM note_history
            WHERE note_id = #{noteId}
            ORDER BY version_no DESC, id DESC
            LIMIT #{limit}
            """)
    List<NoteHistory> selectItemsByNoteId(@Param("noteId") Long noteId, @Param("limit") int limit);

    @Select("""
            SELECT id
            FROM note_history
            WHERE note_id = #{noteId}
            ORDER BY id DESC
            """)
    List<Long> selectIdsByNoteIdDesc(@Param("noteId") Long noteId);
}
