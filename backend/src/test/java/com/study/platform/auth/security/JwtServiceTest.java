package com.study.platform.auth.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private final JwtService jwtService = new JwtService(SECRET, Duration.ofHours(24), Clock.systemUTC());

    @Test
    void shouldRoundTripClaims() {
        String token = jwtService.createToken(7L, "student01", List.of("USER", "ADMIN"));

        JwtUserClaims claims = jwtService.parse(token);

        assertEquals(7L, claims.userId());
        assertEquals("student01", claims.username());
        assertEquals(List.of("USER", "ADMIN"), claims.roles());
        assertNotNull(claims.jti());
        assertTrue(claims.jti().length() > 10);
        assertEquals(24 * 3600, Duration.between(claims.issuedAt(), claims.expiresAt()).toSeconds());
        assertEquals(2, token.chars().filter(ch -> ch == '.').count());
    }

    @Test
    void shouldRejectTamperedToken() {
        String token = jwtService.createToken(1L, "student01", List.of("USER"));
        String tampered = token.substring(0, token.length() - 2) + "aa";

        assertThrows(JwtException.class, () -> jwtService.parse(tampered));
    }

    @Test
    void shouldRejectForeignSignature() {
        JwtService other = new JwtService("abcdef0123456789abcdef0123456789", Duration.ofHours(1), Clock.systemUTC());
        String token = other.createToken(1L, "student01", List.of("USER"));

        assertThrows(JwtException.class, () -> jwtService.parse(token));
    }

    @Test
    void shouldRejectExpiredToken() {
        Clock past = Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC);
        JwtService issuer = new JwtService(SECRET, Duration.ofMinutes(5), past);
        String token = issuer.createToken(3L, "student01", List.of("USER"));

        assertThrows(JwtException.class, () -> jwtService.parse(token));
    }

    @Test
    void shouldRejectShortSecret() {
        assertThrows(IllegalArgumentException.class,
                () -> new JwtService("too-short", Duration.ofHours(1), Clock.systemUTC()));
    }
}
