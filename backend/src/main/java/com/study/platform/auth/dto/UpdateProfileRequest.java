package com.study.platform.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "更新个人资料。邮箱或头像留空表示解绑")
public record UpdateProfileRequest(
        @Schema(description = "昵称", example = "小明")
        @NotBlank(message = "昵称不能为空")
        @Size(max = 60, message = "昵称长度不能超过 60")
        String nickname,

        @Schema(description = "邮箱，留空则解绑", example = "student@example.com")
        @Size(max = 120, message = "邮箱长度不能超过 120")
        String email,

        @Schema(description = "头像 URL，留空则清空", example = "https://example.com/avatar.png")
        @Size(max = 255, message = "头像地址长度不能超过 255")
        String avatar
) {
}
