package com.study.platform.system.controller;

import com.study.platform.common.PageResult;
import com.study.platform.common.Result;
import com.study.platform.common.audit.AuditLog;
import com.study.platform.system.dto.AiLimitRequest;
import com.study.platform.system.dto.UserRolesRequest;
import com.study.platform.system.dto.UserStatusRequest;
import com.study.platform.system.service.AdminService;
import com.study.platform.system.vo.AdminAiStatsVO;
import com.study.platform.system.vo.AdminUserVO;
import com.study.platform.system.vo.AuditLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理后台")
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @Operation(summary = "分页查询用户")
    @GetMapping(value = "/users", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<PageResult<AdminUserVO>> users(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize) {
        return Result.ok(adminService.pageUsers(keyword, status, pageNum, pageSize));
    }

    @Operation(summary = "启用或禁用用户")
    @AuditLog(module = "ADMIN", action = "更新用户状态")
    @PatchMapping(value = "/users/{id:\\d+}/status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<AdminUserVO> updateStatus(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        return Result.ok(adminService.updateStatus(id, request.status()));
    }

    @Operation(summary = "分配角色")
    @AuditLog(module = "ADMIN", action = "分配用户角色")
    @PutMapping(value = "/users/{id:\\d+}/roles", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<AdminUserVO> updateRoles(@PathVariable Long id, @Valid @RequestBody UserRolesRequest request) {
        return Result.ok(adminService.updateRoles(id, request.roleCodes()));
    }

    @Operation(summary = "调整 AI 每日配额")
    @AuditLog(module = "ADMIN", action = "调整AI每日配额")
    @PutMapping(value = "/users/{id:\\d+}/ai-limit", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<AdminUserVO> updateAiLimit(@PathVariable Long id, @Valid @RequestBody AiLimitRequest request) {
        return Result.ok(adminService.updateAiLimit(id, request.aiDailyLimit()));
    }

    @Operation(summary = "强制下线")
    @AuditLog(module = "ADMIN", action = "强制下线")
    @PostMapping(value = "/users/{id:\\d+}/kick", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Void> kick(@PathVariable Long id) {
        adminService.kick(id);
        return Result.ok();
    }

    @Operation(summary = "AI 用量统计")
    @GetMapping(value = "/ai-stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<AdminAiStatsVO> aiStats(@RequestParam(defaultValue = "7") Integer days) {
        return Result.ok(adminService.aiStats(days));
    }

    @Operation(summary = "审计日志")
    @GetMapping(value = "/audit-logs", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<PageResult<AuditLogVO>> auditLogs(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize) {
        return Result.ok(adminService.pageAuditLogs(module, username, status, pageNum, pageSize));
    }
}
