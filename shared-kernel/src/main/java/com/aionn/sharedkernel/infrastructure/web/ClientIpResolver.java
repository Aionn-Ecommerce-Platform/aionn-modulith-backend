package com.aionn.sharedkernel.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {

    public String resolve(HttpServletRequest request) {
        Object resolvedByFilter = request.getAttribute(RequestAttributeKeys.CLIENT_IP);
        if (resolvedByFilter instanceof String clientIp && !clientIp.isBlank()) {
            return clientIp;
        }
        return request.getRemoteAddr();
    }
}
