package com.study.platform.note.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.study.platform.auth.security.SecurityUtils;
import com.study.platform.common.AfterCommit;
import com.study.platform.common.ErrorCode;
import com.study.platform.common.RedisKeys;
import com.study.platform.common.exception.BusinessException;
import com.study.platform.note.dto.NotebookSaveRequest;
import com.study.platform.note.entity.Notebook;
import com.study.platform.note.mapper.IdCountRow;
import com.study.platform.note.mapper.NoteMapper;
import com.study.platform.note.mapper.NotebookMapper;
import com.study.platform.note.vo.NotebookVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotebookServiceImpl implements NotebookService {

    private static final Logger log = LoggerFactory.getLogger(NotebookServiceImpl.class);

    private final NotebookMapper notebookMapper;
    private final NoteMapper noteMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public NotebookServiceImpl(NotebookMapper notebookMapper,
                               NoteMapper noteMapper,
                               StringRedisTemplate stringRedisTemplate) {
        this.notebookMapper = notebookMapper;
        this.noteMapper = noteMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotebookVO> list() {
        Long userId = SecurityUtils.getUserId();
        List<Notebook> notebooks = notebookMapper.selectList(new LambdaQueryWrapper<Notebook>()
                .eq(Notebook::getUserId, userId)
                .orderByAsc(Notebook::getSortOrder)
                .orderByAsc(Notebook::getId));
        Map<Long, Long> counts = countNotes(userId);
        List<NotebookVO> result = new ArrayList<>(notebooks.size());
        for (Notebook notebook : notebooks) {
            result.add(toVo(notebook, counts.getOrDefault(notebook.getId(), 0L)));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotebookVO create(NotebookSaveRequest request) {
        Long userId = SecurityUtils.getUserId();
        Notebook notebook = new Notebook();
        notebook.setUserId(userId);
        notebook.setName(request.name().trim());
        notebook.setDescription(blankToNull(request.description()));
        notebook.setIcon(iconOrDefault(request.icon()));
        notebook.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        notebook.setDeleted(0);
        notebookMapper.insert(notebook);
        return toVo(requireOwned(notebook.getId(), userId), 0L);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotebookVO update(Long id, NotebookSaveRequest request) {
        Long userId = SecurityUtils.getUserId();
        Notebook existing = requireOwned(id, userId);
        String name = request.name().trim();
        String description = blankToNull(request.description());
        String icon = iconOrDefault(request.icon());
        Integer sortOrder = request.sortOrder() == null ? existing.getSortOrder() : request.sortOrder();
        List<Long> noteIds = noteMapper.selectIdsByNotebook(userId, id);
        LambdaUpdateWrapper<Notebook> wrapper = new LambdaUpdateWrapper<Notebook>()
                .eq(Notebook::getId, id)
                .eq(Notebook::getUserId, userId)
                .set(Notebook::getName, name)
                .set(Notebook::getIcon, icon)
                .set(Notebook::getSortOrder, sortOrder);
        if (description == null) {
            wrapper.setSql("description = NULL");
        } else {
            wrapper.set(Notebook::getDescription, description);
        }
        notebookMapper.update(wrapper);
        AfterCommit.run(() -> evictNotes(noteIds));
        return toVo(requireOwned(id, userId), countNotes(userId).getOrDefault(id, 0L));
    }

    /**
     * 逻辑删除不会触发外键的 ON DELETE SET NULL，所以在同一事务里把笔记的 notebook_id 清空。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long userId = SecurityUtils.getUserId();
        requireOwned(id, userId);
        List<Long> noteIds = noteMapper.selectIdsByNotebook(userId, id);
        noteMapper.clearNotebookId(userId, id);
        notebookMapper.delete(new LambdaQueryWrapper<Notebook>()
                .eq(Notebook::getId, id)
                .eq(Notebook::getUserId, userId));
        AfterCommit.run(() -> {
            evictNotes(noteIds);
            evictDashboard(userId);
        });
    }

    private Notebook requireOwned(Long id, Long userId) {
        Notebook notebook = notebookMapper.selectById(id);
        if (notebook == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "笔记本不存在");
        }
        if (!userId.equals(notebook.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该笔记本");
        }
        return notebook;
    }

    private Map<Long, Long> countNotes(Long userId) {
        Map<Long, Long> counts = new HashMap<>();
        for (IdCountRow row : noteMapper.countActiveByNotebook(userId)) {
            if (row.getId() != null && row.getNoteCount() != null) {
                counts.put(row.getId(), row.getNoteCount());
            }
        }
        return counts;
    }

    private NotebookVO toVo(Notebook notebook, long noteCount) {
        return new NotebookVO(
                notebook.getId(),
                notebook.getName(),
                notebook.getDescription(),
                notebook.getIcon(),
                notebook.getSortOrder(),
                noteCount,
                notebook.getCreatedAt(),
                notebook.getUpdatedAt()
        );
    }

    private static String iconOrDefault(String icon) {
        if (icon == null || icon.isBlank()) {
            return Notebook.DEFAULT_ICON;
        }
        return icon.trim();
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void evictNotes(Collection<Long> noteIds) {
        if (noteIds == null || noteIds.isEmpty()) {
            return;
        }
        List<String> keys = new ArrayList<>();
        for (Long noteId : noteIds) {
            if (noteId != null) {
                keys.add(RedisKeys.noteDetail(noteId));
            }
        }
        if (keys.isEmpty()) {
            return;
        }
        try {
            stringRedisTemplate.delete(keys);
        } catch (RuntimeException ex) {
            log.warn("失效笔记缓存失败 count={}", keys.size(), ex);
        }
    }

    private void evictDashboard(Long userId) {
        try {
            stringRedisTemplate.delete(RedisKeys.dashboardOverview(userId));
        } catch (RuntimeException ex) {
            log.warn("失效看板缓存失败 userId={}", userId, ex);
        }
    }
}
