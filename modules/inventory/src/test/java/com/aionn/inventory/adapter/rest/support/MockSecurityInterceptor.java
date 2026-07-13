package com.aionn.inventory.adapter.rest.support;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Test-only interceptor that propagates a previously installed
 * {@link Authentication} (e.g. set by {@link TestAuth}) onto the
 * request as its user principal so argument resolvers can resolve it.
 */
public class MockSecurityInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            Object contextObj = null;
            HttpSession session = request.getSession(false);
            if (session != null) {
                contextObj = session.getAttribute("SPRING_SECURITY_CONTEXT");
            }
            if (contextObj == null) {
                contextObj = request.getAttribute("org.springframework.security.core.context.SecurityContext");
            }
            if (contextObj instanceof SecurityContext context) {
                auth = context.getAuthentication();
            }
        }

        if (auth != null) {
            SecurityContextHolder.getContext().setAuthentication(auth);
            if (request instanceof MockHttpServletRequest mockRequest && mockRequest.getUserPrincipal() == null) {
                mockRequest.setUserPrincipal(auth);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception ex) {
        SecurityContextHolder.clearContext();
    }
}
