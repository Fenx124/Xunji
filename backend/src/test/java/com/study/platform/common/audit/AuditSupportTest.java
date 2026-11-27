package com.study.platform.common.audit;

import com.study.platform.auth.dto.LoginRequest;
import com.study.platform.auth.dto.RegisterRequest;
import com.study.platform.auth.vo.LoginResponse;
import com.study.platform.auth.vo.UserProfileVO;
import com.study.platform.common.Result;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuditSupportTest {

    @Test
    void shouldTakeTheFirstForwardedIpAndTruncate() {
        assertEquals("203.0.113.8", AuditSupport.clientIp("203.0.113.8, 10.0.0.1", "127.0.0.1"));
        assertEquals("127.0.0.1", AuditSupport.clientIp("  ", "127.0.0.1"));
        assertEquals("a".repeat(64), AuditSupport.clientIp("a".repeat(80), "127.0.0.1"));
        assertEquals("x".repeat(500), AuditSupport.truncate("x".repeat(800), AuditSupport.ERROR_MAX));
        assertNull(AuditSupport.truncate("   ", 10));
    }

    @Test
    void shouldRecoverUsernameFromLoginArgsAndSuccessfulResult() {
        assertEquals("student01", AuditSupport.usernameFromArgs(new Object[]{
                new LoginRequest("student01", "secret")
        }));
        assertEquals("newuser", AuditSupport.usernameFromArgs(new Object[]{
                new RegisterRequest("newuser", "secret", "小明", null)
        }));

        UserProfileVO profile = new UserProfileVO(9L, "student01", "小明", null, null, List.of("USER"), 50);
        AuditSupport.Identity identity = AuditSupport.identityFromResult(
                Result.ok(new LoginResponse("token", "Bearer", 86400, profile)));
        assertEquals(9L, identity.userId());
        assertEquals("student01", identity.username());
        assertNull(AuditSupport.identityFromResult(Result.ok()));
    }
}
