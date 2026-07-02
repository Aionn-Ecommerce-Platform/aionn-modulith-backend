package com.aionn.identity.domain.exception;

import com.aionn.sharedkernel.common.exception.DomainException;

public class IdentityException extends DomainException {

    public IdentityException(IdentityErrorCode errorCode) {
        super("Identity", errorCode.getCode(), errorCode.getDefaultMessage());
    }

    public IdentityException(IdentityErrorCode errorCode, String customMessage) {
        super("Identity", errorCode.getCode(), customMessage);
    }
}


