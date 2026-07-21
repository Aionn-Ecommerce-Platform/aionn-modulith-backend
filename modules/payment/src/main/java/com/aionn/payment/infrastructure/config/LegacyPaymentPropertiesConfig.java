package com.aionn.payment.infrastructure.config;

import com.aionn.payment.infrastructure.config.properties.PaymentInvoiceProperties;
import com.aionn.payment.infrastructure.config.properties.PaymentStripeProperties;
import com.aionn.payment.infrastructure.config.properties.PaymentVnpayProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        PaymentInvoiceProperties.class,
        PaymentStripeProperties.class,
        PaymentVnpayProperties.class
})
public class LegacyPaymentPropertiesConfig {
}
