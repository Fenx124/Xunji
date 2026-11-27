package com.study.platform.note.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "置顶与收藏状态")
public record NoteFlagVO(
        @Schema(description = "笔记 ID")
        Long id,
        @Schema(description = "是否置顶")
        @JsonProperty("isPinned")
        boolean isPinned,
        @Schema(description = "是否收藏")
        @JsonProperty("isFavorite")
        boolean isFavorite
) {
}
