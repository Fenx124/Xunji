package com.study.platform.auth.security;

import com.study.platform.common.ErrorCode;
import com.study.platform.common.RedisKeys;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

/**
 * 解析 Authorization: Bearer，校验签名，并对照 Redis 黑名单与管理员踢人时间戳。
 * 无效或已撤销时直接返回 401 JSON，不把异常抛进业务层。
 * Redis 不可用时拒绝放行，避免黑名单失效后旧 Token 继续可用。
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    /** 大于该值视为毫秒时间戳，否则视为秒。 */
    private static final long MILLIS_THRESHOLD = 10_000_000_000L;

    private final JwtService jwtService;
    private final StringRedisTemplate stringRedisTemplate;
    private final RestAuthErrorWriter errorWriter;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   StringRedisTemplate stringRedisTemplate,
                                   RestAuthErrorWriter errorWriter) {
        this.jwtService = jwtService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            errorWriter.write(response, ErrorCode.UNAUTHORIZED, "未登录或 Token 无效");
            return;
        }
        try {
            JwtUserClaims claims = jwtService.parse(token);
            if (isBlacklisted(claims.jti()) || isRevoked(claims.userId(), claims.issuedAt())) {
                SecurityContextHolder.clearContext();
                errorWriter.write(response, ErrorCode.UNAUTHORIZED, "登录已失效，请重新登录");
                return;
            }
            UserPrincipal principal = UserPrincipal.fromToken(claims);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (DataAccessException ex) {
            SecurityContextHolder.clearContext();
            log.warn("Redis 认证状态校验失败: {}", ex.getMessage());
            errorWriter.write(response, ErrorCode.UNAUTHORIZED, "认证状态校验失败，请稍后重试");
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
            log.debug("JWT 校验失败: {}", ex.getMessage());
            errorWriter.write(response, ErrorCode.UNAUTHORIZED, "未登录或 Token 无效");
        }
    }

    private boolean isBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) {
            return true;
        }
        return stringRedisTemplate.opsForValue().get(RedisKeys.authBlacklist(jti)) != null;
    }

    /**
     * auth:revoked:{userId} 存的是踢人时间戳。
     * iat 早于或等于该时间的 Token 失效；用户重新登录后签发的新 Token 仍然有效。
     */
    private boolean isRevoked(Long userId, Instant issuedAt) {
        if (userId == null || issuedAt == null) {
            return true;
        }
        String raw = stringRedisTemplate.opsForValue().get(RedisKeys.authRevoked(userId));
        if (raw == null || raw.isBlank()) {
            return false;
        }
        try {
            long value = Long.parseLong(raw.trim());
            Instant revokedAt = value > MILLIS_THRESHOLD
                    ? Instant.ofEpochMilli(value)
                    : Instant.ofEpochSecond(value);
            return !issuedAt.isAfter(revokedAt);
        } catch (NumberFormatException ex) {
            log.warn("auth:revoked 值无法解析，按已撤销处理: {}", raw);
            return true;
        }
    }
}
