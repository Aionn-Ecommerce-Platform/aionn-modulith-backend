package com.aionn.payment.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "payment.provider.vnpay")
public record PaymentVnpayProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("") String tmnCode,
        @DefaultValue("") String hashSecret,
        @DefaultValue("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html") String payUrl,
        @DefaultValue("") String returnUrl) {
}
