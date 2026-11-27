package com.study.platform.focus.support;

import com.study.platform.common.ErrorCode;
import com.study.platform.common.exception.BusinessException;
import com.study.platform.focus.entity.FocusRecord;

import java.time.LocalDateTime;

/**
 * 专注记录的字段规则。控制器注解和这里各拦一次，服务单测可以直接打到这些分支。
 */
public final class FocusRecordRules {

    public static final int PLANNED_MIN = 1;
    public static final int PLANNED_MAX = 240;
    public static final int ACTUAL_MIN = 0;
    public static final int ACTUAL_MAX = 300;
    public static final int REMARK_MAX = 255;

    private FocusRecordRules() {
    }

    public static void validate(Integer plannedMinutes,
                                Integer actualMinutes,
                                String status,
                                LocalDateTime startedAt,
                                LocalDateTime endedAt,
                                String remark) {
        if (plannedMinutes == null || plannedMinutes < PLANNED_MIN || plannedMinutes > PLANNED_MAX) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "计划专注分钟数需在 1 到 240 之间");
        }
        if (actualMinutes == null || actualMinutes < ACTUAL_MIN || actualMinutes > ACTUAL_MAX) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "实际专注分钟数需在 0 到 300 之间");
        }
        if (!FocusRecord.STATUS_COMPLETED.equals(status) && !FocusRecord.STATUS_ABORTED.equals(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "专注状态只能是 COMPLETED 或 ABORTED");
        }
        if (startedAt == null || endedAt == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "开始时间和结束时间不能为空");
        }
        if (startedAt.isAfter(endedAt)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "开始时间不能晚于结束时间");
        }
        if (remark != null && remark.length() > REMARK_MAX) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "专注备注长度不能超过 255");
        }
    }

    public static void validateOptionalId(Long id, String label) {
        if (id != null && id <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, label + "不合法");
        }
    }

    public static void validateDateOrder(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "开始日期不能晚于结束日期");
        }
    }
}
