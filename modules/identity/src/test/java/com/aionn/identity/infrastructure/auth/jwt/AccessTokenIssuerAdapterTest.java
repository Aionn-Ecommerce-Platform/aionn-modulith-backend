package com.aionn.identity.infrastructure.auth.jwt;

import com.aionn.identity.application.port.out.auth.AccessTokenClaims;
import com.aionn.identity.infrastructure.config.properties.JwtProperties;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccessTokenIssuerAdapterTest {

    private static final String SECRET = "this-is-a-test-only-secret-of-at-least-32-bytes-12345";
    private static final String ISSUER = "aionn-identity-test";

    private final AccessTokenIssuerAdapter adapter = new AccessTokenIssuerAdapter(
            new JwtProperties(ISSUER, SECRET, 15), java.time.Clock.systemUTC());

    @Test
    void issuedTokenIsParsable() {
        String token = adapter.issueAccessToken(
                "user-1", "session-1",
                Instant.now().plus(1, ChronoUnit.HOURS),
                Set.of("BUYER", "SYSTEM_ADMIN"));

        Optional<AccessTokenClaims> parsed = adapter.parseClaims(token);

        assertThat(parsed).isPresent();
        AccessTokenClaims claims = parsed.get();
        assertThat(claims.userId()).isEqualTo("user-1");
        assertThat(claims.sessionId()).isEqualTo("session-1");
        assertThat(claims.jti()).isNotNull();
        assertThat(claims.roles()).contains("BUYER");
        assertThat(claims.roles()).contains("SYSTEM_ADMIN");
    }

    @Test
    void parseClaimsRejectsTokenSignedByDifferentSecret() {
        var altAdapter = new AccessTokenIssuerAdapter(
                new JwtProperties(ISSUER, "different-secret-also-32-bytes-long-12345678", 15), java.time.Clock.systemUTC());
        String token = altAdapter.issueAccessToken(
                "user-1", "session-1", Instant.now().plus(1, ChronoUnit.HOURS), Set.of());

        Optional<AccessTokenClaims> parsed = adapter.parseClaims(token);

        assertThat(parsed).isEmpty();
    }

    @Test
    void parseClaimsRejectsTokenWithDifferentIssuer() {
        var altAdapter = new AccessTokenIssuerAdapter(
                new JwtProperties("other-issuer", SECRET, 15), java.time.Clock.systemUTC());
        String token = altAdapter.issueAccessToken(
                "user-1", "session-1", Instant.now().plus(1, ChronoUnit.HOURS), Set.of());

        Optional<AccessTokenClaims> parsed = adapter.parseClaims(token);

        assertThat(parsed).isEmpty();
    }

    @Test
    void parseClaimsRejectsGarbageInput() {
        assertThat(adapter.parseClaims("garbage")).isEmpty();
        assertThat(adapter.parseClaims("a.b.c")).isEmpty();
    }

    @Test
    void issuedTokensAreUniquePerCall() {
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);
        String t1 = adapter.issueAccessToken("user-1", "session-1", expiresAt, Set.of());
        String t2 = adapter.issueAccessToken("user-1", "session-1", expiresAt, Set.of());

        assertThat(t2).isNotEqualTo(t1);
    }

    @Test
    void extractExpiryReturnsTokenExpiry() {
        Instant sessionExpiry = Instant.now().plus(1, ChronoUnit.HOURS);
        String token = adapter.issueAccessToken("user-1", "session-1", sessionExpiry, Set.of());

        Optional<Instant> expiry = adapter.extractExpiry(token);

        assertThat(expiry).isPresent();
        assertThat(expiry.get().isAfter(Instant.now())).isTrue();
    }

    @Test
    void extractExpiryReturnsEmptyForInvalidToken() {
        assertThat(adapter.extractExpiry("bad")).isEmpty();
    }

    @Test
    void issuingFailsWithoutSecret() {
        var badAdapter = new AccessTokenIssuerAdapter(new JwtProperties(ISSUER, "", 15), java.time.Clock.systemUTC());

        assertThrows(IllegalStateException.class,
                () -> badAdapter.issueAccessToken("user-1", "session-1",
                        Instant.now().plus(1, ChronoUnit.HOURS), Set.of()));
    }

    @Test
    void accessTokenExpiryIsClampedToSessionExpiry() {
        Instant soonExpiry = Instant.now().plus(1, ChronoUnit.MINUTES);
        String token = adapter.issueAccessToken("user-1", "session-1", soonExpiry, Set.of());

        Optional<Instant> expiry = adapter.extractExpiry(token);

        assertThat(expiry).isPresent();
        assertThat(expiry.get().isBefore(Instant.now().plus(2, ChronoUnit.MINUTES))).isTrue();
    }
}
