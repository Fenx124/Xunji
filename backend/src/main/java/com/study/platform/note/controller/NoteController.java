package com.study.platform.note.controller;

import com.study.platform.common.PageResult;
import com.study.platform.common.Result;
import com.study.platform.common.audit.AuditLog;
import com.study.platform.note.dto.NoteCreateRequest;
import com.study.platform.note.dto.NoteFavoriteRequest;
import com.study.platform.note.dto.NotePinRequest;
import com.study.platform.note.dto.NoteUpdateRequest;
import com.study.platform.note.service.NoteService;
import com.study.platform.note.vo.NoteDetailVO;
import com.study.platform.note.vo.NoteFlagVO;
import com.study.platform.note.vo.NoteListItemVO;
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

@Tag(name = "笔记")
@RestController
@RequestMapping("/api/v1/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @Operation(summary = "分页查询笔记")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<PageResult<NoteListItemVO>> page(
            @RequestParam(required = false) Long notebookId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isFavorite,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize) {
        return Result.ok(noteService.page(notebookId, tagId, keyword, isFavorite, pageNum, pageSize));
    }

    @Operation(summary = "回收站")
    @GetMapping(value = "/trash", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<PageResult<NoteListItemVO>> trash(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize) {
        return Result.ok(noteService.trash(pageNum, pageSize));
    }

    @Operation(summary = "笔记详情")
    @GetMapping(value = "/{id:\\d+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<NoteDetailVO> detail(@PathVariable Long id) {
        return Result.ok(noteService.detail(id));
    }

    @Operation(summary = "新建笔记")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<NoteDetailVO> create(@Valid @RequestBody NoteCreateRequest request) {
        return Result.ok(noteService.create(request));
    }

    @Operation(summary = "更新笔记")
    @PutMapping(value = "/{id:\\d+}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<NoteDetailVO> update(@PathVariable Long id, @Valid @RequestBody NoteUpdateRequest request) {
        return Result.ok(noteService.update(id, request));
    }

    @Operation(summary = "切换或设置置顶")
    @PatchMapping(value = "/{id:\\d+}/pin", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<NoteFlagVO> pin(@PathVariable Long id,
                                  @RequestBody(required = false) NotePinRequest request) {
        Boolean pinned = request == null ? null : request.isPinned();
        return Result.ok(noteService.pin(id, pinned));
    }

    @Operation(summary = "切换或设置收藏")
    @PatchMapping(value = "/{id:\\d+}/favorite", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<NoteFlagVO> favorite(@PathVariable Long id,
                                       @RequestBody(required = false) NoteFavoriteRequest request) {
        Boolean favorite = request == null ? null : request.isFavorite();
        return Result.ok(noteService.favorite(id, favorite));
    }

    @Operation(summary = "移入回收站")
    @AuditLog(module = "NOTE", action = "移入回收站")
    @DeleteMapping(value = "/{id:\\d+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Void> moveToTrash(@PathVariable Long id) {
        noteService.moveToTrash(id);
        return Result.ok();
    }

    @Operation(summary = "从回收站恢复")
    @PostMapping(value = "/{id:\\d+}/restore", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<NoteDetailVO> restore(@PathVariable Long id) {
        return Result.ok(noteService.restore(id));
    }

    @Operation(summary = "彻底删除回收站中的笔记")
    @AuditLog(module = "NOTE", action = "彻底删除笔记")
    @DeleteMapping(value = "/{id:\\d+}/permanent", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Void> deletePermanent(@PathVariable Long id) {
        noteService.deletePermanent(id);
        return Result.ok();
    }
}
