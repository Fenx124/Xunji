package com.study.platform.focus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.platform.common.DayTotalRow;
import com.study.platform.focus.entity.FocusRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface FocusRecordMapper extends BaseMapper<FocusRecord> {

    @Select("""
            SELECT CAST(COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END), 0) AS SIGNED) AS completedCount,
                   CAST(COALESCE(SUM(actual_minutes), 0) AS SIGNED) AS totalMinutes,
                   CAST(COUNT(*) AS SIGNED) AS totalSessions
            FROM focus_record
            WHERE user_id = #{userId}
              AND started_at >= #{start}
              AND started_at < #{end}
            """)
    FocusTodayRow summarize(@Param("userId") Long userId,
                            @Param("start") LocalDateTime start,
                            @Param("end") LocalDateTime end);

    @Select("""
            SELECT CAST(COALESCE(SUM(actual_minutes), 0) AS SIGNED)
            FROM focus_record
            WHERE user_id = #{userId}
              AND started_at >= #{start}
              AND started_at < #{end}
            """)
    Long sumMinutes(@Param("userId") Long userId,
                    @Param("start") LocalDateTime start,
                    @Param("end") LocalDateTime end);

    @Select("""
            SELECT DATE_FORMAT(started_at, '%Y-%m-%d') AS bucket,
                   CAST(COUNT(*) AS SIGNED) AS total
            FROM focus_record
            WHERE user_id = #{userId}
              AND started_at >= #{start}
              AND started_at < #{end}
            GROUP BY DATE_FORMAT(started_at, '%Y-%m-%d')
            """)
    List<DayTotalRow> countStartedByDay(@Param("userId") Long userId,
                                        @Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);

    @Select("""
            SELECT DATE_FORMAT(started_at, '%Y-%m-%d') AS bucket,
                   CAST(COALESCE(SUM(actual_minutes), 0) AS SIGNED) AS total
            FROM focus_record
            WHERE user_id = #{userId}
              AND started_at >= #{start}
              AND started_at < #{end}
            GROUP BY DATE_FORMAT(started_at, '%Y-%m-%d')
            """)
    List<DayTotalRow> sumMinutesByDay(@Param("userId") Long userId,
                                      @Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);
}
