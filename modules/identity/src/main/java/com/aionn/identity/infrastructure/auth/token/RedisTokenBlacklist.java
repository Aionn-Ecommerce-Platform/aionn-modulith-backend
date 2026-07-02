package com.aionn.identity.infrastructure.auth.token;

import com.aionn.identity.application.port.out.auth.TokenBlacklistPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisTokenBlacklist implements TokenBlacklistPort {

    private static final String KEY_PREFIX = "identity:token-blacklist:";
    private static final Duration MIN_TTL = Duration.ofSeconds(1);

    private final StringRedisTemplate redisTemplate;

    @Override
    public void blacklist(String jti, Duration ttl) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        Duration effectiveTtl = (ttl == null || ttl.compareTo(MIN_TTL) < 0) ? MIN_TTL : ttl;
        redisTemplate.opsForValue().set(KEY_PREFIX + jti, "1", effectiveTtl);
        log.debug("Blacklisted token jti={}, ttl={}", jti, effectiveTtl);
    }

    @Override
    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + jti));
    }
}
