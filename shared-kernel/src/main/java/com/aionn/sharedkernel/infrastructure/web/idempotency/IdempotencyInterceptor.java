package com.aionn.sharedkernel.infrastructure.web.idempotency;

import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import com.aionn.sharedkernel.adapter.web.support.idempotency.IdempotentRequest;
import com.aionn.sharedkernel.infrastructure.web.RequestAttributeKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;
    private final RedisIdempotencyStore redisIdempotencyStore;
    private final IdempotencyProperties idempotencyProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!idempotencyProperties.isEnabled() || !(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        IdempotentRequest idempotentRequest = handlerMethod.getMethodAnnotation(IdempotentRequest.class);
        if (idempotentRequest == null) {
            return true;
        }

        String clientKey = request.getHeader("Idempotency-Key");
        if (clientKey == null || clientKey.isBlank()) {
            return true;
        }

        String redisKey = buildRedisKey(request, clientKey.trim());
        String requestHash = hashRequest(request);
        Optional<IdempotencyRecord> existing = redisIdempotencyStore.find(redisKey);
        if (existing.isPresent()) {
            return handleExistingRecord(existing.get(), requestHash, response);
        }

        boolean acquired = redisIdempotencyStore.beginProcessing(
                redisKey,
                requestHash,
                Duration.ofSeconds(idempotencyProperties.getProcessingTtlSeconds()));
        if (!acquired) {
            Optional<IdempotencyRecord> racedRecord = redisIdempotencyStore.find(redisKey);
            if (racedRecord.isPresent()) {
                return handleExistingRecord(racedRecord.get(), requestHash, response);
            }
            writeConflict(response, "Unable to acquire idempotency slot for this request");
            return false;
        }

        request.setAttribute(RequestAttributeKeys.IDEMPOTENCY_ACTIVE, true);
        request.setAttribute(RequestAttributeKeys.IDEMPOTENCY_KEY, redisKey);
        request.setAttribute(RequestAttributeKeys.IDEMPOTENCY_REQUEST_HASH, requestHash);
        request.setAttribute(RequestAttributeKeys.IDEMPOTENCY_TTL_SECONDS, idempotentRequest.ttlSeconds());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception ex) {
        Object active = request.getAttribute(RequestAttributeKeys.IDEMPOTENCY_ACTIVE);
        if (!Boolean.TRUE.equals(active)) {
            return;
        }
        if (Boolean.TRUE.equals(request.getAttribute(RequestAttributeKeys.IDEMPOTENCY_COMPLETED))) {
            return;
        }
        Object storedKey = request.getAttribute(RequestAttributeKeys.IDEMPOTENCY_KEY);
        if (storedKey instanceof String redisKey && !redisKey.isBlank()) {
            redisIdempotencyStore.delete(redisKey);
        }
    }

    private boolean handleExistingRecord(
            IdempotencyRecord existingRecord,
            String requestHash,
            HttpServletResponse response) throws IOException {
        if (!requestHash.equals(existingRecord.requestHash())) {
            writeConflict(response, "Idempotency key was already used with a different request payload");
            return false;
        }
        if (existingRecord.isProcessing()) {
            writeConflict(response, "A request with the same idempotency key is already being processed");
            return false;
        }
        if (existingRecord.isCompleted() && existingRecord.response() != null) {
            response.setStatus(existingRecord.response().status());
            response.setContentType(existingRecord.response().contentType());
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader("Idempotent-Replay", "true");
            response.getWriter().write(existingRecord.response().bodyJson());
            return false;
        }
        writeConflict(response, "Invalid idempotency record state");
        return false;
    }

    private String buildRedisKey(HttpServletRequest request, String clientKey) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String principal = authentication == null || authentication.getName() == null
                ? "anonymous"
                : authentication.getName();
        String query = request.getQueryString() == null ? "" : "?" + request.getQueryString();
        return "http:idempotency:" + request.getMethod() + ":" + request.getRequestURI() + query + ":" + principal + ":"
                + clientKey;
    }

    private String hashRequest(HttpServletRequest request) {
        String principal = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getName)
                .orElse("anonymous");
        String query = request.getQueryString() == null ? "" : "?" + request.getQueryString();
        byte[] body = request instanceof CachedBodyHttpServletRequest cached
                ? cached.getCachedBody()
                : new byte[0];
        String payload = request.getMethod() + "\n" + request.getRequestURI() + query + "\n" + principal + "\n";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(payload.getBytes(StandardCharsets.UTF_8));
            digest.update(body);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Failed to hash idempotent request", ex);
        }
    }

    private void writeConflict(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_CONFLICT);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error("409", message)));
    }
}
