package com.aionn.ordering.application.policy;

import com.aionn.ordering.infrastructure.config.properties.OrderingDefaultsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringOrderDefaultsPolicy implements OrderDefaultsPolicy {

    private final OrderingDefaultsProperties properties;

    @Override
    public String defaultCurrency() {
        String currency = properties.currency();
        return (currency == null || currency.isBlank()) ? "VND" : currency.trim().toUpperCase();
    }
}
