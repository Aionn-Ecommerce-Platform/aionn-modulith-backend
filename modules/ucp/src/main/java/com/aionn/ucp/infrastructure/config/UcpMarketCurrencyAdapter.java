package com.aionn.ucp.infrastructure.config;

import com.aionn.ucp.application.port.out.UcpMarketCurrencyPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Exposes the configured market currency to UCP application services. */
@Component
@RequiredArgsConstructor
public class UcpMarketCurrencyAdapter implements UcpMarketCurrencyPort {

    private final UcpProperties properties;

    @Override
    public String marketCurrency() {
        return properties.marketCurrency();
    }
}
