package com.study.platform.common;

/**
 * Redis Key 命名，与接口契约 1.4 节一一对应。
 * 认证态用 String，缓存与计数器后续模块复用同一套前缀。
 */
public final class RedisKeys {

    public static final String AUTH_BLACKLIST_PREFIX = "auth:blacklist:";
    public static final String AUTH_REVOKED_PREFIX = "auth:revoked:";
    public static final String NOTE_DETAIL_PREFIX = "note:detail:";
    public static final String DASHBOARD_OVERVIEW_PREFIX = "dashboard:overview:";
    public static final String AI_RATE_MIN_PREFIX = "ai:rate:min:";
    public static final String AI_QUOTA_DAY_PREFIX = "ai:quota:day:";

    private RedisKeys() {
    }

    /** 主动登出的 JWT 黑名单，TTL 为 Token 剩余有效期。 */
    public static String authBlacklist(String jti) {
        return AUTH_BLACKLIST_PREFIX + jti;
    }

    /** 管理员强制下线时间戳，TTL 24 小时。 */
    public static String authRevoked(Long userId) {
        return AUTH_REVOKED_PREFIX + userId;
    }

    /**
     * 与 {@link #authRevoked(Long)} 相同的键。
     * 禁用账号和强制下线都写这个时间戳，JWT 过滤器按 iat 判断是否失效。
     */
    public static String revokedUser(Long userId) {
        return authRevoked(userId);
    }

    public static String noteDetail(Long noteId) {
        return NOTE_DETAIL_PREFIX + noteId;
    }

    public static String dashboardOverview(Long userId) {
        return DASHBOARD_OVERVIEW_PREFIX + userId;
    }

    public static String aiRateMin(Long userId) {
        return AI_RATE_MIN_PREFIX + userId;
    }

    public static String aiQuotaDay(Long userId, String yyyyMMdd) {
        return AI_QUOTA_DAY_PREFIX + userId + ":" + yyyyMMdd;
    }
}
