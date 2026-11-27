package com.study.platform.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.study.platform.ai.config.AiProperties;
import com.study.platform.ai.entity.AiChatLog;
import com.study.platform.ai.mapper.AiChatLogMapper;
import com.study.platform.ai.provider.AiProviderRouter;
import com.study.platform.ai.provider.RoutedAiResult;
import com.study.platform.ai.support.AiAnswerParser;
import com.study.platform.ai.support.AiPrompts;
import com.study.platform.ai.support.AiText;
import com.study.platform.ai.support.ParsedScheduleDraft;
import com.study.platform.ai.support.SummaryDraft;
import com.study.platform.ai.vo.AiChatLogVO;
import com.study.platform.ai.vo.AiParsedScheduleVO;
import com.study.platform.ai.vo.AiSummarizeVO;
import com.study.platform.auth.security.SecurityUtils;
import com.study.platform.common.ErrorCode;
import com.study.platform.common.PageQuery;
import com.study.platform.common.PageResult;
import com.study.platform.common.StudyDates;
import com.study.platform.common.exception.BusinessException;
import com.study.platform.note.service.NoteAiAccessService;
import com.study.platform.note.service.NoteAiSnapshot;
import com.study.platform.note.service.NoteLinkQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);
    private static final long SSE_TIMEOUT_MS = 60_000L;
    private static final int PROMPT_LIMIT = 15000;
    private static final int RESPONSE_LIMIT = 20000;
    private static final int ERROR_LIMIT = 500;

    private final AiProviderRouter router;
    private final AiProperties properties;
    private final NoteAiAccessService noteAiAccessService;
    private final NoteLinkQueryService noteLinkQueryService;
    private final AiChatLogMapper chatLogMapper;
    private final Executor aiStreamExecutor;

    public AiServiceImpl(AiProviderRouter router,
                         AiProperties properties,
                         NoteAiAccessService noteAiAccessService,
                         NoteLinkQueryService noteLinkQueryService,
                         AiChatLogMapper chatLogMapper,
                         @Qualifier("aiStreamExecutor") Executor aiStreamExecutor) {
        this.router = router;
        this.properties = properties;
        this.noteAiAccessService = noteAiAccessService;
        this.noteLinkQueryService = noteLinkQueryService;
        this.chatLogMapper = chatLogMapper;
        this.aiStreamExecutor = aiStreamExecutor;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiSummarizeVO summarize(Long noteId) {
        Long userId = SecurityUtils.getUserId();
        NoteAiSnapshot note = noteAiAccessService.requireOwned(userId, noteId);
        long started = System.currentTimeMillis();
        String userPrompt = AiPrompts.summaryUser(note.title(), note.content());
        RoutedAiResult routed = router.chat(AiPrompts.summarySystem(), userPrompt);
        SummaryDraft draft = AiAnswerParser.parseSummary(routed.content(), note.title(), note.content());
        noteAiAccessService.saveSummary(userId, note.id(), draft.summary());
        int latency = latencyOf(started);
        insertLog(userId, note.id(), AiChatLog.FEATURE_SUMMARY, routed, userPrompt, draft.summary(), latency, true, null);
        return new AiSummarizeVO(note.id(), draft.summary(), draft.tags(), routed.fallback(), latency);
    }

    @Override
    public SseEmitter streamQa(Long noteId, String question) {
        Long userId = SecurityUtils.getUserId();
        String trimmed = question == null ? "" : question.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "问题不能为空");
        }
        if (trimmed.length() > 2000) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "问题长度不能超过 2000");
        }
        // 归属校验放在返回 SseEmitter 之前，越权时直接走 JSON 错误体。
        NoteAiSnapshot note = null;
        if (noteId != null) {
            note = noteAiAccessService.requireOwned(userId, noteId);
        }
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        Object lock = new Object();
        AtomicBoolean closed = new AtomicBoolean(false);
        emitter.onTimeout(() -> finish(emitter, lock, closed));
        emitter.onError(ex -> closed.set(true));
        emitter.onCompletion(() -> closed.set(true));
        NoteAiSnapshot snapshot = note;
        try {
            aiStreamExecutor.execute(() -> produceQa(userId, snapshot, trimmed, emitter, lock, closed));
        } catch (RejectedExecutionException ex) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, AiProviderRouter.UNAVAILABLE_MESSAGE);
        }
        return emitter;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiParsedScheduleVO parseSchedule(String text) {
        Long userId = SecurityUtils.getUserId();
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "日程描述不能为空");
        }
        if (trimmed.length() > 500) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "日程描述长度不能超过 500");
        }
        LocalDateTime now = StudyDates.now();
        long started = System.currentTimeMillis();
        String userPrompt = AiPrompts.scheduleUser(trimmed);
        RoutedAiResult routed = router.chat(AiPrompts.scheduleSystem(now), userPrompt);
        ParsedScheduleDraft draft = AiAnswerParser.parseSchedule(routed.content(), trimmed, now);
        int latency = latencyOf(started);
        insertLog(userId, null, AiChatLog.FEATURE_SCHEDULE_PARSE, routed, userPrompt, routed.content(), latency, true, null);
        return new AiParsedScheduleVO(
                draft.title(),
                draft.description(),
                draft.startTime(),
                draft.endTime(),
                draft.remindTime(),
                draft.priority(),
                routed.fallback()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AiChatLogVO> logs(Long noteId, String featureType, long pageNum, long pageSize) {
        Long userId = SecurityUtils.getUserId();
        long current = PageQuery.normalizePageNum(pageNum);
        long size = PageQuery.normalizePageSize(pageSize);
        LambdaQueryWrapper<AiChatLog> wrapper = new LambdaQueryWrapper<AiChatLog>()
                .eq(AiChatLog::getUserId, userId)
                .eq(noteId != null, AiChatLog::getNoteId, noteId)
                .orderByDesc(AiChatLog::getCreatedAt)
                .orderByDesc(AiChatLog::getId);
        if (featureType != null && !featureType.isBlank()) {
            wrapper.eq(AiChatLog::getFeatureType, featureType.trim().toUpperCase(Locale.ROOT));
        }
        Page<AiChatLog> page = chatLogMapper.selectPage(new Page<>(current, size), wrapper);
        List<AiChatLog> records = page.getRecords() == null ? List.of() : page.getRecords();
        return new PageResult<>(toVos(userId, records), page.getTotal(), current, size);
    }

    private void produceQa(Long userId,
                           NoteAiSnapshot note,
                           String question,
                           SseEmitter emitter,
                           Object lock,
                           AtomicBoolean closed) {
        long started = System.currentTimeMillis();
        StringBuilder answer = new StringBuilder();
        String userPrompt = AiPrompts.qaUser(
                note == null ? null : note.title(),
                note == null ? null : note.content(),
                question
        );
        try {
            RoutedAiResult routed = router.streamChat(AiPrompts.qaSystem(), userPrompt, (chunk, fallback) -> {
                answer.append(chunk);
                Map<String, Object> frame = new LinkedHashMap<>();
                frame.put("chunk", chunk);
                frame.put("done", false);
                frame.put("fallback", fallback);
                send(emitter, lock, closed, frame);
            });
            int latency = latencyOf(started);
            String response = answer.isEmpty() ? routed.content() : answer.toString();
            Long logId = tryInsert(userId, note == null ? null : note.id(), AiChatLog.FEATURE_NOTE_QA,
                    routed, question, response, latency, true, null);
            Map<String, Object> done = new LinkedHashMap<>();
            done.put("chunk", "");
            done.put("done", true);
            done.put("fallback", routed.fallback());
            if (logId != null) {
                done.put("logId", logId);
            }
            done.put("latencyMs", latency);
            send(emitter, lock, closed, done);
            finish(emitter, lock, closed);
        } catch (BusinessException ex) {
            int latency = latencyOf(started);
            String message = ex.getMessage() == null ? AiProviderRouter.UNAVAILABLE_MESSAGE : ex.getMessage();
            tryInsert(userId, note == null ? null : note.id(), AiChatLog.FEATURE_NOTE_QA,
                    failedRoute(), question, answer.toString(), latency, false, message);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("chunk", "");
            error.put("done", true);
            error.put("error", message);
            error.put("code", ex.getErrorCode().getCode());
            send(emitter, lock, closed, error);
            finish(emitter, lock, closed);
        } catch (RuntimeException ex) {
            log.warn("AI 流式问答失败 userId={}", userId, ex);
            int latency = latencyOf(started);
            tryInsert(userId, note == null ? null : note.id(), AiChatLog.FEATURE_NOTE_QA,
                    failedRoute(), question, answer.toString(), latency, false, AiProviderRouter.UNAVAILABLE_MESSAGE);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("chunk", "");
            error.put("done", true);
            error.put("error", AiProviderRouter.UNAVAILABLE_MESSAGE);
            error.put("code", ErrorCode.SERVICE_UNAVAILABLE.getCode());
            send(emitter, lock, closed, error);
            finish(emitter, lock, closed);
        }
    }

    private RoutedAiResult failedRoute() {
        String provider = properties.getProvider() == null || properties.getProvider().isBlank()
                ? "deepseek" : properties.getProvider().trim();
        String model = properties.getModel() == null || properties.getModel().isBlank()
                ? "deepseek-chat" : properties.getModel().trim();
        return new RoutedAiResult("", 0, 0, false, provider, model);
    }

    private Long tryInsert(Long userId,
                           Long noteId,
                           String featureType,
                           RoutedAiResult routed,
                           String prompt,
                           String response,
                           int latency,
                           boolean success,
                           String errorMessage) {
        try {
            return insertLog(userId, noteId, featureType, routed, prompt, response, latency, success, errorMessage);
        } catch (RuntimeException ex) {
            log.warn("写入 AI 日志失败 userId={} feature={}", userId, featureType, ex);
            return null;
        }
    }

    private Long insertLog(Long userId,
                           Long noteId,
                           String featureType,
                           RoutedAiResult routed,
                           String prompt,
                           String response,
                           int latency,
                           boolean success,
                           String errorMessage) {
        AiChatLog row = new AiChatLog();
        row.setUserId(userId);
        row.setNoteId(noteId);
        row.setFeatureType(featureType);
        row.setProvider(AiText.clamp(routed.provider(), 30));
        row.setModelName(AiText.clamp(routed.modelName(), 60));
        row.setPromptText(AiText.emptyTo(AiText.clamp(prompt, PROMPT_LIMIT), "（空输入）"));
        String responseText = response == null || response.isBlank() ? null : AiText.clamp(response, RESPONSE_LIMIT);
        row.setResponseText(responseText);
        row.setPromptTokens(routed.promptTokens());
        row.setCompletionTokens(routed.completionTokens());
        row.setLatencyMs(Math.max(latency, 0));
        row.setSuccess(success ? AiChatLog.SUCCESS : AiChatLog.FAILED);
        if (errorMessage == null || errorMessage.isBlank()) {
            row.setErrorMessage(null);
        } else {
            row.setErrorMessage(AiText.clamp(errorMessage, ERROR_LIMIT));
        }
        row.setCreatedAt(StudyDates.now());
        chatLogMapper.insert(row);
        return row.getId();
    }

    private List<AiChatLogVO> toVos(Long userId, List<AiChatLog> records) {
        Set<Long> noteIds = new HashSet<>();
        for (AiChatLog record : records) {
            if (record.getNoteId() != null) {
                noteIds.add(record.getNoteId());
            }
        }
        Map<Long, String> titles = noteLinkQueryService.findTitles(userId, noteIds);
        List<AiChatLogVO> result = new ArrayList<>(records.size());
        for (AiChatLog record : records) {
            String response = record.getResponseText() == null ? "" : record.getResponseText();
            String prompt = record.getPromptText() == null ? "" : record.getPromptText();
            result.add(new AiChatLogVO(
                    record.getId(),
                    record.getNoteId(),
                    titles.get(record.getNoteId()),
                    record.getFeatureType(),
                    record.getProvider(),
                    record.getModelName(),
                    prompt,
                    response,
                    record.getPromptTokens(),
                    record.getCompletionTokens(),
                    record.getLatencyMs(),
                    record.getSuccess() != null && record.getSuccess() == AiChatLog.SUCCESS,
                    record.getErrorMessage(),
                    record.getCreatedAt()
            ));
        }
        return result;
    }

    private static void send(SseEmitter emitter, Object lock, AtomicBoolean closed, Map<String, Object> payload) {
        if (closed.get()) {
            return;
        }
        synchronized (lock) {
            if (closed.get()) {
                return;
            }
            try {
                emitter.send(SseEmitter.event().data(payload, MediaType.APPLICATION_JSON));
            } catch (Exception ex) {
                closed.set(true);
                log.debug("SSE 发送中断: {}", ex.getMessage());
            }
        }
    }

    private static void finish(SseEmitter emitter, Object lock, AtomicBoolean closed) {
        synchronized (lock) {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            try {
                emitter.complete();
            } catch (Exception ex) {
                log.debug("结束 SSE 失败: {}", ex.getMessage());
            }
        }
    }

    private static int latencyOf(long started) {
        long cost = System.currentTimeMillis() - started;
        if (cost < 0) {
            return 0;
        }
        if (cost > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) cost;
    }
}
