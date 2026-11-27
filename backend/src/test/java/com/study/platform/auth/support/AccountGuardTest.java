package com.study.platform.auth.support;

import com.study.platform.common.ErrorCode;
import com.study.platform.common.RedisKeys;
import com.study.platform.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountGuardTest {

    @Test
    void shouldForbidDisablingOrKickingSelfAndRemovingOwnAdmin() {
        BusinessException disable = assertThrows(BusinessException.class,
                () -> AccountGuard.rejectSelfDisable(7L, 7L, 0));
        assertEquals(ErrorCode.BAD_REQUEST, disable.getErrorCode());

        BusinessException kick = assertThrows(BusinessException.class,
                () -> AccountGuard.rejectSelfKick(7L, 7L));
        assertEquals(40001, kick.getErrorCode().getCode());

        BusinessException demote = assertThrows(BusinessException.class,
                () -> AccountGuard.rejectSelfAdminRemoval(7L, 7L, List.of("USER")));
        assertEquals(ErrorCode.BAD_REQUEST, demote.getErrorCode());

        AccountGuard.rejectSelfDisable(7L, 8L, 0);
        AccountGuard.rejectSelfDisable(7L, 7L, 1);
        AccountGuard.rejectSelfKick(7L, 8L);
        AccountGuard.rejectSelfAdminRemoval(7L, 7L, List.of("ADMIN", "USER"));
    }

    @Test
    void shouldNormalizeRolesAndRejectUnknownOrEmptyCodes() {
        assertEquals(List.of("USER", "ADMIN"), AccountGuard.normalizeRoles(List.of(" user ", "ADMIN", "user")));
        assertBadRequest(() -> AccountGuard.normalizeRoles(List.of()));
        assertBadRequest(() -> AccountGuard.normalizeRoles(null));
        assertBadRequest(() -> AccountGuard.normalizeRoles(List.of(" ")));
        assertBadRequest(() -> AccountGuard.normalizeRoles(List.of("GUEST")));
        assertBadRequest(() -> AccountGuard.requireStatus(2));
        assertEquals(0, AccountGuard.requireAiDailyLimit(0));
        assertEquals(10000, AccountGuard.requireAiDailyLimit(10000));
        assertBadRequest(() -> AccountGuard.requireAiDailyLimit(-1));
        assertBadRequest(() -> AccountGuard.requireAiDailyLimit(10001));
    }

    @Test
    void revokedUserKeyMatchesTheJwtFilterKey() {
        assertEquals("auth:revoked:18", RedisKeys.revokedUser(18L));
        assertEquals(RedisKeys.authRevoked(18L), RedisKeys.revokedUser(18L));
        assertEquals("dashboard:overview:18", RedisKeys.dashboardOverview(18L));
    }

    private static void assertBadRequest(Runnable action) {
        BusinessException ex = assertThrows(BusinessException.class, action::run);
        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
    }
}
