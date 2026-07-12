package com.aionn.identity.adapter.rest.support.session;

import com.aionn.identity.infrastructure.security.web.SecurityRequestAttributeKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.NativeWebRequest;

import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CurrentSessionIdArgumentResolverTest {

    @Mock
    private MethodParameter methodParameter;
    @Mock
    private NativeWebRequest webRequest;

    private CurrentSessionIdArgumentResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new CurrentSessionIdArgumentResolver();
    }

    @Test
    void supportsParameterWithCurrentSessionIdAndStringType() {
        when(methodParameter.hasParameterAnnotation(CurrentSessionId.class)).thenReturn(true);
        org.mockito.Mockito.<Class<?>>when(methodParameter.getParameterType()).thenReturn(String.class);

        assertThat(resolver.supportsParameter(methodParameter)).isTrue();
    }

    @Test
    void rejectsParameterWithoutAnnotation() {
        when(methodParameter.hasParameterAnnotation(CurrentSessionId.class)).thenReturn(false);

        assertThat(resolver.supportsParameter(methodParameter)).isFalse();
    }

    @Test
    void resolveArgumentReturnsSessionIdFromRequestAttribute() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(SecurityRequestAttributeKeys.SESSION_ID, "session-42");
        when(webRequest.getNativeRequest(jakarta.servlet.http.HttpServletRequest.class))
                .thenReturn(request);

        Object resolved = resolver.resolveArgument(methodParameter, null, webRequest, null);

        assertThat(resolved).isEqualTo("session-42");
    }

    @Test
    void resolveArgumentReturnsNullWhenAttributeMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(webRequest.getNativeRequest(jakarta.servlet.http.HttpServletRequest.class))
                .thenReturn(request);

        Object resolved = resolver.resolveArgument(methodParameter, null, webRequest, null);

        assertThat(resolved).isNull();
    }

    @Test
    void resolveArgumentReturnsNullWhenNativeRequestMissing() {
        when(webRequest.getNativeRequest(jakarta.servlet.http.HttpServletRequest.class))
                .thenReturn(null);

        Object resolved = resolver.resolveArgument(methodParameter, null, webRequest, null);

        assertThat(resolved).isNull();
    }
}
