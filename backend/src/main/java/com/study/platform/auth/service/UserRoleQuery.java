package com.study.platform.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.platform.auth.entity.SysRole;
import com.study.platform.auth.entity.SysUserRole;
import com.study.platform.auth.mapper.SysRoleMapper;
import com.study.platform.auth.mapper.SysUserRoleMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 通过用户-角色关联表读取角色编码，避免在业务方法里重复拼查询。
 */
@Component
public class UserRoleQuery {

    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;

    public UserRoleQuery(SysUserRoleMapper userRoleMapper, SysRoleMapper roleMapper) {
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
    }

    public List<String> listRoleCodes(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return listRoleCodes(List.of(userId)).getOrDefault(userId, List.of());
    }

    public Map<Long, List<String>> listRoleCodes(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<SysUserRole> links = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .in(SysUserRole::getUserId, ids));
        if (links.isEmpty()) {
            return Map.of();
        }
        List<Long> roleIds = links.stream().map(SysUserRole::getRoleId).filter(Objects::nonNull).distinct().toList();
        if (roleIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> codeByRoleId = new HashMap<>();
        for (SysRole role : roleMapper.selectList(new LambdaQueryWrapper<SysRole>().in(SysRole::getId, roleIds))) {
            if (role.getId() != null && role.getRoleCode() != null) {
                codeByRoleId.put(role.getId(), role.getRoleCode());
            }
        }
        Map<Long, List<String>> grouped = new HashMap<>();
        for (SysUserRole link : links) {
            String code = codeByRoleId.get(link.getRoleId());
            if (code == null) {
                continue;
            }
            grouped.computeIfAbsent(link.getUserId(), key -> new ArrayList<>()).add(code);
        }
        Map<Long, List<String>> result = new HashMap<>();
        for (Map.Entry<Long, List<String>> entry : grouped.entrySet()) {
            List<String> codes = entry.getValue().stream().distinct().sorted(Comparator.naturalOrder()).toList();
            result.put(entry.getKey(), codes);
        }
        return result;
    }
}
