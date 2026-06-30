package com.aionn.sharedkernel.infrastructure.web.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RedisIpBlacklistStore {

	private static final String KEY = "security:ip:blacklist";

	private final StringRedisTemplate stringRedisTemplate;

	public boolean isBlacklisted(String ipAddress) {
		if (ipAddress == null || ipAddress.isBlank()) {
			return false;
		}
		try {
			return Boolean.TRUE.equals(stringRedisTemplate.opsForSet().isMember(KEY, ipAddress));
		} catch (Exception ex) {
			log.error("Redis IP blacklist lookup failed for ip {}; failing closed", ipAddress, ex);
			return true;
		}
	}

	public void blacklist(String ipAddress) {
		if (ipAddress == null || ipAddress.isBlank()) {
			return;
		}
		stringRedisTemplate.opsForSet().add(KEY, ipAddress.trim());
	}

	public void unblacklist(String ipAddress) {
		if (ipAddress == null || ipAddress.isBlank()) {
			return;
		}
		stringRedisTemplate.opsForSet().remove(KEY, ipAddress.trim());
	}
}
