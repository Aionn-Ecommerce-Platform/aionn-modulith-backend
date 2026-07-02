package com.aionn.identity.adapter.rest.support;

import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.security.Principal;

public class MockAuthenticationArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return Authentication.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            return auth;
        }

        Principal principal = webRequest.getUserPrincipal();
        if (principal instanceof Authentication) {
            return principal;
        }

        if (principal != null) {
            Authentication mockAuth = org.mockito.Mockito.mock(Authentication.class);
            org.mockito.Mockito.when(mockAuth.getName()).thenReturn(principal.getName());
            return mockAuth;
        }

        throw new InsufficientAuthenticationException("Full authentication is required to access this resource");
    }
}
