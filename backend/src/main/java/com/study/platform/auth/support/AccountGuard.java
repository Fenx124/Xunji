package com.study.platform.auth.support;

import com.study.platform.auth.entity.SysUser;
import com.study.platform.common.ErrorCode;
import com.study.platform.common.exception.BusinessException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 管理员治理的参数与自我保护规则。
 * 放在 auth，是为了让用户写服务和后台服务共用同一套校验，同时不让 auth 反过来依赖 system。
 */
public final class AccountGuard {

    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final Set<String> ROLES = Set.of(ROLE_USER, ROLE_ADMIN);
    public static final int AI_LIMIT_MAX = 10000;

    private AccountGuard() {
    }

    public static void requireStatus(int status) {
        if (status != SysUser.STATUS_DISABLED && status != SysUser.STATUS_ENABLED) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "账号状态只能是 0 或 1");
        }
    }

    public static void rejectSelfDisable(Long operatorId, Long targetId, int status) {
        requireStatus(status);
        if (status == SysUser.STATUS_DISABLED && operatorId != null && operatorId.equals(targetId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能禁用当前登录账号");
        }
    }

    public static void rejectSelfKick(Long operatorId, Long targetId) {
        if (operatorId != null && operatorId.equals(targetId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能强制下线当前登录账号");
        }
    }

    public static int requireAiDailyLimit(Integer limit) {
        if (limit == null || limit < 0 || limit > AI_LIMIT_MAX) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "AI 每日配额需在 0 到 10000 之间");
        }
        return limit;
    }

    /**
     * 去重并转成大写。空列表、空白项、USER/ADMIN 以外的编码都返回 40001。
     */
    public static List<String> normalizeRoles(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "角色不能为空");
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String code : roleCodes) {
            if (code == null || code.isBlank()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "角色编码不能为空");
            }
            String normalized = code.trim().toUpperCase(Locale.ROOT);
            if (!ROLES.contains(normalized)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "角色只能是 USER 或 ADMIN");
            }
            unique.add(normalized);
        }
        if (unique.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "角色不能为空");
        }
        return List.copyOf(unique);
    }

    public static void rejectSelfAdminRemoval(Long operatorId, Long targetId, List<String> normalizedRoles) {
        if (operatorId != null && operatorId.equals(targetId) && (normalizedRoles == null || !normalizedRoles.contains(ROLE_ADMIN))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能移除自己的 ADMIN 角色");
        }
    }
}
