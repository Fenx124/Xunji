package com.study.platform.common.audit;

import com.study.platform.auth.security.SecurityUtils;
import com.study.platform.auth.security.UserPrincipal;
import com.study.platform.system.entity.SysAuditLog;
import com.study.platform.system.mapper.SysAuditLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 环绕带 {@link AuditLog} 的接口，把操作者、耗时和成败写入 sys_audit_log。
 * 写日志本身包在 try/catch 里：审计失败只记警告，业务异常仍按原样抛出。
 */
@Aspect
@Component
public class AuditLogAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);

    private final SysAuditLogMapper auditLogMapper;

    public AuditLogAspect(SysAuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        long started = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            persist(joinPoint, auditLog, result, null, System.currentTimeMillis() - started, SysAuditLog.STATUS_SUCCESS);
            return result;
        } catch (Throwable ex) {
            persist(joinPoint, auditLog, null, ex, System.currentTimeMillis() - started, SysAuditLog.STATUS_FAILED);
            throw ex;
        }
    }

    private void persist(ProceedingJoinPoint joinPoint,
                         AuditLog auditLog,
                         Object result,
                         Throwable error,
                         long costMs,
                         int status) {
        try {
            HttpServletRequest request = currentRequest();
            UserPrincipal principal = SecurityUtils.findCurrentUser();
            Long userId = principal == null ? null : principal.getUserId();
            String username = principal == null ? null : principal.getUsername();
            if (username == null || username.isBlank()) {
                username = AuditSupport.usernameFromArgs(joinPoint.getArgs());
            }
            AuditSupport.Identity identity = AuditSupport.identityFromResult(result);
            if (identity != null) {
                if (userId == null) {
                    userId = identity.userId();
                }
                if (username == null || username.isBlank()) {
                    username = identity.username();
                }
            }

            SysAuditLog row = new SysAuditLog();
            row.setUserId(userId);
            row.setUsername(AuditSupport.truncate(username, AuditSupport.USERNAME_MAX));
            row.setModule(blankToUnknown(auditLog.module()));
            row.setAction(blankToUnknown(auditLog.action()));
            row.setHttpMethod(request == null ? "UNKNOWN" : blankToUnknown(request.getMethod()));
            row.setRequestUri(request == null ? "/" : uriOf(request));
            row.setClientIp(request == null ? null : AuditSupport.clientIp(
                    request.getHeader("X-Forwarded-For"), request.getRemoteAddr()));
            row.setCostMs(toCost(costMs));
            row.setStatus(status);
            row.setErrorMsg(error == null ? null : AuditSupport.truncate(errorText(error), AuditSupport.ERROR_MAX));
            auditLogMapper.insert(row);
        } catch (Exception ex) {
            log.warn("写入审计日志失败，不影响业务调用: {}", ex.getMessage());
        }
    }

    private static HttpServletRequest currentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        return attributes.getRequest();
    }

    private static String uriOf(HttpServletRequest request) {
        String uri = AuditSupport.truncate(request.getRequestURI(), AuditSupport.URI_MAX);
        return uri == null ? "/" : uri;
    }

    private static String blankToUnknown(String value) {
        if (value == null || value.isBlank()) {
            return "UNKNOWN";
        }
        return value.trim();
    }

    private static int toCost(long costMs) {
        if (costMs <= 0) {
            return 0;
        }
        return costMs > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) costMs;
    }

    private static String errorText(Throwable error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            return error.getClass().getSimpleName();
        }
        return message;
    }
}
