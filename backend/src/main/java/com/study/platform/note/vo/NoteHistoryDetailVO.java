package com.study.platform.note.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "笔记历史快照全文")
public record NoteHistoryDetailVO(
        @Schema(description = "快照 ID")
        Long id,
        @Schema(description = "笔记 ID")
        Long noteId,
        @Schema(description = "被替换掉的笔记版本号")
        Integer versionNo,
        @Schema(description = "快照标题")
        String title,
        @Schema(description = "变更备注")
        String changeNote,
        @Schema(description = "快照时间")
        LocalDateTime createdAt,
        @Schema(description = "快照 Markdown 正文")
        String content
) {
}
