package com.aionn.payment.infrastructure.provider.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.provider.vnpay")
public record VnpayProperties(
                String tmnCode,
                String hashSecret,
                String payUrl,
                String returnUrl,
                String frontendReturnUrl,
                String apiUrl,
                String version,
                String command,
                String currCode,
                String locale) {
}
