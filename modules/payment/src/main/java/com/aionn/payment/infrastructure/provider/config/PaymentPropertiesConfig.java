package com.aionn.payment.infrastructure.provider.config;

import com.aionn.payment.infrastructure.config.properties.PaymentInvoiceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        StripeProperties.class,
        VnpayProperties.class,
        StripeConnectProperties.class,
        PaymentInvoiceProperties.class
})
public class PaymentPropertiesConfig {
}
