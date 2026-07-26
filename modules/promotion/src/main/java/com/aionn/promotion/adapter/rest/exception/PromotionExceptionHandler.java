package com.aionn.promotion.adapter.rest.exception;

import com.aionn.promotion.domain.exception.PromotionException;
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
@RestControllerAdvice(basePackages = "com.aionn.promotion.adapter.rest.controller")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PromotionExceptionHandler extends AbstractModuleExceptionHandler {

    public PromotionExceptionHandler() {
        registerErrors(HttpStatus.NOT_FOUND, "PRM_001", "PRM_101", "PRM_203");
        registerErrors(HttpStatus.CONFLICT,
                "PRM_103", "PRM_104", "PRM_201", "PRM_202", "PRM_205");
        registerErrors(HttpStatus.BAD_REQUEST,
                "PRM_002", "PRM_003", "PRM_004", "PRM_102", "PRM_204",
                "PRM_301", "PRM_302", "PRM_900");
        setDefaultStatus(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Override
    protected String moduleDomain() {
        return "Promotion";
    }

    @ExceptionHandler(PromotionException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handlePromotionException(PromotionException ex) {
        log.warn("Promotion exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return handleException(ex);
    }
}
