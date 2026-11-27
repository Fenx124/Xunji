package com.study.platform.auth.security;

import com.study.platform.common.ErrorCode;
import com.study.platform.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;

/**
 * 从 SecurityContext 读取当前登录用户，供业务层做数据归属校验。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UserPrincipal currentUser() {
        UserPrincipal principal = findCurrentUser();
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或 Token 无效");
        }
        return principal;
    }

    /**
     * 登录、注册等匿名接口没有认证主体，审计切面用这个方法避免把“未登录”抛成业务异常。
     */
    public static UserPrincipal findCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        return null;
    }

    public static Long getUserId() {
        return currentUser().getUserId();
    }

    public static String getUsername() {
        return currentUser().getUsername();
    }

    public static List<String> getRoles() {
        return currentUser().getRoles();
    }

    public static String getJti() {
        return currentUser().getJti();
    }

    /**
     * 当前 Token 剩余秒数，已过期时返回 0。登出黑名单按这个 TTL 写入 Redis。
     */
    public static long getTokenRemainingSeconds() {
        Instant expiresAt = currentUser().getExpiresAt();
        if (expiresAt == null) {
            return 0L;
        }
        long remaining = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(remaining, 0L);
    }
}
