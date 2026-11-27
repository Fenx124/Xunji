package com.study.platform.focus.controller;

import com.study.platform.common.PageResult;
import com.study.platform.common.Result;
import com.study.platform.common.audit.AuditLog;
import com.study.platform.focus.dto.FocusRecordCreateRequest;
import com.study.platform.focus.service.FocusService;
import com.study.platform.focus.vo.FocusRecordVO;
import com.study.platform.focus.vo.FocusTodayStatsVO;
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

@Tag(name = "番茄钟")
@RestController
@RequestMapping("/api/v1/focus")
public class FocusController {

    private final FocusService focusService;

    public FocusController(FocusService focusService) {
        this.focusService = focusService;
    }

    @Operation(summary = "上报一次专注记录")
    @AuditLog(module = "FOCUS", action = "上报专注记录")
    @PostMapping(value = "/records", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<FocusRecordVO> create(@Valid @RequestBody FocusRecordCreateRequest request) {
        return Result.ok(focusService.create(request));
    }

    @Operation(summary = "分页查询专注记录")
    @GetMapping(value = "/records", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<PageResult<FocusRecordVO>> page(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize) {
        return Result.ok(focusService.page(startDate, endDate, pageNum, pageSize));
    }

    @Operation(summary = "今日专注统计")
    @GetMapping(value = "/stats/today", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<FocusTodayStatsVO> today() {
        return Result.ok(focusService.today());
    }
}
