package com.aionn.sharedkernel.infrastructure.web.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@Slf4j
@RequiredArgsConstructor
public class IpRateLimiter {

	private static final String KEY_PREFIX = "security:ip-rate-limit:";

	private final StringRedisTemplate stringRedisTemplate;
	private final DefaultRedisScript<Long> rateLimitScript = createScript();

	private static DefaultRedisScript<Long> createScript() {
		DefaultRedisScript<Long> script = new DefaultRedisScript<>();
		script.setLocation(new ClassPathResource("scripts/redis/rate_limit.lua"));
		script.setResultType(Long.class);
		return script;
	}

	public boolean allow(String key, int maxRequests, int windowSeconds) {
		String redisKey = KEY_PREFIX + key;
		try {
			Long result = stringRedisTemplate.execute(
					rateLimitScript,
					Collections.singletonList(redisKey),
					String.valueOf(windowSeconds),
					String.valueOf(maxRequests));
			return Long.valueOf(1L).equals(result);
		} catch (Exception ex) {
			log.error("Redis rate limiter failed for key {}; failing closed", redisKey, ex);
			return false;
		}
	}
}
