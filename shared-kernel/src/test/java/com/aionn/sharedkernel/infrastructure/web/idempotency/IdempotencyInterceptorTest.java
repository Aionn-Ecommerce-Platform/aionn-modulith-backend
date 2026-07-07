package com.aionn.sharedkernel.infrastructure.web.idempotency;

import com.aionn.sharedkernel.adapter.web.support.idempotency.IdempotentRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdempotencyInterceptorTest {
    private static final int MAX_CACHED_BODY_BYTES = 16 * 1024;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final RedisIdempotencyStore redisIdempotencyStore = mock(RedisIdempotencyStore.class);
    private final IdempotencyProperties properties = new IdempotencyProperties();
    private final IdempotencyInterceptor interceptor = new IdempotencyInterceptor(objectMapper, redisIdempotencyStore,
            properties);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void preHandleReplaysStoredResponseForMatchingRequest() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user-1", "n/a"));

        MockHttpServletRequest rawRequest = new MockHttpServletRequest("POST", "/api/v1/kyc");
        rawRequest.addHeader("Idempotency-Key", "abc-123");
        rawRequest.setContentType("application/json");
        rawRequest.setContent("{\"docType\":\"PASSPORT\"}".getBytes(StandardCharsets.UTF_8));
        CachedBodyHttpServletRequest request = new CachedBodyHttpServletRequest(rawRequest, MAX_CACHED_BODY_BYTES);
        MockHttpServletResponse response = new MockHttpServletResponse();

        String requestHash = hash("POST", "/api/v1/kyc", null, "user-1", rawRequest.getContentAsByteArray());
        when(redisIdempotencyStore.find(eq("http:idempotency:POST:/api/v1/kyc:user-1:abc-123")))
                .thenReturn(Optional.of(IdempotencyRecord.completed(
                        requestHash,
                        new IdempotencyRecord.StoredHttpResponse(
                                201,
                                "application/json",
                                "{\"statusCode\":\"201\",\"message\":\"ok\"}"))));

        boolean allowed = interceptor.preHandle(request, response, handlerMethod());

        assertFalse(allowed);
        assertEquals(201, response.getStatus());
        assertEquals("true", response.getHeader("Idempotent-Replay"));
        assertEquals("{\"statusCode\":\"201\",\"message\":\"ok\"}", response.getContentAsString());
        verify(redisIdempotencyStore, never()).beginProcessing(any(), any(), any());
    }

    @Test
    void preHandleRejectsSameKeyForDifferentPayload() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user-1", "n/a"));

        MockHttpServletRequest rawRequest = new MockHttpServletRequest("POST", "/api/v1/kyc");
        rawRequest.addHeader("Idempotency-Key", "abc-123");
        rawRequest.setContentType("application/json");
        rawRequest.setContent("{\"docType\":\"ID_CARD\"}".getBytes(StandardCharsets.UTF_8));
        CachedBodyHttpServletRequest request = new CachedBodyHttpServletRequest(rawRequest, MAX_CACHED_BODY_BYTES);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(redisIdempotencyStore.find(eq("http:idempotency:POST:/api/v1/kyc:user-1:abc-123")))
                .thenReturn(Optional.of(IdempotencyRecord.completed(
                        "different-hash",
                        new IdempotencyRecord.StoredHttpResponse(
                                201,
                                "application/json",
                                "{\"statusCode\":\"201\"}"))));

        boolean allowed = interceptor.preHandle(request, response, handlerMethod());

        assertFalse(allowed);
        assertEquals(409, response.getStatus());
        assertTrue(response.getContentAsString()
                .contains("Idempotency key was already used with a different request payload"));
    }

    @Test
    void preHandleAllowsWhenIdempotencyDisabled() throws Exception {
        properties.setEnabled(false);

        boolean allowed = interceptor.preHandle(
                new MockHttpServletRequest("POST", "/api/v1/kyc"),
                new MockHttpServletResponse(),
                handlerMethod());

        assertTrue(allowed);
        verify(redisIdempotencyStore, never()).find(any());
    }

    @Test
    void preHandleAllowsWhenHandlerIsNotHandlerMethod() throws Exception {
        boolean allowed = interceptor.preHandle(
                new MockHttpServletRequest("POST", "/api/v1/kyc"),
                new MockHttpServletResponse(),
                new Object());

        assertTrue(allowed);
        verify(redisIdempotencyStore, never()).find(any());
    }

    @Test
    void preHandleAllowsWhenAnnotationMissing() throws Exception {
        HandlerMethod plainHandler = new HandlerMethod(
                new DummyController(), DummyController.class.getMethod("plain"));

        boolean allowed = interceptor.preHandle(
                new MockHttpServletRequest("POST", "/api/v1/kyc"),
                new MockHttpServletResponse(),
                plainHandler);

        assertTrue(allowed);
        verify(redisIdempotencyStore, never()).find(any());
    }

    @Test
    void preHandleAllowsWhenIdempotencyKeyHeaderMissing() throws Exception {
        boolean allowed = interceptor.preHandle(
                new MockHttpServletRequest("POST", "/api/v1/kyc"),
                new MockHttpServletResponse(),
                handlerMethod());

        assertTrue(allowed);
        verify(redisIdempotencyStore, never()).find(any());
    }

    @Test
    void preHandleBeginsProcessingForNewRequest() throws Exception {
        MockHttpServletRequest rawRequest = new MockHttpServletRequest("POST", "/api/v1/kyc");
        rawRequest.addHeader("Idempotency-Key", "abc-123");
        rawRequest.setContent("{\"docType\":\"PASSPORT\"}".getBytes(StandardCharsets.UTF_8));
        CachedBodyHttpServletRequest request = new CachedBodyHttpServletRequest(rawRequest, MAX_CACHED_BODY_BYTES);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(redisIdempotencyStore.find(any())).thenReturn(Optional.empty());
        when(redisIdempotencyStore.beginProcessing(any(), any(), any())).thenReturn(true);

        boolean allowed = interceptor.preHandle(request, response, handlerMethod());

        assertTrue(allowed);
        assertEquals(Boolean.TRUE,
                request.getAttribute(
                        com.aionn.sharedkernel.infrastructure.web.RequestAttributeKeys.IDEMPOTENCY_ACTIVE));
        assertEquals("http:idempotency:POST:/api/v1/kyc:anonymous:abc-123",
                request.getAttribute(com.aionn.sharedkernel.infrastructure.web.RequestAttributeKeys.IDEMPOTENCY_KEY));
    }

    @Test
    void preHandleWritesConflictWhenSlotUnavailableAndNoRecord() throws Exception {
        MockHttpServletRequest rawRequest = new MockHttpServletRequest("POST", "/api/v1/kyc");
        rawRequest.addHeader("Idempotency-Key", "abc-123");
        rawRequest.setContent("{}".getBytes(StandardCharsets.UTF_8));
        CachedBodyHttpServletRequest request = new CachedBodyHttpServletRequest(rawRequest, MAX_CACHED_BODY_BYTES);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(redisIdempotencyStore.find(any())).thenReturn(Optional.empty());
        when(redisIdempotencyStore.beginProcessing(any(), any(), any())).thenReturn(false);

        boolean allowed = interceptor.preHandle(request, response, handlerMethod());

        assertFalse(allowed);
        assertEquals(409, response.getStatus());
    }

    @Test
    void preHandleRejectsWhileAnotherRequestIsProcessing() throws Exception {
        MockHttpServletRequest rawRequest = new MockHttpServletRequest("POST", "/api/v1/kyc");
        rawRequest.addHeader("Idempotency-Key", "abc-123");
        rawRequest.setContent("{\"docType\":\"PASSPORT\"}".getBytes(StandardCharsets.UTF_8));
        CachedBodyHttpServletRequest request = new CachedBodyHttpServletRequest(rawRequest, MAX_CACHED_BODY_BYTES);
        MockHttpServletResponse response = new MockHttpServletResponse();

        String requestHash = hash("POST", "/api/v1/kyc", null, "anonymous", rawRequest.getContentAsByteArray());
        when(redisIdempotencyStore.find(any()))
                .thenReturn(Optional.of(IdempotencyRecord.processing(requestHash)));

        boolean allowed = interceptor.preHandle(request, response, handlerMethod());

        assertFalse(allowed);
        assertEquals(409, response.getStatus());
        assertTrue(response.getContentAsString()
                .contains("already being processed"));
    }

    @Test
    void afterCompletionDeletesKeyWhenActiveAndNotCompleted() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/kyc");
        request.setAttribute(
                com.aionn.sharedkernel.infrastructure.web.RequestAttributeKeys.IDEMPOTENCY_ACTIVE, true);
        request.setAttribute(
                com.aionn.sharedkernel.infrastructure.web.RequestAttributeKeys.IDEMPOTENCY_KEY, "redis-key-1");

        interceptor.afterCompletion(request, new MockHttpServletResponse(), handlerObject(), null);

        verify(redisIdempotencyStore).delete("redis-key-1");
    }

    @Test
    void afterCompletionSkipsWhenNotActive() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/kyc");

        interceptor.afterCompletion(request, new MockHttpServletResponse(), handlerObject(), null);

        verify(redisIdempotencyStore, never()).delete(any());
    }

    @Test
    void afterCompletionSkipsWhenAlreadyCompleted() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/kyc");
        request.setAttribute(
                com.aionn.sharedkernel.infrastructure.web.RequestAttributeKeys.IDEMPOTENCY_ACTIVE, true);
        request.setAttribute(
                com.aionn.sharedkernel.infrastructure.web.RequestAttributeKeys.IDEMPOTENCY_COMPLETED, true);
        request.setAttribute(
                com.aionn.sharedkernel.infrastructure.web.RequestAttributeKeys.IDEMPOTENCY_KEY, "redis-key-1");

        interceptor.afterCompletion(request, new MockHttpServletResponse(), handlerObject(), null);

        verify(redisIdempotencyStore, never()).delete(any());
    }

    private Object handlerObject() {
        return new Object();
    }

    private HandlerMethod handlerMethod() throws NoSuchMethodException {
        return new HandlerMethod(new DummyController(), DummyController.class.getMethod("create"));
    }

    private String hash(String method, String uri, String query, String principal, byte[] body) throws Exception {
        String canonical = method + "\n" + uri + (query == null ? "" : "?" + query) + "\n" + principal + "\n";
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(canonical.getBytes(StandardCharsets.UTF_8));
        digest.update(body);
        return HexFormat.of().formatHex(digest.digest());
    }

    static class DummyController {
        @IdempotentRequest
        public void create() {
        }

        public void plain() {
        }
    }
}
