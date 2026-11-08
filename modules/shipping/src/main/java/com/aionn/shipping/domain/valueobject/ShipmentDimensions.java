package com.aionn.shipping.domain.valueobject;

import com.aionn.shipping.domain.exception.ShippingErrorCode;
import com.aionn.shipping.domain.exception.ShippingException;
import java.math.BigDecimal;

public record ShipmentDimensions(
        int weightGram,
        BigDecimal lengthCm,
        BigDecimal widthCm,
        BigDecimal heightCm) {

    /**
     * Volumetric divisor used by domestic carriers (GHN/GHTK/Viettel Post) and
     * marketplaces: 1 kg per 6000 cm³. Accepts both 6000 and the 5000 standard
     * used by some international carriers, but 6000 is the domestic default.
     */
    public static final int DOMESTIC_VOLUMETRIC_DIVISOR = 6000;

    public ShipmentDimensions {
        if (weightGram < 0) {
            throw new ShippingException(ShippingErrorCode.INVALID_ARGUMENT, "weight must be >= 0");
        }
    }

    /** Weight to bill: the greater of actual weight and volumetric weight. */
    public int chargeableWeightGram() {
        return Math.max(weightGram, volumetricWeightGram());
    }

    public int volumetricWeightGram() {
        if (lengthCm == null || widthCm == null || heightCm == null
                || lengthCm.signum() <= 0 || widthCm.signum() <= 0 || heightCm.signum() <= 0) {
            return 0;
        }
        BigDecimal volumeCm3 = lengthCm.multiply(widthCm).multiply(heightCm);
        return volumeCm3
                .multiply(BigDecimal.valueOf(1000))
                .divide(BigDecimal.valueOf(DOMESTIC_VOLUMETRIC_DIVISOR), java.math.RoundingMode.CEILING)
                .intValue();
    }
}
