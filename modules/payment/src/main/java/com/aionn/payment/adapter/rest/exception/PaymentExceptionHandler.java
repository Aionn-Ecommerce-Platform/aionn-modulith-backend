package com.aionn.payment.adapter.rest.exception;

import com.aionn.payment.domain.exception.PaymentException;
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
@RestControllerAdvice(basePackages = "com.aionn.payment.adapter.rest.controller")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PaymentExceptionHandler extends AbstractModuleExceptionHandler {

    public PaymentExceptionHandler() {
        registerErrors(HttpStatus.NOT_FOUND, "PAY_001", "PAY_101", "PAY_201");
        registerErrors(HttpStatus.CONFLICT, "PAY_003");
        registerErrors(HttpStatus.FORBIDDEN, "PAY_102");
        registerErrors(HttpStatus.BAD_REQUEST,
                "PAY_002", "PAY_005", "PAY_006", "PAY_103", "PAY_900");
        registerErrors(HttpStatus.SERVICE_UNAVAILABLE, "PAY_004");
        setDefaultStatus(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Override
    protected String moduleDomain() {
        return "Payment";
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handlePaymentException(PaymentException ex) {
        log.warn("Payment exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return handleException(ex);
    }
}
