package com.study.platform.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "用户登录请求")
public record LoginRequest(
        @Schema(description = "登录用户名", example = "student01")
        @NotBlank(message = "用户名不能为空")
        String username,

        @Schema(description = "登录密码", example = "Passw0rd")
        @NotBlank(message = "密码不能为空")
        String password
) {
}
