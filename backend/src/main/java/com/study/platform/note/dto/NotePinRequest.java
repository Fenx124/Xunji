package com.study.platform.note.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "设置置顶。字段缺省时由服务端取反。")
public record NotePinRequest(
        @Schema(description = "目标置顶状态")
        @JsonProperty("isPinned")
        Boolean isPinned
) {
}
