package com.aionn.identity.adapter.rest.support.client;

import com.aionn.identity.infrastructure.config.properties.AuthProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.lang.reflect.Method;

import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AuthClientTypeArgumentResolverTest {

    @Mock
    private AuthProperties authProperties;

    static class Sample {
        // Dummy handler — the body is deliberately empty because the tests
        // only reflect on its signature (via MethodParameter) to feed the
        // resolver; the method is never invoked.
        public void handle(@AuthClientType String clientType, String otherParam) {
            // no-op: reflection target only
        }
    }

    @Test
    void supportsParameterWithAnnotation() throws Exception {
        var resolver = new AuthClientTypeArgumentResolver(authProperties);
        Method method = Sample.class.getMethod("handle", String.class, String.class);

        assertThat(resolver.supportsParameter(new MethodParameter(method, 0))).isTrue();
        assertThat(resolver.supportsParameter(new MethodParameter(method, 1))).isFalse();
    }

    @Test
    void resolveArgumentReturnsHeaderValue() throws Exception {
        var resolver = new AuthClientTypeArgumentResolver(authProperties);
        when(authProperties.clientTypeHeader()).thenReturn("X-Client-Type");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Client-Type", "mobile");
        var webRequest = new ServletWebRequest(request);

        Object result = resolver.resolveArgument(
                new MethodParameter(Sample.class.getMethod("handle", String.class, String.class), 0),
                null, webRequest, null);

        assertThat(result).isEqualTo("mobile");
    }

    @Test
    void resolveArgumentReturnsNullWhenHeaderMissing() throws Exception {
        var resolver = new AuthClientTypeArgumentResolver(authProperties);
        when(authProperties.clientTypeHeader()).thenReturn("X-Client-Type");

        MockHttpServletRequest request = new MockHttpServletRequest();
        var webRequest = new ServletWebRequest(request);

        Object result = resolver.resolveArgument(
                new MethodParameter(Sample.class.getMethod("handle", String.class, String.class), 0),
                null, webRequest, null);

        assertThat(result).isNull();
    }

    @Test
    void resolveArgumentHandlesNullNativeRequest() throws Exception {
        var resolver = new AuthClientTypeArgumentResolver(authProperties);

        var webRequest = org.mockito.Mockito.mock(org.springframework.web.context.request.NativeWebRequest.class);
        when(webRequest.getNativeRequest(jakarta.servlet.http.HttpServletRequest.class)).thenReturn(null);

        Object result = resolver.resolveArgument(
                new MethodParameter(Sample.class.getMethod("handle", String.class, String.class), 0),
                null, webRequest, null);

        assertThat(result).isNull();
    }
}
