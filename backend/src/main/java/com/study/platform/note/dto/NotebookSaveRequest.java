package com.study.platform.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "新建或修改笔记本")
public record NotebookSaveRequest(
        @Schema(description = "笔记本名称", example = "高数")
        @NotBlank(message = "笔记本名称不能为空")
        @Size(max = 80, message = "笔记本名称长度不能超过 80")
        String name,

        @Schema(description = "简介")
        @Size(max = 255, message = "笔记本简介长度不能超过 255")
        String description,

        @Schema(description = "前端图标标识", example = "Notebook")
        @Size(max = 40, message = "图标标识长度不能超过 40")
        String icon,

        @Schema(description = "排序值，升序", example = "0")
        Integer sortOrder
) {
}
