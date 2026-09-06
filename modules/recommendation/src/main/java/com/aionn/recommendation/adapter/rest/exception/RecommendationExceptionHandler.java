package com.aionn.recommendation.adapter.rest.exception;

import com.aionn.recommendation.domain.exception.RecommendationException;
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
@RestControllerAdvice(basePackages = "com.aionn.recommendation.adapter.rest.controller")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RecommendationExceptionHandler extends AbstractModuleExceptionHandler {

    public RecommendationExceptionHandler() {
        registerErrors(HttpStatus.NOT_FOUND, "REC_001");
        registerErrors(HttpStatus.BAD_REQUEST, "REC_101", "REC_102", "REC_201", "REC_900");
        registerErrors(HttpStatus.INTERNAL_SERVER_ERROR, "REC_301");
        setDefaultStatus(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Override
    protected String moduleDomain() {
        return "Recommendation";
    }

    @ExceptionHandler(RecommendationException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleRecommendationException(
            RecommendationException ex) {
        log.warn("Recommendation exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return handleException(ex);
    }
}
