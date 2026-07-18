package com.aionn.ordering.domain.exception;

import com.aionn.sharedkernel.common.exception.DomainException;

public class OrderingException extends DomainException {

    public OrderingException(OrderingErrorCode errorCode) {
        super("Ordering", errorCode.getCode(), errorCode.getDefaultMessage());
    }

    public OrderingException(OrderingErrorCode errorCode, String message) {
        super("Ordering", errorCode.getCode(), message);
    }
}

