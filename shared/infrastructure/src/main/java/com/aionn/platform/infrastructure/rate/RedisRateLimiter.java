package com.aionn.platform.infrastructure.rate;

import com.aionn.sharedkernel.application.port.RateLimiterPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Redis-backed fixed-window limiter, shared by every application instance.
 * Fails open: a Redis outage must not block notifications or other best-effort work.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisRateLimiter implements RateLimiterPort {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> rateLimitScript = createScript();

    private static DefaultRedisScript<Long> createScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/redis/rate_limit.lua"));
        script.setResultType(Long.class);
        return script;
    }

    @Override
    public boolean allow(String namespace, String key, int maxRequests, int windowSeconds) {
        if (maxRequests <= 0) {
            return true;
        }
        String redisKey = namespace + ":" + key;
        try {
            Long result = redisTemplate.execute(
                    rateLimitScript,
                    List.of(redisKey),
                    String.valueOf(windowSeconds),
                    String.valueOf(maxRequests));
            return Long.valueOf(1L).equals(result);
        } catch (RuntimeException ex) {
            log.warn("Rate limiter unavailable for {}; allowing request", redisKey, ex);
            return true;
        }
    }
}
