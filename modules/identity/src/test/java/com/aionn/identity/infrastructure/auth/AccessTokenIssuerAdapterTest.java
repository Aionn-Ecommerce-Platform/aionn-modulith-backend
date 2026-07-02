package com.aionn.identity.infrastructure.auth;

import com.aionn.identity.application.port.out.auth.AccessTokenClaims;
import com.aionn.identity.infrastructure.auth.jwt.AccessTokenIssuerAdapter;
import com.aionn.identity.infrastructure.config.properties.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessTokenIssuerAdapterTest {

    private static final String SECRET = "this-is-a-256-bit-secret-key-for-tests-only-please-rotate";

    private AccessTokenIssuerAdapter adapter;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties("aionn-identity-test", SECRET, 15);
        adapter = new AccessTokenIssuerAdapter(props);
    }

    @Test
    void issuedTokenCanBeParsedBack() {
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

        String token = adapter.issueAccessToken(
                "user-1", "session-1", expiresAt, Set.of("USER", "ADMIN"));

        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3, "JWT should have three parts");

        Optional<AccessTokenClaims> claims = adapter.parseClaims(token);
        assertTrue(claims.isPresent());
        assertEquals("user-1", claims.get().userId());
        assertEquals("session-1", claims.get().sessionId());
        assertNotNull(claims.get().jti());
        assertTrue(claims.get().roles().contains("USER"));
        assertTrue(claims.get().roles().contains("ADMIN"));
    }

    @Test
    void extractExpiryReturnsNonNullDate() {
        String token = adapter.issueAccessToken(
                "user-2", "session-2", LocalDateTime.now().plusHours(1), Set.of("USER"));

        LocalDateTime expiry = adapter.extractExpiry(token);

        assertNotNull(expiry);
        assertTrue(expiry.isAfter(LocalDateTime.now().minusMinutes(1)));
    }

    @Test
    void parseClaimsReturnsEmptyForGarbageToken() {
        Optional<AccessTokenClaims> claims = adapter.parseClaims("not-a-jwt");

        assertTrue(claims.isEmpty());
    }

    @Test
    void missingSecretFailsToIssueToken() {
        AccessTokenIssuerAdapter broken = new AccessTokenIssuerAdapter(
                new JwtProperties("aionn-identity-test", "  ", 15));

        assertThrows(IllegalStateException.class,
                () -> broken.issueAccessToken("u", "s", LocalDateTime.now().plusHours(1), Set.of()));
    }
}
