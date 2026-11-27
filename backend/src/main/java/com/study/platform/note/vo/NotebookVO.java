package com.study.platform.note.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "笔记本")
public record NotebookVO(
        @Schema(description = "笔记本 ID")
        Long id,
        @Schema(description = "名称")
        String name,
        @Schema(description = "简介")
        String description,
        @Schema(description = "图标")
        String icon,
        @Schema(description = "排序值")
        Integer sortOrder,
        @Schema(description = "未删除笔记数量")
        Long noteCount,
        @Schema(description = "创建时间")
        LocalDateTime createdAt,
        @Schema(description = "更新时间")
        LocalDateTime updatedAt
) {
}
