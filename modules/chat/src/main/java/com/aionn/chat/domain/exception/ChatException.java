package com.aionn.chat.domain.exception;

import com.aionn.sharedkernel.common.exception.DomainException;

public class ChatException extends DomainException {

    public ChatException(ChatErrorCode code) {
        super("Chat", code.getCode(), code.getDefaultMessage());
    }

    public ChatException(ChatErrorCode code, String message) {
        super("Chat", code.getCode(), message);
    }
}

