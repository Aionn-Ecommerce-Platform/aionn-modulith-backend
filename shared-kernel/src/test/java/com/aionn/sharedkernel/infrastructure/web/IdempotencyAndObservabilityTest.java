package com.aionn.sharedkernel.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aionn.sharedkernel.adapter.web.support.idempotency.IdempotentRequest;
import com.aionn.sharedkernel.domain.model.DomainEvent;
import com.aionn.sharedkernel.domain.model.EventEnvelope;
import com.aionn.sharedkernel.infrastructure.event.SpringEventPublisher;
import com.aionn.sharedkernel.infrastructure.persistence.AuditingConfig;
import com.aionn.sharedkernel.infrastructure.web.idempotency.CachedBodyHttpServletRequest;
import com.aionn.sharedkernel.infrastructure.web.idempotency.IdempotencyBodyCachingFilter;
import com.aionn.sharedkernel.infrastructure.web.idempotency.IdempotencyInterceptor;
import com.aionn.sharedkernel.infrastructure.web.idempotency.IdempotencyProperties;
import com.aionn.sharedkernel.infrastructure.web.idempotency.IdempotencyRecord;
import com.aionn.sharedkernel.infrastructure.web.idempotency.IdempotencyResponseBodyAdvice;
import com.aionn.sharedkernel.infrastructure.web.idempotency.IdempotencyWebConfig;
import com.aionn.sharedkernel.infrastructure.web.idempotency.RedisIdempotencyStore;
import com.aionn.sharedkernel.infrastructure.web.observability.RequestIdFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.MethodParameter;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.DelegatingServletInputStream;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.method.HandlerMethod;

class IdempotencyAndObservabilityTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void requestIdFilterUsesIncomingHeaderOrGeneratesOne() throws Exception {
        RequestIdFilter filter = new RequestIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, "req-123");
        filter.doFilter(request, response, (req, res) -> {
            assertEquals("req-123", MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY));
            assertEquals("req-123", ((MockHttpServletResponse) res).getHeader(RequestIdFilter.REQUEST_ID_HEADER));
        });
        assertNull(MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY));

        MockHttpServletRequest blankRequest = new MockHttpServletRequest();
        blankRequest.addHeader(RequestIdFilter.REQUEST_ID_HEADER, " ");
        MockHttpServletResponse generatedResponse = new MockHttpServletResponse();
        filter.doFilter(blankRequest, generatedResponse, (req, res) -> {
            String generated = MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY);
            assertNotNull(generated);
            assertFalse(generated.isBlank());
            assertEquals(generated, ((MockHttpServletResponse) res).getHeader(RequestIdFilter.REQUEST_ID_HEADER));
        });
    }

    @Test
    void cachedBodyRequestAndBodyCachingFilterWrapAndLimitRequestBody() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent("{\"value\":1}".getBytes(StandardCharsets.UTF_8));

        CachedBodyHttpServletRequest cached = new CachedBodyHttpServletRequest(request, 1024);
        assertArrayEquals("{\"value\":1}".getBytes(StandardCharsets.UTF_8), cached.getCachedBody());
        assertEquals("{\"value\":1}", new BufferedReader(new InputStreamReader(cached.getInputStream(), StandardCharsets.UTF_8))
                .readLine());
        assertThrows(UnsupportedOperationException.class, () -> cached.getInputStream().setReadListener(mock(ReadListener.class)));

        MockHttpServletRequest tooLarge = new MockHttpServletRequest();
        tooLarge.setContent("abcdef".getBytes(StandardCharsets.UTF_8));
        assertThrows(IOException.class, () -> new CachedBodyHttpServletRequest(tooLarge, 3));

        IdempotencyProperties properties = new IdempotencyProperties();
        properties.setMaxCachedBodyBytes(1024);
        TestableIdempotencyBodyCachingFilter filter = new TestableIdempotencyBodyCachingFilter(properties);
        MockHttpServletRequest filteredRequest = new MockHttpServletRequest();
        filteredRequest.addHeader("Idempotency-Key", "key-1");
        filteredRequest.setContent("body".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(filteredRequest, response, (req, res) -> {
            assertInstanceOf(CachedBodyHttpServletRequest.class, req);
            assertArrayEquals("body".getBytes(StandardCharsets.UTF_8),
                    ((CachedBodyHttpServletRequest) req).getCachedBody());
        });
        assertTrue(filter.exposedShouldNotFilter(new MockHttpServletRequest()));
        MockHttpServletRequest blankHeader = new MockHttpServletRequest();
        blankHeader.addHeader("Idempotency-Key", " ");
        assertTrue(filter.exposedShouldNotFilter(blankHeader));
        assertFalse(filter.exposedShouldNotFilter(filteredRequest));
    }

    @Test
    void redisIdempotencyStoreReadsWritesAndWrapsFailures() throws Exception {
        String key = "idem:1";
        StringRedisTemplateStub redis = new StringRedisTemplateStub();
        ObjectMapper objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        RedisIdempotencyStore store = new RedisIdempotencyStore(redis.template, objectMapper);

        assertTrue(store.beginProcessing(key, "hash-1", Duration.ofSeconds(30)));
        Optional<IdempotencyRecord> processing = store.find(key);
        assertTrue(processing.isPresent());
        assertTrue(processing.get().isProcessing());

        IdempotencyRecord.StoredHttpResponse storedResponse =
                new IdempotencyRecord.StoredHttpResponse(200, MediaType.APPLICATION_JSON_VALUE, "{\"ok\":true}");
        store.saveCompleted(key, "hash-1", storedResponse, Duration.ofSeconds(60));
        Optional<IdempotencyRecord> completed = store.find(key);
        assertTrue(completed.isPresent());
        assertTrue(completed.get().isCompleted());
        assertEquals(200, completed.get().response().status());

        store.delete(key);
        assertTrue(store.find(key).isEmpty());

        RedisIdempotencyStore failingStore = new RedisIdempotencyStore(redis.template, new ThrowingObjectMapper());
        assertThrows(IllegalStateException.class, () -> failingStore.beginProcessing("x", "y", Duration.ofSeconds(1)));

        redis.failDelete = true;
        assertThrows(IllegalStateException.class, () -> store.delete("broken"));
    }

    @Test
    void idempotencyInterceptorHandlesReplayConflictAndLifecycle() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        RedisIdempotencyStore store = mock(RedisIdempotencyStore.class);
        IdempotencyProperties properties = new IdempotencyProperties();
        properties.setProcessingTtlSeconds(45);
        IdempotencyInterceptor interceptor = new IdempotencyInterceptor(objectMapper, store, properties);

        Method method = IdempotentEndpoint.class.getDeclaredMethod("submit");
        HandlerMethod handler = new HandlerMethod(new IdempotentEndpoint(), method);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/orders");
        request.setQueryString("a=1");
        request.addHeader("Idempotency-Key", " key-1 ");
        request.setContent("{\"x\":1}".getBytes(StandardCharsets.UTF_8));
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request, 1024);
        MockHttpServletResponse response = new MockHttpServletResponse();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("merchant_001", "n/a", AuthorityUtils.NO_AUTHORITIES));
        when(store.find(anyString())).thenReturn(Optional.empty());
        when(store.beginProcessing(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        assertTrue(interceptor.preHandle(cachedRequest, response, handler));
        assertEquals(Boolean.TRUE, cachedRequest.getAttribute(RequestAttributeKeys.IDEMPOTENCY_ACTIVE));
        assertEquals(300, cachedRequest.getAttribute(RequestAttributeKeys.IDEMPOTENCY_TTL_SECONDS));
        assertNotNull(cachedRequest.getAttribute(RequestAttributeKeys.IDEMPOTENCY_REQUEST_HASH));

        cachedRequest.setAttribute(RequestAttributeKeys.IDEMPOTENCY_KEY, "redis-key");
        interceptor.afterCompletion(cachedRequest, response, handler, null);
        verify(store).delete("redis-key");

        MockHttpServletResponse replayResponse = new MockHttpServletResponse();
        String requestHash = (String) cachedRequest.getAttribute(RequestAttributeKeys.IDEMPOTENCY_REQUEST_HASH);
        IdempotencyRecord replay = IdempotencyRecord.completed(
                requestHash,
                new IdempotencyRecord.StoredHttpResponse(201, MediaType.APPLICATION_JSON_VALUE, "{\"id\":1}"));
        when(store.find(anyString())).thenReturn(Optional.of(replay));
        CachedBodyHttpServletRequest replayRequest = new CachedBodyHttpServletRequest(request, 1024);
        assertFalse(interceptor.preHandle(replayRequest, replayResponse, handler));

        MockHttpServletResponse conflictResponse = new MockHttpServletResponse();
        when(store.find(anyString())).thenReturn(Optional.of(IdempotencyRecord.processing("other-hash")));
        CachedBodyHttpServletRequest conflictRequest = new CachedBodyHttpServletRequest(request, 1024);
        assertFalse(interceptor.preHandle(conflictRequest, conflictResponse, handler));
        assertEquals(409, conflictResponse.getStatus());

        MockHttpServletResponse invalidStateResponse = new MockHttpServletResponse();
        when(store.find(anyString())).thenReturn(Optional.of(new IdempotencyRecord("UNKNOWN", requestHash, null)));
        CachedBodyHttpServletRequest invalidStateRequest = new CachedBodyHttpServletRequest(request, 1024);
        assertFalse(interceptor.preHandle(invalidStateRequest, invalidStateResponse, handler));
        assertEquals(409, invalidStateResponse.getStatus());

        MockHttpServletResponse racedResponse = new MockHttpServletResponse();
        when(store.find(anyString())).thenReturn(Optional.empty(), Optional.empty());
        when(store.beginProcessing(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        CachedBodyHttpServletRequest racedRequest = new CachedBodyHttpServletRequest(request, 1024);
        assertFalse(interceptor.preHandle(racedRequest, racedResponse, handler));
        assertEquals(409, racedResponse.getStatus());
    }

    @Test
    void idempotencyAdviceAndWebConfigPersistSuccessfulResponses() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        RedisIdempotencyStore store = mock(RedisIdempotencyStore.class);
        IdempotencyResponseBodyAdvice advice = new IdempotencyResponseBodyAdvice(objectMapper, store);

        Method method = IdempotentEndpoint.class.getDeclaredMethod("submit");
        MethodParameter returnType = new MethodParameter(method, -1);
        assertTrue(advice.supports(returnType, StringHttpMessageConverter.class));

        Method nonAnnotated = IdempotentEndpoint.class.getDeclaredMethod("plain");
        assertFalse(advice.supports(new MethodParameter(nonAnnotated, -1), StringHttpMessageConverter.class));

        MockHttpServletRequest servletRequest = new MockHttpServletRequest("POST", "/orders");
        servletRequest.setAttribute(RequestAttributeKeys.IDEMPOTENCY_ACTIVE, true);
        servletRequest.setAttribute(RequestAttributeKeys.IDEMPOTENCY_KEY, "redis-key");
        servletRequest.setAttribute(RequestAttributeKeys.IDEMPOTENCY_REQUEST_HASH, "hash-1");
        servletRequest.setAttribute(RequestAttributeKeys.IDEMPOTENCY_TTL_SECONDS, 90);
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        servletResponse.setStatus(201);

        Object body = advice.beforeBodyWrite(
                List.of("ok"),
                returnType,
                MediaType.APPLICATION_JSON,
                StringHttpMessageConverter.class,
                new ServletServerHttpRequest(servletRequest),
                new ServletServerHttpResponse(servletResponse));
        assertEquals(List.of("ok"), body);
        verify(store).saveCompleted(eq("redis-key"), eq("hash-1"), any(IdempotencyRecord.StoredHttpResponse.class),
                eq(Duration.ofSeconds(90)));
        assertEquals(Boolean.TRUE, servletRequest.getAttribute(RequestAttributeKeys.IDEMPOTENCY_COMPLETED));
        assertEquals("false", servletResponse.getHeader("Idempotent-Replay"));

        MockHttpServletRequest non2xxRequest = new MockHttpServletRequest("POST", "/orders");
        non2xxRequest.setAttribute(RequestAttributeKeys.IDEMPOTENCY_ACTIVE, true);
        MockHttpServletResponse non2xxResponse = new MockHttpServletResponse();
        non2xxResponse.setStatus(409);
        advice.beforeBodyWrite(
                "ignored",
                returnType,
                MediaType.APPLICATION_JSON,
                StringHttpMessageConverter.class,
                new ServletServerHttpRequest(non2xxRequest),
                new ServletServerHttpResponse(non2xxResponse));
        verify(store, never()).saveCompleted(eq("missing"), anyString(), any(), any());

        ResponseBodyAdvice<Object> sameBody = advice;
        assertSame("body", sameBody.beforeBodyWrite(
                "body",
                returnType,
                MediaType.APPLICATION_JSON,
                StringHttpMessageConverter.class,
                mock(org.springframework.http.server.ServerHttpRequest.class),
                mock(org.springframework.http.server.ServerHttpResponse.class)));

        InterceptorRegistry registry = mock(InterceptorRegistry.class);
        InterceptorRegistration registration = mock(InterceptorRegistration.class);
        IdempotencyInterceptor interceptor = mock(IdempotencyInterceptor.class);
        when(registry.addInterceptor(interceptor)).thenReturn(registration);
        new IdempotencyWebConfig(interceptor).addInterceptors(registry);
        verify(registry).addInterceptor(interceptor);
    }

    @Test
    void springEventPublisherIgnoresNullOrEmptyListsAndPublishesEnvelope() {
        ApplicationEventPublisher delegate = mock(ApplicationEventPublisher.class);
        SpringEventPublisher publisher = new SpringEventPublisher(delegate);
        EventEnvelope envelope = new EventEnvelope("evt-1", "Type", "agg-1", new SampleDomainEvent(), Instant.now());

        publisher.publish((List<EventEnvelope>) null);
        publisher.publish(List.of());
        publisher.publish(List.of(envelope));

        verify(delegate).publishEvent(envelope);
    }

    static final class IdempotentEndpoint {

        @IdempotentRequest
        public List<String> submit() {
            return List.of("ok");
        }

        public String plain() {
            return "plain";
        }
    }

    static final class TestableIdempotencyBodyCachingFilter extends IdempotencyBodyCachingFilter {

        TestableIdempotencyBodyCachingFilter(IdempotencyProperties idempotencyProperties) {
            super(idempotencyProperties);
        }

        boolean exposedShouldNotFilter(MockHttpServletRequest request) {
            return super.shouldNotFilter(request);
        }
    }

    static final class SampleDomainEvent implements DomainEvent {

        @Override
        public Instant occurredAt() {
            return Instant.parse("2026-06-30T00:00:00Z");
        }
    }

    static final class StringRedisTemplateStub {
        final org.springframework.data.redis.core.StringRedisTemplate template =
                mock(org.springframework.data.redis.core.StringRedisTemplate.class);
        final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        final java.util.Map<String, String> storage = new java.util.HashMap<>();
        boolean failDelete;

        @SuppressWarnings("unchecked")
        StringRedisTemplateStub() {
            when(template.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenAnswer(invocation -> storage.get(invocation.getArgument(0)));
            when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenAnswer(invocation -> {
                String key = invocation.getArgument(0);
                if (storage.containsKey(key)) {
                    return false;
                }
                storage.put(key, invocation.getArgument(1));
                return true;
            });
            org.mockito.Mockito.doAnswer(invocation -> {
                storage.put(invocation.getArgument(0), invocation.getArgument(1));
                return null;
            }).when(valueOperations).set(anyString(), anyString(), any(Duration.class));
            when(template.delete(anyString())).thenAnswer(invocation -> {
                if (failDelete) {
                    throw new RuntimeException("boom");
                }
                return storage.remove(invocation.getArgument(0)) != null;
            });
        }
    }

    static final class ThrowingObjectMapper extends ObjectMapper {
        @Override
        public String writeValueAsString(Object value) throws JsonProcessingException {
            throw new JsonProcessingException("boom") {
            };
        }
    }
}
