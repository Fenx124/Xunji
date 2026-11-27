package com.study.platform.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "自然语言解析日程")
public record AiParseScheduleRequest(
        @Schema(description = "口语描述", example = "明天晚上8点前交软件体系结构报告，重要且紧急")
        @NotBlank(message = "日程描述不能为空")
        @Size(max = 500, message = "日程描述长度不能超过 500")
        String text
) {
}
