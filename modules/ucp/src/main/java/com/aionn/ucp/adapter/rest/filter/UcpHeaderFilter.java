package com.aionn.ucp.adapter.rest.filter;

import com.aionn.ucp.adapter.rest.dto.UcpErrorResponse;
import com.aionn.ucp.adapter.rest.dto.UcpMessage;
import com.aionn.ucp.application.port.out.UcpMetricsPort;
import com.aionn.ucp.infrastructure.config.UcpProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class UcpHeaderFilter extends OncePerRequestFilter {

    public static final String HEADER_REQUEST_ID = "X-Request-Id";
    public static final String HEADER_UCP_AGENT = "UCP-Agent";
    public static final String ATTR_UCP_AGENT = "ucp.agent";
    public static final String MDC_REQUEST_ID_KEY = "requestId";

    private static final String CAPABILITY_DISCOVERY = "discovery";
    private static final String CAPABILITY_CART = "cart";
    private static final String CAPABILITY_CHECKOUT = "checkout";
    private static final String CAPABILITY_CATALOG = "catalog";
    private static final String CAPABILITY_IDENTITY_LINKING = "identity_linking";
    private static final String CAPABILITY_ORDER = "order";
    private static final String VALUE_UNKNOWN = "unknown";
    private static final String OP_GET = "get";
    private static final String OP_CREATE = "create";
    private static final String OP_UPDATE = "update";
    private static final String OP_CANCEL = "cancel";

    private static final Pattern SAFE_REQUEST_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_.-]{1,128}$");
    private static final int MAX_AGENT_LENGTH = 256;

    private final UcpProperties properties;
    private final ObjectMapper objectMapper;
    private final Optional<UcpMetricsPort> metricsPort;

    public UcpHeaderFilter(UcpProperties properties) {
        this(properties, Optional.empty());
    }

    @Autowired
    public UcpHeaderFilter(UcpProperties properties, Optional<UcpMetricsPort> metricsPort) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
        this.metricsPort = metricsPort;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/.well-known/ucp") && !path.startsWith("/ucp");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestId = resolveRequestId(request.getHeader(HEADER_REQUEST_ID));
        MDC.put(MDC_REQUEST_ID_KEY, requestId);
        response.setHeader(HEADER_REQUEST_ID, requestId);

        long startTime = System.nanoTime();
        EndpointInfo endpointInfo = resolveEndpointInfo(request.getMethod(), request.getRequestURI());

        try {
            // Check HTTPS requirement if configured
            if (properties != null && properties.requireHttps() && !isSecure(request)) {
                writeErrorResponse(response, HttpStatus.FORBIDDEN,
                        "https_required", "HTTPS is required for all UCP endpoints");
                return;
            }

            // Extract and sanitize UCP-Agent header
            String rawAgent = request.getHeader(HEADER_UCP_AGENT);
            if (rawAgent != null) {
                String sanitizedAgent = sanitizeAgent(rawAgent);
                request.setAttribute(ATTR_UCP_AGENT, sanitizedAgent);
            }

            // Enforce Content-Type for mutating operations
            String method = request.getMethod();
            if (isMutatingMethod(method) && request.getRequestURI().startsWith("/ucp")) {
                String contentType = request.getContentType();
                if (contentType == null || !isJsonContentType(contentType)) {
                    writeErrorResponse(response, HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                            "unsupported_media_type", "Content-Type must be application/json");
                    return;
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            if (metricsPort.isPresent()) {
                Duration duration = Duration.ofNanos(System.nanoTime() - startTime);
                String status = resolveOutcomeStatus(response.getStatus());
                metricsPort.get().recordRequest(endpointInfo.capability(), endpointInfo.operation(), status);
                metricsPort.get().recordLatency(endpointInfo.capability(), endpointInfo.operation(), duration);
            }
            MDC.remove(MDC_REQUEST_ID_KEY);
        }
    }

    private String resolveRequestId(String incoming) {
        if (incoming != null && !incoming.isBlank() && SAFE_REQUEST_ID_PATTERN.matcher(incoming.trim()).matches()) {
            return incoming.trim();
        }
        return UUID.randomUUID().toString();
    }

    private String sanitizeAgent(String agent) {
        if (agent.length() > MAX_AGENT_LENGTH) {
            agent = agent.substring(0, MAX_AGENT_LENGTH);
        }
        return agent.replace('\r', ' ').replace('\n', ' ').trim();
    }

    private boolean isMutatingMethod(String method) {
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method);
    }

    private boolean isJsonContentType(String contentType) {
        try {
            MediaType mediaType = MediaType.parseMediaType(contentType);
            return mediaType.isCompatibleWith(MediaType.APPLICATION_JSON);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isSecure(HttpServletRequest request) {
        return request.isSecure();
    }

    private void writeErrorResponse(HttpServletResponse response, HttpStatus status, String code, String message)
            throws IOException {
        String version = properties != null ? properties.version() : "2026-08-25";
        UcpErrorResponse errorResponse = UcpErrorResponse.of(version, code, message, UcpMessage.SEVERITY_UNRECOVERABLE);

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    private record EndpointInfo(String capability, String operation) {
    }

    private EndpointInfo resolveEndpointInfo(String method, String path) {
        if (path == null) {
            return new EndpointInfo(VALUE_UNKNOWN, VALUE_UNKNOWN);
        }
        if (path.startsWith("/.well-known/ucp")) {
            return new EndpointInfo(CAPABILITY_DISCOVERY, "lookup");
        }
        if (path.startsWith("/ucp/v1/carts") || path.startsWith("/ucp/carts")) {
            return resolveCartEndpoint(method, path);
        }
        if (path.startsWith("/ucp/v1/checkout-sessions") || path.startsWith("/ucp/checkout-sessions")) {
            return resolveCheckoutEndpoint(method, path);
        }
        if (path.startsWith("/ucp/v1/catalog") || path.startsWith("/ucp/catalog")) {
            return resolveCatalogEndpoint(path);
        }
        if (path.startsWith("/ucp/v1/identity") || path.startsWith("/ucp/identity")) {
            return resolveIdentityEndpoint(method);
        }
        if (path.startsWith("/ucp/v1/orders") || path.startsWith("/ucp/orders")) {
            return new EndpointInfo(CAPABILITY_ORDER, OP_GET);
        }
        return new EndpointInfo(VALUE_UNKNOWN, VALUE_UNKNOWN);
    }

    private EndpointInfo resolveCartEndpoint(String method, String path) {
        if (path.endsWith("/cancel")) {
            return new EndpointInfo(CAPABILITY_CART, OP_CANCEL);
        }
        if ("POST".equalsIgnoreCase(method)) {
            return new EndpointInfo(CAPABILITY_CART, OP_CREATE);
        }
        if ("PUT".equalsIgnoreCase(method)) {
            return new EndpointInfo(CAPABILITY_CART, OP_UPDATE);
        }
        return new EndpointInfo(CAPABILITY_CART, OP_GET);
    }

    private EndpointInfo resolveCheckoutEndpoint(String method, String path) {
        if (path.endsWith("/complete")) {
            return new EndpointInfo(CAPABILITY_CHECKOUT, "complete");
        }
        if (path.endsWith("/cancel")) {
            return new EndpointInfo(CAPABILITY_CHECKOUT, OP_CANCEL);
        }
        if ("POST".equalsIgnoreCase(method)) {
            return new EndpointInfo(CAPABILITY_CHECKOUT, OP_CREATE);
        }
        if ("PUT".equalsIgnoreCase(method)) {
            return new EndpointInfo(CAPABILITY_CHECKOUT, OP_UPDATE);
        }
        return new EndpointInfo(CAPABILITY_CHECKOUT, OP_GET);
    }

    private EndpointInfo resolveCatalogEndpoint(String path) {
        if (path.endsWith("/search")) {
            return new EndpointInfo(CAPABILITY_CATALOG, "search");
        }
        if (path.endsWith("/lookup")) {
            return new EndpointInfo(CAPABILITY_CATALOG, "lookup");
        }
        if (path.endsWith("/product")) {
            return new EndpointInfo(CAPABILITY_CATALOG, "product");
        }
        return new EndpointInfo(CAPABILITY_CATALOG, VALUE_UNKNOWN);
    }

    private EndpointInfo resolveIdentityEndpoint(String method) {
        if ("POST".equalsIgnoreCase(method)) {
            return new EndpointInfo(CAPABILITY_IDENTITY_LINKING, "link");
        }
        if ("DELETE".equalsIgnoreCase(method)) {
            return new EndpointInfo(CAPABILITY_IDENTITY_LINKING, "revoke");
        }
        return new EndpointInfo(CAPABILITY_IDENTITY_LINKING, OP_GET);
    }

    private String resolveOutcomeStatus(int httpStatusCode) {
        if (httpStatusCode >= 200 && httpStatusCode < 400) {
            return "success";
        } else if (httpStatusCode >= 400 && httpStatusCode < 500) {
            return "client_error";
        } else {
            return "server_error";
        }
    }
}
