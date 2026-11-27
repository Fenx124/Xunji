package com.study.platform.common.exception;

import com.study.platform.common.ErrorCode;

/**
 * 可预期的业务失败。由全局异常处理器转换成契约中的 code 与 HTTP 状态。
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
