package com.aionn.payment.infrastructure.config.properties;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Validated
@ConfigurationProperties(prefix = "payment.settlement")
public record PaymentSettlementProperties(
        @DecimalMin("0.0000") @DecimalMax("1.0000") @DefaultValue("0.0500") BigDecimal defaultCommissionRate) {
}
