package com.study.platform.note.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "标签")
public record TagVO(
        @Schema(description = "标签 ID")
        Long id,
        @Schema(description = "名称")
        String name,
        @Schema(description = "色值")
        String color,
        @Schema(description = "未删除笔记数量")
        Long noteCount,
        @Schema(description = "创建时间")
        LocalDateTime createdAt
) {
}
