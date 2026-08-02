package com.aionn.payment.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "payment.auto-payout")
public record PaymentAutoPayoutProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("100000") BigDecimal threshold,
        @DefaultValue("VND") String currency,
        @DefaultValue("50") int batchSize,
        @DefaultValue("0 0 2 * * *") String cron) {
}
