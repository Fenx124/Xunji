package com.study.platform.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "用户注册请求")
public record RegisterRequest(
        @Schema(description = "登录用户名", example = "student01")
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 50, message = "用户名长度需在 3 到 50 之间")
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
        String username,

        @Schema(description = "登录密码", example = "Passw0rd")
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度需在 6 到 64 之间")
        String password,

        @Schema(description = "昵称，缺省时使用用户名", example = "小明")
        @Size(max = 60, message = "昵称长度不能超过 60")
        String nickname,

        @Schema(description = "邮箱，可空", example = "student@example.com")
        @Email(message = "邮箱格式不正确")
        @Size(max = 120, message = "邮箱长度不能超过 120")
        String email
) {
}
