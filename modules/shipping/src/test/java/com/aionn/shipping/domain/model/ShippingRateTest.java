package com.aionn.shipping.domain.model;

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
        ShippingRate r = ShippingRate.configure("r1", "HCM", new BigDecimal("30000"), "VND", "<=2kg");

        assertThat(r.getRateId()).isEqualTo("r1");
        assertThat(r.getZoneCode()).isEqualTo("HCM");
        assertThat(r.peekEvents()).anyMatch(env -> env.payload() instanceof ShipmentEvents.ShippingRateConfigured);
    }

    @Test
    void configureRejectsNegativeFee() {
        assertThatThrownBy(() -> ShippingRate.configure("r1", "HCM", new BigDecimal("-1"), "VND", null))
                .isInstanceOf(ShippingException.class)
                .extracting("errorCode")
                .isEqualTo(ShippingErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void configureRejectsBlankZoneCode() {
        assertThatThrownBy(() -> ShippingRate.configure("r1", " ", BigDecimal.ZERO, "VND", null))
                .isInstanceOf(ShippingException.class);
    }

    @Test
    void updateFeeAndConditionEmitsEvent() {
        ShippingRate r = ShippingRate.configure("r1", "HCM", new BigDecimal("30000"), "VND", null);
        r.pullEvents();

        r.update(new BigDecimal("40000"), "<=3kg");

        assertThat(r.getBaseFee()).isEqualByComparingTo(new BigDecimal("40000"));
        assertThat(r.getCondition()).isEqualTo("<=3kg");
        assertThat(r.peekEvents()).hasSize(1);
    }

    @Test
    void updateRejectsNegativeFee() {
        ShippingRate r = ShippingRate.configure("r1", "HCM", new BigDecimal("30000"), "VND", null);

        assertThatThrownBy(() -> r.update(new BigDecimal("-5"), null))
                .isInstanceOf(ShippingException.class);
    }
}
