package com.study.platform.note.service;

import com.study.platform.common.ErrorCode;
import com.study.platform.common.exception.BusinessException;
import com.study.platform.note.entity.Note;
import com.study.platform.note.mapper.NoteMapper;
import com.study.platform.note.mapper.NoteTitleRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
public class NoteLinkQueryServiceImpl implements NoteLinkQueryService {

    private final NoteMapper noteMapper;

    public NoteLinkQueryServiceImpl(NoteMapper noteMapper) {
        this.noteMapper = noteMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public String requireActiveTitle(Long userId, Long noteId) {
        Note note = noteMapper.selectRawById(noteId);
        if (note == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "笔记不存在");
        }
        if (userId == null || !userId.equals(note.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该笔记");
        }
        if (note.inTrash()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "笔记不存在或已在回收站");
        }
        return note.getTitle();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, String> findTitles(Long userId, Collection<Long> noteIds) {
        if (userId == null || noteIds == null || noteIds.isEmpty()) {
            // 不用 Map.of()：调用方会用 noteId=null 去取“未关联笔记”，Map.of().get(null) 会 NPE。
            return new HashMap<>();
        }
        Map<Long, String> titles = new HashMap<>();
        for (NoteTitleRow row : noteMapper.selectTitles(userId, noteIds)) {
            if (row.getId() != null) {
                titles.put(row.getId(), row.getTitle());
            }
        }
        return titles;
    }
}
