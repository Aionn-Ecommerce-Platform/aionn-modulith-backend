package com.aionn.identity.application.port.out.auth;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

public interface AccessTokenIssuerPort {

	String issueAccessToken(String userId, String sessionId, LocalDateTime expiresAt, Set<String> roles);

	LocalDateTime extractExpiry(String token);

	Optional<AccessTokenClaims> parseClaims(String token);
}
