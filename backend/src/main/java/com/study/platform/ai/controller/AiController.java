package com.study.platform.ai.controller;

import com.study.platform.ai.dto.AiParseScheduleRequest;
import com.study.platform.ai.dto.AiQaRequest;
import com.study.platform.ai.dto.AiSummarizeRequest;
import com.study.platform.ai.ratelimit.AiRateLimit;
import com.study.platform.ai.service.AiService;
import com.study.platform.ai.vo.AiChatLogVO;
import com.study.platform.ai.vo.AiParsedScheduleVO;
import com.study.platform.ai.vo.AiSummarizeVO;
import com.study.platform.common.PageResult;
import com.study.platform.common.Result;
import com.study.platform.common.audit.AuditLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "AI 学习助手")
@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @Operation(summary = "笔记智能摘要与标签推荐")
    @AuditLog(module = "AI", action = "笔记AI摘要")
    @AiRateLimit
    @PostMapping(value = "/summarize", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<AiSummarizeVO> summarize(@Valid @RequestBody AiSummarizeRequest request) {
        return Result.ok(aiService.summarize(request.noteId()));
    }

    @Operation(summary = "笔记流式问答")
    @AiRateLimit
    @PostMapping(value = "/qa/stream", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter qa(@Valid @RequestBody AiQaRequest request) {
        return aiService.streamQa(request.noteId(), request.question());
    }

    @Operation(summary = "自然语言解析日程")
    @AuditLog(module = "AI", action = "AI解析日程")
    @AiRateLimit
    @PostMapping(value = "/parse-schedule", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<AiParsedScheduleVO> parseSchedule(@Valid @RequestBody AiParseScheduleRequest request) {
        return Result.ok(aiService.parseSchedule(request.text()));
    }

    @Operation(summary = "分页查看当前用户的 AI 交互历史")
    @GetMapping(value = "/logs", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<PageResult<AiChatLogVO>> logs(
            @RequestParam(required = false) Long noteId,
            @RequestParam(required = false) String featureType,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize) {
        return Result.ok(aiService.logs(noteId, featureType, pageNum, pageSize));
    }
}
