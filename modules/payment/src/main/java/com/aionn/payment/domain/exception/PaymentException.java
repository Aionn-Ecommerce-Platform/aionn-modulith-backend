package com.aionn.payment.domain.exception;

import com.aionn.sharedkernel.common.exception.DomainException;

public class PaymentException extends DomainException {

    public PaymentException(PaymentErrorCode code) {
        super("Payment", code.getCode(), code.getDefaultMessage());
    }

    public PaymentException(PaymentErrorCode code, String message) {
        super("Payment", code.getCode(), message);
    }
}

