package com.aionn.chat.adapter.rest.exception;

import com.aionn.chat.domain.exception.ChatException;
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
@RestControllerAdvice(basePackages = "com.aionn.chat.adapter.rest.controller")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ChatExceptionHandler extends AbstractModuleExceptionHandler {

    public ChatExceptionHandler() {
        registerErrors(HttpStatus.NOT_FOUND,
                "CHT_001", "CHT_101", "CHT_202", "CHT_301");
        registerErrors(HttpStatus.FORBIDDEN, "CHT_002", "CHT_102", "CHT_302");
        registerErrors(HttpStatus.CONFLICT, "CHT_201");
        registerErrors(HttpStatus.BAD_REQUEST,
                "CHT_003", "CHT_103", "CHT_104", "CHT_105", "CHT_106",
                "CHT_203", "CHT_900");
        setDefaultStatus(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Override
    protected String moduleDomain() {
        return "Chat";
    }

    @ExceptionHandler(ChatException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleChatException(ChatException ex) {
        log.warn("Chat exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return handleException(ex);
    }
}
