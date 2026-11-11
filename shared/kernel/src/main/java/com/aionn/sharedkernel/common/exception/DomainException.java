package com.aionn.sharedkernel.common.exception;

import java.util.Objects;

public class DomainException extends RuntimeException {

    private final String domain;
    private final String errorCode;

    public DomainException(String domain, String errorCode, String message) {
        super(message);
        this.domain = Objects.requireNonNull(domain, "domain must not be null");
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    public DomainException(String domain, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.domain = Objects.requireNonNull(domain, "domain must not be null");
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    public String getDomain() {
        return domain;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
