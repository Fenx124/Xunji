package com.study.platform.schedule.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.platform.common.ErrorCode;
import com.study.platform.common.exception.BusinessException;
import com.study.platform.schedule.entity.Schedule;
import com.study.platform.schedule.mapper.ScheduleMapper;
import com.study.platform.schedule.mapper.ScheduleTitleRow;
import com.study.platform.schedule.vo.ScheduleBriefVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScheduleLinkQueryServiceImpl implements ScheduleLinkQueryService {

    private final ScheduleMapper scheduleMapper;

    public ScheduleLinkQueryServiceImpl(ScheduleMapper scheduleMapper) {
        this.scheduleMapper = scheduleMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleBriefVO> listByNoteId(Long userId, Long noteId) {
        if (userId == null || noteId == null) {
            return List.of();
        }
        List<Schedule> schedules = scheduleMapper.selectList(new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getUserId, userId)
                .eq(Schedule::getNoteId, noteId)
                .orderByAsc(Schedule::getStartTime)
                .orderByAsc(Schedule::getId));
        List<ScheduleBriefVO> result = new ArrayList<>(schedules.size());
        for (Schedule schedule : schedules) {
            result.add(new ScheduleBriefVO(
                    schedule.getId(),
                    schedule.getTitle(),
                    schedule.getStartTime(),
                    schedule.getStatus(),
                    schedule.getPriority(),
                    schedule.getSourceType()
            ));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public String requireOwnedTitle(Long userId, Long scheduleId) {
        Schedule schedule = scheduleMapper.selectRawById(scheduleId);
        if (schedule == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "日程不存在");
        }
        if (userId == null || !userId.equals(schedule.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该日程");
        }
        if (schedule.inTrash()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "日程不存在");
        }
        return schedule.getTitle();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, String> findTitles(Long userId, Collection<Long> scheduleIds) {
        if (userId == null || scheduleIds == null || scheduleIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> titles = new HashMap<>();
        for (ScheduleTitleRow row : scheduleMapper.selectTitles(userId, scheduleIds)) {
            if (row.getId() != null) {
                titles.put(row.getId(), row.getTitle());
            }
        }
        return titles;
    }
}
