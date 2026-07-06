package com.aionn.catalog.adapter.rest.exception;

import com.aionn.catalog.domain.exception.CatalogException;
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
@RestControllerAdvice(basePackages = "com.aionn.catalog.adapter.rest.controller")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CatalogExceptionHandler extends AbstractModuleExceptionHandler {

    public CatalogExceptionHandler() {
        registerErrors(HttpStatus.NOT_FOUND,
                "CATALOG_001", "CATALOG_101", "CATALOG_201",
                "CATALOG_301", "CATALOG_305", "CATALOG_401");
        registerErrors(HttpStatus.CONFLICT,
                "CATALOG_002", "CATALOG_004", "CATALOG_102", "CATALOG_103",
                "CATALOG_104", "CATALOG_202", "CATALOG_203", "CATALOG_306");
        registerErrors(HttpStatus.FORBIDDEN,
                "CATALOG_005", "CATALOG_303");
        registerErrors(HttpStatus.BAD_REQUEST,
                "CATALOG_003", "CATALOG_105", "CATALOG_302", "CATALOG_304",
                "CATALOG_307", "CATALOG_308", "CATALOG_309",
                "CATALOG_402", "CATALOG_900");
        setDefaultStatus(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Override
    protected String moduleDomain() {
        return "Catalog";
    }

    @ExceptionHandler(CatalogException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleCatalogException(CatalogException ex) {
        log.warn("Catalog exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return handleException(ex);
    }
}
