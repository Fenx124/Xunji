package com.study.platform.auth.service;

import java.util.Collection;
import java.util.List;

/**
 * 给日程提醒用的只读用户联系方式。调用方不注入 SysUserMapper。
 */
public interface UserContactQueryService {

    List<UserContact> findByIds(Collection<Long> userIds);
}
