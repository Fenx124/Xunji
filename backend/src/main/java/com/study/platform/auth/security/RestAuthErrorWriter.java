package com.study.platform.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.platform.common.ErrorCode;
import com.study.platform.common.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 认证失败时直接写 JSON Result，避免 Spring Security 默认的 HTML 错误页。
 */
@Component
public class RestAuthErrorWriter {

    private final ObjectMapper objectMapper;

    public RestAuthErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response, ErrorCode errorCode, String message) throws IOException {
        response.setStatus(errorCode.getHttpStatus());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Result.fail(errorCode, message));
    }
}
