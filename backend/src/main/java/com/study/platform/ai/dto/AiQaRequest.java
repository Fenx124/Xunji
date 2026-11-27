package com.study.platform.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "笔记流式问答")
public record AiQaRequest(
        @Schema(description = "笔记 ID，可空表示不绑定笔记")
        Long noteId,

        @Schema(description = "问题", example = "进程和线程有什么区别？")
        @NotBlank(message = "问题不能为空")
        @Size(max = 2000, message = "问题长度不能超过 2000")
        String question
) {
}
