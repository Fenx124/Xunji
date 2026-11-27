package com.study.platform.note.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "笔记上的标签摘要")
public record TagSimpleVO(
        @Schema(description = "标签 ID")
        Long id,
        @Schema(description = "名称")
        String name,
        @Schema(description = "色值")
        String color
) {
}
