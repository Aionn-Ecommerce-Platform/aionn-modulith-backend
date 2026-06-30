package com.aionn.sharedkernel.common.exception;

public class ForbiddenException extends DomainException {

    public ForbiddenException(String action) {
        super("Auth", "FORBIDDEN",
                "You do not have permission to perform: " + action);
    }

    public ForbiddenException(String domain, String action) {
        super(domain, "FORBIDDEN",
                "Access denied for action '%s' on domain '%s'".formatted(action, domain));
    }
}
