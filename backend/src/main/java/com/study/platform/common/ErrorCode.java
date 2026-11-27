package com.study.platform.common;

/**
 * 与接口契约 1.3 节对齐的业务错误码。
 * {@code code} 写入响应体，{@code httpStatus} 写入 HTTP 状态行。
 */
public enum ErrorCode {

    SUCCESS(200, 200, "OK"),
    BAD_REQUEST(40001, 400, "请求参数校验失败"),
    UNAUTHORIZED(40101, 401, "未登录或 Token 无效"),
    FORBIDDEN(40301, 403, "无权限访问"),
    NOT_FOUND(40401, 404, "目标资源不存在"),
    CONFLICT(40901, 409, "业务冲突"),
    TOO_MANY_REQUESTS(42901, 429, "请求过于频繁或配额已用尽"),
    INTERNAL_ERROR(50001, 500, "服务器内部异常"),
    SERVICE_UNAVAILABLE(50301, 503, "外部服务不可用");

    private final int code;
    private final int httpStatus;
    private final String message;

    ErrorCode(int code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
