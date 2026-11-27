package com.study.platform.auth.security;

import java.time.Instant;
import java.util.List;

/**
 * 已通过签名校验的 JWT 声明：jti、userId、username、roles、iat、exp。
 */
public record JwtUserClaims(
        String jti,
        Long userId,
        String username,
        List<String> roles,
        Instant issuedAt,
        Instant expiresAt
) {
}
