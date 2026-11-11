package com.aionn.platform.infrastructure.web.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.aionn.sharedkernel.util.IdGenerator;

import java.io.IOException;

// Seeds an X-Request-Id header into SLF4J MDC so every log line in a request
// carries a stable correlation id. Existing X-Request-Id headers from upstream
// proxies are honored; otherwise a ULID is generated, matching the identifier
// style used across the application.
// Must run before any auth/idempotency filter so audit logs from those filters
// can be correlated. Hence Ordered.HIGHEST_PRECEDENCE.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_MDC_KEY = "requestId";
    private static final int MAX_REQUEST_ID_LENGTH = 128;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank() || requestId.length() > MAX_REQUEST_ID_LENGTH) {
            requestId = IdGenerator.ulid();
        }
        try {
            MDC.put(REQUEST_ID_MDC_KEY, requestId);
            response.setHeader(REQUEST_ID_HEADER, requestId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }
}
