package com.study.platform.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.platform.common.DayTotalRow;
import com.study.platform.note.entity.Note;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 笔记表。内置方法受全局逻辑删除约束，只会看到 deleted = 0。
 * 回收站必须使用下面的显式 SQL，直接命中 deleted = 1，否则会被自动追加的 deleted = 0 挡掉。
 */
@Mapper
public interface NoteMapper extends BaseMapper<Note> {

    @Select("""
            SELECT id, user_id, notebook_id, title, content, ai_summary, is_pinned, is_favorite,
                   word_count, version, deleted, deleted_at, created_at, updated_at
            FROM note
            WHERE id = #{id}
            """)
    Note selectRawById(@Param("id") Long id);

    @Select("""
            <script>
            SELECT id, title
            FROM note
            WHERE user_id = #{userId}
              AND id IN
              <foreach collection="ids" item="id" open="(" separator="," close=")">
                #{id}
              </foreach>
            </script>
            """)
    List<NoteTitleRow> selectTitles(@Param("userId") Long userId, @Param("ids") Collection<Long> ids);

    @Select("""
            SELECT notebook_id AS id, COUNT(*) AS noteCount
            FROM note
            WHERE user_id = #{userId} AND deleted = 0 AND notebook_id IS NOT NULL
            GROUP BY notebook_id
            """)
    List<IdCountRow> countActiveByNotebook(@Param("userId") Long userId);

    @Select("""
            SELECT id FROM note
            WHERE user_id = #{userId} AND notebook_id = #{notebookId}
            """)
    List<Long> selectIdsByNotebook(@Param("userId") Long userId, @Param("notebookId") Long notebookId);

    @Update("""
            UPDATE note
            SET notebook_id = NULL
            WHERE user_id = #{userId} AND notebook_id = #{notebookId}
            """)
    int clearNotebookId(@Param("userId") Long userId, @Param("notebookId") Long notebookId);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM note n
            <if test="q.tagId != null">
              INNER JOIN note_tag nt ON nt.note_id = n.id AND nt.tag_id = #{q.tagId}
            </if>
            WHERE n.user_id = #{userId}
              AND n.deleted = 0
            <if test="q.notebookId != null">
              AND n.notebook_id = #{q.notebookId}
            </if>
            <if test="q.favoriteFlag != null">
              AND n.is_favorite = #{q.favoriteFlag}
            </if>
            <if test="q.keyword != null and q.keyword != ''">
              <choose>
                <when test="q.useFulltext">
                  AND MATCH(n.title, n.content) AGAINST(#{q.keyword} IN NATURAL LANGUAGE MODE)
                </when>
                <otherwise>
                  AND (n.title LIKE CONCAT('%', #{q.keyword}, '%') OR n.content LIKE CONCAT('%', #{q.keyword}, '%'))
                </otherwise>
              </choose>
            </if>
            </script>
            """)
    long countActive(@Param("userId") Long userId, @Param("q") NoteSearchQuery query);

    /**
     * 列表不取正文。单字走 LIKE，两个字符及以上走 ngram 全文索引。
     * id 作为次序并列时的稳定次序，避免分页抖动。
     */
    @Select("""
            <script>
            SELECT n.id, n.user_id, n.notebook_id, n.title, n.ai_summary, n.is_pinned, n.is_favorite,
                   n.word_count, n.version, n.deleted, n.deleted_at, n.created_at, n.updated_at
            FROM note n
            <if test="q.tagId != null">
              INNER JOIN note_tag nt ON nt.note_id = n.id AND nt.tag_id = #{q.tagId}
            </if>
            WHERE n.user_id = #{userId}
              AND n.deleted = 0
            <if test="q.notebookId != null">
              AND n.notebook_id = #{q.notebookId}
            </if>
            <if test="q.favoriteFlag != null">
              AND n.is_favorite = #{q.favoriteFlag}
            </if>
            <if test="q.keyword != null and q.keyword != ''">
              <choose>
                <when test="q.useFulltext">
                  AND MATCH(n.title, n.content) AGAINST(#{q.keyword} IN NATURAL LANGUAGE MODE)
                </when>
                <otherwise>
                  AND (n.title LIKE CONCAT('%', #{q.keyword}, '%') OR n.content LIKE CONCAT('%', #{q.keyword}, '%'))
                </otherwise>
              </choose>
            </if>
            ORDER BY n.is_pinned DESC, n.updated_at DESC, n.id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<Note> selectActivePage(@Param("userId") Long userId,
                                @Param("q") NoteSearchQuery query,
                                @Param("offset") long offset,
                                @Param("limit") long limit);

    @Select("""
            SELECT COUNT(*)
            FROM note
            WHERE user_id = #{userId} AND deleted = 1
            """)
    long countTrash(@Param("userId") Long userId);

    @Select("""
            SELECT id, user_id, notebook_id, title, ai_summary, is_pinned, is_favorite,
                   word_count, version, deleted, deleted_at, created_at, updated_at
            FROM note
            WHERE user_id = #{userId} AND deleted = 1
            ORDER BY deleted_at DESC, id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<Note> selectTrashPage(@Param("userId") Long userId,
                               @Param("offset") long offset,
                               @Param("limit") long limit);

    @Update("""
            UPDATE note
            SET deleted = 1, deleted_at = NOW()
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            """)
    int moveToTrash(@Param("id") Long id, @Param("userId") Long userId);

    @Update("""
            UPDATE note
            SET deleted = 0, deleted_at = NULL
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 1
            """)
    int restoreFromTrash(@Param("id") Long id, @Param("userId") Long userId);

    @Delete("""
            DELETE FROM note
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 1
            """)
    int deletePermanent(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 热力图：当天新建或当天有更新的未删除笔记各计 1。同一天又建又改只计一次。
     */
    @Select("""
            SELECT bucket, CAST(COUNT(*) AS SIGNED) AS total
            FROM (
                SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS bucket
                FROM note
                WHERE user_id = #{userId}
                  AND deleted = 0
                  AND created_at >= #{start}
                  AND created_at < #{end}
                UNION ALL
                SELECT DATE_FORMAT(updated_at, '%Y-%m-%d') AS bucket
                FROM note
                WHERE user_id = #{userId}
                  AND deleted = 0
                  AND updated_at >= #{start}
                  AND updated_at < #{end}
                  AND DATE(updated_at) != DATE(created_at)
            ) activity
            GROUP BY bucket
            """)
    List<DayTotalRow> countActivityByDay(@Param("userId") Long userId,
                                         @Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

    @Select("""
            SELECT DATE_FORMAT(updated_at, '%Y-%m-%d') AS bucket,
                   CAST(COUNT(*) AS SIGNED) AS total
            FROM note
            WHERE user_id = #{userId}
              AND deleted = 0
              AND updated_at >= #{start}
              AND updated_at < #{end}
            GROUP BY DATE_FORMAT(updated_at, '%Y-%m-%d')
            """)
    List<DayTotalRow> countUpdatedByDay(@Param("userId") Long userId,
                                        @Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);

    @Select("""
            SELECT name, CAST(SUM(cnt) AS SIGNED) AS total
            FROM (
                SELECT IF(nb.id IS NULL, '未归类', nb.name) AS name,
                       1 AS cnt
                FROM note n
                LEFT JOIN notebook nb
                  ON nb.id = n.notebook_id AND nb.deleted = 0 AND nb.user_id = n.user_id
                WHERE n.user_id = #{userId} AND n.deleted = 0
            ) buckets
            GROUP BY name
            ORDER BY total DESC, name ASC
            """)
    List<NameCountRow> countByNotebook(@Param("userId") Long userId);

    @Select("""
            SELECT t.name AS name,
                   t.color AS color,
                   CAST(COUNT(*) AS SIGNED) AS total
            FROM tag t
            INNER JOIN note_tag nt ON nt.tag_id = t.id
            INNER JOIN note n ON n.id = nt.note_id AND n.user_id = #{userId} AND n.deleted = 0
            WHERE t.user_id = #{userId}
            GROUP BY t.id, t.name, t.color
            ORDER BY total DESC, t.id ASC
            LIMIT #{limit}
            """)
    List<NameCountRow> countByTag(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 只改 AI 摘要，不碰乐观锁 version。
     * 摘要和用户正在保存的正文互不阻塞，避免把 AI 调用变成 409。
     */
    @Update("""
            UPDATE note
            SET ai_summary = #{summary}
            WHERE id = #{id}
              AND user_id = #{userId}
              AND deleted = 0
            """)
    int updateAiSummary(@Param("id") Long id, @Param("userId") Long userId, @Param("summary") String summary);
}
