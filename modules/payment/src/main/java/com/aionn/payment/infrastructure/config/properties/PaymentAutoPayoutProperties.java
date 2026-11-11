package com.aionn.payment.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.math.BigDecimal;

/**
 * Auto-payout keeps a refund reserve on the merchant balance so a later refund
 * cannot push the balance negative. The reserve is the greater of an absolute
 * amount and a percentage of the available balance.
 */
@ConfigurationProperties(prefix = "payment.auto-payout")
public record PaymentAutoPayoutProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("100000") BigDecimal threshold,
        @DefaultValue("10") BigDecimal reservePercent,
        @DefaultValue("0") BigDecimal reserveAmount,
        @DefaultValue("VND") String currency,
        @DefaultValue("50") int batchSize,
        @DefaultValue("0 0 2 * * *") String cron) {
}
