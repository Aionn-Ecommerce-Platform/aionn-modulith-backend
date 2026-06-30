package com.aionn.sharedkernel.common.exception;

public class DomainException extends RuntimeException {

    private final String domain;
    private final String errorCode;

    public DomainException(String domain, String errorCode, String message) {
        super(message);
        this.domain = domain;
        this.errorCode = errorCode;
    }

    public DomainException(String domain, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.domain = domain;
        this.errorCode = errorCode;
    }

    public String getDomain() {
        return domain;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
