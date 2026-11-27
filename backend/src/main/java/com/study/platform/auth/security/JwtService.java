package com.study.platform.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * HS256 JWT。声明包含 jti、userId、username、roles、iat、exp。
 * jti 供登出时写入 Redis 黑名单，iat 供管理员踢人时与 auth:revoked 时间戳比较。
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey secretKey;
    private final Duration ttl;
    private final Clock clock;

    @Autowired
    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expire-hours:24}") long expireHours) {
        this(secret, Duration.ofHours(expireHours), Clock.systemUTC());
        if (secret.startsWith("change-this-")) {
            log.warn("当前使用开发默认 JWT_SECRET，部署前请通过环境变量 JWT_SECRET 替换为至少 32 字节的随机串");
        }
    }

    /**
     * 供单元测试指定时钟与有效期，避免依赖 Spring 容器。
     */
    public JwtService(String secret, Duration ttl, Clock clock) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException("JWT_SECRET 至少需要 32 个字节（HS256）");
        }
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("JWT 有效期必须大于 0");
        }
        if (clock == null) {
            throw new IllegalArgumentException("时钟不能为空");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttl = ttl;
        this.clock = clock;
    }

    public long getExpireSeconds() {
        return ttl.toSeconds();
    }

    public String createToken(Long userId, String username, List<String> roles) {
        Instant issuedAt = Instant.now(clock);
        Instant expiresAt = issuedAt.plus(ttl);
        String jti = UUID.randomUUID().toString();
        List<String> safeRoles = roles == null ? List.of() : List.copyOf(roles);
        return Jwts.builder()
                .id(jti)
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("username", username)
                .claim("roles", safeRoles)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public JwtUserClaims parse(String token) {
        if (token == null || token.isBlank()) {
            throw new JwtException("Token 为空");
        }
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token.trim())
                .getPayload();
        Instant issuedAt = toInstant(claims.getIssuedAt());
        Instant expiresAt = toInstant(claims.getExpiration());
        if (issuedAt == null || expiresAt == null) {
            throw new JwtException("Token 缺少 iat 或 exp");
        }
        return new JwtUserClaims(
                claims.getId(),
                readUserId(claims),
                readUsername(claims),
                readRoles(claims),
                issuedAt,
                expiresAt
        );
    }

    private static Instant toInstant(Date date) {
        return date == null ? null : date.toInstant();
    }

    private static Long readUserId(Claims claims) {
        Object raw = claims.get("userId");
        if (raw instanceof Number number) {
            return number.longValue();
        }
        if (raw != null) {
            return Long.parseLong(String.valueOf(raw));
        }
        return Long.parseLong(claims.getSubject());
    }

    private static String readUsername(Claims claims) {
        Object raw = claims.get("username");
        if (raw == null || String.valueOf(raw).isBlank()) {
            throw new JwtException("Token 缺少 username");
        }
        return String.valueOf(raw);
    }

    private static List<String> readRoles(Claims claims) {
        Object raw = claims.get("roles");
        if (raw instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        if (raw instanceof String text && !text.isBlank()) {
            return List.of(text);
        }
        return List.of();
    }
}
