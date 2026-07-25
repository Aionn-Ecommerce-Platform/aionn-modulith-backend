package com.aionn.shipping.domain.valueobject;

import com.aionn.shipping.domain.exception.ShippingErrorCode;
import com.aionn.shipping.domain.exception.ShippingException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShipmentDimensionsTest {

    @Test
    void shouldCreateShipmentDimensionsSuccessfully() {
        ShipmentDimensions dimensions = new ShipmentDimensions(
                500,
                new BigDecimal("10.5"),
                new BigDecimal("20.0"),
                new BigDecimal("15.2")
        );

        assertThat(dimensions.weightGram()).isEqualTo(500);
        assertThat(dimensions.lengthCm()).isEqualByComparingTo("10.5");
        assertThat(dimensions.widthCm()).isEqualByComparingTo("20.0");
        assertThat(dimensions.heightCm()).isEqualByComparingTo("15.2");
    }

    @Test
    void shouldThrowExceptionWhenWeightIsNegative() {
        assertThatThrownBy(() -> new ShipmentDimensions(
                -1,
                BigDecimal.TEN,
                BigDecimal.TEN,
                BigDecimal.TEN
        ))
        .isInstanceOf(ShippingException.class)
        .hasMessageContaining("weight must be >= 0")
        .extracting(ex -> ((ShippingException) ex).getErrorCode())
        .isEqualTo(ShippingErrorCode.INVALID_ARGUMENT.getCode());
    }
}
