package com.study.platform.system.service;

import com.study.platform.common.PageResult;
import com.study.platform.system.vo.AdminAiStatsVO;
import com.study.platform.system.vo.AdminUserVO;
import com.study.platform.system.vo.AuditLogVO;

import java.util.List;

public interface AdminService {

    PageResult<AdminUserVO> pageUsers(String keyword, Integer status, long pageNum, long pageSize);

    AdminUserVO updateStatus(Long userId, int status);

    AdminUserVO updateRoles(Long userId, List<String> roleCodes);

    AdminUserVO updateAiLimit(Long userId, int aiDailyLimit);

    void kick(Long userId);

    AdminAiStatsVO aiStats(Integer days);

    PageResult<AuditLogVO> pageAuditLogs(String module, String username, Integer status, long pageNum, long pageSize);
}
