package com.study.platform.note.support;

import java.util.Map;

/**
 * 笔记列表里的笔记本名称查找。
 * 未归类笔记的 notebookId 为 null。JDK 的 {@code Map.of()} / {@code Map.copyOf()} 对 null key 会直接抛 NPE，
 * 回收站如果只剩这类笔记，列表组装会变成 500。
 */
public final class NotebookNames {

    private NotebookNames() {
    }

    public static String lookup(Map<Long, String> names, Long notebookId) {
        if (notebookId == null || names == null || names.isEmpty()) {
            return null;
        }
        return names.get(notebookId);
    }
}
