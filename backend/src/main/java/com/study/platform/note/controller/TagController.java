package com.study.platform.note.controller;

import com.study.platform.common.Result;
import com.study.platform.note.dto.TagSaveRequest;
import com.study.platform.note.service.TagService;
import com.study.platform.note.vo.TagVO;
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

@Tag(name = "标签")
@RestController
@RequestMapping("/api/v1/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @Operation(summary = "当前用户的标签")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<List<TagVO>> list() {
        return Result.ok(tagService.list());
    }

    @Operation(summary = "新建标签")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<TagVO> create(@Valid @RequestBody TagSaveRequest request) {
        return Result.ok(tagService.create(request));
    }

    @Operation(summary = "修改标签")
    @PutMapping(value = "/{id:\\d+}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<TagVO> update(@PathVariable Long id, @Valid @RequestBody TagSaveRequest request) {
        return Result.ok(tagService.update(id, request));
    }

    @Operation(summary = "删除标签并解除笔记关联")
    @DeleteMapping(value = "/{id:\\d+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return Result.ok();
    }
}
