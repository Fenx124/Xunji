package com.study.platform.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.study.platform.auth.entity.SysRole;
import com.study.platform.auth.entity.SysUser;
import com.study.platform.auth.entity.SysUserRole;
import com.study.platform.auth.mapper.SysRoleMapper;
import com.study.platform.auth.mapper.SysUserMapper;
import com.study.platform.auth.mapper.SysUserRoleMapper;
import com.study.platform.auth.support.AccountGuard;
import com.study.platform.common.ErrorCode;
import com.study.platform.common.PageQuery;
import com.study.platform.common.PageResult;
import com.study.platform.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class UserAdminServiceImpl implements UserAdminService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final UserRoleQuery userRoleQuery;

    public UserAdminServiceImpl(SysUserMapper userMapper,
                                SysRoleMapper roleMapper,
                                SysUserRoleMapper userRoleMapper,
                                UserRoleQuery userRoleQuery) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.userRoleQuery = userRoleQuery;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ManagedUser> page(String keyword, Integer status, long pageNum, long pageSize) {
        if (status != null) {
            AccountGuard.requireStatus(status);
        }
        long current = PageQuery.normalizePageNum(pageNum);
        long size = PageQuery.normalizePageSize(pageSize);
        String text = keyword == null ? null : keyword.trim();
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .select(SysUser::getId, SysUser::getUsername, SysUser::getNickname, SysUser::getEmail,
                        SysUser::getStatus, SysUser::getAiDailyLimit, SysUser::getCreatedAt)
                .eq(status != null, SysUser::getStatus, status)
                .and(text != null && !text.isEmpty(), query -> query
                        .like(SysUser::getUsername, escapeLike(text))
                        .or().like(SysUser::getNickname, escapeLike(text))
                        .or().like(SysUser::getEmail, escapeLike(text)))
                .orderByDesc(SysUser::getCreatedAt)
                .orderByDesc(SysUser::getId);
        Page<SysUser> page = userMapper.selectPage(new Page<>(current, size), wrapper);
        List<SysUser> users = page.getRecords() == null ? List.of() : page.getRecords();
        return new PageResult<>(toManaged(users), page.getTotal(), current, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ManagedUser updateStatus(Long operatorId, Long userId, int status) {
        AccountGuard.rejectSelfDisable(operatorId, userId, status);
        SysUser user = requireUser(userId);
        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, user.getId())
                .set(SysUser::getStatus, status));
        user.setStatus(status);
        return toManaged(user, userRoleQuery.listRoleCodes(user.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ManagedUser replaceRoles(Long operatorId, Long userId, List<String> roleCodes) {
        List<String> normalized = AccountGuard.normalizeRoles(roleCodes);
        AccountGuard.rejectSelfAdminRemoval(operatorId, userId, normalized);
        SysUser user = requireUser(userId);
        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .in(SysRole::getRoleCode, normalized));
        if (roles.size() != normalized.size()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "系统角色未初始化完整");
        }
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getId()));
        for (SysRole role : roles) {
            SysUserRole link = new SysUserRole();
            link.setUserId(user.getId());
            link.setRoleId(role.getId());
            userRoleMapper.insert(link);
        }
        return toManaged(user, userRoleQuery.listRoleCodes(user.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ManagedUser updateAiLimit(Long userId, int aiDailyLimit) {
        int limit = AccountGuard.requireAiDailyLimit(aiDailyLimit);
        SysUser user = requireUser(userId);
        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, user.getId())
                .set(SysUser::getAiDailyLimit, limit));
        user.setAiDailyLimit(limit);
        return toManaged(user, userRoleQuery.listRoleCodes(user.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public ManagedUser require(Long userId) {
        return toManaged(requireUser(userId), userRoleQuery.listRoleCodes(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManagedUser> findByIds(Collection<Long> userIds) {
        List<Long> ids = UserContactQueryServiceImpl.distinctIds(userIds);
        if (ids.isEmpty()) {
            return List.of();
        }
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .select(SysUser::getId, SysUser::getUsername, SysUser::getNickname, SysUser::getEmail,
                        SysUser::getStatus, SysUser::getAiDailyLimit, SysUser::getCreatedAt)
                .in(SysUser::getId, ids));
        return toManaged(users);
    }

    private SysUser requireUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户不存在");
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private List<ManagedUser> toManaged(List<SysUser> users) {
        if (users == null || users.isEmpty()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>(users.size());
        for (SysUser user : users) {
            ids.add(user.getId());
        }
        Map<Long, List<String>> roles = userRoleQuery.listRoleCodes(ids);
        List<ManagedUser> result = new ArrayList<>(users.size());
        for (SysUser user : users) {
            result.add(toManaged(user, roles.getOrDefault(user.getId(), List.of())));
        }
        return result;
    }

    private static ManagedUser toManaged(SysUser user, List<String> roles) {
        return new ManagedUser(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getStatus(),
                roles == null ? List.of() : roles,
                user.getAiDailyLimit() == null ? SysUser.DEFAULT_AI_DAILY_LIMIT : user.getAiDailyLimit(),
                user.getCreatedAt()
        );
    }

    /**
     * LIKE 通配符按字面量匹配，避免管理员检索 % 时扫全表。
     */
    static String escapeLike(String keyword) {
        return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
