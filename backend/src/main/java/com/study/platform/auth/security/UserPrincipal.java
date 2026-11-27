package com.study.platform.auth.security;

import com.study.platform.auth.entity.SysUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * 认证主体。登录阶段携带数据库资料，过滤器阶段只携带 Token 声明。
 * 角色编码存为 USER / ADMIN，授权时再补上 ROLE_ 前缀。
 */
public class UserPrincipal implements UserDetails {

    private final Long userId;
    private final String username;
    private final String password;
    private final String nickname;
    private final String email;
    private final String avatar;
    private final Integer status;
    private final Integer aiDailyLimit;
    private final List<String> roles;
    private final String jti;
    private final Instant issuedAt;
    private final Instant expiresAt;
    private final Collection<? extends GrantedAuthority> authorities;

    private UserPrincipal(Long userId,
                          String username,
                          String password,
                          String nickname,
                          String email,
                          String avatar,
                          Integer status,
                          Integer aiDailyLimit,
                          List<String> roles,
                          String jti,
                          Instant issuedAt,
                          Instant expiresAt) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.email = email;
        this.avatar = avatar;
        this.status = status;
        this.aiDailyLimit = aiDailyLimit;
        this.roles = roles == null ? List.of() : List.copyOf(roles);
        this.jti = jti;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.authorities = this.roles.stream()
                .filter(role -> role != null && !role.isBlank())
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    public static UserPrincipal fromAccount(SysUser user, List<String> roles) {
        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getNickname(),
                user.getEmail(),
                user.getAvatar(),
                user.getStatus(),
                user.getAiDailyLimit(),
                roles,
                null,
                null,
                null
        );
    }

    public static UserPrincipal fromToken(JwtUserClaims claims) {
        return new UserPrincipal(
                claims.userId(),
                claims.username(),
                null,
                null,
                null,
                null,
                SysUser.STATUS_ENABLED,
                null,
                claims.roles(),
                claims.jti(),
                claims.issuedAt(),
                claims.expiresAt()
        );
    }

    public Long getUserId() {
        return userId;
    }

    public String getNickname() {
        return nickname;
    }

    public String getEmail() {
        return email;
    }

    public String getAvatar() {
        return avatar;
    }

    public Integer getStatus() {
        return status;
    }

    public Integer getAiDailyLimit() {
        return aiDailyLimit;
    }

    public List<String> getRoles() {
        return roles;
    }

    public String getJti() {
        return jti;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password == null ? "" : password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status != null && status == SysUser.STATUS_ENABLED;
    }
}
