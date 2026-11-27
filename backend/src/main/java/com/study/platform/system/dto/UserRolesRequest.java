package com.study.platform.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "分配角色")
public record UserRolesRequest(
        @Schema(description = "角色编码，USER / ADMIN", example = "[\"USER\",\"ADMIN\"]")
        @NotEmpty(message = "角色不能为空")
        List<String> roleCodes
) {
}
