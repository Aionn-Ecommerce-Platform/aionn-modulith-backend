package com.aionn.identity.infrastructure.registration;

import com.aionn.identity.application.port.out.registration.RegistrationRateLimiterPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "identity.registration.ratelimit", name = "provider", havingValue = "redis", matchIfMissing = true)
public class RedisRegistrationRateLimiter implements RegistrationRateLimiterPort {

    private static final String KEY_PREFIX = "identity:ratelimit:";

    private static final String LUA_SCRIPT = """
            redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', ARGV[2])
            local count = redis.call('ZCARD', KEYS[1])
            if count >= tonumber(ARGV[3]) then
              return 0
            end
            redis.call('ZADD', KEYS[1], ARGV[1], ARGV[4])
            redis.call('EXPIRE', KEYS[1], ARGV[5])
            return 1
            """;

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> rateLimitScript;

    public RedisRegistrationRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.rateLimitScript = new DefaultRedisScript<>(LUA_SCRIPT, Long.class);
    }

    @Override
    public boolean check(String scope, String key, int maxAttempts, int windowSeconds) {
        if (key == null || key.isBlank()) {
            return true;
        }

        String bucket = KEY_PREFIX + scope + ":" + key;
        long now = Instant.now().getEpochSecond();
        long windowStart = now - windowSeconds;
        // UUID guarantees uniqueness across threads/instances; the previous
        // "now:threadId" form collided when multiple requests landed in the same second
        // on the same thread, silently swallowing one of the attempts in the ZSET.
        String member = now + ":" + UUID.randomUUID();

        Long allowed = redisTemplate.execute(
                rateLimitScript,
                List.of(bucket),
                Long.toString(now),
                Long.toString(windowStart),
                Integer.toString(maxAttempts),
                member,
                Long.toString(windowSeconds * 2L));

        if (allowed == null || allowed == 0L) {
            log.debug("Rate limit exceeded: scope={}, key={}, max={}", scope, key, maxAttempts);
            return false;
        }
        return true;
    }
}
