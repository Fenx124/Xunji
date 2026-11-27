package com.study.platform.dashboard.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "首页学习数据看板")
@JsonIgnoreProperties(ignoreUnknown = true)
public record DashboardOverviewVO(
        @Schema(description = "汇总指标")
        Summary summary,
        @Schema(description = "近 90 天活跃热力，含 0 计数的日期")
        List<HeatCell> heatmap,
        @Schema(description = "近 7 天趋势")
        List<TrendPoint> weeklyTrend,
        @Schema(description = "笔记本笔记数")
        List<NameCount> notebookDistribution,
        @Schema(description = "标签笔记数，最多 10 个")
        List<TagCount> tagDistribution
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Summary(
            long totalNotes,
            long todayPendingSchedules,
            double monthCompletionRate,
            long weekFocusMinutes,
            long todayFocusMinutes
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HeatCell(String date, long count) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TrendPoint(String date, long notesUpdated, long schedulesCompleted, long focusMinutes) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NameCount(String name, long count) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TagCount(String name, String color, long count) {
    }
}
