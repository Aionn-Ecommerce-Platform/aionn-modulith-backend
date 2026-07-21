package com.aionn.payment.infrastructure.provider.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ StripeProperties.class, VnpayProperties.class })
public class PaymentPropertiesConfig {
}
