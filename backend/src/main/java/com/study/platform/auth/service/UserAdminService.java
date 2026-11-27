package com.study.platform.auth.service;

import com.study.platform.common.PageResult;

import java.util.Collection;
import java.util.List;

/**
 * 管理员对用户资料的读写。system 模块通过这个接口访问用户表，不注入用户 Mapper。
 */
public interface UserAdminService {

    PageResult<ManagedUser> page(String keyword, Integer status, long pageNum, long pageSize);

    ManagedUser updateStatus(Long operatorId, Long userId, int status);

    ManagedUser replaceRoles(Long operatorId, Long userId, List<String> roleCodes);

    ManagedUser updateAiLimit(Long userId, int aiDailyLimit);

    ManagedUser require(Long userId);

    List<ManagedUser> findByIds(Collection<Long> userIds);
}
