package com.aionn.shipping.domain.exception;

import com.aionn.sharedkernel.common.exception.DomainException;

public class ShippingException extends DomainException {

    public ShippingException(ShippingErrorCode code) {
        super("Shipping", code.getCode(), code.getDefaultMessage());
    }

    public ShippingException(ShippingErrorCode code, String message) {
        super("Shipping", code.getCode(), message);
    }
}

