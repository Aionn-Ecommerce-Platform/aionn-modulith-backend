package com.aionn.identity.infrastructure.security;

import com.aionn.identity.application.port.out.auth.AccessTokenClaims;
import com.aionn.identity.application.port.out.auth.AccessTokenIssuerPort;
import com.aionn.identity.application.port.out.auth.TokenBlacklistPort;
import com.aionn.identity.infrastructure.security.web.BearerAuthenticationFilter;
import com.aionn.identity.infrastructure.security.web.SecurityRequestAttributeKeys;
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
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BearerAuthenticationFilterTest {

    @Mock
    private AccessTokenIssuerPort tokenIssuer;
    @Mock
    private TokenBlacklistPort tokenBlacklist;
    @Mock
    private FilterChain filterChain;

    private BearerAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new BearerAuthenticationFilter(tokenIssuer, tokenBlacklist);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validBearerTokenSetsAuthenticationAndSessionAttribute() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer good-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AccessTokenClaims claims = new AccessTokenClaims("user-1", "session-1", "jti-1", List.of("USER"));
        when(tokenIssuer.parseClaims("good-token")).thenReturn(Optional.of(claims));
        when(tokenBlacklist.isBlacklisted("jti-1")).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("user-1", auth.getPrincipal());
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_USER".equals(a.getAuthority())));
        assertEquals("session-1", request.getAttribute(SecurityRequestAttributeKeys.SESSION_ID));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void missingAuthorizationHeaderPassesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void invalidTokenPassesThroughWithoutAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenIssuer.parseClaims("bad-token")).thenReturn(Optional.empty());

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void blacklistedTokenIsNotAuthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer revoked-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AccessTokenClaims claims = new AccessTokenClaims("user-2", "session-2", "jti-revoked", List.of());
        when(tokenIssuer.parseClaims("revoked-token")).thenReturn(Optional.of(claims));
        when(tokenBlacklist.isBlacklisted("jti-revoked")).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}
