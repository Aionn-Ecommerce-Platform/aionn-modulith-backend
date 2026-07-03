package com.aionn.identity.application.port.out.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

public interface AccessTokenIssuerPort {

    String issueAccessToken(String userId, String sessionId, Instant expiresAt, Set<String> roles);

    Optional<Instant> extractExpiry(String token);

    Optional<AccessTokenClaims> parseClaims(String token);
}
