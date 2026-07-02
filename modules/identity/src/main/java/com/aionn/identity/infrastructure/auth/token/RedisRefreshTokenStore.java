package com.aionn.identity.infrastructure.auth.token;

import com.aionn.identity.application.port.out.auth.RefreshTokenStorePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStorePort {

    private static final String TOKEN_KEY_PREFIX = "identity:auth:refresh:";
    private static final String SESSION_INDEX_PREFIX = "identity:auth:refresh:session:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void store(String tokenId, String sessionId, Duration ttl) {
        String tokenHash = sha256(tokenId);
        redisTemplate.opsForValue().set(tokenKey(tokenHash), sessionId, ttl);
        redisTemplate.opsForSet().add(sessionIndexKey(sessionId), tokenHash);
        redisTemplate.expire(sessionIndexKey(sessionId), ttl);
    }

    @Override
    public Optional<String> findSessionId(String tokenId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(tokenKey(sha256(tokenId))));
    }

    @Override
    public Optional<String> consumeSessionId(String tokenId) {
        // GETDEL is atomic on the server side: any concurrent refresh that races with
        // us
        // will see a null payload and be rejected, eliminating the replay window
        // between
        // findSessionId() and revoke().
        String tokenHash = sha256(tokenId);
        String sessionId = redisTemplate.opsForValue().getAndDelete(tokenKey(tokenHash));
        if (sessionId != null) {
            redisTemplate.opsForSet().remove(sessionIndexKey(sessionId), tokenHash);
        }
        return Optional.ofNullable(sessionId);
    }

    @Override
    public void revoke(String tokenId) {
        String tokenHash = sha256(tokenId);
        // Remove the index entry too so the per-session set does not accumulate stale
        // hashes
        // that grow unbounded until the index key TTL fires.
        String sessionId = redisTemplate.opsForValue().getAndDelete(tokenKey(tokenHash));
        if (sessionId != null) {
            redisTemplate.opsForSet().remove(sessionIndexKey(sessionId), tokenHash);
        }
    }

    @Override
    public void revokeBySessionId(String sessionId) {
        String indexKey = sessionIndexKey(sessionId);
        var members = redisTemplate.opsForSet().members(indexKey);
        if (members != null) {
            for (String tokenHash : members) {
                redisTemplate.delete(tokenKey(tokenHash));
            }
        }
        redisTemplate.delete(indexKey);
    }

    private static String tokenKey(String tokenHash) {
        return TOKEN_KEY_PREFIX + tokenHash;
    }

    private static String sessionIndexKey(String sessionId) {
        return SESSION_INDEX_PREFIX + sessionId;
    }

    private static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
