package com.study.platform.note.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "笔记列表项")
public record NoteListItemVO(
        @Schema(description = "笔记 ID")
        Long id,
        @Schema(description = "笔记本 ID")
        Long notebookId,
        @Schema(description = "笔记本名称")
        String notebookName,
        @Schema(description = "标题")
        String title,
        @Schema(description = "AI 摘要")
        String aiSummary,
        @Schema(description = "是否置顶")
        @JsonProperty("isPinned")
        boolean isPinned,
        @Schema(description = "是否收藏")
        @JsonProperty("isFavorite")
        boolean isFavorite,
        @Schema(description = "正文字数")
        Integer wordCount,
        @Schema(description = "乐观锁版本")
        Integer version,
        @Schema(description = "标签")
        List<TagSimpleVO> tags,
        @Schema(description = "移入回收站时间")
        LocalDateTime deletedAt,
        @Schema(description = "创建时间")
        LocalDateTime createdAt,
        @Schema(description = "更新时间")
        LocalDateTime updatedAt
) {
}
