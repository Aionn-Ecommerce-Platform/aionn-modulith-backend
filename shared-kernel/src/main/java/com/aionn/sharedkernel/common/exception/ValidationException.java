package com.aionn.sharedkernel.common.exception;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ValidationException extends DomainException {

    private final List<FieldError> fieldErrors;

    public ValidationException(String domain, String errorCode, String message) {
        super(domain, errorCode, message);
        this.fieldErrors = Collections.emptyList();
    }

    public ValidationException(String domain, List<FieldError> fieldErrors) {
        super(domain, "VALIDATION_FAILED",
                "Validation failed for %s: %d error(s)".formatted(domain, validateFieldErrors(fieldErrors).size()));
        this.fieldErrors = validateFieldErrors(fieldErrors);
    }

    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }

    public record FieldError(String field, String message) implements java.io.Serializable {
    }

    private static List<FieldError> validateFieldErrors(List<FieldError> fieldErrors) {
        Objects.requireNonNull(fieldErrors, "fieldErrors must not be null");
        return List.copyOf(fieldErrors.stream()
                .map(error -> Objects.requireNonNull(error, "fieldError must not be null"))
                .map(error -> new FieldError(
                        Objects.requireNonNull(error.field(), "fieldError.field must not be null"),
                        Objects.requireNonNull(error.message(), "fieldError.message must not be null")))
                .toList());
    }
}
