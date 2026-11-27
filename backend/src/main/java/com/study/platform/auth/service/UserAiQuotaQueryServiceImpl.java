package com.study.platform.auth.service;

import com.study.platform.auth.entity.SysUser;
import com.study.platform.auth.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAiQuotaQueryServiceImpl implements UserAiQuotaQueryService {

    private final SysUserMapper userMapper;

    public UserAiQuotaQueryServiceImpl(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public int currentDailyLimit(Long userId) {
        if (userId == null) {
            return 0;
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            return 0;
        }
        if (user.getAiDailyLimit() == null) {
            return SysUser.DEFAULT_AI_DAILY_LIMIT;
        }
        return user.getAiDailyLimit();
    }
}
