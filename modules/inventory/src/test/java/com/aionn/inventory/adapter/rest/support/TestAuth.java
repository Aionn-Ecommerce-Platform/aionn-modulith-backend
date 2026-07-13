package com.aionn.inventory.adapter.rest.support;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Arrays;
import java.util.List;

/**
 * Test helper that installs a {@link Authentication} into the
 * {@link SecurityContextHolder} for a single MockMvc request, without depending
 * on {@code spring-security-test}.
 *
 * <p>
 * Use as a {@link RequestPostProcessor}:
 * {@code mockMvc.perform(post("/...").with(TestAuth.authUser("alice", "USER")))}.
 * </p>
 */
public final class TestAuth {

    private TestAuth() {
    }

    public static RequestPostProcessor authUser(String principal, String... roles) {
        return request -> {
            List<SimpleGrantedAuthority> authorities = roles.length == 0
                    ? List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    : Arrays.stream(roles)
                            .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                            .map(SimpleGrantedAuthority::new)
                            .toList();
            Authentication auth = new UsernamePasswordAuthenticationToken(principal, "N/A", authorities);
            SecurityContextHolder.setContext(new SecurityContextImpl(auth));
            request.setUserPrincipal(auth);
            return request;
        };
    }

    public static RequestPostProcessor authAdmin(String adminId, String... roles) {
        return authUser(adminId, roles.length == 0 ? new String[]{"ROLE_SYSTEM_ADMIN"} : roles);
    }
}
