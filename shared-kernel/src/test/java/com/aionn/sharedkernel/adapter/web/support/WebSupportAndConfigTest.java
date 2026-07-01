package com.aionn.sharedkernel.adapter.web.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aionn.sharedkernel.adapter.web.support.clientip.ClientIp;
import com.aionn.sharedkernel.adapter.web.support.clientip.ClientIpArgumentResolver;
import com.aionn.sharedkernel.adapter.web.support.versioning.ApiVersion;
import com.aionn.sharedkernel.adapter.web.support.versioning.ApiVersionRequestCondition;
import com.aionn.sharedkernel.adapter.web.support.versioning.ApiVersionRequestMappingHandlerMapping;
import com.aionn.sharedkernel.adapter.web.support.versioning.ApiVersioningConfig;
import com.aionn.sharedkernel.domain.model.DomainEvent;
import com.aionn.sharedkernel.domain.model.EventEnvelope;
import com.aionn.sharedkernel.infrastructure.event.SpringEventPublisher;
import com.aionn.sharedkernel.infrastructure.persistence.AuditingConfig;
import com.aionn.sharedkernel.infrastructure.web.ClientIpResolver;
import com.aionn.sharedkernel.infrastructure.web.RequestAttributeKeys;
import com.aionn.sharedkernel.infrastructure.web.i18n.I18nWebConfig;
import com.aionn.sharedkernel.infrastructure.web.i18n.LocaleInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

class WebSupportAndConfigTest {

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
        SecurityContextHolder.clearContext();
    }

    @Test
    void apiVersionRequestConditionMatchesAcrossAllSupportedSources() {
        ApiVersionRequestCondition condition = new ApiVersionRequestCondition(2);

        MockHttpServletRequest pathRequest = new MockHttpServletRequest("GET", "/api/v2/products");
        MockHttpServletRequest paramRequest = new MockHttpServletRequest("GET", "/api/products");
        paramRequest.setParameter("version", "2");
        MockHttpServletRequest headerRequest = new MockHttpServletRequest("GET", "/api/products");
        headerRequest.addHeader("X-API-Version", "2");
        MockHttpServletRequest acceptRequest = new MockHttpServletRequest("GET", "/api/products");
        acceptRequest.addHeader("Accept", "application/vnd.aionn.v2+json");
        MockHttpServletRequest defaultRequest = new MockHttpServletRequest("GET", "/api/products");

        assertNotNull(condition.getMatchingCondition(pathRequest));
        assertNotNull(condition.getMatchingCondition(paramRequest));
        assertNotNull(condition.getMatchingCondition(headerRequest));
        assertNotNull(condition.getMatchingCondition(acceptRequest));
        assertNull(condition.getMatchingCondition(defaultRequest));
        assertEquals(2, condition.combine(new ApiVersionRequestCondition(2)).getVersion());
        assertTrue(condition.compareTo(new ApiVersionRequestCondition(1), pathRequest) < 0);
    }

    @Test
    void apiVersionMappingAndConfigCreateCustomConditions() throws Exception {
        ExposedApiVersionMapping mapping = new ExposedApiVersionMapping();
        RequestCondition<?> typeCondition = mapping.typeCondition(VersionedController.class);
        RequestCondition<?> methodCondition = mapping.methodCondition(
                VersionedController.class.getDeclaredMethod("v2Method"));

        assertInstanceOf(ApiVersionRequestCondition.class, typeCondition);
        assertInstanceOf(ApiVersionRequestCondition.class, methodCondition);
        assertNull(mapping.methodCondition(VersionedController.class.getDeclaredMethod("plainMethod")));

        ApiVersioningConfig config = new ApiVersioningConfig();
        RequestMappingHandlerMapping handlerMapping = config.getRequestMappingHandlerMapping();
        assertInstanceOf(ApiVersionRequestMappingHandlerMapping.class, handlerMapping);
    }

    @Test
    void clientIpResolverAndArgumentResolverUseRequestAttributeFirst() throws Exception {
        ClientIpResolver clientIpResolver = new ClientIpResolver();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.setAttribute(RequestAttributeKeys.CLIENT_IP, "203.0.113.5");

        assertEquals("203.0.113.5", clientIpResolver.resolve(request));

        ClientIpArgumentResolver resolver = new ClientIpArgumentResolver(clientIpResolver);
        Method method = VersionedController.class.getDeclaredMethod("ipMethod", String.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        NativeWebRequest webRequest = mock(NativeWebRequest.class);
        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(request);

        assertTrue(resolver.supportsParameter(parameter));
        assertEquals("203.0.113.5", resolver.resolveArgument(parameter, new ModelAndViewContainer(), webRequest, null));

        Method plainMethod = VersionedController.class.getDeclaredMethod("plainArgument", String.class);
        assertFalse(resolver.supportsParameter(new MethodParameter(plainMethod, 0)));
        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(null);
        assertNull(resolver.resolveArgument(parameter, new ModelAndViewContainer(), webRequest, null));
    }

    @Test
    void localeInterceptorAndConfigRegisterAndResetLocale() {
        LocaleInterceptor interceptor = new LocaleInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.name(), "/");
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader("Accept-Language", "en-US,en;q=0.9");
        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertEquals(Locale.forLanguageTag("en-US"), LocaleContextHolder.getLocale());
        interceptor.afterCompletion(request, response, new Object(), null);

        MockHttpServletRequest defaultRequest = new MockHttpServletRequest(HttpMethod.GET.name(), "/");
        interceptor.preHandle(defaultRequest, response, new Object());
        assertEquals(Locale.forLanguageTag("vi"), LocaleContextHolder.getLocale());

        InterceptorRegistry registry = mock(InterceptorRegistry.class);
        InterceptorRegistration registration = mock(InterceptorRegistration.class);
        when(registry.addInterceptor(interceptor)).thenReturn(registration);
        new I18nWebConfig(interceptor).addInterceptors(registry);
        verify(registry).addInterceptor(interceptor);
    }

    @Test
    void eventPublisherAndAuditorConfigUseExpectedDefaults() {
        ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
        SpringEventPublisher publisher = new SpringEventPublisher(applicationEventPublisher);
        EventEnvelope envelope = new EventEnvelope("evt-1", "Type", "agg-1", new SampleDomainEvent(), Instant.now());

        publisher.publish((List<EventEnvelope>) null);
        publisher.publish(List.of());
        publisher.publish(List.of(envelope));
        verify(applicationEventPublisher).publishEvent(envelope);

        AuditingConfig auditingConfig = new AuditingConfig();
        assertEquals(Optional.of("system"), auditingConfig.auditorProvider().getCurrentAuditor());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("merchant_001", "n/a", AuthorityUtils.NO_AUTHORITIES));
        assertEquals(Optional.of("merchant_001"), auditingConfig.auditorProvider().getCurrentAuditor());
    }

    @ApiVersion(1)
    static class VersionedController {
        @ApiVersion(2)
        void v2Method() {
            // Empty on purpose; only annotation metadata matters for mapping tests.
        }

        void plainMethod() {
            // Empty on purpose; this method exists to verify absence of version metadata.
        }

        void ipMethod(@ClientIp String clientIp) {
            // Empty on purpose; argument resolver support is the behavior under test.
        }

        void plainArgument(String value) {
            // Empty on purpose; used as a non-annotated control method.
        }
    }

    static class ExposedApiVersionMapping extends ApiVersionRequestMappingHandlerMapping {
        RequestCondition<?> typeCondition(Class<?> handlerType) {
            return super.getCustomTypeCondition(handlerType);
        }

        RequestCondition<?> methodCondition(Method method) {
            return super.getCustomMethodCondition(method);
        }
    }

    static class SampleDomainEvent implements DomainEvent {
        @Override
        public Instant occurredAt() {
            return Instant.parse("2026-06-30T00:00:00Z");
        }
    }
}
