package com.aionn.shipping.adapter.rest.exception;

import com.aionn.shipping.domain.exception.ShippingException;
import com.aionn.platform.web.exception.AbstractModuleExceptionHandler;
import com.aionn.platform.web.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
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
        registerErrors(HttpStatus.UNPROCESSABLE_CONTENT, "SHP_006");
        registerErrors(HttpStatus.FORBIDDEN, "SHP_005");
        setDefaultStatus(HttpStatus.UNPROCESSABLE_CONTENT);
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

    @ExceptionHandler({ConcurrencyFailureException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleConflict(RuntimeException ex) {
        log.warn("Shipping conflict: {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT,
                "The resource was modified concurrently or violates a constraint",
                "CONFLICT", null);
    }
}
