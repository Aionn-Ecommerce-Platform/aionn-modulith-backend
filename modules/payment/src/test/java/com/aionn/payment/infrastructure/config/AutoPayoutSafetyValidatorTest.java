package com.aionn.payment.infrastructure.config;

import com.aionn.payment.infrastructure.config.properties.PaymentAutoPayoutProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AutoPayoutSafetyValidatorTest {

    @Test
    void acceptsDisabledAutoPayout() {
        AutoPayoutSafetyValidator validator = new AutoPayoutSafetyValidator(properties(false));

        assertThatCode(validator::rejectUnsafeAutoPayout).doesNotThrowAnyException();
    }

    @Test
    void rejectsAutoPayoutUntilRefundReservePolicyExists() {
        AutoPayoutSafetyValidator validator = new AutoPayoutSafetyValidator(properties(true));

        assertThatThrownBy(validator::rejectUnsafeAutoPayout)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("refundable-revenue reserve policy");
    }

    private static PaymentAutoPayoutProperties properties(boolean enabled) {
        return new PaymentAutoPayoutProperties(
                enabled, BigDecimal.valueOf(100_000), "VND", 50, "0 0 2 * * *");
    }
}
