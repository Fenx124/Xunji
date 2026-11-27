package com.study.platform.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "审计日志")
public record AuditLogVO(
        @Schema(description = "日志 ID")
        Long id,
        @Schema(description = "用户 ID")
        Long userId,
        @Schema(description = "用户名快照")
        String username,
        @Schema(description = "模块")
        String module,
        @Schema(description = "操作")
        String action,
        @Schema(description = "HTTP 方法")
        String httpMethod,
        @Schema(description = "请求 URI")
        String requestUri,
        @Schema(description = "客户端 IP")
        String clientIp,
        @Schema(description = "耗时毫秒")
        Integer costMs,
        @Schema(description = "1 成功，0 异常")
        Integer status,
        @Schema(description = "异常摘要")
        String errorMsg,
        @Schema(description = "操作时间")
        LocalDateTime createdAt
) {
}
