package com.study.platform.note.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.platform.common.DayTotalRow;
import com.study.platform.note.entity.Note;
import com.study.platform.note.mapper.NameCountRow;
import com.study.platform.note.mapper.NoteMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NoteDashboardQueryServiceImpl implements NoteDashboardQueryService {

    private static final String DEFAULT_TAG_COLOR = "#409EFF";

    private final NoteMapper noteMapper;

    public NoteDashboardQueryServiceImpl(NoteMapper noteMapper) {
        this.noteMapper = noteMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveNotes(Long userId) {
        Long total = noteMapper.selectCount(new LambdaQueryWrapper<Note>().eq(Note::getUserId, userId));
        return total == null ? 0L : total;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<LocalDate, Long> countActivityByDay(Long userId, LocalDateTime startInclusive, LocalDateTime endExclusive) {
        return toMap(noteMapper.countActivityByDay(userId, startInclusive, endExclusive));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<LocalDate, Long> countUpdatedByDay(Long userId, LocalDateTime startInclusive, LocalDateTime endExclusive) {
        return toMap(noteMapper.countUpdatedByDay(userId, startInclusive, endExclusive));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotebookSlice> notebookDistribution(Long userId) {
        List<NameCountRow> rows = noteMapper.countByNotebook(userId);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<NotebookSlice> slices = new ArrayList<>(rows.size());
        for (NameCountRow row : rows) {
            if (row.getName() == null || row.getName().isBlank() || row.getTotal() == null || row.getTotal() <= 0) {
                continue;
            }
            slices.add(new NotebookSlice(row.getName(), row.getTotal()));
        }
        return slices;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagSlice> topTags(Long userId, int limit) {
        int size = limit < 1 ? 10 : limit;
        List<NameCountRow> rows = noteMapper.countByTag(userId, size);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<TagSlice> slices = new ArrayList<>(rows.size());
        for (NameCountRow row : rows) {
            if (row.getName() == null || row.getName().isBlank()) {
                continue;
            }
            String color = row.getColor() == null || row.getColor().isBlank() ? DEFAULT_TAG_COLOR : row.getColor();
            slices.add(new TagSlice(row.getName(), color, row.getTotal() == null ? 0L : row.getTotal()));
        }
        return slices;
    }

    private static Map<LocalDate, Long> toMap(List<DayTotalRow> rows) {
        Map<LocalDate, Long> map = new HashMap<>();
        if (rows == null) {
            return map;
        }
        for (DayTotalRow row : rows) {
            if (row.getBucket() == null || row.getBucket().isBlank()) {
                continue;
            }
            map.put(LocalDate.parse(row.getBucket()), row.getTotal() == null ? 0L : row.getTotal());
        }
        return map;
    }
}
