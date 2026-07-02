package com.aionn.identity.adapter.rest.exception;

import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class IdentityAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("errorCode", IdentityErrorCode.AUTHENTICATION_REQUIRED.getCode());
        body.put("domain", "Identity");
        var envelope = ApiResponse.error(
                String.valueOf(HttpStatus.UNAUTHORIZED.value()),
                IdentityErrorCode.AUTHENTICATION_REQUIRED.getDefaultMessage(),
                body);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), envelope);
    }
}
