package com.aionn.shipping.domain.valueobject;

import com.aionn.shipping.domain.exception.ShippingErrorCode;
import com.aionn.shipping.domain.exception.ShippingException;
import java.math.BigDecimal;

public record ShipmentDimensions(
        int weightGram,
        BigDecimal lengthCm,
        BigDecimal widthCm,
        BigDecimal heightCm) {

    public ShipmentDimensions {
        if (weightGram < 0) {
            throw new ShippingException(ShippingErrorCode.INVALID_ARGUMENT, "weight must be >= 0");
        }
    }
}
