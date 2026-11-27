package com.study.platform.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "笔记摘要结果")
public record AiSummarizeVO(
        @Schema(description = "笔记 ID")
        Long noteId,
        @Schema(description = "中文摘要，不超过 300 字")
        String summary,
        @Schema(description = "推荐标签，3 到 5 个")
        List<String> suggestedTags,
        @Schema(description = "是否走了本地降级")
        boolean fallback,
        @Schema(description = "耗时毫秒")
        long latencyMs
) {
}
