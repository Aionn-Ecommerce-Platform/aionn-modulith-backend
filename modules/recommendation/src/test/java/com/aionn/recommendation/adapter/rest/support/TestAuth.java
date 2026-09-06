package com.aionn.recommendation.adapter.rest.support;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

/**
 * Installs an {@link Authentication} for a single MockMvc request without depending on
 * {@code spring-security-test}. Mirrors the helper the other modules use.
 */
public final class TestAuth {

    private TestAuth() {
    }

    public static RequestPostProcessor authUser(String principal) {
        return request -> {
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    principal, "N/A", List.of(new SimpleGrantedAuthority("ROLE_USER")));
            SecurityContextHolder.setContext(new SecurityContextImpl(auth));
            request.setUserPrincipal(auth);
            return request;
        };
    }
}
