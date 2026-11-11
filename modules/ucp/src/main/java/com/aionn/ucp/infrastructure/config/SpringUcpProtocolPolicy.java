package com.aionn.ucp.infrastructure.config;

import com.aionn.ucp.application.UcpProtocolConstants;
import com.aionn.ucp.application.policy.UcpProtocolPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class SpringUcpProtocolPolicy implements UcpProtocolPolicy {

    private final UcpServingProperties servingProperties;
    private final UcpProperties ucpProperties;

    @Override
    public Duration cartExpiry() {
        return Duration.ofDays(servingProperties.cartExpiryDays());
    }

    @Override
    public Duration checkoutTtl() {
        return Duration.ofHours(servingProperties.checkoutTtlHours());
    }

    @Override
    public String storefrontBaseUrl() {
        String url = servingProperties.storefrontBaseUrl();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    @Override
    public int catalogSearchDefaultLimit() {
        return servingProperties.catalogSearchDefaultLimit();
    }

    @Override
    public int catalogSearchMaxLimit() {
        return Math.max(catalogSearchDefaultLimit(), servingProperties.catalogSearchMaxLimit());
    }

    @Override
    public String marketCurrency() {
        String currency = ucpProperties.marketCurrency();
        return currency == null || currency.isBlank()
                ? UcpProtocolConstants.DEFAULT_MARKET_CURRENCY
                : currency.trim().toUpperCase();
    }
}
