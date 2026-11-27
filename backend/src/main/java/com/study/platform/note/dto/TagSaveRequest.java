package com.study.platform.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "新建或修改标签")
public record TagSaveRequest(
        @Schema(description = "标签名", example = "重点")
        @NotBlank(message = "标签名称不能为空")
        @Size(max = 40, message = "标签名称长度不能超过 40")
        String name,

        @Schema(description = "色值", example = "#409EFF")
        @Size(max = 20, message = "标签颜色长度不能超过 20")
        String color
) {
}
