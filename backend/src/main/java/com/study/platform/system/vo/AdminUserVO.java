package com.study.platform.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理员看到的用户")
public record AdminUserVO(
        @Schema(description = "用户 ID")
        Long id,
        @Schema(description = "用户名")
        String username,
        @Schema(description = "昵称")
        String nickname,
        @Schema(description = "邮箱")
        String email,
        @Schema(description = "1 正常，0 禁用")
        Integer status,
        @Schema(description = "角色编码")
        List<String> roles,
        @Schema(description = "AI 每日配额")
        Integer aiDailyLimit,
        @Schema(description = "创建时间")
        LocalDateTime createdAt
) {
}
