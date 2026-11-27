package com.study.platform.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.study.platform.auth.security.SecurityUtils;
import com.study.platform.auth.service.ManagedUser;
import com.study.platform.auth.service.UserAdminService;
import com.study.platform.auth.support.AccountGuard;
import com.study.platform.common.ErrorCode;
import com.study.platform.common.PageQuery;
import com.study.platform.common.PageResult;
import com.study.platform.common.RedisKeys;
import com.study.platform.common.StudyDates;
import com.study.platform.common.exception.BusinessException;
import com.study.platform.system.entity.SysAuditLog;
import com.study.platform.system.mapper.AiDailyUsageRow;
import com.study.platform.system.mapper.AiFeatureUsageRow;
import com.study.platform.system.mapper.AiUsageMapper;
import com.study.platform.system.mapper.AiUsageTotalRow;
import com.study.platform.system.mapper.AiUserUsageRow;
import com.study.platform.system.mapper.SysAuditLogMapper;
import com.study.platform.system.support.AiStatsMath;
import com.study.platform.system.vo.AdminAiStatsVO;
import com.study.platform.system.vo.AdminUserVO;
import com.study.platform.system.vo.AuditLogVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminServiceImpl implements AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);
    private static final Duration REVOKED_TTL = Duration.ofHours(24);

    private final UserAdminService userAdminService;
    private final AiUsageMapper aiUsageMapper;
    private final SysAuditLogMapper auditLogMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public AdminServiceImpl(UserAdminService userAdminService,
                            AiUsageMapper aiUsageMapper,
                            SysAuditLogMapper auditLogMapper,
                            StringRedisTemplate stringRedisTemplate) {
        this.userAdminService = userAdminService;
        this.aiUsageMapper = aiUsageMapper;
        this.auditLogMapper = auditLogMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminUserVO> pageUsers(String keyword, Integer status, long pageNum, long pageSize) {
        PageResult<ManagedUser> page = userAdminService.page(keyword, status, pageNum, pageSize);
        return new PageResult<>(toUserVos(page.getRecords()), page.getTotal(), page.getPageNum(), page.getPageSize());
    }

    @Override
    public AdminUserVO updateStatus(Long userId, int status) {
        ManagedUser updated = userAdminService.updateStatus(SecurityUtils.getUserId(), userId, status);
        if (status == 0) {
            // 禁用后立刻写入撤销时间戳，已签发的 JWT 在下一次请求就被过滤器拒绝。
            revokeNow(userId);
        }
        return toUserVo(updated);
    }

    @Override
    public AdminUserVO updateRoles(Long userId, List<String> roleCodes) {
        return toUserVo(userAdminService.replaceRoles(SecurityUtils.getUserId(), userId, roleCodes));
    }

    @Override
    public AdminUserVO updateAiLimit(Long userId, int aiDailyLimit) {
        return toUserVo(userAdminService.updateAiLimit(userId, aiDailyLimit));
    }

    @Override
    public void kick(Long userId) {
        AccountGuard.rejectSelfKick(SecurityUtils.getUserId(), userId);
        userAdminService.require(userId);
        revokeNow(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminAiStatsVO aiStats(Integer days) {
        int window = AiStatsMath.clampDays(days);
        LocalDate today = StudyDates.today();
        LocalDate startDay = today.minusDays(window - 1L);
        LocalDateTime start = StudyDates.startOf(startDay);
        LocalDateTime end = StudyDates.nextDay(today);
        LocalDateTime todayStart = StudyDates.startOf(today);

        AiUsageTotalRow total = aiUsageMapper.summarize(start, end);
        long totalCalls = total == null || total.getTotalCalls() == null ? 0L : total.getTotalCalls();
        long successCalls = total == null || total.getSuccessCalls() == null ? 0L : total.getSuccessCalls();
        long totalLatency = total == null || total.getTotalLatencyMs() == null ? 0L : total.getTotalLatencyMs();
        long totalTokens = total == null || total.getTotalTokens() == null ? 0L : total.getTotalTokens();

        List<AiFeatureUsageRow> featureRows = aiUsageMapper.byFeature(start, end);
        List<AdminAiStatsVO.FeatureStat> features = new ArrayList<>();
        if (featureRows != null) {
            for (AiFeatureUsageRow row : featureRows) {
                features.add(new AdminAiStatsVO.FeatureStat(
                        row.getFeatureType(),
                        row.getCalls() == null ? 0L : row.getCalls(),
                        row.getAvgLatencyMs() == null ? 0L : row.getAvgLatencyMs()
                ));
            }
        }

        List<AiUserUsageRow> userRows = aiUsageMapper.byUser(start, end, todayStart, end);
        List<AdminAiStatsVO.UserStat> users = toUserStats(userRows);

        Map<String, AiDailyUsageRow> byDay = new HashMap<>();
        List<AiDailyUsageRow> dailyRows = aiUsageMapper.daily(start, end);
        if (dailyRows != null) {
            for (AiDailyUsageRow row : dailyRows) {
                if (row.getBucket() != null) {
                    byDay.put(row.getBucket(), row);
                }
            }
        }
        List<AdminAiStatsVO.DailyStat> trend = new ArrayList<>(window);
        for (LocalDate day : StudyDates.inclusiveDaysEnding(today, window)) {
            AiDailyUsageRow row = byDay.get(day.toString());
            trend.add(new AdminAiStatsVO.DailyStat(
                    day.toString(),
                    row == null || row.getCalls() == null ? 0L : row.getCalls(),
                    row == null || row.getSuccessCalls() == null ? 0L : row.getSuccessCalls()
            ));
        }

        return new AdminAiStatsVO(
                totalCalls,
                successCalls,
                AiStatsMath.successRate(successCalls, totalCalls),
                AiStatsMath.averageLatency(totalLatency, totalCalls),
                totalTokens,
                features,
                users,
                trend
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AuditLogVO> pageAuditLogs(String module, String username, Integer status,
                                                long pageNum, long pageSize) {
        if (status != null && status != SysAuditLog.STATUS_FAILED && status != SysAuditLog.STATUS_SUCCESS) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "审计状态只能是 0 或 1");
        }
        long current = PageQuery.normalizePageNum(pageNum);
        long size = PageQuery.normalizePageSize(pageSize);
        String moduleText = blankToNull(module);
        String usernameText = blankToNull(username);
        Page<SysAuditLog> page = auditLogMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<SysAuditLog>()
                .eq(moduleText != null, SysAuditLog::getModule, moduleText)
                .like(usernameText != null, SysAuditLog::getUsername, usernameText)
                .eq(status != null, SysAuditLog::getStatus, status)
                .orderByDesc(SysAuditLog::getCreatedAt)
                .orderByDesc(SysAuditLog::getId));
        List<SysAuditLog> records = page.getRecords() == null ? List.of() : page.getRecords();
        List<AuditLogVO> vos = new ArrayList<>(records.size());
        for (SysAuditLog record : records) {
            vos.add(new AuditLogVO(
                    record.getId(),
                    record.getUserId(),
                    record.getUsername(),
                    record.getModule(),
                    record.getAction(),
                    record.getHttpMethod(),
                    record.getRequestUri(),
                    record.getClientIp(),
                    record.getCostMs(),
                    record.getStatus(),
                    record.getErrorMsg(),
                    record.getCreatedAt()
            ));
        }
        return new PageResult<>(vos, page.getTotal(), current, size);
    }

    private List<AdminAiStatsVO.UserStat> toUserStats(List<AiUserUsageRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (AiUserUsageRow row : rows) {
            if (row.getUserId() != null) {
                ids.add(row.getUserId());
            }
        }
        Map<Long, ManagedUser> users = new HashMap<>();
        for (ManagedUser user : userAdminService.findByIds(ids)) {
            users.put(user.id(), user);
        }
        List<AdminAiStatsVO.UserStat> stats = new ArrayList<>(rows.size());
        for (AiUserUsageRow row : rows) {
            ManagedUser user = users.get(row.getUserId());
            stats.add(new AdminAiStatsVO.UserStat(
                    row.getUserId(),
                    user == null ? null : user.username(),
                    user == null ? null : user.nickname(),
                    row.getCalls() == null ? 0L : row.getCalls(),
                    row.getTodayCalls() == null ? 0L : row.getTodayCalls(),
                    user == null || user.aiDailyLimit() == null ? 0 : user.aiDailyLimit()
            ));
        }
        return stats;
    }

    private void revokeNow(Long userId) {
        try {
            stringRedisTemplate.opsForValue().set(
                    RedisKeys.revokedUser(userId),
                    String.valueOf(Instant.now().getEpochSecond()),
                    REVOKED_TTL
            );
        } catch (DataAccessException ex) {
            log.error("写入用户撤销时间戳失败 userId={}", userId, ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "登录状态失效失败，请稍后重试");
        }
    }

    private static List<AdminUserVO> toUserVos(List<ManagedUser> users) {
        if (users == null || users.isEmpty()) {
            return List.of();
        }
        List<AdminUserVO> vos = new ArrayList<>(users.size());
        for (ManagedUser user : users) {
            vos.add(toUserVo(user));
        }
        return vos;
    }

    private static AdminUserVO toUserVo(ManagedUser user) {
        return new AdminUserVO(
                user.id(),
                user.username(),
                user.nickname(),
                user.email(),
                user.status(),
                user.roles() == null ? List.of() : user.roles(),
                user.aiDailyLimit(),
                user.createdAt()
        );
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
