package com.study.platform.auth.service;

/**
 * 读取用户当前的 AI 日配额。AI 模块依赖这个接口，不注入 SysUserMapper。
 */
public interface UserAiQuotaQueryService {

    /**
     * 返回 sys_user.ai_daily_limit。用户不存在时返回 0，调用方会拒绝本次请求。
     */
    int currentDailyLimit(Long userId);
}
