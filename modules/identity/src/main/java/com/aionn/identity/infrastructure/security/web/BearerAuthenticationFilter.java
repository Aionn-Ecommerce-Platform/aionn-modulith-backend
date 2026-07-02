package com.aionn.identity.infrastructure.security.web;

import com.aionn.identity.application.port.out.auth.AccessTokenClaims;
import com.aionn.identity.application.port.out.auth.AccessTokenIssuerPort;
import com.aionn.identity.application.port.out.auth.TokenBlacklistPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class BearerAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final AccessTokenIssuerPort tokenIssuer;
    private final TokenBlacklistPort tokenBlacklist;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Don't overwrite an already-authenticated context (e.g. set by a preceding
        // filter or @WithMockUser in tests). Spring Security's standard pattern.
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(HEADER);
        if (header == null || !header.startsWith(PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(PREFIX.length()).trim();
        Optional<AccessTokenClaims> parsed = tokenIssuer.parseClaims(token);
        if (parsed.isEmpty()) {
            log.debug("Bearer token failed signature/expiry validation");
            filterChain.doFilter(request, response);
            return;
        }

        AccessTokenClaims claims = parsed.get();
        if (claims.userId() == null || claims.sessionId() == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (claims.jti() != null && tokenBlacklist.isBlacklisted(claims.jti())) {
            log.debug("Bearer token jti={} is blacklisted", claims.jti());
            filterChain.doFilter(request, response);
            return;
        }

        List<String> roles = claims.roles();
        Collection<? extends GrantedAuthority> authorities;
        if (!roles.isEmpty()) {
            authorities = roles.stream()
                    .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();
        } else {
            authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }

        var authentication = new UsernamePasswordAuthenticationToken(claims.userId(), token, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        request.setAttribute(SecurityRequestAttributeKeys.SESSION_ID, claims.sessionId());

        filterChain.doFilter(request, response);
    }
}
