package com.aionn.notification.adapter.rest.exception;

import com.aionn.notification.domain.exception.NotificationException;
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
@RestControllerAdvice(basePackages = "com.aionn.notification.adapter.rest.controller")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class NotificationExceptionHandler extends AbstractModuleExceptionHandler {

    public NotificationExceptionHandler() {
        registerErrors(HttpStatus.NOT_FOUND,
                "NTF_001", "NTF_101", "NTF_201", "NTF_301", "NTF_401");
        registerErrors(HttpStatus.CONFLICT, "NTF_102");
        registerErrors(HttpStatus.FORBIDDEN, "NTF_002");
        registerErrors(HttpStatus.BAD_REQUEST,
                "NTF_003", "NTF_103", "NTF_202", "NTF_302", "NTF_900");
        setDefaultStatus(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Override
    protected String moduleDomain() {
        return "Notification";
    }

    @ExceptionHandler(NotificationException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleNotificationException(NotificationException ex) {
        log.warn("Notification exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return handleException(ex);
    }
}
