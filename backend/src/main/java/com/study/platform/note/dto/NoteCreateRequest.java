package com.study.platform.note.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "新建笔记")
public record NoteCreateRequest(
        @Schema(description = "笔记本 ID，空表示未归类")
        Long notebookId,

        @Schema(description = "标题", example = "极限的定义")
        @NotBlank(message = "笔记标题不能为空")
        @Size(max = 200, message = "笔记标题长度不能超过 200")
        String title,

        @Schema(description = "Markdown 正文")
        @NotNull(message = "笔记正文不能为空")
        String content,

        @Schema(description = "标签 ID 列表")
        List<Long> tagIds,

        @Schema(description = "是否置顶")
        @JsonProperty("isPinned")
        Boolean isPinned,

        @Schema(description = "是否收藏")
        @JsonProperty("isFavorite")
        Boolean isFavorite
) {
}
