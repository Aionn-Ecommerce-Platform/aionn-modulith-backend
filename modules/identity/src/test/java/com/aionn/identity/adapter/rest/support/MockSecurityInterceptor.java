package com.aionn.identity.adapter.rest.support;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

public class MockSecurityInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
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
            if (request instanceof MockHttpServletRequest mockRequest) {
                mockRequest.setUserPrincipal(auth);
            }
        }

        if (handler instanceof HandlerMethod handlerMethod) {
            boolean requiresAuth = false;
            for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
                if (Authentication.class.isAssignableFrom(parameter.getParameterType())) {
                    requiresAuth = true;
                    break;
                }
            }
            if (requiresAuth && request.getUserPrincipal() == null) {
                // afterCompletion doesn't run when preHandle throws, so clear
                // the thread-local context ourselves — otherwise stale auth
                // bleeds into the next JUnit method reusing the same thread
                // and lets an unauthenticated request appear authenticated.
                SecurityContextHolder.clearContext();
                throw new InsufficientAuthenticationException("Full authentication is required to access this resource");
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        SecurityContextHolder.clearContext();
    }
}
