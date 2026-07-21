package com.aionn.payment.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "payment.invoice")
public record PaymentInvoiceProperties(
        @DefaultValue("local") String provider,
        @DefaultValue("https://invoices.test/") String baseUrl) {
}
