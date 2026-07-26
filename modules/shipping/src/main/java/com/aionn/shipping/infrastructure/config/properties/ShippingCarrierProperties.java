package com.aionn.shipping.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "shipping.carrier")
public record ShippingCarrierProperties(
                @DefaultValue("assume-success") String provider,
                @DefaultValue("") String webhookSecret,
                @DefaultValue("3") int defaultExpectedDeliveryDays,
                @DefaultValue("30000") BigDecimal defaultQuoteAmount,
                @DefaultValue("VND") String defaultCurrency,
                @DefaultValue("https://labels.test/") String mockLabelBaseUrl,
                @DefaultValue Ghn ghn) {

        public record Ghn(
                        @DefaultValue("") String apiToken,
                        @DefaultValue("") String shopId,
                        @DefaultValue("https://online-gateway.ghn.vn") String baseUrl) {
        }
}
