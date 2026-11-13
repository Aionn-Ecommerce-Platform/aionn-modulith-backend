package com.aionn.ucp.adapter.rest.filter;

import com.aionn.ucp.adapter.rest.dto.response.UcpErrorResponse;
import com.aionn.ucp.adapter.rest.dto.UcpMessage;
import com.aionn.ucp.application.UcpProtocolConstants;
import com.aionn.ucp.infrastructure.config.UcpProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

/**
 * Authenticates the UCP platform/agent on {@code /ucp/**} separately from the buyer JWT.
 * Buyer present: validate/record the platform key, buyer stays principal.
 * No buyer + valid {@code X-UCP-Api-Key}: sets {@code ROLE_UCP_PLATFORM}.
 * No buyer + missing/invalid key: 401. No keys provisioned: no-op (local dev).
 */
@Component
public class UcpAgentAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER_API_KEY = "X-UCP-Api-Key";
    public static final String ATTR_PLATFORM_ID = "ucp.platformId";
    public static final String PLATFORM_PRINCIPAL_PREFIX = "ucp:platform:";

    private static final Logger log = LoggerFactory.getLogger(UcpAgentAuthenticationFilter.class);
    private static final String ROLE_UCP_PLATFORM = "ROLE_UCP_PLATFORM";

    private final Map<String, String> platformKeys; // apiKey -> platformId
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UcpProperties properties;

    public UcpAgentAuthenticationFilter(UcpProperties properties) {
        this.properties = properties;
        this.platformKeys = properties != null ? properties.parsePlatformApiKeys() : Map.of();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Only gate the capability endpoints, not the public discovery document.
        return !path.startsWith("/ucp");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // No keys provisioned: no-op. Buyer JWT handles its own auth; anonymous falls through.
        if (platformKeys.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Buyer already authenticated; still record a supplied platform key for observability.
        var existing = SecurityContextHolder.getContext().getAuthentication();
        boolean buyerAuthenticated = existing != null && existing.isAuthenticated()
                && !"anonymousUser".equals(existing.getPrincipal());
        if (buyerAuthenticated) {
            attachPlatformAttributeIfValid(request);
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader(HEADER_API_KEY);
        if (apiKey == null || apiKey.isBlank()) {
            writeError(response, HttpStatus.UNAUTHORIZED, "signature_missing",
                    "UCP requests require a platform credential or a buyer session");
            return;
        }

        String platformId = lookupPlatform(apiKey.trim());
        if (platformId == null) {
            log.debug("UCP platform credential rejected for {}", request.getRequestURI());
            writeError(response, HttpStatus.UNAUTHORIZED, "signature_invalid",
                    "Invalid platform credential");
            return;
        }
        bindPlatform(platformId, request);
        filterChain.doFilter(request, response);
    }

    private void attachPlatformAttributeIfValid(HttpServletRequest request) {
        String apiKey = request.getHeader(HEADER_API_KEY);
        if (apiKey == null || apiKey.isBlank()) {
            return;
        }
        String platformId = lookupPlatform(apiKey.trim());
        if (platformId != null) {
            request.setAttribute(ATTR_PLATFORM_ID, platformId);
        }
    }

    private void bindPlatform(String platformId, HttpServletRequest request) {
        String agent = (String) request.getAttribute(UcpHeaderFilter.ATTR_UCP_AGENT);
        String principal = agent != null && !agent.isBlank() ? agent : platformId;
        var auth = new UsernamePasswordAuthenticationToken(
                PLATFORM_PRINCIPAL_PREFIX + platformId,
                null,
                List.of(new SimpleGrantedAuthority(ROLE_UCP_PLATFORM)));
        auth.setDetails(principal);
        SecurityContextHolder.getContext().setAuthentication(auth);
        request.setAttribute(ATTR_PLATFORM_ID, platformId);
    }

    /** Constant-time lookup so the response doesn't reveal a matching key prefix. */
    private String lookupPlatform(String candidate) {
        String matched = null;
        byte[] cand = candidate.getBytes(StandardCharsets.UTF_8);
        for (Map.Entry<String, String> e : platformKeys.entrySet()) {
            byte[] known = e.getKey().getBytes(StandardCharsets.UTF_8);
            if (MessageDigest.isEqual(cand, known)) {
                matched = e.getValue();
            }
        }
        return matched;
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String code, String message)
            throws IOException {
        String version = properties != null ? properties.version() : UcpProtocolConstants.VERSION;
        UcpErrorResponse error = UcpErrorResponse.of(version, code, message, UcpMessage.SEVERITY_UNRECOVERABLE);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
