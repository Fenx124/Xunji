package com.study.platform.common.audit;

import com.study.platform.auth.dto.LoginRequest;
import com.study.platform.auth.dto.RegisterRequest;
import com.study.platform.auth.vo.LoginResponse;
import com.study.platform.auth.vo.UserProfileVO;
import com.study.platform.common.Result;

/**
 * 审计字段的纯函数：IP、截断、登录/注册时从入参或返回值补用户身份。
 */
public final class AuditSupport {

    public static final int ERROR_MAX = 500;
    public static final int USERNAME_MAX = 50;
    public static final int URI_MAX = 255;
    public static final int IP_MAX = 64;

    private AuditSupport() {
    }

    public record Identity(Long userId, String username) {
    }

    /**
     * X-Forwarded-For 取第一段，空则退回 remoteAddr。结果截断到列宽。
     */
    public static String clientIp(String forwardedFor, String remoteAddr) {
        String candidate = firstForwarded(forwardedFor);
        if (candidate == null) {
            candidate = remoteAddr;
        }
        return truncate(candidate, IP_MAX);
    }

    public static String firstForwarded(String forwardedFor) {
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return null;
        }
        String first = forwardedFor.split(",")[0].trim();
        return first.isEmpty() ? null : first;
    }

    public static String truncate(String value, int max) {
        if (value == null || max < 1) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, max);
    }

    public static String usernameFromArgs(Object[] args) {
        if (args == null) {
            return null;
        }
        for (Object arg : args) {
            if (arg instanceof LoginRequest login) {
                return login.username();
            }
            if (arg instanceof RegisterRequest register) {
                return register.username();
            }
        }
        return null;
    }

    public static Identity identityFromResult(Object result) {
        Object data = result;
        if (result instanceof Result<?> wrapped) {
            data = wrapped.getData();
        }
        if (data instanceof LoginResponse login && login.user() != null) {
            return new Identity(login.user().id(), login.user().username());
        }
        if (data instanceof UserProfileVO profile) {
            return new Identity(profile.id(), profile.username());
        }
        return null;
    }
}
