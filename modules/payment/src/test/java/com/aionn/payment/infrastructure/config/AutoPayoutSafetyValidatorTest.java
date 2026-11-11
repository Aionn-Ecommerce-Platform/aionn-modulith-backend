package com.aionn.payment.infrastructure.config;

import com.aionn.payment.infrastructure.config.properties.PaymentAutoPayoutProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AutoPayoutSafetyValidatorTest {

    @Test
    void acceptsDisabledAutoPayout() {
        AutoPayoutSafetyValidator validator = new AutoPayoutSafetyValidator(properties(false, "10"));

        assertThatCode(validator::rejectUnsafeAutoPayout).doesNotThrowAnyException();
    }

    @Test
    void acceptsAutoPayoutWithPercentageReserve() {
        AutoPayoutSafetyValidator validator = new AutoPayoutSafetyValidator(properties(true, "10"));

        assertThatCode(validator::rejectUnsafeAutoPayout).doesNotThrowAnyException();
    }

    @Test
    void rejectsAutoPayoutWithoutReservePolicy() {
        AutoPayoutSafetyValidator validator = new AutoPayoutSafetyValidator(properties(true, "0"));

        assertThatThrownBy(validator::rejectUnsafeAutoPayout)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("refundable-revenue reserve policy");
    }

    @Test
    void rejectsInvalidReservePercent() {
        AutoPayoutSafetyValidator validator = new AutoPayoutSafetyValidator(properties(true, "100"));

        assertThatThrownBy(validator::rejectUnsafeAutoPayout)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("reserve-percent");
    }

    private static PaymentAutoPayoutProperties properties(boolean enabled, String reservePercent) {
        return new PaymentAutoPayoutProperties(
                enabled, BigDecimal.valueOf(100_000), new BigDecimal(reservePercent),
                BigDecimal.ZERO, "VND", 50, "0 0 2 * * *");
    }
}
