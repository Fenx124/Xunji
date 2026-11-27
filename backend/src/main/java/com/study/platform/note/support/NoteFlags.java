package com.study.platform.note.support;

/**
 * 置顶 / 收藏：请求没带目标值时取反，带了就按目标值写入。
 */
public final class NoteFlags {

    private NoteFlags() {
    }

    public static boolean isOn(Integer flag) {
        return flag != null && flag == 1;
    }

    public static int toInt(boolean value) {
        return value ? 1 : 0;
    }

    public static boolean resolve(Boolean requested, boolean current) {
        if (requested == null) {
            return !current;
        }
        return requested;
    }
}
