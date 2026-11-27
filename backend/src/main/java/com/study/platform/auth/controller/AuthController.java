package com.study.platform.auth.controller;

import com.study.platform.auth.dto.LoginRequest;
import com.study.platform.auth.dto.RegisterRequest;
import com.study.platform.auth.service.AuthService;
import com.study.platform.auth.vo.LoginResponse;
import com.study.platform.auth.vo.UserProfileVO;
import com.study.platform.common.Result;
import com.study.platform.common.audit.AuditLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "用户注册")
    @SecurityRequirements
    @AuditLog(module = "AUTH", action = "注册")
    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<UserProfileVO> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok("注册成功", authService.register(request));
    }

    @Operation(summary = "用户登录")
    @SecurityRequirements
    @AuditLog(module = "AUTH", action = "登录")
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok("登录成功", authService.login(request));
    }

    @Operation(summary = "退出登录")
    @AuditLog(module = "AUTH", action = "退出登录")
    @PostMapping(value = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Void> logout() {
        authService.logout();
        return Result.ok("注销成功", null);
    }

    @Operation(summary = "当前登录用户")
    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<UserProfileVO> me() {
        return Result.ok(authService.currentUser());
    }
}
