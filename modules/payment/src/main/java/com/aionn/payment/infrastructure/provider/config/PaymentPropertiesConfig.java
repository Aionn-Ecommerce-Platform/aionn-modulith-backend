package com.aionn.payment.infrastructure.provider.config;

import com.aionn.payment.infrastructure.config.properties.PaymentAnalyticsProperties;
import com.aionn.payment.infrastructure.config.properties.PaymentDefaultsProperties;
import com.aionn.payment.infrastructure.config.properties.PaymentInvoiceProperties;
import com.aionn.payment.infrastructure.config.properties.PaymentAutoPayoutProperties;
import com.aionn.payment.infrastructure.config.properties.PaymentSettlementProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        StripeProperties.class,
        VnpayProperties.class,
        StripeConnectProperties.class,
        PaymentAutoPayoutProperties.class,
        PaymentInvoiceProperties.class,
        PaymentAnalyticsProperties.class,
        PaymentSettlementProperties.class,
        PaymentDefaultsProperties.class
})
public class PaymentPropertiesConfig {
}
