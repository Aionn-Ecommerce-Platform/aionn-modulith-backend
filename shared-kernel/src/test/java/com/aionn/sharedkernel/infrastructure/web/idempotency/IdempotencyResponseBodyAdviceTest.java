package com.aionn.sharedkernel.infrastructure.web.idempotency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.aionn.sharedkernel.infrastructure.web.RequestAttributeKeys;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class IdempotencyResponseBodyAdviceTest {

    private final RedisIdempotencyStore store = mock(RedisIdempotencyStore.class);

    private static MethodParameter anyReturnType() throws NoSuchMethodException {
        return new MethodParameter(Object.class.getDeclaredMethod("toString"), -1);
    }

    private static ServletServerHttpRequest wrap(MockHttpServletRequest request) {
        return new ServletServerHttpRequest(request);
    }

    private static ServletServerHttpResponse wrap(MockHttpServletResponse response) {
        return new ServletServerHttpResponse(response);
    }

    @Test
    void returnsBodyWhenIdempotencyAttributesMissing() throws Exception {
        IdempotencyResponseBodyAdvice advice = new IdempotencyResponseBodyAdvice(new ObjectMapper(), store);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/orders");
        request.setAttribute(RequestAttributeKeys.IDEMPOTENCY_ACTIVE, true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        Object result = advice.beforeBodyWrite("body", anyReturnType(), MediaType.APPLICATION_JSON,
                StringHttpMessageConverter.class, wrap(request), wrap(response));

        assertEquals("body", result);
        verify(store, never()).saveCompleted(anyString(), anyString(), any(), any());
    }

    @Test
    void wrapsSerializationFailure() throws Exception {
        ObjectMapper throwingMapper = new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) throws JsonProcessingException {
                throw new JsonProcessingException("boom") {
                };
            }
        };
        IdempotencyResponseBodyAdvice advice = new IdempotencyResponseBodyAdvice(throwingMapper, store);
        MockHttpServletRequest request = activeRequestWithKeys();
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        MethodParameter returnType = anyReturnType();
        ServletServerHttpRequest wrappedRequest = wrap(request);
        ServletServerHttpResponse wrappedResponse = wrap(response);

        assertThrows(IllegalStateException.class, () -> advice.beforeBodyWrite("body", returnType,
                MediaType.APPLICATION_JSON, StringHttpMessageConverter.class, wrappedRequest, wrappedResponse));
    }

    @Test
    void swallowsStoreFailureButMarksCompleted() throws Exception {
        IdempotencyResponseBodyAdvice advice = new IdempotencyResponseBodyAdvice(new ObjectMapper(), store);
        doThrow(new RuntimeException("redis down"))
                .when(store).saveCompleted(eq("redis-key"), eq("hash-1"),
                        any(IdempotencyRecord.StoredHttpResponse.class), eq(Duration.ofSeconds(90)));
        MockHttpServletRequest request = activeRequestWithKeys();
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        Object result = advice.beforeBodyWrite("body", anyReturnType(), MediaType.APPLICATION_JSON,
                StringHttpMessageConverter.class, wrap(request), wrap(response));

        assertEquals("body", result);
        assertEquals(Boolean.TRUE, request.getAttribute(RequestAttributeKeys.IDEMPOTENCY_COMPLETED));
    }

    private static MockHttpServletRequest activeRequestWithKeys() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/orders");
        request.setAttribute(RequestAttributeKeys.IDEMPOTENCY_ACTIVE, true);
        request.setAttribute(RequestAttributeKeys.IDEMPOTENCY_KEY, "redis-key");
        request.setAttribute(RequestAttributeKeys.IDEMPOTENCY_REQUEST_HASH, "hash-1");
        request.setAttribute(RequestAttributeKeys.IDEMPOTENCY_TTL_SECONDS, 90);
        return request;
    }

}
