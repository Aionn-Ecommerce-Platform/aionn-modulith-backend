package com.aionn.identity.infrastructure.security.web;

import com.aionn.identity.application.port.out.auth.AccessTokenClaims;
import com.aionn.identity.application.port.out.auth.AccessTokenIssuerPort;
import com.aionn.identity.application.port.out.auth.AuthSessionPersistencePort;
import com.aionn.identity.application.port.out.auth.TokenBlacklistPort;
import com.aionn.identity.application.port.out.user.UserPersistencePort;
import com.aionn.identity.domain.model.AuthSession;
import com.aionn.identity.domain.model.IdentityUser;
import com.aionn.identity.domain.valueobject.AuthSessionStatus;
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
import java.time.Clock;
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
    private final AuthSessionPersistencePort authSessionPersistencePort;
    private final UserPersistencePort userPersistencePort;
    private final Clock clock;

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

        if (claims.jti() != null) {
            boolean revoked;
            try {
                revoked = tokenBlacklist.isBlacklisted(claims.jti());
            } catch (RuntimeException ex) {
                // Fail closed: if the blacklist store is unreachable we cannot
                // prove the token has not been revoked, so treat this request
                // as unauthenticated instead of accepting it silently.
                log.warn("Blacklist check failed for jti={}, denying auth", claims.jti(), ex);
                filterChain.doFilter(request, response);
                return;
            }
            if (revoked) {
                log.debug("Bearer token jti={} is blacklisted", claims.jti());
                filterChain.doFilter(request, response);
                return;
            }
        }

        if (!isSessionAndUserActive(claims)) {
            filterChain.doFilter(request, response);
            return;
        }

        List<String> roles = claims.roles();
        Collection<? extends GrantedAuthority> authorities = roles.isEmpty()
                ? List.of()
                : roles.stream()
                        .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                        .toList();

        var authentication = new UsernamePasswordAuthenticationToken(claims.userId(), token, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        request.setAttribute(SecurityRequestAttributeKeys.SESSION_ID, claims.sessionId());

        filterChain.doFilter(request, response);
    }

    /**
     * A signed access token is not enough: the session may have been revoked (logout, logout-all,
     * password reset, admin revoke) and the user may have been suspended/banned/deleted. Checking
     * both makes revocation effective before the access token's natural expiry.
     */
    private boolean isSessionAndUserActive(AccessTokenClaims claims) {
        try {
            Optional<AuthSession> sessionOpt = authSessionPersistencePort.findById(claims.sessionId());
            if (sessionOpt.isEmpty()) {
                return false;
            }
            AuthSession session = sessionOpt.get();
            if (!AuthSessionStatus.ACTIVE.equals(session.getStatus())
                    || session.isExpired(clock)
                    || !claims.userId().equals(session.getUserId())) {
                return false;
            }
            Optional<IdentityUser> userOpt = userPersistencePort.findById(claims.userId());
            return userOpt.isPresent() && userOpt.get().isActive();
        } catch (RuntimeException ex) {
            log.warn("Session/user lookup failed while authenticating; denying auth", ex);
            return false;
        }
    }
}
