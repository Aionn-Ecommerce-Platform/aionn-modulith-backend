package com.aionn.sharedkernel.common.exception;

public class ConflictException extends DomainException {

    public ConflictException(String domain, String errorCode, String message) {
        super(domain, errorCode, message);
    }
}
