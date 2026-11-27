package com.study.platform.schedule.support;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 艾宾浩斯复习间隔：1、2、4、7、15 天。
 * 每条日程固定在当天 09:00–09:30，08:45 提醒。标题写入 schedule.title（最长 160）。
 */
public final class EbbinghausPlanner {

    public static final List<Integer> INTERVAL_DAYS = List.of(1, 2, 4, 7, 15);
    public static final int TITLE_MAX_LENGTH = 160;

    public record ReviewSlot(
            int sequence,
            int daysLater,
            String title,
            LocalDateTime startTime,
            LocalDateTime endTime,
            LocalDateTime remindTime
    ) {
    }

    private EbbinghausPlanner() {
    }

    public static List<ReviewSlot> generate(LocalDate baseDate, String noteTitle) {
        if (baseDate == null) {
            throw new IllegalArgumentException("baseDate 不能为空");
        }
        List<ReviewSlot> slots = new ArrayList<>(INTERVAL_DAYS.size());
        for (int i = 0; i < INTERVAL_DAYS.size(); i++) {
            int days = INTERVAL_DAYS.get(i);
            int sequence = i + 1;
            LocalDate day = baseDate.plusDays(days);
            slots.add(new ReviewSlot(
                    sequence,
                    days,
                    buildTitle(noteTitle, sequence, days),
                    day.atTime(9, 0, 0),
                    day.atTime(9, 30, 0),
                    day.atTime(8, 45, 0)
            ));
        }
        return List.copyOf(slots);
    }

    public static String buildTitle(String noteTitle, int sequence, int days) {
        String prefix = "复习：";
        String suffix = "（第" + sequence + "次·" + days + "天后）";
        String name = noteTitle == null ? "" : noteTitle;
        int room = TITLE_MAX_LENGTH - prefix.length() - suffix.length();
        if (room < 0) {
            room = 0;
        }
        if (name.length() > room) {
            name = name.substring(0, room);
        }
        return prefix + name + suffix;
    }
}
