package com.study.platform.note.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 看板读取笔记统计的只读入口。看板不注入 NoteMapper。
 */
public interface NoteDashboardQueryService {

    record NotebookSlice(String name, long count) {
    }

    record TagSlice(String name, String color, long count) {
    }

    long countActiveNotes(Long userId);

    /** 当天新建或更新过的未删除笔记数，同一天只计一次。 */
    Map<LocalDate, Long> countActivityByDay(Long userId, LocalDateTime startInclusive, LocalDateTime endExclusive);

    /** 按 updated_at 所在日期计数，供近 7 日趋势。 */
    Map<LocalDate, Long> countUpdatedByDay(Long userId, LocalDateTime startInclusive, LocalDateTime endExclusive);

    List<NotebookSlice> notebookDistribution(Long userId);

    List<TagSlice> topTags(Long userId, int limit);
}
