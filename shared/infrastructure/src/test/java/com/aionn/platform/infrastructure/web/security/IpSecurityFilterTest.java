package com.aionn.platform.infrastructure.web.security;

import com.aionn.platform.infrastructure.web.RequestAttributeKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IpSecurityFilterTest {

    @Mock
    private IpRateLimiter ipRateLimiter;
    @Mock
    private RedisIpBlacklistStore ipBlacklistStore;

    private SecurityIpProperties properties;
    private IpSecurityFilter filter;

    @BeforeEach
    void setUp() {
        properties = new SecurityIpProperties();
        filter = new IpSecurityFilter(new ObjectMapper().findAndRegisterModules(),
                ipRateLimiter, ipBlacklistStore, properties);
    }

    @Test
    void optionsRequestsBypassEveryCheck() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/orders");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        verify(ipBlacklistStore, never()).isBlacklisted(anyString());
    }

    @Test
    void blocksBlacklistedAddresses() throws Exception {
        when(ipBlacklistStore.isBlacklisted("1.1.1.1")).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders");
        request.setRemoteAddr("1.1.1.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("blacklisted");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void rejectsRequestsAboveTheRateLimit() throws Exception {
        properties.setRateLimits(List.of(rule("/api/v1/**", "POST", 5)));
        when(ipRateLimiter.allow(anyString(), anyInt(), anyInt())).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/orders");
        request.setRemoteAddr("2.2.2.2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("Too many requests");
    }

    @Test
    void allowsRequestsWithinTheRateLimitAndPublishesTheClientIp() throws Exception {
        properties.setRateLimits(List.of(rule("/api/v1/**", "POST", 5)));
        when(ipRateLimiter.allow(anyString(), eq(5), eq(60))).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/orders");
        request.setRemoteAddr("3.3.3.3");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(request.getAttribute(RequestAttributeKeys.CLIENT_IP)).isEqualTo("3.3.3.3");
    }

    @Test
    void resolvesTheFirstUntrustedHopFromTheForwardedHeader() throws Exception {
        properties.setTrustedProxies(List.of("10.0.0.1"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders");
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "1.1.1.1, 10.0.0.1");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(request.getAttribute(RequestAttributeKeys.CLIENT_IP)).isEqualTo("1.1.1.1");
    }

    @Test
    void fallsBackToTheRemoteAddressWhenForwardedHopsAreBlank() throws Exception {
        properties.setTrustedProxies(List.of("10.0.0.1"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders");
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "  ,  ");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(request.getAttribute(RequestAttributeKeys.CLIENT_IP)).isEqualTo("10.0.0.1");
    }

    @Test
    void ignoresRulesWithoutAPathOrMethod() throws Exception {
        SecurityIpProperties.RateLimitRule rule = new SecurityIpProperties.RateLimitRule();
        properties.setRateLimits(List.of(rule));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        verify(ipRateLimiter, never()).allow(any(), anyInt(), anyInt());
    }

    @Test
    void ignoresRulesForOtherMethodsOrPaths() throws Exception {
        properties.setRateLimits(List.of(rule("/other/**", "POST", 5)));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        verify(ipRateLimiter, never()).allow(any(), anyInt(), anyInt());
    }

    private static SecurityIpProperties.RateLimitRule rule(String path, String method, int maxRequests) {
        SecurityIpProperties.RateLimitRule rule = new SecurityIpProperties.RateLimitRule();
        rule.setPath(path);
        rule.setMethod(method);
        rule.setMaxRequests(maxRequests);
        rule.setWindowSeconds(60);
        return rule;
    }
}
