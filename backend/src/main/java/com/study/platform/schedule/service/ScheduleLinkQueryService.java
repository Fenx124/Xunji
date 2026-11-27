package com.study.platform.schedule.service;

import com.study.platform.schedule.vo.ScheduleBriefVO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 给笔记详情和番茄钟用的只读日程查询。
 * 其他模块依赖这个接口，而不是 ScheduleMapper，避免循环依赖。
 */
public interface ScheduleLinkQueryService {

    List<ScheduleBriefVO> listByNoteId(Long userId, Long noteId);

    /**
     * 日程必须属于该用户且未删除。不存在返回 40401，越权返回 40301。
     */
    String requireOwnedTitle(Long userId, Long scheduleId);

    /**
     * 批量取未删除日程的标题。不属于该用户的 id 不会出现在结果里。
     */
    Map<Long, String> findTitles(Long userId, Collection<Long> scheduleIds);
}
