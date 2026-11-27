package com.study.platform.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "全站 AI 用量")
public record AdminAiStatsVO(
        @Schema(description = "窗口内调用次数")
        long totalCalls,
        @Schema(description = "窗口内成功次数")
        long successCalls,
        @Schema(description = "成功率百分比，保留 1 位小数")
        double successRate,
        @Schema(description = "平均耗时毫秒")
        long avgLatencyMs,
        @Schema(description = "输入加输出 Token")
        long totalTokens,
        List<FeatureStat> byFeature,
        List<UserStat> byUser,
        List<DailyStat> dailyTrend
) {
    public record FeatureStat(String featureType, long calls, long avgLatencyMs) {
    }

    public record UserStat(
            Long userId,
            String username,
            String nickname,
            long calls,
            long todayCalls,
            int aiDailyLimit
    ) {
    }

    public record DailyStat(String date, long calls, long successCalls) {
    }
}
