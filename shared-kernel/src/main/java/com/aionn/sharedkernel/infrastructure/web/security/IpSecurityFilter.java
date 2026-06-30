package com.aionn.sharedkernel.infrastructure.web.security;

import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import com.aionn.sharedkernel.infrastructure.web.RequestAttributeKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class IpSecurityFilter extends OncePerRequestFilter {

	private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
	private static final UrlPathHelper PATH_HELPER = new UrlPathHelper();

	private final ObjectMapper objectMapper;
	private final IpRateLimiter ipRateLimiter;
	private final RedisIpBlacklistStore ipBlacklistStore;
	private final SecurityIpProperties securityIpProperties;

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		if (HttpMethod.OPTIONS.matches(request.getMethod())) {
			filterChain.doFilter(request, response);
			return;
		}

		String clientIp = extractClientIp(request);
		request.setAttribute(RequestAttributeKeys.CLIENT_IP, clientIp);

		if (clientIp != null && ipBlacklistStore.isBlacklisted(clientIp)) {
			log.warn("Blocked request from blacklisted IP: {}", clientIp);
			writeError(response, HttpStatus.FORBIDDEN, "Access denied: Your IP is blacklisted");
			return;
		}

		String path = PATH_HELPER.getRequestUri(request);
		for (SecurityIpProperties.RateLimitRule rule : securityIpProperties.getRateLimits()) {
			if (matchesRule(path, request.getMethod(), rule)) {
				String key = String.format("%s:%s:%s",
						clientIp == null ? "unknown" : clientIp,
						rule.getMethod().toUpperCase(),
						rule.getPath());
				boolean allowed = ipRateLimiter.allow(key, rule.getMaxRequests(), rule.getWindowSeconds());
				if (!allowed) {
					log.warn("Rate limited IP {} on {} {}", clientIp, request.getMethod(), path);
					writeError(response, HttpStatus.TOO_MANY_REQUESTS, "Too many requests from this IP");
					return;
				}
				break;
			}
		}
		filterChain.doFilter(request, response);
	}

	private boolean matchesRule(String path, String method, SecurityIpProperties.RateLimitRule rule) {
		if (rule.getPath() == null || rule.getMethod() == null) {
			return false;
		}
		return PATH_MATCHER.match(rule.getPath(), path)
				&& rule.getMethod().equalsIgnoreCase(method);
	}

	private String extractClientIp(HttpServletRequest request) {
		String remoteAddr = request.getRemoteAddr();
		Set<String> trustedProxies = Set.copyOf(securityIpProperties.getTrustedProxies());
		if (!trustedProxies.isEmpty() && trustedProxies.contains(remoteAddr)) {
			String forwardedFor = request.getHeader("X-Forwarded-For");
			if (forwardedFor != null && !forwardedFor.isBlank()) {
				List<String> hops = List.of(forwardedFor.split(","));
				if (!hops.isEmpty()) {
					return hops.get(0).trim();
				}
			}
		}
		return remoteAddr;
	}

	private void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		ApiResponse<Void> body = ApiResponse.error(String.valueOf(status.value()), message);
		response.getWriter().write(objectMapper.writeValueAsString(body));
	}
}
