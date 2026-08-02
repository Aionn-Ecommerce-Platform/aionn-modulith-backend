package com.aionn.payment.infrastructure.provider.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "payment.stripe.connect")
public record StripeConnectProperties(
        @DefaultValue("http://localhost:3000/merchant/settings/stripe/refresh") String refreshUrl,
        @DefaultValue("http://localhost:3000/merchant/settings/stripe/return") String returnUrl) {}
