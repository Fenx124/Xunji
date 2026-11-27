package com.study.platform.schedule.service;

import com.study.platform.schedule.dto.EbbinghausRequest;
import com.study.platform.schedule.dto.ScheduleCreateRequest;
import com.study.platform.schedule.dto.ScheduleUpdateRequest;
import com.study.platform.schedule.vo.ScheduleVO;

import java.util.List;

public interface ScheduleService {

    List<ScheduleVO> list(String startDate, String endDate, Integer status, Integer priority, Long noteId);

    ScheduleVO create(ScheduleCreateRequest request);

    ScheduleVO update(Long id, ScheduleUpdateRequest request);

    ScheduleVO updateStatus(Long id, Integer status);

    void delete(Long id);

    List<ScheduleVO> createEbbinghaus(EbbinghausRequest request);

    List<ScheduleVO> upcomingReminders();
}
