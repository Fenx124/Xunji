package com.study.platform.note.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "更新笔记（带乐观锁版本号）")
public record NoteUpdateRequest(
        @Schema(description = "笔记本 ID，空表示移出笔记本")
        Long notebookId,

        @Schema(description = "标题")
        @NotBlank(message = "笔记标题不能为空")
        @Size(max = 200, message = "笔记标题长度不能超过 200")
        String title,

        @Schema(description = "Markdown 正文")
        @NotNull(message = "笔记正文不能为空")
        String content,

        @Schema(description = "标签 ID 列表，空数组表示清空标签")
        List<Long> tagIds,

        @Schema(description = "是否置顶，缺省则保持原值")
        @JsonProperty("isPinned")
        Boolean isPinned,

        @Schema(description = "是否收藏，缺省则保持原值")
        @JsonProperty("isFavorite")
        Boolean isFavorite,

        @Schema(description = "乐观锁版本号", example = "1")
        @NotNull(message = "版本号不能为空")
        Integer version
) {
}
