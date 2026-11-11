package com.aionn.identity.application.port.out.auth;

import java.time.Duration;
import java.util.Optional;

public interface RefreshTokenStorePort {

    void store(String tokenId, String sessionId, Duration ttl);

    Optional<String> findSessionId(String tokenId);

    Optional<String> consumeSessionId(String tokenId);

    /**
     * Returns the session id associated with a token that was already consumed. Used to detect
     * replay of a rotated refresh token and revoke the whole token family.
     */
    Optional<String> findConsumedSessionId(String tokenId);

    void revoke(String tokenId);

    void revokeBySessionId(String sessionId);
}
