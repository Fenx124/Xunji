package com.study.platform.note.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.study.platform.note.vo.TagSimpleVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 笔记详情的 Cache-Aside 载荷，不含关联日程。
 * 日程由 ScheduleLinkQueryService 每次现查，避免日程模块更新后笔记缓存仍是旧关联。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NoteCoreCache(
        Long id,
        Long userId,
        Long notebookId,
        String notebookName,
        String title,
        String content,
        String aiSummary,
        @JsonProperty("isPinned")
        boolean isPinned,
        @JsonProperty("isFavorite")
        boolean isFavorite,
        Integer wordCount,
        Integer version,
        List<TagSimpleVO> tags,
        LocalDateTime deletedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
