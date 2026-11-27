package com.study.platform.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "AI 交互历史")
public record AiChatLogVO(
        @Schema(description = "日志 ID")
        Long id,
        @Schema(description = "关联笔记 ID")
        Long noteId,
        @Schema(description = "关联笔记标题")
        String noteTitle,
        @Schema(description = "SUMMARY / NOTE_QA / SCHEDULE_PARSE")
        String featureType,
        @Schema(description = "提供方")
        String provider,
        @Schema(description = "模型名")
        String modelName,
        @Schema(description = "用户输入")
        String promptText,
        @Schema(description = "模型返回")
        String responseText,
        @Schema(description = "输入 Token")
        Integer promptTokens,
        @Schema(description = "输出 Token")
        Integer completionTokens,
        @Schema(description = "耗时毫秒")
        Integer latencyMs,
        @Schema(description = "是否成功")
        boolean success,
        @Schema(description = "失败原因")
        String errorMessage,
        @Schema(description = "调用时间")
        LocalDateTime createdAt
) {
}
