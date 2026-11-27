package com.study.platform.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "笔记智能摘要")
public record AiSummarizeRequest(
        @Schema(description = "笔记 ID", example = "1")
        @NotNull(message = "笔记 ID 不能为空")
        Long noteId
) {
}
