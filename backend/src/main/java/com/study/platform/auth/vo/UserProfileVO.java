package com.study.platform.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "当前用户资料")
public record UserProfileVO(
        @Schema(description = "用户 ID", example = "1")
        Long id,
        @Schema(description = "用户名", example = "student01")
        String username,
        @Schema(description = "昵称", example = "小明")
        String nickname,
        @Schema(description = "邮箱")
        String email,
        @Schema(description = "头像 URL")
        String avatar,
        @Schema(description = "角色编码列表", example = "[\"USER\"]")
        List<String> roles,
        @Schema(description = "AI 每日配额", example = "50")
        Integer aiDailyLimit
) {
}
