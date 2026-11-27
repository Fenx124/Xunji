package com.study.platform.focus.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.study.platform.auth.security.SecurityUtils;
import com.study.platform.common.AfterCommit;
import com.study.platform.common.PageQuery;
import com.study.platform.common.PageResult;
import com.study.platform.common.RedisKeys;
import com.study.platform.common.StudyDates;
import com.study.platform.focus.dto.FocusRecordCreateRequest;
import com.study.platform.focus.entity.FocusRecord;
import com.study.platform.focus.mapper.FocusRecordMapper;
import com.study.platform.focus.support.FocusRecordRules;
import com.study.platform.focus.vo.FocusRecordVO;
import com.study.platform.focus.vo.FocusTodayStatsVO;
import com.study.platform.note.service.NoteLinkQueryService;
import com.study.platform.schedule.service.ScheduleLinkQueryService;
import com.study.platform.schedule.support.ScheduleTimeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class FocusServiceImpl implements FocusService {

    private static final Logger log = LoggerFactory.getLogger(FocusServiceImpl.class);

    private final FocusRecordMapper focusRecordMapper;
    private final FocusStatsQueryService focusStatsQueryService;
    private final NoteLinkQueryService noteLinkQueryService;
    private final ScheduleLinkQueryService scheduleLinkQueryService;
    private final StringRedisTemplate stringRedisTemplate;

    public FocusServiceImpl(FocusRecordMapper focusRecordMapper,
                            FocusStatsQueryService focusStatsQueryService,
                            NoteLinkQueryService noteLinkQueryService,
                            ScheduleLinkQueryService scheduleLinkQueryService,
                            StringRedisTemplate stringRedisTemplate) {
        this.focusRecordMapper = focusRecordMapper;
        this.focusStatsQueryService = focusStatsQueryService;
        this.noteLinkQueryService = noteLinkQueryService;
        this.scheduleLinkQueryService = scheduleLinkQueryService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FocusRecordVO create(FocusRecordCreateRequest request) {
        Long userId = SecurityUtils.getUserId();
        FocusRecordRules.validateOptionalId(request.noteId(), "笔记");
        FocusRecordRules.validateOptionalId(request.scheduleId(), "日程");
        FocusRecordRules.validate(
                request.plannedMinutes(),
                request.actualMinutes(),
                request.status(),
                request.startedAt(),
                request.endedAt(),
                request.remark()
        );
        String noteTitle = null;
        if (request.noteId() != null) {
            noteTitle = noteLinkQueryService.requireActiveTitle(userId, request.noteId());
        }
        String scheduleTitle = null;
        if (request.scheduleId() != null) {
            scheduleTitle = scheduleLinkQueryService.requireOwnedTitle(userId, request.scheduleId());
        }

        FocusRecord record = new FocusRecord();
        record.setUserId(userId);
        record.setNoteId(request.noteId());
        record.setScheduleId(request.scheduleId());
        record.setPlannedMinutes(request.plannedMinutes());
        record.setActualMinutes(request.actualMinutes());
        record.setStatus(request.status());
        record.setStartedAt(request.startedAt());
        record.setEndedAt(request.endedAt());
        record.setRemark(blankToNull(request.remark()));
        record.setCreatedAt(StudyDates.now());
        focusRecordMapper.insert(record);
        // 事务提交后再删看板缓存，避免并发读把旧统计写回去。
        AfterCommit.run(() -> evictDashboard(userId));
        return toVo(record, noteTitle, scheduleTitle);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<FocusRecordVO> page(String startDate, String endDate, long pageNum, long pageSize) {
        Long userId = SecurityUtils.getUserId();
        LocalDateTime start = ScheduleTimeParser.parseBound(startDate, false);
        LocalDateTime end = ScheduleTimeParser.parseBound(endDate, true);
        FocusRecordRules.validateDateOrder(start, end);
        long current = PageQuery.normalizePageNum(pageNum);
        long size = PageQuery.normalizePageSize(pageSize);

        Page<FocusRecord> page = focusRecordMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<FocusRecord>()
                .eq(FocusRecord::getUserId, userId)
                .ge(start != null, FocusRecord::getStartedAt, start)
                .le(end != null, FocusRecord::getStartedAt, end)
                .orderByDesc(FocusRecord::getStartedAt)
                .orderByDesc(FocusRecord::getId));
        List<FocusRecord> records = page.getRecords() == null ? List.of() : page.getRecords();
        return new PageResult<>(toVos(userId, records), page.getTotal(), current, size);
    }

    @Override
    @Transactional(readOnly = true)
    public FocusTodayStatsVO today() {
        Long userId = SecurityUtils.getUserId();
        LocalDate today = StudyDates.today();
        return focusStatsQueryService.todayStats(userId, StudyDates.startOf(today), StudyDates.nextDay(today));
    }

    private List<FocusRecordVO> toVos(Long userId, List<FocusRecord> records) {
        Set<Long> noteIds = new HashSet<>();
        Set<Long> scheduleIds = new HashSet<>();
        for (FocusRecord record : records) {
            if (record.getNoteId() != null) {
                noteIds.add(record.getNoteId());
            }
            if (record.getScheduleId() != null) {
                scheduleIds.add(record.getScheduleId());
            }
        }
        Map<Long, String> noteTitles = noteLinkQueryService.findTitles(userId, noteIds);
        Map<Long, String> scheduleTitles = scheduleLinkQueryService.findTitles(userId, scheduleIds);
        List<FocusRecordVO> result = new ArrayList<>(records.size());
        for (FocusRecord record : records) {
            result.add(toVo(record, noteTitles.get(record.getNoteId()), scheduleTitles.get(record.getScheduleId())));
        }
        return result;
    }

    private static FocusRecordVO toVo(FocusRecord record, String noteTitle, String scheduleTitle) {
        return new FocusRecordVO(
                record.getId(),
                record.getNoteId(),
                noteTitle,
                record.getScheduleId(),
                scheduleTitle,
                record.getPlannedMinutes(),
                record.getActualMinutes(),
                record.getStatus(),
                record.getStartedAt(),
                record.getEndedAt(),
                record.getRemark(),
                record.getCreatedAt()
        );
    }

    private void evictDashboard(Long userId) {
        try {
            stringRedisTemplate.delete(RedisKeys.dashboardOverview(userId));
        } catch (RuntimeException ex) {
            log.warn("失效看板缓存失败 userId={}", userId, ex);
        }
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
