package com.study.platform.dashboard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.platform.auth.security.SecurityUtils;
import com.study.platform.common.RedisKeys;
import com.study.platform.common.StudyDates;
import com.study.platform.dashboard.support.DashboardMath;
import com.study.platform.dashboard.vo.DashboardOverviewVO;
import com.study.platform.focus.service.FocusStatsQueryService;
import com.study.platform.note.service.NoteDashboardQueryService;
import com.study.platform.schedule.service.ScheduleDashboardQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 首页看板 Cache-Aside：先读 Redis，失败或未命中再聚合 MySQL，写回缓存 TTL 5 分钟。
 * Redis 异常只记警告，统计仍然从数据库返回。
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final int HEATMAP_DAYS = 90;
    private static final int TREND_DAYS = 7;
    private static final int TAG_LIMIT = 10;

    private final NoteDashboardQueryService noteDashboardQueryService;
    private final ScheduleDashboardQueryService scheduleDashboardQueryService;
    private final FocusStatsQueryService focusStatsQueryService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public DashboardServiceImpl(NoteDashboardQueryService noteDashboardQueryService,
                                ScheduleDashboardQueryService scheduleDashboardQueryService,
                                FocusStatsQueryService focusStatsQueryService,
                                StringRedisTemplate stringRedisTemplate,
                                ObjectMapper objectMapper) {
        this.noteDashboardQueryService = noteDashboardQueryService;
        this.scheduleDashboardQueryService = scheduleDashboardQueryService;
        this.focusStatsQueryService = focusStatsQueryService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardOverviewVO overview() {
        Long userId = SecurityUtils.getUserId();
        DashboardOverviewVO cached = readCache(userId);
        if (cached != null) {
            return cached;
        }
        DashboardOverviewVO fresh = load(userId);
        writeCache(userId, fresh);
        return fresh;
    }

    private DashboardOverviewVO load(Long userId) {
        LocalDate today = StudyDates.today();
        LocalDateTime todayStart = StudyDates.startOf(today);
        LocalDateTime tomorrow = StudyDates.nextDay(today);
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDateTime monthStartTime = StudyDates.startOf(monthStart);
        LocalDateTime nextMonth = StudyDates.startOf(monthStart.plusMonths(1));
        List<LocalDate> heatDays = DashboardMath.lastDays(today, HEATMAP_DAYS);
        List<LocalDate> trendDays = DashboardMath.lastDays(today, TREND_DAYS);
        LocalDateTime heatStart = StudyDates.startOf(heatDays.get(0));
        LocalDateTime trendStart = StudyDates.startOf(trendDays.get(0));

        long totalNotes = noteDashboardQueryService.countActiveNotes(userId);
        long todayPending = scheduleDashboardQueryService.countPending(userId, todayStart, tomorrow);
        ScheduleDashboardQueryService.Completion completion =
                scheduleDashboardQueryService.monthCompletion(userId, monthStartTime, nextMonth);
        long weekFocus = focusStatsQueryService.sumMinutes(userId, trendStart, tomorrow);
        long todayFocus = focusStatsQueryService.sumMinutes(userId, todayStart, tomorrow);

        Map<LocalDate, Long> noteActivity =
                noteDashboardQueryService.countActivityByDay(userId, heatStart, tomorrow);
        Map<LocalDate, Long> scheduleDone =
                scheduleDashboardQueryService.countCompletedByDay(userId, heatStart, tomorrow);
        Map<LocalDate, Long> focusStarted =
                focusStatsQueryService.countStartedByDay(userId, heatStart, tomorrow);
        Map<LocalDate, Long> notesUpdated =
                noteDashboardQueryService.countUpdatedByDay(userId, trendStart, tomorrow);
        Map<LocalDate, Long> focusMinutes =
                focusStatsQueryService.sumMinutesByDay(userId, trendStart, tomorrow);

        List<DashboardOverviewVO.NameCount> notebooks = new ArrayList<>();
        for (NoteDashboardQueryService.NotebookSlice slice : noteDashboardQueryService.notebookDistribution(userId)) {
            notebooks.add(new DashboardOverviewVO.NameCount(slice.name(), slice.count()));
        }
        List<DashboardOverviewVO.TagCount> tags = new ArrayList<>();
        for (NoteDashboardQueryService.TagSlice slice : noteDashboardQueryService.topTags(userId, TAG_LIMIT)) {
            tags.add(new DashboardOverviewVO.TagCount(slice.name(), slice.color(), slice.count()));
        }

        return new DashboardOverviewVO(
                new DashboardOverviewVO.Summary(
                        totalNotes,
                        todayPending,
                        DashboardMath.completionRate(completion.completed(), completion.nonCancelled()),
                        weekFocus,
                        todayFocus
                ),
                DashboardMath.heatmap(heatDays, noteActivity, scheduleDone, focusStarted),
                DashboardMath.weeklyTrend(trendDays, notesUpdated, scheduleDone, focusMinutes),
                notebooks,
                tags
        );
    }

    private DashboardOverviewVO readCache(Long userId) {
        String key = RedisKeys.dashboardOverview(userId);
        String json;
        try {
            json = stringRedisTemplate.opsForValue().get(key);
        } catch (RuntimeException ex) {
            log.warn("读取看板缓存失败，回退 MySQL userId={}", userId, ex);
            return null;
        }
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            DashboardOverviewVO cached = objectMapper.readValue(json, DashboardOverviewVO.class);
            if (cached == null || cached.summary() == null || cached.heatmap() == null || cached.weeklyTrend() == null) {
                return null;
            }
            return cached;
        } catch (Exception ex) {
            log.warn("看板缓存反序列化失败，回退 MySQL userId={}", userId, ex);
            return null;
        }
    }

    private void writeCache(Long userId, DashboardOverviewVO overview) {
        try {
            stringRedisTemplate.opsForValue().set(
                    RedisKeys.dashboardOverview(userId),
                    objectMapper.writeValueAsString(overview),
                    CACHE_TTL
            );
        } catch (Exception ex) {
            log.warn("写入看板缓存失败 userId={}", userId, ex);
        }
    }
}
