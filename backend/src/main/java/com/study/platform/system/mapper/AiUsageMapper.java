package com.study.platform.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 只读聚合 ai_chat_log。用量监控属于 system，不依赖尚未落地的 AI 业务 Mapper。
 * 聚合结果 CAST 成整数，避免 MySQL SUM 返回 DECIMAL 时映射失败。
 */
@Mapper
public interface AiUsageMapper {

    @Select("""
            SELECT CAST(COUNT(*) AS SIGNED) AS totalCalls,
                   CAST(COALESCE(SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END), 0) AS SIGNED) AS successCalls,
                   CAST(COALESCE(SUM(latency_ms), 0) AS SIGNED) AS totalLatencyMs,
                   CAST(COALESCE(SUM(COALESCE(prompt_tokens, 0) + COALESCE(completion_tokens, 0)), 0) AS SIGNED) AS totalTokens
            FROM ai_chat_log
            WHERE created_at >= #{start}
              AND created_at < #{end}
            """)
    AiUsageTotalRow summarize(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("""
            SELECT feature_type AS featureType,
                   CAST(COUNT(*) AS SIGNED) AS calls,
                   CAST(ROUND(AVG(latency_ms)) AS SIGNED) AS avgLatencyMs
            FROM ai_chat_log
            WHERE created_at >= #{start}
              AND created_at < #{end}
            GROUP BY feature_type
            ORDER BY calls DESC, feature_type ASC
            """)
    List<AiFeatureUsageRow> byFeature(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("""
            SELECT user_id AS userId,
                   CAST(COUNT(*) AS SIGNED) AS calls,
                   CAST(COALESCE(SUM(CASE WHEN created_at >= #{todayStart} AND created_at < #{todayEnd} THEN 1 ELSE 0 END), 0) AS SIGNED) AS todayCalls
            FROM ai_chat_log
            WHERE created_at >= #{start}
              AND created_at < #{end}
            GROUP BY user_id
            ORDER BY calls DESC, user_id ASC
            """)
    List<AiUserUsageRow> byUser(@Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end,
                                @Param("todayStart") LocalDateTime todayStart,
                                @Param("todayEnd") LocalDateTime todayEnd);

    @Select("""
            SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS bucket,
                   CAST(COUNT(*) AS SIGNED) AS calls,
                   CAST(COALESCE(SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END), 0) AS SIGNED) AS successCalls
            FROM ai_chat_log
            WHERE created_at >= #{start}
              AND created_at < #{end}
            GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d')
            """)
    List<AiDailyUsageRow> daily(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
