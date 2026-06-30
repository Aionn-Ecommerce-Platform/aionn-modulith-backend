package com.aionn.sharedkernel.infrastructure.web.idempotency;

import com.aionn.sharedkernel.adapter.web.support.idempotency.IdempotentRequest;
import com.aionn.sharedkernel.infrastructure.web.RequestAttributeKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.time.Duration;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class IdempotencyResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;
    private final RedisIdempotencyStore redisIdempotencyStore;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return returnType.getMethodAnnotation(IdempotentRequest.class) != null;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)
                || !(response instanceof ServletServerHttpResponse servletResponse)) {
            return body;
        }

        var servlet = servletRequest.getServletRequest();
        if (!Boolean.TRUE.equals(servlet.getAttribute(RequestAttributeKeys.IDEMPOTENCY_ACTIVE))) {
            return body;
        }

        int status = servletResponse.getServletResponse().getStatus();
        if (status < 200 || status >= 300) {
            return body;
        }

        String redisKey = (String) servlet.getAttribute(RequestAttributeKeys.IDEMPOTENCY_KEY);
        String requestHash = (String) servlet.getAttribute(RequestAttributeKeys.IDEMPOTENCY_REQUEST_HASH);
        Integer ttlSeconds = (Integer) servlet.getAttribute(RequestAttributeKeys.IDEMPOTENCY_TTL_SECONDS);
        if (redisKey == null || requestHash == null || ttlSeconds == null) {
            return body;
        }

        try {
            String contentType = selectedContentType == null
                    ? MediaType.APPLICATION_JSON_VALUE
                    : selectedContentType.toString();
            String bodyJson = objectMapper.writeValueAsString(body);
            redisIdempotencyStore.saveCompleted(
                    redisKey,
                    requestHash,
                    new IdempotencyRecord.StoredHttpResponse(status, contentType, bodyJson),
                    Duration.ofSeconds(ttlSeconds));
            servlet.setAttribute(RequestAttributeKeys.IDEMPOTENCY_COMPLETED, true);
            servletResponse.getServletResponse().setHeader("Idempotent-Replay", "false");
        } catch (Exception ex) {
            log.error("Failed to persist idempotent response for {}", redisKey, ex);
            throw new IllegalStateException("Failed to persist idempotent response", ex);
        }
        return body;
    }
}
