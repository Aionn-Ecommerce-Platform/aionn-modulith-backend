package com.aionn.payment.infrastructure.config;

import com.aionn.payment.infrastructure.config.properties.PaymentAutoPayoutProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AutoPayoutSafetyValidator {

    private final PaymentAutoPayoutProperties properties;

    @PostConstruct
    void rejectUnsafeAutoPayout() {
        if (properties.enabled()) {
            throw new IllegalStateException(
                    "payment.auto-payout.enabled cannot be true until a refundable-revenue reserve policy is configured");
        }
    }
}
