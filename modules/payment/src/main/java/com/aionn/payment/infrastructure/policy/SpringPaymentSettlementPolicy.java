package com.aionn.payment.infrastructure.policy;

import com.aionn.payment.application.policy.PaymentSettlementPolicy;
import com.aionn.payment.infrastructure.config.properties.PaymentSettlementProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class SpringPaymentSettlementPolicy implements PaymentSettlementPolicy {

    private final PaymentSettlementProperties properties;

    @Override
    public BigDecimal defaultCommissionRate() {
        return properties.defaultCommissionRate();
    }
}
