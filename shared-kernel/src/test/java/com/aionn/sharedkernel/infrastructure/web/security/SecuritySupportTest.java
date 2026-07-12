package com.aionn.sharedkernel.infrastructure.web.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aionn.sharedkernel.infrastructure.web.RequestAttributeKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SecuritySupportTest {

    @Test
    void securityPropertiesTrimAndFilterConfiguredLists() {
        SecurityIpProperties properties = new SecurityIpProperties();
        SecurityIpProperties.Cors cors = new SecurityIpProperties.Cors();

        properties.setTrustedProxies(Arrays.asList(" 127.0.0.1 ", "", null, "10.0.0.1"));
        cors.setAllowedOrigins(Arrays.asList(" https://aionn.vn ", "", null, "https://admin.aionn.vn"));
        properties.setCors(cors);

        assertEquals(List.of("127.0.0.1", "10.0.0.1"), properties.getTrustedProxies());
        assertEquals(List.of("https://aionn.vn", "https://admin.aionn.vn"), properties.getCors().getAllowedOrigins());
    }

    @Test
    void redisIpBlacklistStoreHandlesHappyPathAndFailures() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        SetOperations<String, String> setOperations = mock();
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.isMember(anyString(), anyString())).thenReturn(true);

        RedisIpBlacklistStore store = new RedisIpBlacklistStore(redisTemplate);

        assertTrue(store.isBlacklisted("1.1.1.1"));
        assertFalse(store.isBlacklisted(" "));
        store.blacklist(" 1.1.1.1 ");
        store.unblacklist(" 1.1.1.1 ");
        verify(setOperations).add(anyString(), anyString());
        verify(setOperations).remove(anyString(), anyString());

        when(setOperations.isMember(anyString(), anyString())).thenThrow(new RuntimeException("redis down"));
        assertTrue(store.isBlacklisted("2.2.2.2"));
    }

    @Test
    void ipRateLimiterReturnsFalseWhenRedisFailsAndTrueForAllowedResult() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(), anyList(), anyString(), anyString())).thenReturn(1L);

        IpRateLimiter limiter = new IpRateLimiter(redisTemplate);

        assertTrue(limiter.allow("1.1.1.1:POST:/api", 3, 60));

        when(redisTemplate.execute(any(), anyList(), anyString(), anyString()))
                .thenThrow(new RuntimeException("redis down"));
        assertFalse(limiter.allow("1.1.1.1:POST:/api", 3, 60));
    }

    @Test
    void ipSecurityFilterHandlesOptionsBlacklistRateLimitAndTrustedProxy() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        IpRateLimiter rateLimiter = mock(IpRateLimiter.class);
        RedisIpBlacklistStore blacklistStore = mock(RedisIpBlacklistStore.class);
        SecurityIpProperties properties = new SecurityIpProperties();
        SecurityIpProperties.RateLimitRule rule = new SecurityIpProperties.RateLimitRule();
        rule.setMethod("POST");
        rule.setPath("/api/**");
        rule.setMaxRequests(1);
        rule.setWindowSeconds(60);
        properties.setRateLimits(List.of(rule));
        properties.setTrustedProxies(List.of("10.0.0.1"));

        IpSecurityFilter filter = new IpSecurityFilter(objectMapper, rateLimiter, blacklistStore, properties);
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest options = new MockHttpServletRequest("OPTIONS", "/api/v1/orders");
        MockHttpServletResponse optionsResponse = new MockHttpServletResponse();
        filter.doFilter(options, optionsResponse, chain);
        verify(chain).doFilter(options, optionsResponse);

        MockHttpServletRequest blocked = new MockHttpServletRequest("POST", "/api/v1/orders");
        blocked.setRemoteAddr("1.1.1.1");
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        FilterChain blockedChain = mock(FilterChain.class);
        when(blacklistStore.isBlacklisted("1.1.1.1")).thenReturn(true);
        filter.doFilter(blocked, blockedResponse, blockedChain);
        assertEquals(403, blockedResponse.getStatus());
        verify(blockedChain, never()).doFilter(any(), any());

        MockHttpServletRequest limited = new MockHttpServletRequest("POST", "/api/v1/orders");
        limited.setRemoteAddr("1.1.1.2");
        MockHttpServletResponse limitedResponse = new MockHttpServletResponse();
        FilterChain limitedChain = mock(FilterChain.class);
        when(blacklistStore.isBlacklisted("1.1.1.2")).thenReturn(false);
        when(rateLimiter.allow(anyString(), anyInt(), anyInt())).thenReturn(false);
        filter.doFilter(limited, limitedResponse, limitedChain);
        assertEquals(429, limitedResponse.getStatus());
        verify(limitedChain, never()).doFilter(any(), any());

        MockHttpServletRequest forwarded = new MockHttpServletRequest("POST", "/api/v1/orders");
        forwarded.setRemoteAddr("10.0.0.1");
        forwarded.addHeader("X-Forwarded-For", "10.0.0.2, 203.0.113.10");
        MockHttpServletResponse forwardedResponse = new MockHttpServletResponse();
        FilterChain forwardedChain = mock(FilterChain.class);
        when(rateLimiter.allow(anyString(), anyInt(), anyInt())).thenReturn(true);
        filter.doFilter(forwarded, forwardedResponse, forwardedChain);
        assertEquals("203.0.113.10", forwarded.getAttribute(RequestAttributeKeys.CLIENT_IP));
        verify(blacklistStore).isBlacklisted("203.0.113.10");
        verify(forwardedChain).doFilter(forwarded, forwardedResponse);
    }
}
