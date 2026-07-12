package com.aionn.identity.infrastructure.auth.jwt;

import com.aionn.identity.application.port.out.auth.AccessTokenClaims;
import com.aionn.identity.application.port.out.auth.AccessTokenIssuerPort;
import com.aionn.identity.infrastructure.config.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AccessTokenIssuerAdapter implements AccessTokenIssuerPort {

    private final JwtProperties jwtProperties;

    @Override
    public String issueAccessToken(String userId, String sessionId, Instant expiresAt, Set<String> roles) {
        Instant now = Instant.now(Clock.systemUTC());
        Instant accessExpiry = now.plusSeconds(jwtProperties.accessTokenExpiryMinutes() * 60L);
        if (accessExpiry.isAfter(expiresAt)) {
            accessExpiry = expiresAt;
        }

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId)
                .claim("sid", sessionId)
                .claim("roles", roles != null ? roles : Set.of())
                .issuer(jwtProperties.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(accessExpiry))
                .signWith(signingKey(), Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public Optional<Instant> extractExpiry(String token) {
        return parse(token)
                .map(Claims::getExpiration)
                .map(Date::toInstant);
    }

    @Override
    public Optional<AccessTokenClaims> parseClaims(String token) {
        return parse(token).map(this::toAccessTokenClaims);
    }

    private AccessTokenClaims toAccessTokenClaims(Claims claims) {
        List<?> rawRoles = claims.get("roles", List.class);
        List<String> roles = rawRoles == null ? null : rawRoles.stream().map(String::valueOf).toList();
        return new AccessTokenClaims(
                claims.getSubject(),
                claims.get("sid", String.class),
                claims.getId(),
                roles);
    }

    private Optional<Claims> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey())
                    .requireIssuer(jwtProperties.issuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private SecretKey signingKey() {
        String secret = jwtProperties.secret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Missing required configuration: IDENTITY_JWT_SECRET");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(bytes);
    }
}
