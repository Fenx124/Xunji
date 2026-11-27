package com.study.platform.note.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "设置收藏。字段缺省时由服务端取反。")
public record NoteFavoriteRequest(
        @Schema(description = "目标收藏状态")
        @JsonProperty("isFavorite")
        Boolean isFavorite
) {
}
