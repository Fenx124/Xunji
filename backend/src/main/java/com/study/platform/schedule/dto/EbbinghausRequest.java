package com.study.platform.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "按艾宾浩斯曲线为一篇笔记生成 5 条复习日程")
public record EbbinghausRequest(
        @Schema(description = "笔记 ID", example = "1")
        @NotNull(message = "笔记 ID 不能为空")
        Long noteId,

        @Schema(description = "基准日期 yyyy-MM-dd，缺省为今天", example = "2026-11-20")
        String baseDate
) {
}
