package com.aionn.shipping.adapter.rest.exception;

import com.aionn.shipping.domain.exception.ShippingException;
import com.aionn.sharedkernel.adapter.web.exception.AbstractModuleExceptionHandler;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice(basePackages = "com.aionn.shipping.adapter.rest.controller")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ShippingExceptionHandler extends AbstractModuleExceptionHandler {

    public ShippingExceptionHandler() {
        registerErrors(HttpStatus.NOT_FOUND, "SHP_001", "SHP_101");
        registerErrors(HttpStatus.CONFLICT, "SHP_003", "SHP_102");
        registerErrors(HttpStatus.BAD_REQUEST, "SHP_002", "SHP_900");
        registerErrors(HttpStatus.SERVICE_UNAVAILABLE, "SHP_004");
        registerErrors(HttpStatus.FORBIDDEN, "SHP_005");
        setDefaultStatus(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Override
    protected String moduleDomain() {
        return "Shipping";
    }

    @ExceptionHandler(ShippingException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleShippingException(ShippingException ex) {
        log.warn("Shipping exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return handleException(ex);
    }
}
