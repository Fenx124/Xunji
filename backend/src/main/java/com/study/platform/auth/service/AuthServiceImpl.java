package com.study.platform.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.study.platform.auth.dto.LoginRequest;
import com.study.platform.auth.dto.RegisterRequest;
import com.study.platform.auth.dto.UpdatePasswordRequest;
import com.study.platform.auth.dto.UpdateProfileRequest;
import com.study.platform.auth.entity.SysRole;
import com.study.platform.auth.entity.SysUser;
import com.study.platform.auth.entity.SysUserRole;
import com.study.platform.auth.mapper.SysRoleMapper;
import com.study.platform.auth.mapper.SysUserMapper;
import com.study.platform.auth.mapper.SysUserRoleMapper;
import com.study.platform.auth.security.JwtService;
import com.study.platform.auth.security.SecurityUtils;
import com.study.platform.auth.security.UserPrincipal;
import com.study.platform.auth.vo.LoginResponse;
import com.study.platform.auth.vo.UserProfileVO;
import com.study.platform.common.ErrorCode;
import com.study.platform.common.RedisKeys;
import com.study.platform.common.exception.BusinessException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final String ROLE_USER = "USER";

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final UserRoleQuery userRoleQuery;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final StringRedisTemplate stringRedisTemplate;

    public AuthServiceImpl(SysUserMapper userMapper,
                           SysRoleMapper roleMapper,
                           SysUserRoleMapper userRoleMapper,
                           UserRoleQuery userRoleQuery,
                           UserDetailsService userDetailsService,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           StringRedisTemplate stringRedisTemplate) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.userRoleQuery = userRoleQuery;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 注册同时写入用户与 USER 角色，任一失败整笔回滚。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileVO register(RegisterRequest request) {
        String username = request.username().trim();
        String nickname = request.nickname() == null ? "" : request.nickname().trim();
        if (nickname.isEmpty()) {
            nickname = username;
        }
        String email = normalizeEmail(request.email());
        assertUsernameAvailable(username);
        if (email != null) {
            assertEmailAvailable(email, null);
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(nickname);
        user.setEmail(email);
        user.setStatus(SysUser.STATUS_ENABLED);
        user.setAiDailyLimit(SysUser.DEFAULT_AI_DAILY_LIMIT);
        user.setDeleted(0);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名或邮箱已存在");
        }
        if (user.getId() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "用户创建失败");
        }

        SysRole userRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, ROLE_USER));
        if (userRole == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "系统未初始化 USER 角色");
        }
        SysUserRole link = new SysUserRole();
        link.setUserId(user.getId());
        link.setRoleId(userRole.getId());
        userRoleMapper.insert(link);
        return toProfile(user, List.of(ROLE_USER));
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String username = request.username().trim();
        UserPrincipal principal;
        try {
            principal = (UserPrincipal) userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException ex) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (!passwordEncoder.matches(request.password(), principal.getPassword())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (!principal.isEnabled()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }
        String accessToken = jwtService.createToken(principal.getUserId(), principal.getUsername(), principal.getRoles());
        UserProfileVO profile = new UserProfileVO(
                principal.getUserId(),
                principal.getUsername(),
                principal.getNickname(),
                principal.getEmail(),
                principal.getAvatar(),
                principal.getRoles(),
                principal.getAiDailyLimit()
        );
        return new LoginResponse(accessToken, "Bearer", jwtService.getExpireSeconds(), profile);
    }

    /**
     * 把当前 jti 写入黑名单，TTL 等于 Token 剩余寿命，过期后键自动删除。
     */
    @Override
    public void logout() {
        String jti = SecurityUtils.getJti();
        long ttl = SecurityUtils.getTokenRemainingSeconds();
        if (jti == null || jti.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或 Token 无效");
        }
        if (ttl <= 0) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(RedisKeys.authBlacklist(jti), "1", Duration.ofSeconds(ttl));
        } catch (DataAccessException ex) {
            log.error("写入 JWT 黑名单失败", ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "注销失败，请稍后重试");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileVO currentUser() {
        SysUser user = requireActiveUser(SecurityUtils.getUserId());
        return toProfile(user, userRoleQuery.listRoleCodes(user.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileVO updateProfile(UpdateProfileRequest request) {
        SysUser user = requireActiveUser(SecurityUtils.getUserId());
        String nickname = request.nickname().trim();
        String email = normalizeEmail(request.email());
        String avatar = blankToNull(request.avatar());
        if (email != null) {
            assertEmailAvailable(email, user.getId());
        }
        try {
            userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                    .eq(SysUser::getId, user.getId())
                    .set(SysUser::getNickname, nickname)
                    .set(SysUser::getEmail, email)
                    .set(SysUser::getAvatar, avatar));
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.CONFLICT, "邮箱已被占用");
        }
        user.setNickname(nickname);
        user.setEmail(email);
        user.setAvatar(avatar);
        return toProfile(user, userRoleQuery.listRoleCodes(user.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(UpdatePasswordRequest request) {
        SysUser user = requireActiveUser(SecurityUtils.getUserId());
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "旧密码不正确");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "新密码不能与旧密码相同");
        }
        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, user.getId())
                .set(SysUser::getPassword, passwordEncoder.encode(request.newPassword())));
    }

    private SysUser requireActiveUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录已失效");
        }
        if (user.getStatus() == null || user.getStatus() != SysUser.STATUS_ENABLED) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }
        return user;
    }

    private void assertUsernameAvailable(String username) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
        }
    }

    private void assertEmailAvailable(String email, Long excludeUserId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email);
        if (excludeUserId != null) {
            wrapper.ne(SysUser::getId, excludeUserId);
        }
        Long count = userMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "邮箱已被占用");
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String trimmed = email.trim();
        try {
            InternetAddress address = new InternetAddress(trimmed);
            address.validate();
        } catch (AddressException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "邮箱格式不正确");
        }
        return trimmed;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private UserProfileVO toProfile(SysUser user, List<String> roles) {
        return new UserProfileVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getAvatar(),
                roles,
                user.getAiDailyLimit()
        );
    }
}
