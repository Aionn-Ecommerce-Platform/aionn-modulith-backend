package com.aionn.payment.infrastructure.config;

import com.aionn.payment.infrastructure.config.properties.PaymentAutoPayoutProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class AutoPayoutSafetyValidator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final PaymentAutoPayoutProperties properties;

    @PostConstruct
    void rejectUnsafeAutoPayout() {
        if (!properties.enabled()) {
            return;
        }
        if (properties.threshold() == null || properties.threshold().signum() <= 0) {
            throw new IllegalStateException("payment.auto-payout.threshold must be positive");
        }
        if (properties.currency() == null || properties.currency().isBlank()) {
            throw new IllegalStateException("payment.auto-payout.currency must not be blank");
        }
        BigDecimal reservePercent = properties.reservePercent() == null
                ? BigDecimal.ZERO : properties.reservePercent();
        BigDecimal reserveAmount = properties.reserveAmount() == null
                ? BigDecimal.ZERO : properties.reserveAmount();
        if (reservePercent.signum() < 0 || reservePercent.compareTo(HUNDRED) >= 0) {
            throw new IllegalStateException(
                    "payment.auto-payout.reserve-percent must be in [0, 100)");
        }
        if (reserveAmount.signum() < 0) {
            throw new IllegalStateException("payment.auto-payout.reserve-amount must not be negative");
        }
        if (reservePercent.signum() == 0 && reserveAmount.signum() == 0) {
            throw new IllegalStateException(
                    "payment.auto-payout requires a refundable-revenue reserve policy: set "
                            + "reserve-percent > 0 or reserve-amount > 0");
        }
    }
}
