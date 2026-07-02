package com.aionn.identity.infrastructure.auth.jwt;

import com.aionn.identity.application.port.out.auth.AccessTokenClaims;
import com.aionn.identity.infrastructure.config.properties.JwtProperties;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessTokenIssuerAdapterTest {

    private static final String SECRET = "this-is-a-test-only-secret-of-at-least-32-bytes-12345";
    private static final String ISSUER = "aionn-identity-test";

    private final AccessTokenIssuerAdapter adapter = new AccessTokenIssuerAdapter(
            new JwtProperties(ISSUER, SECRET, 15));

    @Test
    void issuedTokenIsParsable() {
        String token = adapter.issueAccessToken(
                "user-1", "session-1",
                LocalDateTime.now().plusHours(1),
                Set.of("BUYER", "SYSTEM_ADMIN"));

        Optional<AccessTokenClaims> parsed = adapter.parseClaims(token);

        assertTrue(parsed.isPresent());
        AccessTokenClaims claims = parsed.get();
        assertEquals("user-1", claims.userId());
        assertEquals("session-1", claims.sessionId());
        assertNotNull(claims.jti());
        assertTrue(claims.roles().contains("BUYER"));
        assertTrue(claims.roles().contains("SYSTEM_ADMIN"));
    }

    @Test
    void parseClaimsRejectsTokenSignedByDifferentSecret() {
        var altAdapter = new AccessTokenIssuerAdapter(
                new JwtProperties(ISSUER, "different-secret-also-32-bytes-long-12345678", 15));
        String token = altAdapter.issueAccessToken(
                "user-1", "session-1", LocalDateTime.now().plusHours(1), Set.of());

        Optional<AccessTokenClaims> parsed = adapter.parseClaims(token);

        assertTrue(parsed.isEmpty());
    }

    @Test
    void parseClaimsRejectsTokenWithDifferentIssuer() {
        var altAdapter = new AccessTokenIssuerAdapter(
                new JwtProperties("other-issuer", SECRET, 15));
        String token = altAdapter.issueAccessToken(
                "user-1", "session-1", LocalDateTime.now().plusHours(1), Set.of());

        Optional<AccessTokenClaims> parsed = adapter.parseClaims(token);

        assertTrue(parsed.isEmpty());
    }

    @Test
    void parseClaimsRejectsGarbageInput() {
        assertTrue(adapter.parseClaims("garbage").isEmpty());
        assertTrue(adapter.parseClaims("a.b.c").isEmpty());
    }

    @Test
    void issuedTokensAreUniquePerCall() {
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
        String t1 = adapter.issueAccessToken("user-1", "session-1", expiresAt, Set.of());
        String t2 = adapter.issueAccessToken("user-1", "session-1", expiresAt, Set.of());

        assertNotEquals(t1, t2);
    }

    @Test
    void extractExpiryReturnsTokenExpiry() {
        LocalDateTime sessionExpiry = LocalDateTime.now().plusHours(1);
        String token = adapter.issueAccessToken("user-1", "session-1", sessionExpiry, Set.of());

        LocalDateTime expiry = adapter.extractExpiry(token);

        assertNotNull(expiry);
        assertTrue(expiry.isAfter(LocalDateTime.now()));
    }

    @Test
    void extractExpiryRejectsInvalidToken() {
        assertThrows(IllegalArgumentException.class, () -> adapter.extractExpiry("bad"));
    }

    @Test
    void issuingFailsWithoutSecret() {
        var badAdapter = new AccessTokenIssuerAdapter(new JwtProperties(ISSUER, "", 15));

        assertThrows(IllegalStateException.class,
                () -> badAdapter.issueAccessToken("user-1", "session-1",
                        LocalDateTime.now().plusHours(1), Set.of()));
    }

    @Test
    void accessTokenExpiryIsClampedToSessionExpiry() {
        LocalDateTime soonExpiry = LocalDateTime.now().plusMinutes(1);
        String token = adapter.issueAccessToken("user-1", "session-1", soonExpiry, Set.of());

        LocalDateTime expiry = adapter.extractExpiry(token);

        assertTrue(expiry.isBefore(LocalDateTime.now().plusMinutes(2)));
    }
}
