package com.aionn.notification.domain.exception;

import com.aionn.sharedkernel.common.exception.DomainException;

public class NotificationException extends DomainException {

    public NotificationException(NotificationErrorCode code) {
        super("Notification", code.getCode(), code.getDefaultMessage());
    }

    public NotificationException(NotificationErrorCode code, String message) {
        super("Notification", code.getCode(), message);
    }
}

