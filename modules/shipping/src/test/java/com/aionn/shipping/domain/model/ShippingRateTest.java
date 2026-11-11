package com.aionn.shipping.domain.model;

import static com.aionn.sharedkernel.testing.fixture.TestClocks.FIXED;

import com.aionn.shipping.domain.event.ShipmentEvents;
import com.aionn.shipping.domain.exception.ShippingErrorCode;
import com.aionn.shipping.domain.exception.ShippingException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShippingRateTest {

    @Test
    void configureEmitsEvent() {
        ShippingRate r = ShippingRate.configure("r1", "HCM", new BigDecimal("30000"), "VND", "<=2kg", FIXED);

        assertThat(r.getRateId()).isEqualTo("r1");
        assertThat(r.getZoneCode()).isEqualTo("HCM");
        assertThat(r.peekEvents()).anyMatch(env -> env.payload() instanceof ShipmentEvents.ShippingRateConfigured);
    }

    @Test
    void configureRejectsNegativeFee() {
        assertThatThrownBy(() -> ShippingRate.configure("r1", "HCM", new BigDecimal("-1"), "VND", null, FIXED))
                .isInstanceOf(ShippingException.class)
                .extracting("errorCode")
                .isEqualTo(ShippingErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void configureRejectsBlankZoneCode() {
        assertThatThrownBy(() -> ShippingRate.configure("r1", " ", BigDecimal.ZERO, "VND", null, FIXED))
                .isInstanceOf(ShippingException.class);
    }

    @Test
    void updateFeeAndConditionEmitsEvent() {
        ShippingRate r = ShippingRate.configure("r1", "HCM", new BigDecimal("30000"), "VND", null, FIXED);
        r.pullEvents();

        r.update(new BigDecimal("40000"), "<=3kg", FIXED);

        assertThat(r.getBaseFee()).isEqualByComparingTo(new BigDecimal("40000"));
        assertThat(r.getCondition()).isEqualTo("<=3kg");
        assertThat(r.peekEvents()).hasSize(1);
    }

    @Test
    void updateRejectsNegativeFee() {
        ShippingRate r = ShippingRate.configure("r1", "HCM", new BigDecimal("30000"), "VND", null, FIXED);

        assertThatThrownBy(() -> r.update(new BigDecimal("-5"), null, FIXED))
                .isInstanceOf(ShippingException.class);
    }

    private static ShippingRate ladderRate(String baseFee, String stepFee, String maxFee, String freeThreshold) {
        return ShippingRate.configure("r1", "HCM", new BigDecimal(baseFee), "VND", "ladder",
                1000, 500, stepFee == null ? BigDecimal.ZERO : new BigDecimal(stepFee),
                maxFee == null ? null : new BigDecimal(maxFee),
                freeThreshold == null ? null : new BigDecimal(freeThreshold), FIXED);
    }

    @Test
    void feeForReturnsBaseFeeWithinFirstWeightTier() {
        ShippingRate r = ladderRate("30000", "5000", null, null);

        assertThat(r.feeFor(1000, new BigDecimal("100000"))).isEqualByComparingTo("30000");
        assertThat(r.feeFor(500, new BigDecimal("100000"))).isEqualByComparingTo("30000");
    }

    @Test
    void feeForAddsStepFeePerStartedStepAboveBaseWeight() {
        ShippingRate r = ladderRate("30000", "5000", null, null);

        // 1200g: 200g over the 1000g base -> one 500g step
        assertThat(r.feeFor(1200, null)).isEqualByComparingTo("35000");
        // 1600g: 600g over -> two steps (rounded up)
        assertThat(r.feeFor(1600, null)).isEqualByComparingTo("40000");
        // 2000g: exactly two steps
        assertThat(r.feeFor(2000, null)).isEqualByComparingTo("40000");
    }

    @Test
    void feeForCapsAtMaxFee() {
        ShippingRate r = ladderRate("30000", "5000", "45000", null);

        assertThat(r.feeFor(5000, null)).isEqualByComparingTo("45000");
    }

    @Test
    void feeForWaivesShippingAtFreeshipThreshold() {
        ShippingRate r = ladderRate("30000", "5000", null, "500000");

        assertThat(r.feeFor(3000, new BigDecimal("500000"))).isEqualByComparingTo("0");
        assertThat(r.feeFor(3000, new BigDecimal("499999"))).isEqualByComparingTo("50000");
        // A null subtotal (unknown) is not treated as free.
        assertThat(r.feeFor(3000, null)).isEqualByComparingTo("50000");
    }

    @Test
    void flatRateWithoutStepFeeIgnoresWeight() {
        ShippingRate r = ShippingRate.configure("r1", "HCM", new BigDecimal("25000"), "VND", "flat", FIXED);

        assertThat(r.feeFor(99999, null)).isEqualByComparingTo("25000");
    }

    @Test
    void configureRejectsNonPositiveWeightLadder() {
        assertThatThrownBy(() -> ShippingRate.configure("r1", "HCM", BigDecimal.TEN, "VND", null,
                0, 500, BigDecimal.ZERO, null, null, FIXED))
                .isInstanceOf(ShippingException.class);
        assertThatThrownBy(() -> ShippingRate.configure("r1", "HCM", BigDecimal.TEN, "VND", null,
                1000, 0, BigDecimal.ZERO, null, null, FIXED))
                .isInstanceOf(ShippingException.class);
    }
}
