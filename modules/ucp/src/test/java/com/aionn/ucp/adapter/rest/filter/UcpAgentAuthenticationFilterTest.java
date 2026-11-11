package com.aionn.ucp.adapter.rest.filter;

import com.aionn.ucp.infrastructure.config.UcpProperties;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UcpAgentAuthenticationFilterTest {

    private UcpProperties props(String keys) {
        return new UcpProperties(
                "2026-08-25",
                "http://localhost:8080/ucp/v1",
                "Aionn Test",
                Set.of("https://ucp.dev/schemas/"),
                false,
                new UcpProperties.Capabilities(false, false, false, false, false),
                keys,
                "VND",
                "");
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void noOpWhenNoKeysProvisioned() throws ServletException, IOException {
        UcpAgentAuthenticationFilter filter = new UcpAgentAuthenticationFilter(props(""));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/ucp/v1/checkout-sessions");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200); // passed through
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void skipsNonUcpPaths() throws ServletException, IOException {
        UcpAgentAuthenticationFilter filter = new UcpAgentAuthenticationFilter(props("plat_a=secretA"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/catalog");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void rejectsAnonymousRequestWithoutKey() throws ServletException, IOException {
        UcpAgentAuthenticationFilter filter = new UcpAgentAuthenticationFilter(props("plat_a=secretA"));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/ucp/v1/checkout-sessions");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("signature_missing");
    }

    @Test
    void rejectsInvalidKey() throws ServletException, IOException {
        UcpAgentAuthenticationFilter filter = new UcpAgentAuthenticationFilter(props("plat_a=secretA"));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/ucp/v1/checkout-sessions");
        request.addHeader(UcpAgentAuthenticationFilter.HEADER_API_KEY, "wrong-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("signature_invalid");
    }

    @Test
    void bindsPlatformIdentityForValidKeyWithoutBuyer() throws ServletException, IOException {
        UcpAgentAuthenticationFilter filter = new UcpAgentAuthenticationFilter(props("plat_a=secretA"));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/ucp/v1/checkout-sessions");
        request.addHeader(UcpAgentAuthenticationFilter.HEADER_API_KEY, "secretA");
        request.setAttribute(UcpHeaderFilter.ATTR_UCP_AGENT, "https://platform.example/profile");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("ucp:platform:plat_a");
        assertThat(request.getAttribute(UcpAgentAuthenticationFilter.ATTR_PLATFORM_ID)).isEqualTo("plat_a");
    }

    @Test
    void keepsBuyerPrincipalAndOnlyStampsPlatformAttribute() throws ServletException, IOException {
        UcpAgentAuthenticationFilter filter = new UcpAgentAuthenticationFilter(props("plat_a=secretA"));
        // Buyer already authenticated upstream by the bearer filter.
        var buyer = new UsernamePasswordAuthenticationToken(
                "user-1", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(buyer);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/ucp/v1/checkout-sessions");
        request.addHeader(UcpAgentAuthenticationFilter.HEADER_API_KEY, "secretA");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("user-1");
        assertThat(request.getAttribute(UcpAgentAuthenticationFilter.ATTR_PLATFORM_ID)).isEqualTo("plat_a");
    }
}
