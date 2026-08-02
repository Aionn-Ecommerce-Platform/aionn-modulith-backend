package com.aionn.payment.infrastructure.provider.config;

import com.aionn.payment.infrastructure.config.properties.PaymentInvoiceProperties;
import com.aionn.payment.infrastructure.config.properties.PaymentAutoPayoutProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        StripeProperties.class,
        VnpayProperties.class,
        StripeConnectProperties.class,
        PaymentAutoPayoutProperties.class,
        PaymentInvoiceProperties.class
})
public class PaymentPropertiesConfig {
}
