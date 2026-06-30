package com.aionn.sharedkernel.common.exception;

import java.util.Collections;
import java.util.List;

public class ValidationException extends DomainException {

    private final List<FieldError> fieldErrors;

    public ValidationException(String domain, String errorCode, String message) {
        super(domain, errorCode, message);
        this.fieldErrors = Collections.emptyList();
    }

    public ValidationException(String domain, List<FieldError> fieldErrors) {
        super(domain, "VALIDATION_FAILED",
                "Validation failed for %s: %d error(s)".formatted(domain, fieldErrors.size()));
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }

    public record FieldError(String field, String message) implements java.io.Serializable {
    }
}
