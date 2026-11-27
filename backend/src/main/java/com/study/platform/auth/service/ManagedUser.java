package com.study.platform.auth.service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理后台看到的用户快照。密码不离开 auth 模块。
 */
public record ManagedUser(
        Long id,
        String username,
        String nickname,
        String email,
        Integer status,
        List<String> roles,
        Integer aiDailyLimit,
        LocalDateTime createdAt
) {
}
