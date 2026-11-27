package com.study.platform.note.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.study.platform.auth.security.SecurityUtils;
import com.study.platform.common.AfterCommit;
import com.study.platform.common.ErrorCode;
import com.study.platform.common.RedisKeys;
import com.study.platform.common.exception.BusinessException;
import com.study.platform.note.dto.TagSaveRequest;
import com.study.platform.note.entity.Tag;
import com.study.platform.note.mapper.IdCountRow;
import com.study.platform.note.mapper.NoteTagMapper;
import com.study.platform.note.mapper.TagMapper;
import com.study.platform.note.vo.TagVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TagServiceImpl implements TagService {

    private static final Logger log = LoggerFactory.getLogger(TagServiceImpl.class);
    private static final String DUPLICATE_NAME = "标签名称已存在";

    private final TagMapper tagMapper;
    private final NoteTagMapper noteTagMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public TagServiceImpl(TagMapper tagMapper,
                          NoteTagMapper noteTagMapper,
                          StringRedisTemplate stringRedisTemplate) {
        this.tagMapper = tagMapper;
        this.noteTagMapper = noteTagMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagVO> list() {
        Long userId = SecurityUtils.getUserId();
        List<Tag> tags = tagMapper.selectList(new LambdaQueryWrapper<Tag>()
                .eq(Tag::getUserId, userId)
                .orderByAsc(Tag::getCreatedAt)
                .orderByAsc(Tag::getId));
        Map<Long, Long> counts = countNotes(userId);
        List<TagVO> result = new ArrayList<>(tags.size());
        for (Tag tag : tags) {
            result.add(toVo(tag, counts.getOrDefault(tag.getId(), 0L)));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TagVO create(TagSaveRequest request) {
        Long userId = SecurityUtils.getUserId();
        String name = request.name().trim();
        assertNameAvailable(userId, name, null);
        Tag tag = new Tag();
        tag.setUserId(userId);
        tag.setName(name);
        tag.setColor(colorOrDefault(request.color()));
        try {
            tagMapper.insert(tag);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.CONFLICT, DUPLICATE_NAME);
        }
        return toVo(requireOwned(tag.getId(), userId), 0L);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TagVO update(Long id, TagSaveRequest request) {
        Long userId = SecurityUtils.getUserId();
        requireOwned(id, userId);
        String name = request.name().trim();
        assertNameAvailable(userId, name, id);
        List<Long> noteIds = noteTagMapper.selectNoteIdsByTag(id);
        try {
            tagMapper.update(new LambdaUpdateWrapper<Tag>()
                    .eq(Tag::getId, id)
                    .eq(Tag::getUserId, userId)
                    .set(Tag::getName, name)
                    .set(Tag::getColor, colorOrDefault(request.color())));
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.CONFLICT, DUPLICATE_NAME);
        }
        AfterCommit.run(() -> evictNotes(noteIds));
        return toVo(requireOwned(id, userId), countNotes(userId).getOrDefault(id, 0L));
    }

    /**
     * 标签没有逻辑删除。先解除 note_tag，再删除标签本身，避免留下悬空关联。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long userId = SecurityUtils.getUserId();
        requireOwned(id, userId);
        List<Long> noteIds = noteTagMapper.selectNoteIdsByTag(id);
        noteTagMapper.deleteByTagId(id);
        tagMapper.delete(new LambdaQueryWrapper<Tag>()
                .eq(Tag::getId, id)
                .eq(Tag::getUserId, userId));
        AfterCommit.run(() -> {
            evictNotes(noteIds);
            evictDashboard(userId);
        });
    }

    private void assertNameAvailable(Long userId, String name, Long excludeId) {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<Tag>()
                .eq(Tag::getUserId, userId)
                .eq(Tag::getName, name);
        if (excludeId != null) {
            wrapper.ne(Tag::getId, excludeId);
        }
        Long count = tagMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, DUPLICATE_NAME);
        }
    }

    private Tag requireOwned(Long id, Long userId) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "标签不存在");
        }
        if (!userId.equals(tag.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该标签");
        }
        return tag;
    }

    private Map<Long, Long> countNotes(Long userId) {
        Map<Long, Long> counts = new HashMap<>();
        for (IdCountRow row : noteTagMapper.countActiveNotesByTag(userId)) {
            if (row.getId() != null && row.getNoteCount() != null) {
                counts.put(row.getId(), row.getNoteCount());
            }
        }
        return counts;
    }

    private TagVO toVo(Tag tag, long noteCount) {
        return new TagVO(tag.getId(), tag.getName(), tag.getColor(), noteCount, tag.getCreatedAt());
    }

    private static String colorOrDefault(String color) {
        if (color == null || color.isBlank()) {
            return Tag.DEFAULT_COLOR;
        }
        return color.trim();
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
