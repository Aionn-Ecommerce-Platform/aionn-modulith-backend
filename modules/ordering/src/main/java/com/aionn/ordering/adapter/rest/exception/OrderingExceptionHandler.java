package com.aionn.ordering.adapter.rest.exception;

import com.aionn.ordering.domain.exception.OrderingException;
import com.aionn.platform.web.exception.AbstractModuleExceptionHandler;
import com.aionn.platform.web.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice(basePackages = "com.aionn.ordering.adapter.rest.controller")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OrderingExceptionHandler extends AbstractModuleExceptionHandler {

    public OrderingExceptionHandler() {
        registerErrors(HttpStatus.NOT_FOUND,
                "ORD_001", "ORD_003", "ORD_101", "ORD_201");
        registerErrors(HttpStatus.CONFLICT, "ORD_103", "ORD_104", "ORD_110", "ORD_202");
        registerErrors(HttpStatus.FORBIDDEN, "ORD_002", "ORD_102", "ORD_106");
        registerErrors(HttpStatus.BAD_REQUEST,
                "ORD_004", "ORD_105", "ORD_107", "ORD_108", "ORD_109", "ORD_900");
        setDefaultStatus(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Override
    protected String moduleDomain() {
        return "Ordering";
    }

    @ExceptionHandler(OrderingException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleOrderingException(OrderingException ex) {
        log.warn("Ordering exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return handleException(ex);
    }
}
