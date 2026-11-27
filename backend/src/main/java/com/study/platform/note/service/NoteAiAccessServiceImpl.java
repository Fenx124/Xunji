package com.study.platform.note.service;

import com.study.platform.common.AfterCommit;
import com.study.platform.common.ErrorCode;
import com.study.platform.common.RedisKeys;
import com.study.platform.common.exception.BusinessException;
import com.study.platform.note.entity.Note;
import com.study.platform.note.mapper.NoteMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 笔记归属校验与摘要回写。摘要更新故意绕过乐观锁，只改 ai_summary 一列。
 */
@Service
public class NoteAiAccessServiceImpl implements NoteAiAccessService {

    private static final Logger log = LoggerFactory.getLogger(NoteAiAccessServiceImpl.class);
    /** 与 note.ai_summary 列宽一致，调用方还会先压到 300 字。 */
    private static final int SUMMARY_COLUMN_LIMIT = 1000;

    private final NoteMapper noteMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public NoteAiAccessServiceImpl(NoteMapper noteMapper, StringRedisTemplate stringRedisTemplate) {
        this.noteMapper = noteMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public NoteAiSnapshot requireOwned(Long userId, Long noteId) {
        if (noteId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "笔记 ID 不能为空");
        }
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
        String title = note.getTitle() == null ? "" : note.getTitle();
        String content = note.getContent() == null ? "" : note.getContent();
        return new NoteAiSnapshot(note.getId(), title, content);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSummary(Long userId, Long noteId, String summary) {
        String stored = clamp(summary, SUMMARY_COLUMN_LIMIT);
        int rows = noteMapper.updateAiSummary(noteId, userId, stored);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "笔记不存在");
        }
        // 事务提交后再删详情缓存，避免并发读把旧摘要写回 Redis。
        AfterCommit.run(() -> evictDetail(noteId));
    }

    private void evictDetail(Long noteId) {
        try {
            stringRedisTemplate.delete(RedisKeys.noteDetail(noteId));
        } catch (RuntimeException ex) {
            log.warn("失效笔记缓存失败 noteId={}", noteId, ex);
        }
    }

    private static String clamp(String text, int max) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, max);
    }
}
