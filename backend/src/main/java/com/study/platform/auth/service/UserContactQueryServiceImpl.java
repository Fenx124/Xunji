package com.study.platform.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.platform.auth.entity.SysUser;
import com.study.platform.auth.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class UserContactQueryServiceImpl implements UserContactQueryService {

    private final SysUserMapper userMapper;

    public UserContactQueryServiceImpl(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserContact> findByIds(Collection<Long> userIds) {
        List<Long> ids = distinctIds(userIds);
        if (ids.isEmpty()) {
            return List.of();
        }
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .select(SysUser::getId, SysUser::getNickname, SysUser::getEmail)
                .in(SysUser::getId, ids));
        List<UserContact> contacts = new ArrayList<>(users.size());
        for (SysUser user : users) {
            contacts.add(new UserContact(user.getId(), user.getNickname(), blankToNull(user.getEmail())));
        }
        return contacts;
    }

    static List<Long> distinctIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return userIds.stream().filter(id -> id != null && id > 0).distinct().toList();
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
