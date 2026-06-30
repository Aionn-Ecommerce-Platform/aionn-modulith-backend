package com.aionn.sharedkernel.common.exception;

public class UnauthorizedException extends DomainException {

    public UnauthorizedException() {
        super("Auth", "UNAUTHORIZED", "Authentication required");
    }

    public UnauthorizedException(String message) {
        super("Auth", "UNAUTHORIZED", message);
    }
}
