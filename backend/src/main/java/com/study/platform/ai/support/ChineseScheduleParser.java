package com.study.platform.ai.support;

import com.study.platform.common.config.JacksonConfig;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 把中文时间口语解析成日程字段。只覆盖学习场景里常见的今天/明天/下周和上/下午钟点。
 */
public final class ChineseScheduleParser {

    private static final Pattern TIME = Pattern.compile(
            "(凌晨|早上|上午|中午|下午|晚上|傍晚)?\\s*([0-9]{1,2}|[零一二两三四五六七八九十]{1,3})\\s*(?:点|时|:|：)\\s*(半|([0-9]{1,2}|[零一二两三四五六七八九十]{1,3})\\s*分?)?"
    );
    private static final Pattern WEEKDAY = Pattern.compile("(下|本)?(周|星期|礼拜)([一二三四五六日天])");
    private static final Pattern DEADLINE = Pattern.compile("([点时分]|\\d)\\s*前");

    private ChineseScheduleParser() {
    }

    public static ParsedScheduleDraft parse(String text, LocalDateTime now) {
        String raw = text == null ? "" : text.trim();
        LocalDateTime clock = now == null ? LocalDateTime.of(2026, 1, 1, 9, 0) : now;
        LocalDate date = resolveDate(raw, clock.toLocalDate());
        LocalTime time = resolveTime(raw);
        boolean due = isDeadline(raw);
        LocalDateTime point = LocalDateTime.of(date, time == null ? LocalTime.of(due ? 18 : 9, 0) : time);
        LocalDateTime start;
        LocalDateTime end;
        LocalDateTime remind;
        if (due) {
            end = point;
            start = point.minusMinutes(30);
            // 提醒放在时间块开始之前，解析器会拒绝不早于 start 的 remindTime。
            remind = start.minusMinutes(15);
        } else {
            start = point;
            end = point.plusHours(1);
            remind = point.minusMinutes(15);
        }
        String description = raw.isEmpty() ? null : AiText.clamp(raw, 1000);
        return new ParsedScheduleDraft(titleOf(raw), description, start, end, remind, priorityOf(raw));
    }

    public static int priorityOf(String text) {
        if (text == null || text.isBlank()) {
            return 2;
        }
        boolean important = text.contains("重要") && !text.contains("不重要");
        boolean urgent = isUrgent(text);
        if (important && urgent) {
            return 1;
        }
        if (important) {
            return 2;
        }
        if (urgent) {
            return 3;
        }
        if (text.contains("不重要")) {
            return 4;
        }
        return 2;
    }

    public static boolean isDeadline(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        if (text.contains("截止") || text.contains("之前") || text.contains("以前")) {
            return true;
        }
        if (text.toUpperCase(Locale.ROOT).contains("DDL")) {
            return true;
        }
        return DEADLINE.matcher(text).find();
    }

    public static String format(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return time.format(JacksonConfig.DATE_TIME_FORMATTER);
    }

    private static boolean isUrgent(String text) {
        boolean urgentWord = text.contains("紧急") && !text.contains("不紧急");
        return urgentWord || text.contains("截止") || text.toUpperCase(Locale.ROOT).contains("DDL") || isDeadline(text);
    }

    private static LocalDate resolveDate(String text, LocalDate today) {
        if (text.contains("大后天")) {
            return today.plusDays(3);
        }
        if (text.contains("后天")) {
            return today.plusDays(2);
        }
        if (text.contains("明天")) {
            return today.plusDays(1);
        }
        if (text.contains("今天")) {
            return today;
        }
        Matcher week = WEEKDAY.matcher(text);
        if (week.find()) {
            int iso = isoDay(week.group(3));
            String prefix = week.group(1);
            if ("下".equals(prefix)) {
                LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                return monday.plusWeeks(1).plusDays(iso - 1L);
            }
            if ("本".equals(prefix)) {
                LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                return monday.plusDays(iso - 1L);
            }
            return today.with(TemporalAdjusters.nextOrSame(DayOfWeek.of(iso)));
        }
        return today;
    }

    private static int isoDay(String token) {
        return switch (token) {
            case "一" -> 1;
            case "二" -> 2;
            case "三" -> 3;
            case "四" -> 4;
            case "五" -> 5;
            case "六" -> 6;
            case "日", "天" -> 7;
            default -> 1;
        };
    }

    private static LocalTime resolveTime(String text) {
        Matcher matcher = TIME.matcher(text);
        while (matcher.find()) {
            int hour = parseNumber(matcher.group(2));
            if (hour < 0) {
                continue;
            }
            hour = adjustHour(matcher.group(1), hour);
            if (hour < 0 || hour > 23) {
                continue;
            }
            int minute = 0;
            String minuteToken = matcher.group(3);
            if (minuteToken != null && !minuteToken.isBlank()) {
                if (minuteToken.startsWith("半")) {
                    minute = 30;
                } else if (matcher.group(4) != null) {
                    minute = parseNumber(matcher.group(4));
                }
            }
            if (minute < 0 || minute > 59) {
                minute = 0;
            }
            return LocalTime.of(hour, minute);
        }
        return null;
    }

    private static int adjustHour(String period, int hour) {
        if (hour < 0 || hour > 23) {
            return -1;
        }
        if (period == null || period.isEmpty()) {
            return hour;
        }
        return switch (period) {
            case "凌晨" -> hour == 12 ? 0 : hour;
            case "早上", "上午" -> hour;
            case "中午" -> (hour == 0 || hour == 12) ? 12 : (hour < 6 ? hour + 12 : hour);
            case "下午", "晚上", "傍晚" -> hour < 12 ? hour + 12 : hour;
            default -> hour;
        };
    }

    static int parseNumber(String raw) {
        if (raw == null || raw.isBlank()) {
            return -1;
        }
        String text = raw.trim();
        if (text.chars().allMatch(Character::isDigit)) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ex) {
                return -1;
            }
        }
        int total = 0;
        int current = 0;
        for (int i = 0; i < text.length(); i++) {
            char token = text.charAt(i);
            int digit = switch (token) {
                case '零' -> 0;
                case '一' -> 1;
                case '二', '两' -> 2;
                case '三' -> 3;
                case '四' -> 4;
                case '五' -> 5;
                case '六' -> 6;
                case '七' -> 7;
                case '八' -> 8;
                case '九' -> 9;
                case '十' -> -10;
                default -> Integer.MIN_VALUE;
            };
            if (digit == Integer.MIN_VALUE) {
                return -1;
            }
            if (digit == -10) {
                total += (current == 0 ? 1 : current) * 10;
                current = 0;
            } else {
                current = digit;
            }
        }
        return total + current;
    }

    static String titleOf(String raw) {
        String text = raw == null ? "" : raw;
        text = TIME.matcher(text).replaceAll(" ");
        text = text.replace("大后天", " ").replace("今天", " ").replace("明天", " ").replace("后天", " ");
        text = WEEKDAY.matcher(text).replaceAll(" ");
        text = text.replaceAll("(?i)ddl", " ");
        String[] noise = {
                "重要且紧急", "紧急且重要", "很重要", "不重要不紧急",
                "不紧急", "不重要", "截止", "之前", "以前", "重要", "紧急"
        };
        for (String word : noise) {
            text = text.replace(word, " ");
        }
        text = text.replace("且", " ");
        text = text.replaceAll("^[前\\s]+", "");
        text = text.replaceAll("[，。！？、,.!；;：:\\s]+", " ").trim();
        text = text.replaceAll(" {2,}", " ");
        if (text.isEmpty()) {
            return "学习日程";
        }
        return AiText.clamp(text, 160);
    }
}
