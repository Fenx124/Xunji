package com.study.platform.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.platform.note.entity.NoteTag;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 复合主键关联表。业务代码只走下面的显式 SQL，不调用 selectById / deleteById。
 */
@Mapper
public interface NoteTagMapper extends BaseMapper<NoteTag> {

    @Insert("INSERT INTO note_tag (note_id, tag_id) VALUES (#{noteId}, #{tagId})")
    int insertLink(@Param("noteId") Long noteId, @Param("tagId") Long tagId);

    @Delete("DELETE FROM note_tag WHERE note_id = #{noteId}")
    int deleteByNoteId(@Param("noteId") Long noteId);

    @Delete("DELETE FROM note_tag WHERE tag_id = #{tagId}")
    int deleteByTagId(@Param("tagId") Long tagId);

    @Select("SELECT note_id FROM note_tag WHERE tag_id = #{tagId}")
    List<Long> selectNoteIdsByTag(@Param("tagId") Long tagId);

    @Select("""
            <script>
            SELECT nt.note_id AS noteId, t.id AS id, t.name AS name, t.color AS color
            FROM note_tag nt
            INNER JOIN tag t ON t.id = nt.tag_id
            WHERE nt.note_id IN
            <foreach collection="noteIds" item="noteId" open="(" separator="," close=")">
              #{noteId}
            </foreach>
            ORDER BY t.id ASC
            </script>
            """)
    List<NoteTagRow> selectTagsByNoteIds(@Param("noteIds") Collection<Long> noteIds);

    @Select("""
            SELECT nt.tag_id AS id, COUNT(*) AS noteCount
            FROM note_tag nt
            INNER JOIN note n ON n.id = nt.note_id
            WHERE n.user_id = #{userId} AND n.deleted = 0
            GROUP BY nt.tag_id
            """)
    List<IdCountRow> countActiveNotesByTag(@Param("userId") Long userId);
}
