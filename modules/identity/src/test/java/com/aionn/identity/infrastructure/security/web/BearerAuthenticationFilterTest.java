package com.aionn.identity.infrastructure.security.web;

import com.aionn.identity.application.port.out.auth.AccessTokenClaims;
import com.aionn.identity.application.port.out.auth.AccessTokenIssuerPort;
import com.aionn.identity.application.port.out.auth.TokenBlacklistPort;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BearerAuthenticationFilterTest {

    @Mock
    private AccessTokenIssuerPort tokenIssuer;
    @Mock
    private TokenBlacklistPort tokenBlacklist;
    @Mock
    private FilterChain chain;

    private BearerAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new BearerAuthenticationFilter(tokenIssuer, tokenBlacklist);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void noAuthorizationHeaderSkipsAuthentication() throws Exception {
        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void nonBearerHeaderSkipsAuthentication() throws Exception {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void invalidTokenContinuesChainWithoutAuthentication() throws Exception {
        request.addHeader("Authorization", "Bearer bad-token");
        when(tokenIssuer.parseClaims("bad-token")).thenReturn(Optional.empty());

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void blacklistedJtiSkipsAuthentication() throws Exception {
        request.addHeader("Authorization", "Bearer good-token");
        when(tokenIssuer.parseClaims("good-token")).thenReturn(Optional.of(
                new AccessTokenClaims("user-1", "session-1", "jti-1", List.of("BUYER"))));
        when(tokenBlacklist.isBlacklisted("jti-1")).thenReturn(true);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void claimsWithMissingUserIdSkipAuthentication() throws Exception {
        request.addHeader("Authorization", "Bearer t");
        when(tokenIssuer.parseClaims("t")).thenReturn(Optional.of(
                new AccessTokenClaims(null, "session-1", "jti", List.of())));

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void claimsWithMissingSessionIdSkipAuthentication() throws Exception {
        request.addHeader("Authorization", "Bearer t");
        when(tokenIssuer.parseClaims("t")).thenReturn(Optional.of(
                new AccessTokenClaims("user-1", null, "jti", List.of())));

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void validTokenSetsAuthenticationAndSessionAttribute() throws Exception {
        request.addHeader("Authorization", "Bearer good-token");
        when(tokenIssuer.parseClaims("good-token")).thenReturn(Optional.of(
                new AccessTokenClaims("user-1", "session-1", "jti-1", List.of("BUYER", "MERCHANT"))));
        when(tokenBlacklist.isBlacklisted("jti-1")).thenReturn(false);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("user-1", auth.getPrincipal());
        Set<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toSet());
        assertTrue(roles.contains("ROLE_BUYER"));
        assertTrue(roles.contains("ROLE_MERCHANT"));
        assertEquals("session-1", request.getAttribute(SecurityRequestAttributeKeys.SESSION_ID));
    }

    @Test
    void emptyRolesFallsBackToRoleUser() throws Exception {
        request.addHeader("Authorization", "Bearer t");
        when(tokenIssuer.parseClaims("t")).thenReturn(Optional.of(
                new AccessTokenClaims("user-1", "session-1", "jti", List.of())));
        when(tokenBlacklist.isBlacklisted("jti")).thenReturn(false);

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        Set<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of("ROLE_USER"), roles);
    }

    @Test
    void filterIsAppliedOncePerRequest() throws Exception {
        request.addHeader("Authorization", "Bearer t");
        when(tokenIssuer.parseClaims("t")).thenReturn(Optional.empty());

        filter.doFilter(request, response, chain);
        filter.doFilter(request, response, chain);

        verify(chain, times(2)).doFilter(request, response);
    }
}
