package com.study.platform.note.support;

import java.util.List;
import java.util.Objects;

/**
 * 笔记历史快照的纯规则：何时写入，以及超出上限后丢掉哪些 id。
 * 调用方按 id 降序传入，列表尾部就是更旧的快照。
 */
public final class NoteHistoryRules {

    public static final int KEEP = 10;
    public static final String CHANGE_BEFORE_SAVE = "保存前快照";
    public static final String CHANGE_BEFORE_RESTORE = "回滚前快照";

    private NoteHistoryRules() {
    }

    /**
     * 即将写入的标题会先 trim。只有标题或正文和库中的值不同才需要快照。
     * 置顶、收藏、标签、笔记本的变化不会出现在这两个字段里。
     */
    public static boolean needsSnapshot(String storedTitle, String storedContent, String nextTitle, String nextContent) {
        return !Objects.equals(storedTitle, persistedTitle(nextTitle))
                || !Objects.equals(storedContent, nextContent);
    }

    public static String persistedTitle(String title) {
        return title == null ? null : title.trim();
    }

    /**
     * @param idsNewestFirst 同一笔记的快照 id，必须已按 id 降序
     * @return 需要删除的更旧 id；未超过 {@link #KEEP} 时为空
     */
    public static List<Long> idsBeyondKeep(List<Long> idsNewestFirst) {
        if (idsNewestFirst == null || idsNewestFirst.size() <= KEEP) {
            return List.of();
        }
        return List.copyOf(idsNewestFirst.subList(KEEP, idsNewestFirst.size()));
    }
}
