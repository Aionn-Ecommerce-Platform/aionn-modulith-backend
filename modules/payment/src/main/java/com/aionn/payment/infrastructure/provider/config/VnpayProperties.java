package com.aionn.payment.infrastructure.provider.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "payment.provider.vnpay")
public record VnpayProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("") String tmnCode,
        @DefaultValue("") String hashSecret,
        @DefaultValue("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html") String payUrl,
        @DefaultValue("") String returnUrl,
        @DefaultValue("http://localhost:3000/payments/return") String frontendReturnUrl,
        @DefaultValue("https://sandbox.vnpayment.vn/merchant_webapi/api/transaction") String apiUrl,
        @DefaultValue("2.1.0") String version,
        @DefaultValue("pay") String command,
        @DefaultValue("VND") String currCode,
        @DefaultValue("vn") String locale) {
}
