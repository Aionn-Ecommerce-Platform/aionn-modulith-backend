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
        .hasMessageContaining("weight must be > 0")
        .extracting(ex -> ((ShippingException) ex).getErrorCode())
        .isEqualTo(ShippingErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void volumetricWeightUsesDomesticDivisorOf6000() {
        // 30 × 20 × 40 = 24,000 cm³ -> 4,000 g at 6000 cm³/kg
        ShipmentDimensions dims = new ShipmentDimensions(500, new BigDecimal("30"),
                new BigDecimal("20"), new BigDecimal("40"));

        assertThat(dims.volumetricWeightGram()).isEqualTo(4000);
    }

    @Test
    void chargeableWeightIsTheGreaterOfActualAndVolumetric() {
        ShipmentDimensions bulky = new ShipmentDimensions(500, new BigDecimal("30"),
                new BigDecimal("20"), new BigDecimal("40"));
        ShipmentDimensions heavy = new ShipmentDimensions(10000, new BigDecimal("10"),
                new BigDecimal("10"), new BigDecimal("10"));

        assertThat(bulky.chargeableWeightGram()).isEqualTo(4000);
        assertThat(heavy.chargeableWeightGram()).isEqualTo(10000);
    }

    @Test
    void volumetricWeightIsZeroWhenBoxIsIncomplete() {
        assertThat(new ShipmentDimensions(500, null, null, null).volumetricWeightGram()).isZero();
        assertThat(new ShipmentDimensions(500, BigDecimal.TEN, null, BigDecimal.TEN)
                .volumetricWeightGram()).isZero();
    }
}
