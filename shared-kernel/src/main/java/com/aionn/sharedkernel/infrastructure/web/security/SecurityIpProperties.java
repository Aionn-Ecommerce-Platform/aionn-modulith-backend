package com.aionn.sharedkernel.infrastructure.web.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "security.ip")
@Getter
@Setter
public class SecurityIpProperties {

	private List<String> trustedProxies = new ArrayList<>();
	private List<RateLimitRule> rateLimits = new ArrayList<>();
	private Cors cors = new Cors();

	public void setTrustedProxies(List<String> trustedProxies) {
		if (trustedProxies == null) {
			this.trustedProxies = new ArrayList<>();
			return;
		}
		this.trustedProxies = trustedProxies.stream()
				.filter(s -> s != null && !s.isBlank())
				.map(String::trim)
				.toList();
	}

	@Getter
	@Setter
	public static class RateLimitRule {
		private String path;
		private String method;
		private int maxRequests;
		private int windowSeconds;
	}

	@Getter
	@Setter
	public static class Cors {
		private List<String> allowedOrigins = new ArrayList<>();

		public void setAllowedOrigins(List<String> allowedOrigins) {
			if (allowedOrigins == null) {
				this.allowedOrigins = new ArrayList<>();
				return;
			}
			this.allowedOrigins = allowedOrigins.stream()
					.filter(origin -> origin != null && !origin.isBlank())
					.map(String::trim)
					.toList();
		}
	}
}
