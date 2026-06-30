package com.aionn.sharedkernel.infrastructure.web.idempotency;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class IdempotencyBodyCachingFilter extends OncePerRequestFilter {

    private final IdempotencyProperties idempotencyProperties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String idempotencyKey = request.getHeader("Idempotency-Key");
        return idempotencyKey == null || idempotencyKey.isBlank();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(
                new CachedBodyHttpServletRequest(request, idempotencyProperties.getMaxCachedBodyBytes()),
                response);
    }
}
