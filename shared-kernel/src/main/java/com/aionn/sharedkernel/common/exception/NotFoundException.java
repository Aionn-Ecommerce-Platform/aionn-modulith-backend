package com.aionn.sharedkernel.common.exception;

public class NotFoundException extends DomainException {

    public NotFoundException(String resourceType, String resourceId) {
        super(resourceType, "NOT_FOUND",
                "%s with id '%s' not found".formatted(resourceType, resourceId));
    }

    public NotFoundException(String resourceType, String resourceId, String customMessage) {
        super(resourceType, "NOT_FOUND", customMessage);
    }
}
