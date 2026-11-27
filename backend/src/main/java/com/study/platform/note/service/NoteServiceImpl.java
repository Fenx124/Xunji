package com.study.platform.note.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.platform.auth.security.SecurityUtils;
import com.study.platform.common.AfterCommit;
import com.study.platform.common.ErrorCode;
import com.study.platform.common.PageResult;
import com.study.platform.common.RedisKeys;
import com.study.platform.common.exception.BusinessException;
import com.study.platform.note.dto.NoteCreateRequest;
import com.study.platform.note.dto.NoteUpdateRequest;
import com.study.platform.note.entity.Note;
import com.study.platform.note.entity.NoteHistory;
import com.study.platform.note.entity.Notebook;
import com.study.platform.note.entity.Tag;
import com.study.platform.note.mapper.NoteHistoryMapper;
import com.study.platform.note.mapper.NoteMapper;
import com.study.platform.note.mapper.NoteSearchQuery;
import com.study.platform.note.mapper.NoteTagMapper;
import com.study.platform.note.mapper.NoteTagRow;
import com.study.platform.note.mapper.NotebookMapper;
import com.study.platform.note.mapper.TagMapper;
import com.study.platform.note.support.NotebookNames;
import com.study.platform.note.support.NoteHistoryRules;
import com.study.platform.note.support.NoteFlags;
import com.study.platform.note.support.NoteKeyword;
import com.study.platform.note.support.NotePageQuery;
import com.study.platform.note.support.NoteWordCounter;
import com.study.platform.note.vo.NoteDetailVO;
import com.study.platform.note.vo.NoteFlagVO;
import com.study.platform.note.vo.NoteHistoryDetailVO;
import com.study.platform.note.vo.NoteHistoryItemVO;
import com.study.platform.note.vo.NoteListItemVO;
import com.study.platform.note.vo.TagSimpleVO;
import com.study.platform.schedule.service.ScheduleLinkQueryService;
import com.study.platform.schedule.vo.ScheduleBriefVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class NoteServiceImpl implements NoteService {

    private static final Logger log = LoggerFactory.getLogger(NoteServiceImpl.class);
    private static final Duration NOTE_CACHE_TTL = Duration.ofMinutes(15);
    private static final int MAX_KEYWORD_LENGTH = 100;
    static final String VERSION_CONFLICT = "笔记已被其他窗口修改，请刷新后重试";

    private final NoteMapper noteMapper;
    private final NoteHistoryMapper noteHistoryMapper;
    private final NotebookMapper notebookMapper;
    private final TagMapper tagMapper;
    private final NoteTagMapper noteTagMapper;
    private final ScheduleLinkQueryService scheduleLinkQueryService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public NoteServiceImpl(NoteMapper noteMapper,
                           NoteHistoryMapper noteHistoryMapper,
                           NotebookMapper notebookMapper,
                           TagMapper tagMapper,
                           NoteTagMapper noteTagMapper,
                           ScheduleLinkQueryService scheduleLinkQueryService,
                           StringRedisTemplate stringRedisTemplate,
                           ObjectMapper objectMapper) {
        this.noteMapper = noteMapper;
        this.noteHistoryMapper = noteHistoryMapper;
        this.notebookMapper = notebookMapper;
        this.tagMapper = tagMapper;
        this.noteTagMapper = noteTagMapper;
        this.scheduleLinkQueryService = scheduleLinkQueryService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<NoteListItemVO> page(Long notebookId, Long tagId, String keyword, Boolean isFavorite,
                                           long pageNum, long pageSize) {
        Long userId = SecurityUtils.getUserId();
        if (notebookId != null) {
            requireNotebook(userId, notebookId);
        }
        if (tagId != null) {
            requireTags(userId, List.of(tagId));
        }
        NoteKeyword.Parsed parsed = NoteKeyword.parse(keyword);
        if (parsed.value() != null && parsed.value().length() > MAX_KEYWORD_LENGTH) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "关键词长度不能超过 100");
        }
        long current = NotePageQuery.normalizePageNum(pageNum);
        long size = NotePageQuery.normalizePageSize(pageSize);
        NoteSearchQuery query = new NoteSearchQuery();
        query.setNotebookId(notebookId);
        query.setTagId(tagId);
        if (parsed.mode() != NoteKeyword.Mode.NONE) {
            query.setKeyword(parsed.value());
            query.setUseFulltext(parsed.mode() == NoteKeyword.Mode.FULLTEXT);
        }
        if (isFavorite != null) {
            query.setFavoriteFlag(Boolean.TRUE.equals(isFavorite) ? 1 : 0);
        }
        long total = noteMapper.countActive(userId, query);
        if (total == 0) {
            return PageResult.empty(current, size);
        }
        List<Note> notes = noteMapper.selectActivePage(userId, query, NotePageQuery.offset(current, size), size);
        return new PageResult<>(toListItems(userId, notes), total, current, size);
    }

    /**
     * Cache-Aside：先读 note:detail:{noteId}，Redis 异常或缓存损坏时回退 MySQL。
     * 关联日程不放进缓存，避免日程模块更新后这里还是旧数据。
     */
    @Override
    @Transactional(readOnly = true)
    public NoteDetailVO detail(Long id) {
        Long userId = SecurityUtils.getUserId();
        NoteCoreCache cached = readCache(id);
        if (cached != null) {
            if (!userId.equals(cached.userId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该笔记");
            }
            return toDetail(cached, loadSchedules(userId, id));
        }
        Note note = requireActiveOwned(id, userId);
        NoteCoreCache fresh = buildCore(note);
        writeCache(fresh);
        return toDetail(fresh, loadSchedules(userId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoteDetailVO create(NoteCreateRequest request) {
        Long userId = SecurityUtils.getUserId();
        Long notebookId = request.notebookId();
        if (notebookId != null) {
            requireNotebook(userId, notebookId);
        }
        List<Long> tagIds = normalizeIds(request.tagIds());
        requireTags(userId, tagIds);
        Note note = new Note();
        note.setUserId(userId);
        note.setNotebookId(notebookId);
        note.setTitle(request.title().trim());
        note.setContent(request.content());
        note.setIsPinned(NoteFlags.toInt(Boolean.TRUE.equals(request.isPinned())));
        note.setIsFavorite(NoteFlags.toInt(Boolean.TRUE.equals(request.isFavorite())));
        note.setWordCount(NoteWordCounter.countNonWhitespace(request.content()));
        note.setVersion(1);
        note.setDeleted(Note.ACTIVE);
        noteMapper.insert(note);
        replaceTags(note.getId(), tagIds);
        AfterCommit.run(() -> evictDashboard(userId));
        return assembleActive(note.getId(), userId);
    }

    /**
     * 乐观锁：客户端 version 不一致，或并发更新导致影响行数为 0，都返回 40901。
     * 标题或正文相对库中值有变化时，先把旧内容写入 note_history，再更新笔记。
     * 只改置顶、收藏、标签或笔记本时不写快照。新建笔记不走这里。
     * note_tag 在同一事务里按请求全量替换。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoteDetailVO update(Long id, NoteUpdateRequest request) {
        Long userId = SecurityUtils.getUserId();
        Note existing = requireActiveOwned(id, userId);
        if (!Objects.equals(existing.getVersion(), request.version())) {
            throw new BusinessException(ErrorCode.CONFLICT, VERSION_CONFLICT);
        }
        Long notebookId = request.notebookId();
        if (notebookId != null) {
            requireNotebook(userId, notebookId);
        }
        List<Long> tagIds = normalizeIds(request.tagIds());
        requireTags(userId, tagIds);
        String nextTitle = NoteHistoryRules.persistedTitle(request.title());
        if (NoteHistoryRules.needsSnapshot(existing.getTitle(), existing.getContent(), nextTitle, request.content())) {
            insertSnapshot(id, userId, existing.getVersion(), existing.getTitle(), existing.getContent(),
                    NoteHistoryRules.CHANGE_BEFORE_SAVE);
        }
        LambdaUpdateWrapper<Note> wrapper = new LambdaUpdateWrapper<Note>()
                .eq(Note::getId, id)
                .eq(Note::getUserId, userId)
                .eq(Note::getVersion, request.version())
                .set(Note::getTitle, nextTitle)
                .set(Note::getContent, request.content())
                .set(Note::getWordCount, NoteWordCounter.countNonWhitespace(request.content()))
                .set(Note::getVersion, request.version() + 1);
        assign(wrapper, Note::getNotebookId, "notebook_id", notebookId);
        if (request.isPinned() != null) {
            wrapper.set(Note::getIsPinned, NoteFlags.toInt(request.isPinned()));
        }
        if (request.isFavorite() != null) {
            wrapper.set(Note::getIsFavorite, NoteFlags.toInt(request.isFavorite()));
        }
        int rows = noteMapper.update(wrapper);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, VERSION_CONFLICT);
        }
        replaceTags(id, tagIds);
        AfterCommit.run(() -> {
            evictNote(id);
            evictDashboard(userId);
        });
        return assembleActive(id, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NoteHistoryItemVO> listHistory(Long noteId) {
        Long userId = SecurityUtils.getUserId();
        requireActiveOwned(noteId, userId);
        List<NoteHistory> rows = noteHistoryMapper.selectItemsByNoteId(noteId, NoteHistoryRules.KEEP);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<NoteHistoryItemVO> items = new ArrayList<>(rows.size());
        for (NoteHistory row : rows) {
            items.add(toHistoryItem(row));
        }
        return items;
    }

    @Override
    @Transactional(readOnly = true)
    public NoteHistoryDetailVO historyDetail(Long noteId, Long historyId) {
        Long userId = SecurityUtils.getUserId();
        requireActiveOwned(noteId, userId);
        return toHistoryDetail(requireHistory(noteId, historyId));
    }

    /**
     * 回滚前先把当前标题和正文再存一条，避免覆盖后找不回来。
     * version 按库中当前值加 1，不要求客户端回传旧版本。
     * 并发把版本改掉时整段回滚，并返回 40901。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoteDetailVO restoreHistory(Long noteId, Long historyId) {
        Long userId = SecurityUtils.getUserId();
        Note existing = requireActiveOwned(noteId, userId);
        NoteHistory snapshot = requireHistory(noteId, historyId);
        insertSnapshot(noteId, userId, existing.getVersion(), existing.getTitle(), existing.getContent(),
                NoteHistoryRules.CHANGE_BEFORE_RESTORE);
        LambdaUpdateWrapper<Note> wrapper = new LambdaUpdateWrapper<Note>()
                .eq(Note::getId, noteId)
                .eq(Note::getUserId, userId)
                .eq(Note::getVersion, existing.getVersion())
                .set(Note::getTitle, snapshot.getTitle())
                .set(Note::getContent, snapshot.getContent())
                .set(Note::getWordCount, NoteWordCounter.countNonWhitespace(snapshot.getContent()))
                .set(Note::getVersion, existing.getVersion() + 1);
        int rows = noteMapper.update(wrapper);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, VERSION_CONFLICT);
        }
        AfterCommit.run(() -> {
            evictNote(noteId);
            evictDashboard(userId);
        });
        return assembleActive(noteId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoteFlagVO pin(Long id, Boolean isPinned) {
        return updateFlag(id, isPinned, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoteFlagVO favorite(Long id, Boolean isFavorite) {
        return updateFlag(id, isFavorite, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveToTrash(Long id) {
        Long userId = SecurityUtils.getUserId();
        requireActiveOwned(id, userId);
        int rows = noteMapper.moveToTrash(id, userId);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "笔记不存在");
        }
        AfterCommit.run(() -> {
            evictNote(id);
            evictDashboard(userId);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<NoteListItemVO> trash(long pageNum, long pageSize) {
        Long userId = SecurityUtils.getUserId();
        long current = NotePageQuery.normalizePageNum(pageNum);
        long size = NotePageQuery.normalizePageSize(pageSize);
        long total = noteMapper.countTrash(userId);
        if (total == 0) {
            return PageResult.empty(current, size);
        }
        List<Note> notes = noteMapper.selectTrashPage(userId, NotePageQuery.offset(current, size), size);
        return new PageResult<>(toListItems(userId, notes), total, current, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoteDetailVO restore(Long id) {
        Long userId = SecurityUtils.getUserId();
        requireTrashedOwned(id, userId);
        int rows = noteMapper.restoreFromTrash(id, userId);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "笔记不存在");
        }
        AfterCommit.run(() -> {
            evictNote(id);
            evictDashboard(userId);
        });
        return assembleActive(id, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePermanent(Long id) {
        Long userId = SecurityUtils.getUserId();
        requireTrashedOwned(id, userId);
        noteTagMapper.deleteByNoteId(id);
        int rows = noteMapper.deletePermanent(id, userId);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "笔记不存在");
        }
        AfterCommit.run(() -> {
            evictNote(id);
            evictDashboard(userId);
        });
    }

    private NoteFlagVO updateFlag(Long id, Boolean requested, boolean pin) {
        Long userId = SecurityUtils.getUserId();
        Note note = requireActiveOwned(id, userId);
        boolean current = pin ? NoteFlags.isOn(note.getIsPinned()) : NoteFlags.isOn(note.getIsFavorite());
        boolean next = NoteFlags.resolve(requested, current);
        LambdaUpdateWrapper<Note> wrapper = new LambdaUpdateWrapper<Note>()
                .eq(Note::getId, id)
                .eq(Note::getUserId, userId);
        if (pin) {
            wrapper.set(Note::getIsPinned, NoteFlags.toInt(next));
        } else {
            wrapper.set(Note::getIsFavorite, NoteFlags.toInt(next));
        }
        int rows = noteMapper.update(wrapper);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "笔记不存在");
        }
        AfterCommit.run(() -> evictNote(id));
        Note fresh = noteMapper.selectById(id);
        if (fresh == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "笔记不存在");
        }
        return new NoteFlagVO(fresh.getId(), NoteFlags.isOn(fresh.getIsPinned()), NoteFlags.isOn(fresh.getIsFavorite()));
    }

    private void insertSnapshot(Long noteId, Long userId, Integer versionNo, String title, String content,
                                String changeNote) {
        NoteHistory row = new NoteHistory();
        row.setNoteId(noteId);
        row.setUserId(userId);
        row.setVersionNo(versionNo);
        row.setTitle(title == null ? "" : title);
        row.setContent(content == null ? "" : content);
        row.setChangeNote(changeNote);
        noteHistoryMapper.insert(row);
        trimHistory(noteId);
    }

    private void trimHistory(Long noteId) {
        List<Long> extras = NoteHistoryRules.idsBeyondKeep(noteHistoryMapper.selectIdsByNoteIdDesc(noteId));
        if (extras.isEmpty()) {
            return;
        }
        noteHistoryMapper.delete(new LambdaQueryWrapper<NoteHistory>().in(NoteHistory::getId, extras));
    }

    private NoteHistory requireHistory(Long noteId, Long historyId) {
        NoteHistory row = noteHistoryMapper.selectById(historyId);
        if (row == null || !noteId.equals(row.getNoteId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "历史版本不存在");
        }
        return row;
    }

    private NoteHistoryItemVO toHistoryItem(NoteHistory row) {
        return new NoteHistoryItemVO(
                row.getId(),
                row.getNoteId(),
                row.getVersionNo(),
                row.getTitle(),
                row.getChangeNote(),
                row.getCreatedAt()
        );
    }

    private NoteHistoryDetailVO toHistoryDetail(NoteHistory row) {
        return new NoteHistoryDetailVO(
                row.getId(),
                row.getNoteId(),
                row.getVersionNo(),
                row.getTitle(),
                row.getChangeNote(),
                row.getCreatedAt(),
                row.getContent()
        );
    }

    private NoteDetailVO assembleActive(Long noteId, Long userId) {
        Note note = requireActiveOwned(noteId, userId);
        return toDetail(buildCore(note), loadSchedules(userId, noteId));
    }

    private Note requireActiveOwned(Long id, Long userId) {
        Note note = requireVisible(id, userId);
        if (note.inTrash()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "笔记不存在");
        }
        return note;
    }

    private Note requireTrashedOwned(Long id, Long userId) {
        Note note = requireVisible(id, userId);
        if (!note.inTrash()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "笔记不在回收站");
        }
        return note;
    }

    private Note requireVisible(Long id, Long userId) {
        Note note = noteMapper.selectRawById(id);
        if (note == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "笔记不存在");
        }
        if (!userId.equals(note.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该笔记");
        }
        return note;
    }

    private void requireNotebook(Long userId, Long notebookId) {
        Notebook notebook = notebookMapper.selectById(notebookId);
        if (notebook == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "笔记本不存在");
        }
        if (!userId.equals(notebook.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该笔记本");
        }
    }

    private void requireTags(Long userId, List<Long> tagIds) {
        if (tagIds.isEmpty()) {
            return;
        }
        List<Tag> tags = tagMapper.selectList(new LambdaQueryWrapper<Tag>().in(Tag::getId, tagIds));
        Map<Long, Tag> byId = new HashMap<>();
        for (Tag tag : tags) {
            byId.put(tag.getId(), tag);
        }
        for (Long tagId : tagIds) {
            Tag tag = byId.get(tagId);
            if (tag == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "标签不存在");
            }
            if (!userId.equals(tag.getUserId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该标签");
            }
        }
    }

    private void replaceTags(Long noteId, List<Long> tagIds) {
        noteTagMapper.deleteByNoteId(noteId);
        for (Long tagId : tagIds) {
            noteTagMapper.insertLink(noteId, tagId);
        }
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Set<Long> distinct = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id != null) {
                distinct.add(id);
            }
        }
        return List.copyOf(distinct);
    }

    private NoteCoreCache buildCore(Note note) {
        String notebookName = null;
        if (note.getNotebookId() != null) {
            Notebook notebook = notebookMapper.selectById(note.getNotebookId());
            if (notebook != null && note.getUserId().equals(notebook.getUserId())) {
                notebookName = notebook.getName();
            }
        }
        List<TagSimpleVO> tags = loadTags(List.of(note.getId())).getOrDefault(note.getId(), List.of());
        return new NoteCoreCache(
                note.getId(),
                note.getUserId(),
                note.getNotebookId(),
                notebookName,
                note.getTitle(),
                note.getContent(),
                note.getAiSummary(),
                NoteFlags.isOn(note.getIsPinned()),
                NoteFlags.isOn(note.getIsFavorite()),
                note.getWordCount(),
                note.getVersion(),
                List.copyOf(tags),
                note.getDeletedAt(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }

    private List<NoteListItemVO> toListItems(Long userId, List<Note> notes) {
        if (notes == null || notes.isEmpty()) {
            return List.of();
        }
        List<Long> noteIds = new ArrayList<>(notes.size());
        Set<Long> notebookIds = new LinkedHashSet<>();
        for (Note note : notes) {
            noteIds.add(note.getId());
            if (note.getNotebookId() != null) {
                notebookIds.add(note.getNotebookId());
            }
        }
        Map<Long, String> notebookNames = loadNotebookNames(userId, notebookIds);
        Map<Long, List<TagSimpleVO>> tags = loadTags(noteIds);
        List<NoteListItemVO> items = new ArrayList<>(notes.size());
        for (Note note : notes) {
            items.add(new NoteListItemVO(
                    note.getId(),
                    note.getNotebookId(),
                    NotebookNames.lookup(notebookNames, note.getNotebookId()),
                    note.getTitle(),
                    note.getAiSummary(),
                    NoteFlags.isOn(note.getIsPinned()),
                    NoteFlags.isOn(note.getIsFavorite()),
                    note.getWordCount(),
                    note.getVersion(),
                    tags.getOrDefault(note.getId(), List.of()),
                    note.getDeletedAt(),
                    note.getCreatedAt(),
                    note.getUpdatedAt()
            ));
        }
        return items;
    }

    private Map<Long, String> loadNotebookNames(Long userId, Collection<Long> notebookIds) {
        if (notebookIds == null || notebookIds.isEmpty()) {
            return Map.of();
        }
        List<Notebook> notebooks = notebookMapper.selectList(new LambdaQueryWrapper<Notebook>()
                .eq(Notebook::getUserId, userId)
                .in(Notebook::getId, notebookIds));
        Map<Long, String> names = new HashMap<>();
        for (Notebook notebook : notebooks) {
            names.put(notebook.getId(), notebook.getName());
        }
        return names;
    }

    private Map<Long, List<TagSimpleVO>> loadTags(Collection<Long> noteIds) {
        if (noteIds == null || noteIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<TagSimpleVO>> grouped = new HashMap<>();
        for (NoteTagRow row : noteTagMapper.selectTagsByNoteIds(noteIds)) {
            grouped.computeIfAbsent(row.getNoteId(), key -> new ArrayList<>())
                    .add(new TagSimpleVO(row.getId(), row.getName(), row.getColor()));
        }
        return grouped;
    }

    private List<ScheduleBriefVO> loadSchedules(Long userId, Long noteId) {
        List<ScheduleBriefVO> schedules = scheduleLinkQueryService.listByNoteId(userId, noteId);
        return schedules == null ? List.of() : schedules;
    }

    private NoteDetailVO toDetail(NoteCoreCache cache, List<ScheduleBriefVO> schedules) {
        List<TagSimpleVO> tags = cache.tags() == null ? List.of() : cache.tags();
        return new NoteDetailVO(
                cache.id(),
                cache.notebookId(),
                cache.notebookName(),
                cache.title(),
                cache.aiSummary(),
                cache.isPinned(),
                cache.isFavorite(),
                cache.wordCount(),
                cache.version(),
                tags,
                cache.deletedAt(),
                cache.createdAt(),
                cache.updatedAt(),
                cache.content(),
                schedules
        );
    }

    private NoteCoreCache readCache(Long noteId) {
        String key = RedisKeys.noteDetail(noteId);
        String json;
        try {
            json = stringRedisTemplate.opsForValue().get(key);
        } catch (RuntimeException ex) {
            log.warn("读取笔记缓存失败，回退 MySQL noteId={}", noteId, ex);
            return null;
        }
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            NoteCoreCache cache = objectMapper.readValue(json, NoteCoreCache.class);
            if (cache == null || cache.id() == null || cache.userId() == null || cache.tags() == null) {
                return null;
            }
            return cache;
        } catch (Exception ex) {
            log.warn("笔记缓存反序列化失败，回退 MySQL noteId={}", noteId, ex);
            evictNote(noteId);
            return null;
        }
    }

    private void writeCache(NoteCoreCache cache) {
        try {
            stringRedisTemplate.opsForValue().set(
                    RedisKeys.noteDetail(cache.id()),
                    objectMapper.writeValueAsString(cache),
                    NOTE_CACHE_TTL
            );
        } catch (Exception ex) {
            log.warn("写入笔记缓存失败 noteId={}", cache.id(), ex);
        }
    }

    private void evictNote(Long noteId) {
        try {
            stringRedisTemplate.delete(RedisKeys.noteDetail(noteId));
        } catch (RuntimeException ex) {
            log.warn("失效笔记缓存失败 noteId={}", noteId, ex);
        }
    }

    private void evictDashboard(Long userId) {
        try {
            stringRedisTemplate.delete(RedisKeys.dashboardOverview(userId));
        } catch (RuntimeException ex) {
            log.warn("失效看板缓存失败 userId={}", userId, ex);
        }
    }

    private static <T> void assign(LambdaUpdateWrapper<T> wrapper,
                                   com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, ?> column,
                                   String columnName,
                                   Object value) {
        if (value == null) {
            wrapper.setSql(columnName + " = NULL");
        } else {
            wrapper.set(column, value);
        }
    }
}
