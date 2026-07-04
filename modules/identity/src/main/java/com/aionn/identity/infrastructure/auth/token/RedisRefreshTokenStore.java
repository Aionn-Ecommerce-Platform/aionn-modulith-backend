package com.aionn.identity.infrastructure.auth.token;

import com.aionn.identity.application.port.out.auth.RefreshTokenStorePort;
import com.aionn.sharedkernel.util.Sha256Hasher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStorePort {

    private static final String TOKEN_KEY_PREFIX = "identity:auth:refresh:";
    private static final String SESSION_INDEX_PREFIX = "identity:auth:refresh:session:";

    // Server-side atomic revoke-by-session:
    // KEYS[1] = session index key, ARGV[1] = token key prefix.
    // Drains the index set and deletes each token key inside the same call, so a
    // concurrent store() that adds a new token+index entry after we read cannot
    // outrun the delete.
    private static final DefaultRedisScript<Long> REVOKE_BY_SESSION_SCRIPT = new DefaultRedisScript<>(
            "local members = redis.call('SMEMBERS', KEYS[1]) "
                    + "for _, hash in ipairs(members) do "
                    + "  redis.call('DEL', ARGV[1] .. hash) "
                    + "end "
                    + "redis.call('DEL', KEYS[1]) "
                    + "return #members",
            Long.class);

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
        redisTemplate.execute(
                REVOKE_BY_SESSION_SCRIPT,
                List.of(sessionIndexKey(sessionId)),
                TOKEN_KEY_PREFIX);
    }

    private static String tokenKey(String tokenHash) {
        return TOKEN_KEY_PREFIX + tokenHash;
    }

    private static String sessionIndexKey(String sessionId) {
        return SESSION_INDEX_PREFIX + sessionId;
    }

    private static String sha256(String value) {
        return Sha256Hasher.hexDigest(value);
    }
}
