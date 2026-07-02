package com.aionn.identity.adapter.rest.support.client;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class ClientUserAgentArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String UNKNOWN_USER_AGENT = "unknown";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(ClientUserAgent.class)
                && String.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            return UNKNOWN_USER_AGENT;
        }
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        if (userAgent == null || userAgent.isBlank()) {
            return UNKNOWN_USER_AGENT;
        }
        return userAgent;
    }
}
