package com.aionn.ucp.domain.exception;

import com.aionn.sharedkernel.common.exception.DomainException;

public class UcpProtocolException extends DomainException {

    private final int statusCode;
    private final String severity;
    private final String path;

    public UcpProtocolException(int statusCode, String errorCode, String message, String severity) {
        this(statusCode, errorCode, message, severity, null, null);
    }

    public UcpProtocolException(int statusCode, String errorCode, String message, String severity, String path) {
        this(statusCode, errorCode, message, severity, path, null);
    }

    public UcpProtocolException(int statusCode, String errorCode, String message, String severity, String path,
            Throwable cause) {
        super("ucp", errorCode, message, cause);
        this.statusCode = statusCode;
        this.severity = severity;
        this.path = path;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getSeverity() {
        return severity;
    }

    public String getPath() {
        return path;
    }
}
