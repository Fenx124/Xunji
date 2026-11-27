package com.study.platform.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.platform.common.DayTotalRow;
import com.study.platform.schedule.entity.Schedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Mapper
public interface ScheduleMapper extends BaseMapper<Schedule> {

    /**
     * 绕过逻辑删除，用来区分“不存在”和“属于别人”。
     */
    @Select("""
            SELECT id, user_id, note_id, title, description, start_time, end_time, remind_time,
                   reminded, priority, status, source_type, completed_at, deleted, created_at, updated_at
            FROM schedule
            WHERE id = #{id}
            """)
    Schedule selectRawById(@Param("id") Long id);

    /**
     * 未完成且将在 24 小时内到达提醒时间，或提醒时间已过仍未完成。
     * 没有提醒时间时，用开始时间代替。
     */
    @Select("""
            SELECT id, user_id, note_id, title, description, start_time, end_time, remind_time,
                   reminded, priority, status, source_type, completed_at, deleted, created_at, updated_at
            FROM schedule
            WHERE user_id = #{userId}
              AND deleted = 0
              AND status = 0
              AND (
                    (remind_time IS NOT NULL AND remind_time <= DATE_ADD(NOW(), INTERVAL 24 HOUR))
                 OR (remind_time IS NULL AND start_time <= DATE_ADD(NOW(), INTERVAL 24 HOUR))
              )
            ORDER BY COALESCE(remind_time, start_time) ASC, id ASC
            """)
    List<Schedule> selectUpcomingReminders(@Param("userId") Long userId);

    /**
     * 到期且尚未发送的待办。单轮最多 200 条，避免一次扫描把邮件通道打满。
     */
    @Select("""
            SELECT id, user_id, note_id, title, description, start_time, end_time, remind_time,
                   reminded, priority, status, source_type, completed_at, deleted, created_at, updated_at
            FROM schedule
            WHERE deleted = 0
              AND status = 0
              AND reminded = 0
              AND remind_time IS NOT NULL
              AND remind_time <= NOW()
            ORDER BY remind_time ASC, id ASC
            LIMIT 200
            """)
    List<Schedule> selectDueReminders();

    @Update("""
            UPDATE schedule
            SET reminded = 1
            WHERE id = #{id} AND deleted = 0 AND reminded = 0
            """)
    int markReminded(@Param("id") Long id);

    @Select("""
            <script>
            SELECT id, title
            FROM schedule
            WHERE user_id = #{userId}
              AND deleted = 0
              AND id IN
              <foreach collection="ids" item="id" open="(" separator="," close=")">
                #{id}
              </foreach>
            </script>
            """)
    List<ScheduleTitleRow> selectTitles(@Param("userId") Long userId, @Param("ids") Collection<Long> ids);

    @Select("""
            SELECT CAST(COALESCE(SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END), 0) AS SIGNED) AS completed,
                   CAST(COALESCE(SUM(CASE WHEN status != 2 THEN 1 ELSE 0 END), 0) AS SIGNED) AS counted
            FROM schedule
            WHERE user_id = #{userId}
              AND deleted = 0
              AND start_time >= #{start}
              AND start_time < #{end}
            """)
    CompletionCountRow summarizeCompletion(@Param("userId") Long userId,
                                           @Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end);

    @Select("""
            SELECT DATE_FORMAT(completed_at, '%Y-%m-%d') AS bucket,
                   CAST(COUNT(*) AS SIGNED) AS total
            FROM schedule
            WHERE user_id = #{userId}
              AND deleted = 0
              AND status = 1
              AND completed_at IS NOT NULL
              AND completed_at >= #{start}
              AND completed_at < #{end}
            GROUP BY DATE_FORMAT(completed_at, '%Y-%m-%d')
            """)
    List<DayTotalRow> countCompletedByDay(@Param("userId") Long userId,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);
}
