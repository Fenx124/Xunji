package com.study.platform.auth.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.platform.auth.entity.SysRole;
import com.study.platform.auth.entity.SysUser;
import com.study.platform.auth.entity.SysUserRole;
import com.study.platform.auth.mapper.SysRoleMapper;
import com.study.platform.auth.mapper.SysUserMapper;
import com.study.platform.auth.mapper.SysUserRoleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 启动时补齐 USER / ADMIN 角色，并在管理员不存在时按环境变量创建。
 * 数据库不可用只记警告，不中断应用启动。
 */
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;
    private final String adminUsername;
    private final String adminPassword;

    public AdminInitializer(SysUserMapper userMapper,
                            SysRoleMapper roleMapper,
                            SysUserRoleMapper userRoleMapper,
                            PasswordEncoder passwordEncoder,
                            PlatformTransactionManager transactionManager,
                            @Value("${app.init-admin.username:admin}") String adminUsername,
                            @Value("${app.init-admin.password:}") String adminPassword) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            transactionTemplate.executeWithoutResult(status -> bootstrap());
        } catch (Exception ex) {
            log.warn("数据库不可用，已跳过默认角色与管理员初始化：{}", ex.getMessage());
            log.debug("管理员初始化异常详情", ex);
        }
    }

    private void bootstrap() {
        SysRole userRole = ensureRole("USER", "普通学习者", "拥有个人笔记、日程、番茄钟、AI助手与看板权限");
        SysRole adminRole = ensureRole("ADMIN", "系统管理员", "拥有用户管理、角色分配、AI限流监控与审计日志权限");
        ensureAdmin(userRole, adminRole);
    }

    private SysRole ensureRole(String code, String name, String description) {
        SysRole existing = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, code));
        if (existing != null) {
            return existing;
        }
        SysRole created = new SysRole();
        created.setRoleCode(code);
        created.setRoleName(name);
        created.setDescription(description);
        try {
            roleMapper.insert(created);
            return created;
        } catch (DataIntegrityViolationException ex) {
            SysRole concurrent = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, code));
            if (concurrent != null) {
                return concurrent;
            }
            throw ex;
        }
    }

    private void ensureAdmin(SysRole userRole, SysRole adminRole) {
        if (adminUsername == null || adminUsername.isBlank()) {
            log.warn("未配置 INIT_ADMIN_USERNAME，跳过默认管理员创建");
            return;
        }
        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("未配置 INIT_ADMIN_PASSWORD，跳过默认管理员创建");
            return;
        }
        String username = adminUsername.trim();
        SysUser existing = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (existing != null) {
            log.info("默认管理员账号 [{}] 已存在，跳过创建", username);
            return;
        }
        SysUser admin = new SysUser();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setNickname("系统管理员");
        admin.setStatus(SysUser.STATUS_ENABLED);
        admin.setAiDailyLimit(SysUser.DEFAULT_AI_DAILY_LIMIT);
        admin.setDeleted(0);
        try {
            userMapper.insert(admin);
        } catch (DataIntegrityViolationException ex) {
            log.warn("默认管理员用户名 [{}] 已被占用（可能包含逻辑删除记录），跳过创建", username);
            return;
        }
        // 同时授予 USER，便于演示账号使用笔记、日程等普通功能
        bindRole(admin.getId(), userRole.getId());
        bindRole(admin.getId(), adminRole.getId());
        log.warn("已创建默认管理员账号 [{}]，请在首次登录后立即修改初始密码", username);
    }

    private void bindRole(Long userId, Long roleId) {
        if (userId == null || roleId == null) {
            return;
        }
        Long count = userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getRoleId, roleId));
        if (count != null && count > 0) {
            return;
        }
        SysUserRole link = new SysUserRole();
        link.setUserId(userId);
        link.setRoleId(roleId);
        userRoleMapper.insert(link);
    }
}
