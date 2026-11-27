package com.study.platform.auth.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.platform.auth.entity.SysUser;
import com.study.platform.auth.mapper.SysUserMapper;
import com.study.platform.auth.service.UserRoleQuery;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 从数据库加载用户，供登录校验使用，同时避免 Spring Security 生成随机内存账号。
 * 用户不存在与密码错误由 AuthService 统一返回模糊提示。
 */
@Service
public class AuthUserDetailsService implements UserDetailsService {

    private final SysUserMapper userMapper;
    private final UserRoleQuery userRoleQuery;

    public AuthUserDetailsService(SysUserMapper userMapper, UserRoleQuery userRoleQuery) {
        this.userMapper = userMapper;
        this.userRoleQuery = userRoleQuery;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("用户名为空");
        }
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username.trim()));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        return UserPrincipal.fromAccount(user, userRoleQuery.listRoleCodes(user.getId()));
    }
}
