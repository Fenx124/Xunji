package com.study.platform.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "登录成功响应")
public record LoginResponse(
        @Schema(description = "访问令牌")
        String accessToken,
        @Schema(description = "令牌类型", example = "Bearer")
        String tokenType,
        @Schema(description = "有效期（秒）", example = "86400")
        long expiresIn,
        @Schema(description = "当前用户")
        UserProfileVO user
) {
}
