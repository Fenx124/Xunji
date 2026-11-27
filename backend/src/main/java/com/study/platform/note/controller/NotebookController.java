package com.study.platform.note.controller;

import com.study.platform.common.Result;
import com.study.platform.note.dto.NotebookSaveRequest;
import com.study.platform.note.service.NotebookService;
import com.study.platform.note.vo.NotebookVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "笔记本")
@RestController
@RequestMapping("/api/v1/notebooks")
public class NotebookController {

    private final NotebookService notebookService;

    public NotebookController(NotebookService notebookService) {
        this.notebookService = notebookService;
    }

    @Operation(summary = "当前用户的笔记本")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<List<NotebookVO>> list() {
        return Result.ok(notebookService.list());
    }

    @Operation(summary = "新建笔记本")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<NotebookVO> create(@Valid @RequestBody NotebookSaveRequest request) {
        return Result.ok(notebookService.create(request));
    }

    @Operation(summary = "修改笔记本")
    @PutMapping(value = "/{id:\\d+}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<NotebookVO> update(@PathVariable Long id, @Valid @RequestBody NotebookSaveRequest request) {
        return Result.ok(notebookService.update(id, request));
    }

    @Operation(summary = "删除笔记本，其下笔记改为未归类")
    @DeleteMapping(value = "/{id:\\d+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Void> delete(@PathVariable Long id) {
        notebookService.delete(id);
        return Result.ok();
    }
}
