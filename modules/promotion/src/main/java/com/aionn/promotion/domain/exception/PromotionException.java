package com.aionn.promotion.domain.exception;

import com.aionn.sharedkernel.common.exception.DomainException;

public class PromotionException extends DomainException {

    public PromotionException(PromotionErrorCode code) {
        super("Promotion", code.getCode(), code.getDefaultMessage());
    }

    public PromotionException(PromotionErrorCode code, String message) {
        super("Promotion", code.getCode(), message);
    }
}

