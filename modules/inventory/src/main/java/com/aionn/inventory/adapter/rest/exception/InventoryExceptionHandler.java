package com.aionn.inventory.adapter.rest.exception;

import com.aionn.inventory.domain.exception.InventoryException;
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
@RestControllerAdvice(basePackages = "com.aionn.inventory.adapter.rest.controller")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InventoryExceptionHandler extends AbstractModuleExceptionHandler {

    public InventoryExceptionHandler() {
        registerErrors(HttpStatus.NOT_FOUND,
                "INV_001", "INV_101", "INV_201", "INV_301");
        registerErrors(HttpStatus.CONFLICT,
                "INV_102", "INV_105");
        registerErrors(HttpStatus.FORBIDDEN,
                "INV_002", "INV_204");
        registerErrors(HttpStatus.BAD_REQUEST,
                "INV_003", "INV_103", "INV_104", "INV_106", "INV_107",
                "INV_202", "INV_203", "INV_302", "INV_401", "INV_900");
        setDefaultStatus(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Override
    protected String moduleDomain() {
        return "Inventory";
    }

    @ExceptionHandler(InventoryException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleInventoryException(InventoryException ex) {
        log.warn("Inventory exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return handleException(ex);
    }
}
