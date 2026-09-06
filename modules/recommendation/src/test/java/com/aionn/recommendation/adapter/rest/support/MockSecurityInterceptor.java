package com.aionn.recommendation.adapter.rest.support;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Propagates an authentication installed by {@link TestAuth} onto the request principal, so a
 * standalone MockMvc setup behaves like the real Spring Security flow. Clears the context afterwards
 * so one authenticated request cannot leak into the next test.
 */
public class MockSecurityInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null
                && request instanceof MockHttpServletRequest mockRequest
                && mockRequest.getUserPrincipal() == null) {
            mockRequest.setUserPrincipal(auth);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception ex) {
        SecurityContextHolder.clearContext();
    }
}
