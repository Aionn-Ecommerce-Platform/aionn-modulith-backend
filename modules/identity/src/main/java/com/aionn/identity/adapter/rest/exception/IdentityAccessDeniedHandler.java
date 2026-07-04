package com.aionn.identity.adapter.rest.exception;

import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class IdentityAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("errorCode", IdentityErrorCode.ACCESS_DENIED.getCode());
        body.put("domain", "Identity");
        var envelope = ApiResponse.error(
                String.valueOf(HttpStatus.FORBIDDEN.value()),
                IdentityErrorCode.ACCESS_DENIED.getDefaultMessage(),
                body);
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), envelope);
    }
}
