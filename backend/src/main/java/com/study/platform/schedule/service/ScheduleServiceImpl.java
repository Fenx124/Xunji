package com.study.platform.schedule.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.study.platform.auth.security.SecurityUtils;
import com.study.platform.common.AfterCommit;
import com.study.platform.common.ErrorCode;
import com.study.platform.common.RedisKeys;
import com.study.platform.common.exception.BusinessException;
import com.study.platform.note.service.NoteLinkQueryService;
import com.study.platform.schedule.dto.EbbinghausRequest;
import com.study.platform.schedule.dto.ScheduleCreateRequest;
import com.study.platform.schedule.dto.ScheduleUpdateRequest;
import com.study.platform.schedule.entity.Schedule;
import com.study.platform.schedule.mapper.ScheduleMapper;
import com.study.platform.schedule.support.EbbinghausPlanner;
import com.study.platform.schedule.support.ScheduleSources;
import com.study.platform.schedule.support.ScheduleRemindPolicy;
import com.study.platform.schedule.support.ScheduleStatusTransitions;
import com.study.platform.schedule.support.ScheduleTimeParser;
import com.study.platform.schedule.vo.ScheduleVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ScheduleServiceImpl implements ScheduleService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleServiceImpl.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final ScheduleMapper scheduleMapper;
    private final NoteLinkQueryService noteLinkQueryService;
    private final StringRedisTemplate stringRedisTemplate;

    public ScheduleServiceImpl(ScheduleMapper scheduleMapper,
                               NoteLinkQueryService noteLinkQueryService,
                               StringRedisTemplate stringRedisTemplate) {
        this.scheduleMapper = scheduleMapper;
        this.noteLinkQueryService = noteLinkQueryService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleVO> list(String startDate, String endDate, Integer status, Integer priority, Long noteId) {
        Long userId = SecurityUtils.getUserId();
        assertStatus(status);
        assertPriority(priority);
        LocalDateTime start = ScheduleTimeParser.parseBound(startDate, false);
        LocalDateTime end = ScheduleTimeParser.parseBound(endDate, true);
        if (start != null && end != null && start.isAfter(end)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "开始日期不能晚于结束日期");
        }
        List<Schedule> schedules = scheduleMapper.selectList(new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getUserId, userId)
                .eq(status != null, Schedule::getStatus, status)
                .eq(priority != null, Schedule::getPriority, priority)
                .eq(noteId != null, Schedule::getNoteId, noteId)
                .ge(start != null, Schedule::getStartTime, start)
                .le(end != null, Schedule::getStartTime, end)
                .orderByAsc(Schedule::getStartTime)
                .orderByAsc(Schedule::getId));
        return toVos(userId, schedules);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduleVO create(ScheduleCreateRequest request) {
        Long userId = SecurityUtils.getUserId();
        assertTimeRange(request.startTime(), request.endTime());
        String noteTitle = null;
        if (request.noteId() != null) {
            noteTitle = noteLinkQueryService.requireActiveTitle(userId, request.noteId());
        }
        int priority = request.priority() == null
                ? Schedule.PRIORITY_IMPORTANT_NOT_URGENT
                : request.priority();
        Schedule schedule = new Schedule();
        schedule.setUserId(userId);
        schedule.setNoteId(request.noteId());
        schedule.setTitle(request.title().trim());
        schedule.setDescription(blankToNull(request.description()));
        schedule.setStartTime(request.startTime());
        schedule.setEndTime(request.endTime());
        schedule.setRemindTime(request.remindTime());
        schedule.setReminded(0);
        schedule.setPriority(priority);
        schedule.setStatus(Schedule.STATUS_TODO);
        schedule.setSourceType(ScheduleSources.forCreate(request.sourceType()));
        schedule.setDeleted(0);
        scheduleMapper.insert(schedule);
        if (schedule.getId() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "日程创建失败");
        }
        AfterCommit.run(() -> evictDashboard(userId));
        return toVo(requireOwned(schedule.getId(), userId), noteTitle);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduleVO update(Long id, ScheduleUpdateRequest request) {
        Long userId = SecurityUtils.getUserId();
        Schedule existing = requireOwned(id, userId);
        assertTimeRange(request.startTime(), request.endTime());
        String noteTitle = null;
        if (request.noteId() != null) {
            noteTitle = noteLinkQueryService.requireActiveTitle(userId, request.noteId());
        }
        int priority = request.priority() == null
                ? (existing.getPriority() == null ? Schedule.PRIORITY_IMPORTANT_NOT_URGENT : existing.getPriority())
                : request.priority();
        LocalDateTime now = LocalDateTime.now(ZONE);
        int status = existing.getStatus() == null ? Schedule.STATUS_TODO : existing.getStatus();
        LocalDateTime completedAt = existing.getCompletedAt();
        if (request.status() != null) {
            ScheduleStatusTransitions.Result transition = ScheduleStatusTransitions.apply(
                    status, completedAt, request.status(), now);
            status = transition.status();
            completedAt = transition.completedAt();
        }
        int reminded = existing.getReminded() == null ? 0 : existing.getReminded();
        if (ScheduleRemindPolicy.shouldReset(existing.getRemindTime(), request.remindTime(), now)) {
            reminded = 0;
        }
        LambdaUpdateWrapper<Schedule> wrapper = new LambdaUpdateWrapper<Schedule>()
                .eq(Schedule::getId, id)
                .eq(Schedule::getUserId, userId)
                .set(Schedule::getTitle, request.title().trim())
                .set(Schedule::getStartTime, request.startTime())
                .set(Schedule::getReminded, reminded)
                .set(Schedule::getPriority, priority)
                .set(Schedule::getStatus, status);
        assign(wrapper, Schedule::getDescription, "description", blankToNull(request.description()));
        assign(wrapper, Schedule::getNoteId, "note_id", request.noteId());
        assign(wrapper, Schedule::getEndTime, "end_time", request.endTime());
        assign(wrapper, Schedule::getRemindTime, "remind_time", request.remindTime());
        assign(wrapper, Schedule::getCompletedAt, "completed_at", completedAt);
        int rows = scheduleMapper.update(wrapper);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "日程不存在");
        }
        AfterCommit.run(() -> evictDashboard(userId));
        return toVo(requireOwned(id, userId), noteTitle);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduleVO updateStatus(Long id, Integer status) {
        Long userId = SecurityUtils.getUserId();
        Schedule existing = requireOwned(id, userId);
        assertStatus(status);
        ScheduleStatusTransitions.Result transition = ScheduleStatusTransitions.apply(
                existing.getStatus() == null ? Schedule.STATUS_TODO : existing.getStatus(),
                existing.getCompletedAt(),
                status,
                LocalDateTime.now(ZONE));
        LambdaUpdateWrapper<Schedule> wrapper = new LambdaUpdateWrapper<Schedule>()
                .eq(Schedule::getId, id)
                .eq(Schedule::getUserId, userId)
                .set(Schedule::getStatus, transition.status());
        assign(wrapper, Schedule::getCompletedAt, "completed_at", transition.completedAt());
        int rows = scheduleMapper.update(wrapper);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "日程不存在");
        }
        AfterCommit.run(() -> evictDashboard(userId));
        String noteTitle = titleOf(userId, existing.getNoteId());
        return toVo(requireOwned(id, userId), noteTitle);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long userId = SecurityUtils.getUserId();
        requireOwned(id, userId);
        scheduleMapper.delete(new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getId, id)
                .eq(Schedule::getUserId, userId));
        AfterCommit.run(() -> evictDashboard(userId));
    }

    /**
     * 以基准日（缺省为上海时区的今天）向后生成 1/2/4/7/15 天共 5 条复习日程。
     * 任一条插入失败则整批回滚。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ScheduleVO> createEbbinghaus(EbbinghausRequest request) {
        Long userId = SecurityUtils.getUserId();
        String noteTitle = noteLinkQueryService.requireActiveTitle(userId, request.noteId());
        LocalDate baseDate = request.baseDate() == null || request.baseDate().isBlank()
                ? LocalDate.now(ZONE)
                : ScheduleTimeParser.parseDate(request.baseDate());
        List<EbbinghausPlanner.ReviewSlot> slots = EbbinghausPlanner.generate(baseDate, noteTitle);
        List<Long> ids = new ArrayList<>(slots.size());
        for (EbbinghausPlanner.ReviewSlot slot : slots) {
            Schedule schedule = new Schedule();
            schedule.setUserId(userId);
            schedule.setNoteId(request.noteId());
            schedule.setTitle(slot.title());
            schedule.setStartTime(slot.startTime());
            schedule.setEndTime(slot.endTime());
            schedule.setRemindTime(slot.remindTime());
            schedule.setReminded(0);
            schedule.setPriority(Schedule.PRIORITY_IMPORTANT_NOT_URGENT);
            schedule.setStatus(Schedule.STATUS_TODO);
            schedule.setSourceType(Schedule.SOURCE_EBBINGHAUS);
            schedule.setDeleted(0);
            scheduleMapper.insert(schedule);
            if (schedule.getId() == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "复习日程创建失败");
            }
            ids.add(schedule.getId());
        }
        AfterCommit.run(() -> evictDashboard(userId));
        List<Schedule> created = scheduleMapper.selectList(new LambdaQueryWrapper<Schedule>()
                .in(Schedule::getId, ids)
                .orderByAsc(Schedule::getStartTime)
                .orderByAsc(Schedule::getId));
        return toVos(userId, created);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleVO> upcomingReminders() {
        Long userId = SecurityUtils.getUserId();
        return toVos(userId, scheduleMapper.selectUpcomingReminders(userId));
    }

    private Schedule requireOwned(Long id, Long userId) {
        Schedule schedule = scheduleMapper.selectRawById(id);
        if (schedule == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "日程不存在");
        }
        if (!userId.equals(schedule.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该日程");
        }
        if (schedule.inTrash()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "日程不存在");
        }
        return schedule;
    }

    private String titleOf(Long userId, Long noteId) {
        if (noteId == null) {
            return null;
        }
        return noteLinkQueryService.findTitles(userId, List.of(noteId)).get(noteId);
    }

    private List<ScheduleVO> toVos(Long userId, List<Schedule> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            return List.of();
        }
        Set<Long> noteIds = new HashSet<>();
        for (Schedule schedule : schedules) {
            if (schedule.getNoteId() != null) {
                noteIds.add(schedule.getNoteId());
            }
        }
        Map<Long, String> titles = noteLinkQueryService.findTitles(userId, noteIds);
        List<ScheduleVO> result = new ArrayList<>(schedules.size());
        for (Schedule schedule : schedules) {
            result.add(toVo(schedule, titles.get(schedule.getNoteId())));
        }
        return result;
    }

    private ScheduleVO toVo(Schedule schedule, String noteTitle) {
        return new ScheduleVO(
                schedule.getId(),
                schedule.getNoteId(),
                noteTitle,
                schedule.getTitle(),
                schedule.getDescription(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getRemindTime(),
                schedule.getReminded() != null && schedule.getReminded() == 1,
                schedule.getPriority(),
                schedule.getStatus(),
                schedule.getSourceType(),
                schedule.getCompletedAt(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt()
        );
    }

    private void evictDashboard(Long userId) {
        try {
            stringRedisTemplate.delete(RedisKeys.dashboardOverview(userId));
        } catch (RuntimeException ex) {
            log.warn("失效看板缓存失败 userId={}", userId, ex);
        }
    }

    private static void assertTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "结束时间不能早于开始时间");
        }
    }

    private static void assertStatus(Integer status) {
        if (status != null && (status < Schedule.STATUS_TODO || status > Schedule.STATUS_CANCELLED)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "状态取值范围为 0 到 2");
        }
    }

    private static void assertPriority(Integer priority) {
        if (priority != null && (priority < Schedule.PRIORITY_IMPORTANT_URGENT
                || priority > Schedule.PRIORITY_NOT_IMPORTANT_NOT_URGENT)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "优先级取值范围为 1 到 4");
        }
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static <T> void assign(LambdaUpdateWrapper<T> wrapper,
                                   com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, ?> column,
                                   String columnName,
                                   Object value) {
        if (value == null) {
            wrapper.setSql(columnName + " = NULL");
        } else {
            wrapper.set(column, value);
        }
    }
}
