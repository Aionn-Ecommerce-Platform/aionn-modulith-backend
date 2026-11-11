package com.aionn.ucp.application;

import com.aionn.ucp.application.policy.UcpProtocolPolicy;

import java.time.Duration;

/** Deterministic serving policy for application-service unit tests. */
public final class TestUcpProtocolPolicy implements UcpProtocolPolicy {

    private final String marketCurrency;

    public TestUcpProtocolPolicy() {
        this("USD");
    }

    public TestUcpProtocolPolicy(String marketCurrency) {
        this.marketCurrency = marketCurrency;
    }

    @Override
    public Duration cartExpiry() {
        return Duration.ofDays(7);
    }

    @Override
    public Duration checkoutTtl() {
        return Duration.ofHours(6);
    }

    @Override
    public String storefrontBaseUrl() {
        return "https://aionn.vn";
    }

    @Override
    public int catalogSearchDefaultLimit() {
        return 10;
    }

    @Override
    public int catalogSearchMaxLimit() {
        return 100;
    }

    @Override
    public String marketCurrency() {
        return marketCurrency;
    }
}
