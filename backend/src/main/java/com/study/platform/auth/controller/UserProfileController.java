package com.study.platform.auth.controller;

import com.study.platform.auth.dto.UpdatePasswordRequest;
import com.study.platform.auth.dto.UpdateProfileRequest;
import com.study.platform.auth.service.AuthService;
import com.study.platform.auth.vo.UserProfileVO;
import com.study.platform.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "个人中心")
@RestController
@RequestMapping("/api/v1/user")
public class UserProfileController {

    private final AuthService authService;

    public UserProfileController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "更新昵称、邮箱、头像")
    @PutMapping(value = "/profile", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<UserProfileVO> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return Result.ok("更新成功", authService.updateProfile(request));
    }

    @Operation(summary = "修改密码")
    @PutMapping(value = "/password", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Void> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        authService.updatePassword(request);
        return Result.ok("密码修改成功", null);
    }
}
