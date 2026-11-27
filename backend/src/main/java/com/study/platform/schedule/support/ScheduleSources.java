package com.study.platform.schedule.support;

import com.study.platform.common.ErrorCode;
import com.study.platform.common.exception.BusinessException;
import com.study.platform.schedule.entity.Schedule;

import java.util.Locale;

/**
 * 创建日程时的来源。客户端只能声明 MANUAL 或 AI。
 * EBBINGHAUS 由批量复习接口写入，避免被普通创建请求伪造。
 */
public final class ScheduleSources {

    private ScheduleSources() {
    }

    public static String forCreate(String sourceType) {
        if (sourceType == null || sourceType.isBlank()) {
            return Schedule.SOURCE_MANUAL;
        }
        String normalized = sourceType.trim().toUpperCase(Locale.ROOT);
        if (Schedule.SOURCE_MANUAL.equals(normalized) || Schedule.SOURCE_AI.equals(normalized)) {
            return normalized;
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "日程来源仅支持 MANUAL 或 AI");
    }
}
