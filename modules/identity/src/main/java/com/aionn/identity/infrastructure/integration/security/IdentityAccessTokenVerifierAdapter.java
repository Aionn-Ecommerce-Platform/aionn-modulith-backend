package com.aionn.identity.infrastructure.integration.security;

import com.aionn.identity.application.port.out.auth.AccessTokenClaims;
import com.aionn.identity.application.port.out.auth.AccessTokenIssuerPort;
import com.aionn.identity.application.port.out.auth.AuthSessionPersistencePort;
import com.aionn.identity.application.port.out.auth.TokenBlacklistPort;
import com.aionn.identity.application.port.out.user.UserPersistencePort;
import com.aionn.identity.domain.model.AuthSession;
import com.aionn.identity.domain.valueobject.AuthSessionStatus;
import com.aionn.contracts.port.identity.AccessTokenVerifierPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdentityAccessTokenVerifierAdapter implements AccessTokenVerifierPort {

    private static final String BEARER = "Bearer ";

    private final AccessTokenIssuerPort tokenIssuer;
    private final AuthSessionPersistencePort authSessionPersistencePort;
    private final UserPersistencePort userPersistencePort;
    private final TokenBlacklistPort tokenBlacklist;
    private final Clock clock;

    @Override
    public Optional<String> verifyAndExtractUserId(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER)) {
            return Optional.empty();
        }
        String token = authorizationHeader.substring(BEARER.length()).trim();
        Optional<AccessTokenClaims> parsed = tokenIssuer.parseClaims(token);
        if (parsed.isEmpty()) {
            return Optional.empty();
        }
        AccessTokenClaims claims = parsed.get();
        String sessionId = claims.sessionId();
        String userId = claims.userId();
        String jti = claims.jti();
        if (sessionId == null || userId == null) {
            return Optional.empty();
        }
        try {
            if (jti != null && tokenBlacklist.isBlacklisted(jti)) {
                return Optional.empty();
            }
            Optional<AuthSession> sessionOpt = authSessionPersistencePort.findById(sessionId);
            if (sessionOpt.isEmpty()
                    || !AuthSessionStatus.ACTIVE.equals(sessionOpt.get().getStatus())
                    || sessionOpt.get().isExpired(clock)
                    || !userId.equals(sessionOpt.get().getUserId())) {
                return Optional.empty();
            }
            if (userPersistencePort.findById(userId).filter(user -> user.isActive()).isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(userId);
        } catch (RuntimeException ex) {
            // Fail closed when Redis or the database cannot prove the token is still valid.
            log.warn("Token verification failed while checking revocation state", ex);
            return Optional.empty();
        }
    }
}
