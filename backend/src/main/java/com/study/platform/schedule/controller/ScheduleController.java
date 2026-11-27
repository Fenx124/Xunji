package com.study.platform.schedule.controller;

import com.study.platform.common.Result;
import com.study.platform.common.audit.AuditLog;
import com.study.platform.schedule.dto.EbbinghausRequest;
import com.study.platform.schedule.dto.ScheduleCreateRequest;
import com.study.platform.schedule.dto.ScheduleStatusRequest;
import com.study.platform.schedule.dto.ScheduleUpdateRequest;
import com.study.platform.schedule.service.ScheduleService;
import com.study.platform.schedule.vo.ScheduleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "日程")
@RestController
@RequestMapping("/api/v1/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @Operation(summary = "按时间或筛选条件查询日程")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<List<ScheduleVO>> list(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer priority,
            @RequestParam(required = false) Long noteId) {
        return Result.ok(scheduleService.list(startDate, endDate, status, priority, noteId));
    }

    @Operation(summary = "未来 24 小时内或已过提醒时间的未完成日程")
    @GetMapping(value = "/reminders/upcoming", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<List<ScheduleVO>> upcoming() {
        return Result.ok(scheduleService.upcomingReminders());
    }

    @Operation(summary = "新建日程")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<ScheduleVO> create(@Valid @RequestBody ScheduleCreateRequest request) {
        return Result.ok(scheduleService.create(request));
    }

    @Operation(summary = "按艾宾浩斯曲线生成 5 条复习日程")
    @AuditLog(module = "SCHEDULE", action = "生成艾宾浩斯复习日程")
    @PostMapping(value = "/ebbinghaus", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<List<ScheduleVO>> ebbinghaus(@Valid @RequestBody EbbinghausRequest request) {
        return Result.ok(scheduleService.createEbbinghaus(request));
    }

    @Operation(summary = "更新日程")
    @PutMapping(value = "/{id:\\d+}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<ScheduleVO> update(@PathVariable Long id, @Valid @RequestBody ScheduleUpdateRequest request) {
        return Result.ok(scheduleService.update(id, request));
    }

    @Operation(summary = "切换日程状态")
    @PatchMapping(value = "/{id:\\d+}/status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<ScheduleVO> updateStatus(@PathVariable Long id, @Valid @RequestBody ScheduleStatusRequest request) {
        return Result.ok(scheduleService.updateStatus(id, request.status()));
    }

    @Operation(summary = "删除日程")
    @DeleteMapping(value = "/{id:\\d+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Void> delete(@PathVariable Long id) {
        scheduleService.delete(id);
        return Result.ok();
    }
}
